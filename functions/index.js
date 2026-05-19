const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("crypto");

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const secretoCodigoRecuperacion = defineSecret(
  "CLANK_SECRETO_CODIGO_RECUPERACION"
);

async function borrarConsultaEnLotes(consulta, tamanyoLote = 400) {
  const db = admin.firestore();
  let totalBorrados = 0;

  while (true) {
    const instantanea = await consulta.limit(tamanyoLote).get();

    if (instantanea.empty) {
      break;
    }

    const lote = db.batch();

    instantanea.docs.forEach(documento => {
      lote.delete(documento.ref);
    });

    await lote.commit();
    totalBorrados += instantanea.size;

    if (instantanea.size < tamanyoLote) {
      break;
    }
  }

  return totalBorrados;
}

function agregarUrlSiExiste(conjunto, valor) {
  if (typeof valor === "string" && valor.trim().length > 0) {
    conjunto.add(valor.trim());
  }
}

function extraerArchivoStorageDesdeUrl(urlDescarga) {
  if (typeof urlDescarga !== "string" || urlDescarga.trim().length === 0) {
    return null;
  }

  try {
    const url = new URL(urlDescarga);
    const partesRuta = url.pathname.split("/").filter(Boolean);

    const indiceBucket = partesRuta.indexOf("b");
    const indiceObjeto = partesRuta.indexOf("o");

    if (
      indiceBucket === -1 ||
      indiceObjeto === -1 ||
      indiceBucket + 1 >= partesRuta.length ||
      indiceObjeto + 1 >= partesRuta.length
    ) {
      return null;
    }

    const bucket = decodeURIComponent(partesRuta[indiceBucket + 1]);
    const rutaArchivo = decodeURIComponent(
      partesRuta.slice(indiceObjeto + 1).join("/")
    );

    if (!bucket || !rutaArchivo) {
      return null;
    }

    return {
      bucket,
      rutaArchivo,
    };
  } catch (error) {
    return null;
  }
}

async function borrarArchivoStorageDesdeUrl(urlDescarga) {
  const archivo = extraerArchivoStorageDesdeUrl(urlDescarga);

  if (!archivo) {
    return {
      borrado: false,
      omitido: true,
    };
  }

  try {
    await admin
      .storage()
      .bucket(archivo.bucket)
      .file(archivo.rutaArchivo)
      .delete();

    return {
      borrado: true,
      omitido: false,
    };
  } catch (error) {
    if (error.code === 404 || error.code === "404") {
      return {
        borrado: false,
        omitido: true,
      };
    }

    console.error("No se pudo borrar archivo de Storage:", {
      urlDescarga,
      error,
    });

    return {
      borrado: false,
      omitido: false,
    };
  }
}

async function decrementarNumLikesSinNegativos(clankId, cantidad) {
  if (!clankId || cantidad <= 0) {
    return;
  }

  const db = admin.firestore();
  const referenciaClank = db.collection("clanks").doc(clankId);

  await db.runTransaction(async transaccion => {
    const documentoClank = await transaccion.get(referenciaClank);

    if (!documentoClank.exists) {
      return;
    }

    const datosClank = documentoClank.data();
    const numLikesActual = Number(datosClank.numLikes || 0);
    const nuevoNumLikes = Math.max(0, numLikesActual - cantidad);

    transaccion.update(referenciaClank, {
      numLikes: nuevoNumLikes,
    });
  });
}

/** Solicita un código de recuperación */
exports.solicitarCodigoRecuperacion = onCall(
  {
    secrets: [secretoCodigoRecuperacion],
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    const correo = typeof request.data?.correo === "string"
      ? request.data.correo.trim().toLowerCase()
      : "";

    const patronCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!correo || !patronCorreo.test(correo)) {
      throw new HttpsError(
        "invalid-argument",
        "El correo electrónico no es válido."
      );
    }

    let usuario;

    try {
      usuario = await admin.auth().getUserByEmail(correo);
    } catch (error) {
      if (error.code === "auth/user-not-found") {
        return {
          correcto: false,
          motivo: "CORREO_NO_REGISTRADO",
        };
      }

      console.error("Error al buscar usuario por correo:", error);

      throw new HttpsError(
        "internal",
        "No se ha podido procesar la solicitud."
      );
    }

    const codigo = crypto.randomInt(100000, 1000000).toString();

    const codigoHash = crypto
      .createHmac("sha256", secretoCodigoRecuperacion.value())
      .update(`${correo}:${codigo}`)
      .digest("hex");

    const idRecuperacion = crypto
      .createHash("sha256")
      .update(correo)
      .digest("hex");

    const fechaCaducidad = admin.firestore.Timestamp.fromMillis(
      Date.now() + 10 * 60 * 1000
    );

    const db = admin.firestore();

    await db
      .collection("recuperaciones_contrasenya")
      .doc(idRecuperacion)
      .set({
        uid: usuario.uid,
        correo: correo,
        codigoHash: codigoHash,
        intentosFallidos: 0,
        usado: false,
        codigoVerificado: false,
        cambioEnCurso: false,
        creadoEn: admin.firestore.FieldValue.serverTimestamp(),
        caducaEn: fechaCaducidad,
      });

    await db.collection("mail").add({
      to: [correo],
      message: {
        subject: "Código de recuperación de contraseña - Clank",
        text:
          `Tu código de recuperación es ${codigo}. ` +
          `Caduca en 10 minutos. ` +
          `Si no has solicitado este cambio, ignora este correo.`,
        html:
          `<p>Tu código de recuperación es <strong>${codigo}</strong>.</p>` +
          `<p>Caduca en 10 minutos.</p>` +
          `<p>Si no has solicitado este cambio, ignora este correo.</p>`,
      },
    });

    return {
      correcto: true,
    };
  }
);

