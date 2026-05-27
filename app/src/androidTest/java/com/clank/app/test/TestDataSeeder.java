package com.clank.app.test;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestDataSeeder {

    public static final String TEST_UID = "test-uid-fase4";
    public static final String TEST_EMAIL = "test-fase4@clank.test";
    public static final String TEST_PASSWORD = "Password123!";
    public static final String TEST_NOMBRE = "Usuario Test Fase4";
    public static final String TEST_USUARIO_CLANK = "usuario_test_fase4";

    public static final String TEST_CLANK_ID = "test-clank-fase4";
    public static final String TEST_CLANK_SIN_HERR_ID = "test-clank-sin-herr-fase4";
    public static final String TEST_CLANK_TITULO = "Clank de prueba Fase4";
    public static final int TEST_CLANK_NUM_LIKES_INICIAL = 0;

    public static final String TEST_BOCETO_ID = "test-boceto-fase4";
    public static final String TEST_BOCETO_TITULO = "Boceto de prueba Fase4";

    public static final String TEST_CATEGORIA_ID = "test-cat";
    public static final String TEST_CATEGORIA_NOMBRE = "Categoría de prueba";

    private static final String COL_CLANKS = "clanks";
    private static final String COL_USUARIOS = "usuarios";
    private static final String COL_CATEGORIAS = "categorias";

    public static final String TEST_BUSQUEDA_UID = "test-uid-busqueda-fase4";
    public static final String TEST_BUSQUEDA_EMAIL = "test-busqueda-fase4@clank.test";
    public static final String TEST_BUSQUEDA_NOMBRE = "Usuario Busqueda Fase4";
    public static final String TEST_BUSQUEDA_USUARIO_CLANK = "usuario_busqueda_fase4_unico";

    public static final String TEST_BUSQUEDA_CLANK_ID = "test-clank-busqueda-fase4";
    public static final String TEST_BUSQUEDA_CLANK_TITULO = "Clank busqueda unico Fase4";
    public static final String TEST_BUSQUEDA_CLANK_DESCRIPCION =
            "Descripcion busqueda unica para regresion global";

    private static final String SUB_MATERIALES = "materiales";
    private static final String SUB_HERRAMIENTAS = "herramientas";
    private static final String SUB_INSTRUCCIONES = "instrucciones";
    private static final String SUB_LIKES = "likes";

    private static final long TIMEOUT_S = 20;
    private static final int MAX_INTENTOS_AUTH = 4;
    private static final long ESPERA_REINTENTO_AUTH_MS = 800;

    private final FirebaseFirestore db;
    private String uidAutenticadoTest;

    public TestDataSeeder() {
        this.db = FirebaseFirestore.getInstance();
    }

    public String getTestUid() {
        return TEST_UID;
    }

    ///////////////////////// Auth Emulator /////////////////////////

    public String crearOIniciarSesionUsuarioAuthTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();

        AuthResult resultado = crearOIniciarSesionUsuarioAuthTestConReintentos(auth);

        FirebaseUser usuario = resultado.getUser();

        if (usuario == null || usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new IllegalStateException(
                    "No se pudo obtener UID del usuario autenticado de test."
            );
        }

        uidAutenticadoTest = usuario.getUid();
        return uidAutenticadoTest;
    }

    private AuthResult crearOIniciarSesionUsuarioAuthTestConReintentos(FirebaseAuth auth)
            throws ExecutionException, InterruptedException, TimeoutException {

        ExecutionException ultimoErrorExecution = null;
        TimeoutException ultimoErrorTimeout = null;

        for (int intento = 1; intento <= MAX_INTENTOS_AUTH; intento++) {
            try {
                return Tasks.await(
                        auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD),
                        TIMEOUT_S,
                        TimeUnit.SECONDS
                );

            } catch (ExecutionException errorCreacion) {
                Throwable causa = errorCreacion.getCause();

                if (causa instanceof FirebaseAuthUserCollisionException) {
                    try {
                        return Tasks.await(
                                auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD),
                                TIMEOUT_S,
                                TimeUnit.SECONDS
                        );

                    } catch (ExecutionException errorLogin) {
                        if (!esErrorTransitorioAuth(errorLogin)
                                || intento == MAX_INTENTOS_AUTH) {
                            throw errorLogin;
                        }

                        ultimoErrorExecution = errorLogin;

                    } catch (TimeoutException errorTimeout) {
                        if (intento == MAX_INTENTOS_AUTH) {
                            throw errorTimeout;
                        }

                        ultimoErrorTimeout = errorTimeout;
                    }

                } else {
                    if (!esErrorTransitorioAuth(errorCreacion)
                            || intento == MAX_INTENTOS_AUTH) {
                        throw errorCreacion;
                    }

                    ultimoErrorExecution = errorCreacion;
                }

            } catch (TimeoutException errorTimeout) {
                if (intento == MAX_INTENTOS_AUTH) {
                    throw errorTimeout;
                }

                ultimoErrorTimeout = errorTimeout;
            }

            auth.signOut();
            Thread.sleep(ESPERA_REINTENTO_AUTH_MS * intento);
        }

        if (ultimoErrorExecution != null) {
            throw ultimoErrorExecution;
        }

        if (ultimoErrorTimeout != null) {
            throw ultimoErrorTimeout;
        }

        throw new IllegalStateException(
                "No se pudo crear o iniciar sesión con el usuario Auth de test."
        );
    }

    private boolean esErrorTransitorioAuth(Exception error) {
        Throwable actual = error;

        while (actual != null) {
            String mensaje = actual.getMessage();

            if (mensaje != null) {
                String mensajeNormalizado = mensaje.toLowerCase();

                if (mensajeNormalizado.contains("unexpected end of stream")
                        || mensajeNormalizado.contains("connection reset")
                        || mensajeNormalizado.contains("failed to connect")
                        || mensajeNormalizado.contains("timeout")
                        || mensajeNormalizado.contains("network error")
                        || mensajeNormalizado.contains("internal error has occurred")) {
                    return true;
                }
            }

            actual = actual.getCause();
        }

        return false;
    }

    public String getUidAutenticadoTest() {
        if (uidAutenticadoTest != null && !uidAutenticadoTest.isEmpty()) {
            return uidAutenticadoTest;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null || usuario.getUid() == null || usuario.getUid().isEmpty()) {
            throw new IllegalStateException(
                    "No hay usuario autenticado de test. Llama antes a crearOIniciarSesionUsuarioAuthTest()."
            );
        }

        uidAutenticadoTest = usuario.getUid();
        return uidAutenticadoTest;
    }

    public void cerrarSesionAuthTest() {
        FirebaseAuth.getInstance().signOut();
        uidAutenticadoTest = null;
    }

    ///////////////////////// usuarios /////////////////////////

    public void insertarUsuarioTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", TEST_UID);
        usuario.put("nombre", TEST_NOMBRE);
        usuario.put("correo", TEST_EMAIL);
        usuario.put("telefono", "600000000");
        usuario.put("usuarioClank", TEST_USUARIO_CLANK);
        usuario.put("fotoPerfil", "");
        usuario.put("fotoPortada", "");
        usuario.put("fechaCreacion", "2026-05-22");
        usuario.put("fechaNacimiento", "2000-01-01");
        usuario.put("enLinea", false);

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(TEST_UID)
                        .set(usuario),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarUsuarioAutenticadoTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getUidAutenticadoTest();

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", uid);
        usuario.put("nombre", TEST_NOMBRE);
        usuario.put("correo", TEST_EMAIL);
        usuario.put("telefono", "600000000");
        usuario.put("usuarioClank", TEST_USUARIO_CLANK);
        usuario.put("fotoPerfil", "");
        usuario.put("fotoPortada", "");
        usuario.put("fechaCreacion", "2026-05-22");
        usuario.put("fechaNacimiento", "2000-01-01");
        usuario.put("enLinea", false);

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(uid)
                        .set(usuario),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuarioFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(TEST_UID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuarioAutenticadoFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid;

        try {
            uid = getUidAutenticadoTest();
        } catch (IllegalStateException error) {
            return;
        }

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(uid)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// categorías /////////////////////////

    public void insertarCategoriaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> categoria = new HashMap<>();
        categoria.put("categoria", TEST_CATEGORIA_NOMBRE);

        Tasks.await(
                db.collection(COL_CATEGORIAS)
                        .document(TEST_CATEGORIA_ID)
                        .set(categoria),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarCategoriaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_CATEGORIAS)
                        .document(TEST_CATEGORIA_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// clanks acabados /////////////////////////

    public void insertarClankTest(int numLikes, boolean conSubcol)
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_ID);
        clank.put("usuarioId", TEST_UID);
        clank.put("titulo", TEST_CLANK_TITULO);
        clank.put("descripcion", "Descripción de prueba para tests de integración");
        clank.put("portada", "");
        clank.put("tiempo", 1);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", numLikes);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (conSubcol) {
            insertarSubcoleccionesTest(TEST_CLANK_ID);
        }
    }

    public void insertarClankAutenticadoTest(int numLikes, boolean conSubcol)
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getUidAutenticadoTest();

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_ID);
        clank.put("usuarioId", uid);
        clank.put("titulo", TEST_CLANK_TITULO);
        clank.put("descripcion", "Descripción de prueba para tests de integración");
        clank.put("portada", "");
        clank.put("tiempo", 1);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", numLikes);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (conSubcol) {
            insertarSubcoleccionesTest(TEST_CLANK_ID);
        }
    }

    public void eliminarClankTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_CLANK_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarClankSinHerramientas()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_SIN_HERR_ID);
        clank.put("usuarioId", TEST_UID);
        clank.put("titulo", "Clank sin herramientas Fase4");
        clank.put("descripcion", "Sin herramientas para verificar visibilidad GONE");
        clank.put("portada", "");
        clank.put("tiempo", 2);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", 0);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId", "test-mat-sh-01");
        mat.put("material", "Material sin herr");
        mat.put("cantidad", 1);

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .collection(SUB_MATERIALES)
                        .document("test-mat-sh-01"),
                mat
        );

        Map<String, Object> inst = new HashMap<>();
        inst.put("instId", "test-inst-sh-01");
        inst.put("orden", 1);
        inst.put("instruccion", "Paso único sin herramientas");
        inst.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-sh-01"),
                inst
        );

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarClankSinHerramientas()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// bocetos /////////////////////////

    public void insertarBocetoAutenticadoTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getUidAutenticadoTest();

        Map<String, Object> boceto = new HashMap<>();
        boceto.put("clankId", TEST_BOCETO_ID);
        boceto.put("usuarioId", uid);
        boceto.put("titulo", TEST_BOCETO_TITULO);
        boceto.put("descripcion", "Descripción de boceto para tests de perfil");
        boceto.put("portada", "");
        boceto.put("tiempo", 1);
        boceto.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        boceto.put("estadoAcabado", false);
        boceto.put("numLikes", 0);
        boceto.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .set(boceto),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarBocetoAutenticadoCompletoTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getUidAutenticadoTest();

        Map<String, Object> boceto = new HashMap<>();
        boceto.put("clankId", TEST_BOCETO_ID);
        boceto.put("usuarioId", uid);
        boceto.put("titulo", TEST_BOCETO_TITULO);
        boceto.put("descripcion", "Descripción de boceto para tests de edición");
        boceto.put("portada", "");
        boceto.put("tiempo", 1);
        boceto.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        boceto.put("estadoAcabado", false);
        boceto.put("numLikes", 0);
        boceto.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .set(boceto),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        WriteBatch batch = db.batch();

        Map<String, Object> material = new HashMap<>();
        material.put("matId", "test-boceto-mat-01");
        material.put("material", "Papel de boceto");
        material.put("cantidad", 3);

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .collection(SUB_MATERIALES)
                        .document("test-boceto-mat-01"),
                material
        );

        Map<String, Object> herramienta = new HashMap<>();
        herramienta.put("herrId", "test-boceto-herr-01");
        herramienta.put("herramienta", "Tijeras de boceto");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .collection(SUB_HERRAMIENTAS)
                        .document("test-boceto-herr-01"),
                herramienta
        );

        Map<String, Object> instruccion = new HashMap<>();
        instruccion.put("instId", "test-boceto-inst-01");
        instruccion.put("orden", 1);
        instruccion.put("instruccion", "Primer paso del boceto");
        instruccion.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-boceto-inst-01"),
                instruccion
        );

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarBocetoAutenticadoTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_BOCETO_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_BOCETO_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_BOCETO_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_BOCETO_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_BOCETO_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// subcolecciones /////////////////////////

    public void insertarSubcoleccionesTest(String clankId)
            throws ExecutionException, InterruptedException, TimeoutException {

        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId", "test-mat-01");
        mat.put("material", "Lana de prueba");
        mat.put("cantidad", 2);

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_MATERIALES)
                        .document("test-mat-01"),
                mat
        );

        Map<String, Object> herr = new HashMap<>();
        herr.put("herrId", "test-herr-01");
        herr.put("herramienta", "Agujas de prueba");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_HERRAMIENTAS)
                        .document("test-herr-01"),
                herr
        );

        Map<String, Object> inst1 = new HashMap<>();
        inst1.put("instId", "test-inst-01");
        inst1.put("orden", 1);
        inst1.put("instruccion", "Primer paso de prueba");
        inst1.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-01"),
                inst1
        );

        Map<String, Object> inst2 = new HashMap<>();
        inst2.put("instId", "test-inst-02");
        inst2.put("orden", 2);
        inst2.put("instruccion", "Segundo paso de prueba");
        inst2.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-02"),
                inst2
        );

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// likes /////////////////////////

    public void insertarLikeTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> like = new HashMap<>();
        like.put("uid", TEST_UID);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .collection(SUB_LIKES)
                        .document(TEST_UID)
                        .set(like),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarLikeTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .collection(SUB_LIKES)
                        .document(TEST_UID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// datos exclusivos de búsqueda /////////////////////////

    public void insertarUsuarioBusquedaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", TEST_BUSQUEDA_UID);
        usuario.put("nombre", TEST_BUSQUEDA_NOMBRE);
        usuario.put("correo", TEST_BUSQUEDA_EMAIL);
        usuario.put("telefono", "611111111");
        usuario.put("usuarioClank", TEST_BUSQUEDA_USUARIO_CLANK);
        usuario.put("fotoPerfil", "");
        usuario.put("fotoPortada", "");
        usuario.put("fechaCreacion", new Date());
        usuario.put("fechaNacimiento", "2000-01-01");
        usuario.put("enLinea", false);

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(TEST_BUSQUEDA_UID)
                        .set(usuario),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarClankBusquedaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_BUSQUEDA_CLANK_ID);
        clank.put("usuarioId", TEST_BUSQUEDA_UID);
        clank.put("titulo", TEST_BUSQUEDA_CLANK_TITULO);
        clank.put("descripcion", TEST_BUSQUEDA_CLANK_DESCRIPCION);
        clank.put("portada", "");
        clank.put("tiempo", 1);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", 0);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_BUSQUEDA_CLANK_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarDatosBusquedaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_BUSQUEDA_CLANK_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_BUSQUEDA_CLANK_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_BUSQUEDA_CLANK_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_BUSQUEDA_CLANK_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_BUSQUEDA_CLANK_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(TEST_BUSQUEDA_UID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// utilidades privadas /////////////////////////

    private void borrarSubcoleccion(String clankId, String subcoleccion)
            throws ExecutionException, InterruptedException, TimeoutException {

        QuerySnapshot docs = Tasks.await(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(subcoleccion)
                        .get(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (docs.isEmpty()) {
            return;
        }

        WriteBatch batch = db.batch();

        for (DocumentSnapshot doc : docs.getDocuments()) {
            batch.delete(doc.getReference());
        }

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuariosConUsuarioClankTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        QuerySnapshot docs = Tasks.await(
                db.collection(COL_USUARIOS)
                        .whereEqualTo("usuarioClank", TEST_USUARIO_CLANK)
                        .get(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (docs.isEmpty()) {
            return;
        }

        WriteBatch batch = db.batch();

        for (DocumentSnapshot doc : docs.getDocuments()) {
            batch.delete(doc.getReference());
        }

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }
}