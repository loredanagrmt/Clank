package com.clank.app.util;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;
public class TraductorTarjetaClank {

  private final Context contextoAplicacion;
  private final LanguageIdentifier identificador;

  //cache de traductores ya descargados
  private final Map<String, Translator> traductoresListos = new HashMap<>();
  //cache de tareas de descarga en curso
  private final Map<String, Task<Void>> descargasEnCurso = new HashMap<>();

  public TraductorTarjetaClank(Context context) {
    this.contextoAplicacion = context.getApplicationContext();
    this.identificador = LanguageIdentification.getClient();
  }

    public static class TextoTarjetaTraducido {
        public final String titulo;
        public final String descripcion;

        public TextoTarjetaTraducido(String titulo, String descripcion) {
            this.titulo = titulo != null ? titulo : "";
            this.descripcion = descripcion != null ? descripcion : "";
        }
    }

  public Task<TextoTarjetaTraducido> traducirSiProcede(
    String tituloOriginal,
    String descripcionOriginal
  ) {
    String tituloSeguro = tituloOriginal != null ? tituloOriginal : "";
    String descripcionSegura = descripcionOriginal != null ? descripcionOriginal : "";
    String textoParaDetectar = construirTextoDeteccion(tituloSeguro, descripcionSegura);

    if (textoParaDetectar.trim().isEmpty()) {
      return Tasks.forResult(new TextoTarjetaTraducido(tituloSeguro, descripcionSegura));
    }

    String etiquetaIdiomaDestino =
      GestorIdioma.getInstance(contextoAplicacion).getIdiomaActual();
    String idiomaDestino = TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

    if (idiomaDestino == null) {
      return Tasks.forResult(new TextoTarjetaTraducido(tituloSeguro, descripcionSegura));
    }

    return identificador.identifyLanguage(textoParaDetectar)
      .continueWithTask(tareaDeteccion -> {
        if (!tareaDeteccion.isSuccessful()) {
          return Tasks.forResult(
            new TextoTarjetaTraducido(tituloSeguro, descripcionSegura));
        }

        String etiquetaIdiomaOrigen = tareaDeteccion.getResult();

        if (etiquetaIdiomaOrigen == null
          || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {
          return Tasks.forResult(
            new TextoTarjetaTraducido(tituloSeguro, descripcionSegura));
        }

        String idiomaOrigen =
          TranslateLanguage.fromLanguageTag(etiquetaIdiomaOrigen);

        if (idiomaOrigen == null || idiomaOrigen.equals(idiomaDestino)) {
          return Tasks.forResult(
            new TextoTarjetaTraducido(tituloSeguro, descripcionSegura));
        }

        return traducirConTraductorCacheado(
          tituloSeguro, descripcionSegura,
          idiomaOrigen, idiomaDestino
        );
      });
  }

private Task<TextoTarjetaTraducido> traducirConTraductorCacheado(
    String titulo,
    String descripcion,
    String idiomaOrigen,
    String idiomaDestino
    ) {
    String clavePar = idiomaOrigen + "->" + idiomaDestino;

    if (traductoresListos.containsKey(clavePar)) {
      Translator traductor = traductoresListos.get(clavePar);
      return traducirConTraductor(traductor, titulo, descripcion);
    }
    Task<Void> descargaExistente = descargasEnCurso.get(clavePar);
    if (descargaExistente != null) {
      return descargaExistente.continueWithTask(t -> {
        Translator traductor = traductoresListos.get(clavePar);
        if (traductor == null) {
          return Tasks.forResult(new TextoTarjetaTraducido(titulo, descripcion));
        }
        return traducirConTraductor(traductor, titulo, descripcion);
      });
    }
    TranslatorOptions opciones = new TranslatorOptions.Builder()
      .setSourceLanguage(idiomaOrigen)
      .setTargetLanguage(idiomaDestino)
      .build();

    Translator nuevoTraductor = Translation.getClient(opciones);
    DownloadConditions condiciones = new DownloadConditions.Builder().build();

    Task<Void> tareaDescarga = nuevoTraductor
      .downloadModelIfNeeded(condiciones)
      .addOnSuccessListener(v -> {
        traductoresListos.put(clavePar, nuevoTraductor);
        descargasEnCurso.remove(clavePar);
      })
      .addOnFailureListener(e -> {
        nuevoTraductor.close();
        descargasEnCurso.remove(clavePar);
      });

    descargasEnCurso.put(clavePar, tareaDescarga);

    return tareaDescarga.continueWithTask(t -> {
      Translator traductor = traductoresListos.get(clavePar);
      if (traductor == null) {
        return Tasks.forResult(new TextoTarjetaTraducido(titulo, descripcion));
      }
      return traducirConTraductor(traductor, titulo, descripcion);
    });
  }

  private Task<TextoTarjetaTraducido> traducirConTraductor(
    Translator traductor,
    String titulo,
    String descripcion
  ) {
    Task<String> tareaTitulo = traducirTextoSeguro(traductor, titulo);
    Task<String> tareaDescripcion = traducirTextoSeguro(traductor, descripcion);

    return Tasks.whenAllComplete(tareaTitulo, tareaDescripcion)
      .continueWith(t -> {
        String tituloTraducido = tareaTitulo.isSuccessful()
          && tareaTitulo.getResult() != null
          ? tareaTitulo.getResult() : titulo;

        String descripcionTraducida = tareaDescripcion.isSuccessful()
          && tareaDescripcion.getResult() != null
          ? tareaDescripcion.getResult() : descripcion;

        return new TextoTarjetaTraducido(tituloTraducido, descripcionTraducida);
      });
    }

  private Task<String> traducirTextoSeguro(Translator traductor, String textoOriginal) {
    String textoSeguro = textoOriginal != null ? textoOriginal : "";

        if (textoSeguro.trim().isEmpty()) {
            return Tasks.forResult(textoSeguro);
        }

        return traductor.translate(textoSeguro)
          .continueWith(tarea -> {
        if (tarea.isSuccessful() && tarea.getResult() != null) {
          return tarea.getResult();
        }
        return textoSeguro;
      });
  }

  private String construirTextoDeteccion(String titulo, String descripcion) {
    StringBuilder texto = new StringBuilder();

        if (titulo != null && !titulo.trim().isEmpty()) {
            texto.append(titulo.trim());
        }

      if (descripcion != null && !descripcion.trim().isEmpty()) {
      if (texto.length() > 0) texto.append("\n");
            texto.append(descripcion.trim());
        }

        return texto.toString();
    }

  public void cerrar() {
    identificador.close();
    for (Translator t : traductoresListos.values()) {
      t.close();
    }
    traductoresListos.clear();
    descargasEnCurso.clear();
  }
}