/** Verifica el código de recuperación */
exports.verificarCodigoRecuperacion = onCall(
  {
    secrets: [secretoCodigoRecuperacion],
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    const correo = typeof request.data?.correo === "string"
      ? request.data.correo.trim().toLowerCase()
      : "";

    const codigo = typeof request.data?.codigo === "string"
      ? request.data.codigo.trim()
      : "";

    const patronCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const patronCodigo = /^\d{6}$/;

    if (!correo || !patronCorreo.test(correo)) {
      throw new HttpsError(
        "invalid-argument",
        "El correo electrónico no es válido."
      );
    }

    if (!codigo || !patronCodigo.test(codigo)) {
      throw new HttpsError(
        "invalid-argument",
        "El código debe tener 6 dígitos."
      );
    }

    const db = admin.firestore();

    const idRecuperacion = crypto
      .createHash("sha256")
      .update(correo)
      .digest("hex");

    const referenciaRecuperacion = db
      .collection("recuperaciones_contrasenya")
      .doc(idRecuperacion);

    const tokenRecuperacion = crypto.randomBytes(32).toString("hex");

    const tokenHash = crypto
      .createHmac("sha256", secretoCodigoRecuperacion.value())
      .update(`${correo}:${tokenRecuperacion}`)
      .digest("hex");

    const tokenCaducaEn = admin.firestore.Timestamp.fromMillis(
      Date.now() + 10 * 60 * 1000
    );

    const resultado = await db.runTransaction(async transaccion => {
      const documento = await transaccion.get(referenciaRecuperacion);

      if (!documento.exists) {
        return {
          correcto: false,
          motivo: "CODIGO_INVALIDO",
        };
      }

      const datos = documento.data();

      if (datos.usado === true) {
        return {
          correcto: false,
          motivo: "CODIGO_INVALIDO",
        };
      }

      const caducaEn = datos.caducaEn;

      if (!caducaEn || typeof caducaEn.toMillis !== "function") {
        return {
          correcto: false,
          motivo: "CODIGO_CADUCADO",
        };
      }

      if (caducaEn.toMillis() <= Date.now()) {
        return {
          correcto: false,
          motivo: "CODIGO_CADUCADO",
        };
      }

      const intentosFallidos = Number(datos.intentosFallidos || 0);

      if (intentosFallidos >= 5) {
        return {
          correcto: false,
          motivo: "DEMASIADOS_INTENTOS",
        };
      }

      const codigoHashCalculado = crypto
        .createHmac("sha256", secretoCodigoRecuperacion.value())
        .update(`${correo}:${codigo}`)
        .digest("hex");

      const codigoHashGuardado = datos.codigoHash;

      const hashValido =
        typeof codigoHashGuardado === "string" &&
        codigoHashGuardado.length === codigoHashCalculado.length;

      const codigoCoincide =
        hashValido &&
        crypto.timingSafeEqual(
          Buffer.from(codigoHashGuardado, "hex"),
          Buffer.from(codigoHashCalculado, "hex")
        );

      if (!codigoCoincide) {
        transaccion.update(referenciaRecuperacion, {
          intentosFallidos: admin.firestore.FieldValue.increment(1),
          ultimoIntentoEn: admin.firestore.FieldValue.serverTimestamp(),
        });

        return {
          correcto: false,
          motivo: "CODIGO_INVALIDO",
        };
      }

      transaccion.update(referenciaRecuperacion, {
        codigoVerificado: true,
        codigoVerificadoEn: admin.firestore.FieldValue.serverTimestamp(),
        intentosFallidos: 0,
        tokenHash: tokenHash,
        tokenCaducaEn: tokenCaducaEn,
      });

      return {
        correcto: true,
        motivo: "CODIGO_VALIDO",
        tokenRecuperacion: tokenRecuperacion,
      };
    });

    return resultado;
  }
);

/** Actualiza la contraseña tras verificar token */
exports.actualizarContrasenyaRecuperacion = onCall(
  {
    secrets: [secretoCodigoRecuperacion],
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    const correo = typeof request.data?.correo === "string"
      ? request.data.correo.trim().toLowerCase()
      : "";

    const tokenRecuperacion =
      typeof request.data?.tokenRecuperacion === "string"
        ? request.data.tokenRecuperacion.trim()
        : "";

    const nuevaContrasenya =
      typeof request.data?.nuevaContrasenya === "string"
        ? request.data.nuevaContrasenya
        : "";

    const patronCorreo = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const patronToken = /^[a-f0-9]{64}$/i;

    if (!correo || !patronCorreo.test(correo)) {
      throw new HttpsError(
        "invalid-argument",
        "El correo electrónico no es válido."
      );
    }

    if (!tokenRecuperacion || !patronToken.test(tokenRecuperacion)) {
      throw new HttpsError(
        "invalid-argument",
        "El token de recuperación no es válido."
      );
    }

    if (!nuevaContrasenya || nuevaContrasenya.length < 6) {
      return {
        correcto: false,
        motivo: "CONTRASENYA_DEBIL",
      };
    }

    const db = admin.firestore();

    const idRecuperacion = crypto
      .createHash("sha256")
      .update(correo)
      .digest("hex");

    const referenciaRecuperacion = db
      .collection("recuperaciones_contrasenya")
      .doc(idRecuperacion);

    const tokenHashCalculado = crypto
      .createHmac("sha256", secretoCodigoRecuperacion.value())
      .update(`${correo}:${tokenRecuperacion}`)
      .digest("hex");

    const resultadoValidacion = await db.runTransaction(
      async transaccion => {
        const documento = await transaccion.get(referenciaRecuperacion);

        if (!documento.exists) {
          return {
            correcto: false,
            motivo: "TOKEN_INVALIDO",
          };
        }

        const datos = documento.data();

        if (
          datos.usado === true ||
          datos.codigoVerificado !== true ||
          datos.cambioEnCurso === true
        ) {
          return {
            correcto: false,
            motivo: "TOKEN_INVALIDO",
          };
        }

        const tokenCaducaEn = datos.tokenCaducaEn;

        if (
          !tokenCaducaEn ||
          typeof tokenCaducaEn.toMillis !== "function" ||
          tokenCaducaEn.toMillis() <= Date.now()
        ) {
          return {
            correcto: false,
            motivo: "TOKEN_CADUCADO",
          };
        }

        const tokenHashGuardado = datos.tokenHash;

        const hashValido =
          typeof tokenHashGuardado === "string" &&
          tokenHashGuardado.length === tokenHashCalculado.length;

        const tokenCoincide =
          hashValido &&
          crypto.timingSafeEqual(
            Buffer.from(tokenHashGuardado, "hex"),
            Buffer.from(tokenHashCalculado, "hex")
          );

        if (!tokenCoincide) {
          return {
            correcto: false,
            motivo: "TOKEN_INVALIDO",
          };
        }

        if (
          typeof datos.uid !== "string" ||
          datos.uid.trim().length === 0
        ) {
          return {
            correcto: false,
            motivo: "ERROR_GENERAL",
          };
        }

        transaccion.update(referenciaRecuperacion, {
          cambioEnCurso: true,
          cambioEnCursoEn: admin.firestore.FieldValue.serverTimestamp(),
        });

        return {
          correcto: true,
          uid: datos.uid,
        };
      }
    );

    if (!resultadoValidacion.correcto) {
      return resultadoValidacion;
    }

    try {
      await admin.auth().updateUser(resultadoValidacion.uid, {
        password: nuevaContrasenya,
      });

      await referenciaRecuperacion.update({
        usado: true,
        usadoEn: admin.firestore.FieldValue.serverTimestamp(),
        contrasenyaActualizadaEn:
          admin.firestore.FieldValue.serverTimestamp(),
        cambioEnCurso: false,
        codigoVerificado: false,
        tokenHash: admin.firestore.FieldValue.delete(),
        tokenCaducaEn: admin.firestore.FieldValue.delete(),
      });

      return {
        correcto: true,
      };
    } catch (error) {
      console.error("Error actualizando contraseña:", error);

      await referenciaRecuperacion.update({
        cambioEnCurso: false,
      });

      throw new HttpsError(
        "internal",
        "No se ha podido actualizar la contraseña."
      );
    }
  }
);

/** Elimina de forma completa la cuenta del usuario autenticado */
exports.eliminarCuentaCompleta = onCall(
  {
    timeoutSeconds: 540,
    memory: "512MiB",
  },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError(
        "unauthenticated",
        "Debes iniciar sesión para eliminar la cuenta."
      );
    }

    const uid = request.auth.uid;
    const db = admin.firestore();

    const resumen = {
      clanks: 0,
      comentarios: 0,
      likes: 0,
      herramientas: 0,
      instrucciones: 0,
      archivosStorageBorrados: 0,
      archivosStorageFallidos: 0,
      usuarioFirestoreBorrado: false,
      usuarioAuthBorrado: false,
    };

    const urlsStorage = new Set();

    const referenciaUsuario = db.collection("usuarios").doc(uid);
    const documentoUsuario = await referenciaUsuario.get();

    if (documentoUsuario.exists) {
      const datosUsuario = documentoUsuario.data();
      agregarUrlSiExiste(urlsStorage, datosUsuario.fotoPerfil);
    }

    const clanksUsuario = await db
      .collection("clanks")
      .where("usuarioId", "==", uid)
      .get();

    const idsClanksPropios = new Set();

    for (const documentoClank of clanksUsuario.docs) {
      const clankId = documentoClank.id;
      const datosClank = documentoClank.data();

      idsClanksPropios.add(clankId);
      agregarUrlSiExiste(urlsStorage, datosClank.portada);

      const instrucciones = await documentoClank.ref
        .collection("instrucciones")
        .get();

      instrucciones.docs.forEach(documentoInstruccion => {
        const datosInstruccion = documentoInstruccion.data();
        agregarUrlSiExiste(urlsStorage, datosInstruccion.imagen);
      });

      resumen.instrucciones += await borrarConsultaEnLotes(
        documentoClank.ref.collection("instrucciones")
      );

      resumen.herramientas += await borrarConsultaEnLotes(
        documentoClank.ref.collection("herramientas")
      );

      resumen.comentarios += await borrarConsultaEnLotes(
        db.collection("comentarios").where("clankId", "==", clankId)
      );

      resumen.likes += await borrarConsultaEnLotes(
        db.collection("likes").doc(clankId).collection("usuarios")
      );

      await db.collection("likes").doc(clankId).delete();
      await documentoClank.ref.delete();

      resumen.clanks += 1;
    }

    resumen.comentarios += await borrarConsultaEnLotes(
      db.collection("comentarios").where("usuarioId", "==", uid)
    );

    const clanksRestantes = await db.collection("clanks").get();

    for (const documentoClank of clanksRestantes.docs) {
      const clankId = documentoClank.id;

      if (idsClanksPropios.has(clankId)) {
        continue;
      }

      const subcoleccionUsuariosLike = db
        .collection("likes")
        .doc(clankId)
        .collection("usuarios");

      const referenciasABorrar = new Map();

      const likePorUid = await subcoleccionUsuariosLike.doc(uid).get();

      if (likePorUid.exists) {
        referenciasABorrar.set(
          likePorUid.ref.path,
          likePorUid.ref
        );
      }

      const likesPorCampoUsuario = await subcoleccionUsuariosLike
        .where("usuarioId", "==", uid)
        .get();

      likesPorCampoUsuario.docs.forEach(documentoLike => {
        referenciasABorrar.set(
          documentoLike.ref.path,
          documentoLike.ref
        );
      });

      const cantidadLikesUsuario = referenciasABorrar.size;

      for (const referenciaLike of referenciasABorrar.values()) {
        await referenciaLike.delete();
      }

      if (cantidadLikesUsuario > 0) {
        resumen.likes += cantidadLikesUsuario;
        await decrementarNumLikesSinNegativos(
          clankId,
          cantidadLikesUsuario
        );
      }
    }

    if (documentoUsuario.exists) {
      await referenciaUsuario.delete();
      resumen.usuarioFirestoreBorrado = true;
    }

    for (const urlStorage of urlsStorage) {
      const resultadoBorradoStorage =
        await borrarArchivoStorageDesdeUrl(urlStorage);

      if (resultadoBorradoStorage.borrado) {
        resumen.archivosStorageBorrados += 1;
      } else if (!resultadoBorradoStorage.omitido) {
        resumen.archivosStorageFallidos += 1;
      }
    }

    try {
      await admin.auth().deleteUser(uid);
      resumen.usuarioAuthBorrado = true;
    } catch (error) {
      if (error.code !== "auth/user-not-found") {
        console.error("No se pudo borrar usuario de Firebase Auth:", error);

        throw new HttpsError(
          "internal",
          "No se ha podido eliminar la cuenta de autenticación."
        );
      }
    }

    return {
      correcto: true,
      resumen,
    };
  }
);