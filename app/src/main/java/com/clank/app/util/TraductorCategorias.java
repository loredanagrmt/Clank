package com.clank.app.util;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class TraductorCategorias {

    private static final String TAG = "TraductorCategorias";
    private final Context contextoAplicacion;
  private final LanguageIdentifier identificador;


  @Inject
    public TraductorCategorias(@ApplicationContext Context contextoAplicacion) {
        this.contextoAplicacion = contextoAplicacion;
    this.identificador = LanguageIdentification.getClient();
    }

    public Task<List<String[]>> traducirSiProcede(List<String[]> categoriasOriginales) {
        List<String[]> categoriasSeguras = copiarCategorias(categoriasOriginales);

        if (categoriasSeguras.isEmpty()) {
            Log.d(TAG, "No hay categorías para traducir.");
            return Tasks.forResult(categoriasSeguras);
        }

        String textoParaDetectar = construirTextoDeteccion(categoriasSeguras);

        if (textoParaDetectar.trim().isEmpty()) {
            Log.d(TAG, "No hay texto útil en categorías para detectar idioma.");
            return Tasks.forResult(categoriasSeguras);
        }

        String etiquetaIdiomaDestino =
                GestorIdioma.getInstance(contextoAplicacion)
                        .getIdiomaActual();

        String idiomaDestino =
                TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

        Log.d(TAG, "Idioma destino de categorías: " + etiquetaIdiomaDestino);

        if (idiomaDestino == null) {
            Log.d(TAG, "Idioma destino no compatible con ML Kit.");
            return Tasks.forResult(categoriasSeguras);
        }

        return identificador.identifyLanguage(textoParaDetectar)
                .continueWithTask(tareaDeteccion -> {
                    if (!tareaDeteccion.isSuccessful()) {
                        Log.e(TAG, "Error detectando idioma de categorías",
                                tareaDeteccion.getException());
                        return Tasks.forResult(categoriasSeguras);
                    }

                    String etiquetaIdiomaOrigen =
                            tareaDeteccion.getResult();

                    Log.d(TAG, "Idioma detectado en categorías: " + etiquetaIdiomaOrigen);

                    if (etiquetaIdiomaOrigen == null
                            || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {
                        Log.d(TAG, "Idioma de categorías indeterminado.");
                        return Tasks.forResult(categoriasSeguras);
                    }

                    String idiomaOrigen =
                            TranslateLanguage.fromLanguageTag(etiquetaIdiomaOrigen);

                    if (idiomaOrigen == null) {
                        Log.d(TAG, "Idioma origen de categorías no compatible.");
                        return Tasks.forResult(categoriasSeguras);
                    }

                    if (idiomaOrigen.equals(idiomaDestino)) {
                        Log.d(TAG, "Categorías ya están en el idioma destino.");
                        return Tasks.forResult(categoriasSeguras);
                    }

                    return traducirCategorias(
                            categoriasSeguras,
                            idiomaOrigen,
                            idiomaDestino
                    );
                });
    }

    private Task<List<String[]>> traducirCategorias(
            List<String[]> categorias,
            String idiomaOrigen,
            String idiomaDestino
    ) {
        TranslatorOptions opciones =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(idiomaOrigen)
                        .setTargetLanguage(idiomaDestino)
                        .build();

        Translator traductor =
                Translation.getClient(opciones);

        DownloadConditions condiciones =
                new DownloadConditions.Builder()
                        .build();

        Log.d(TAG, "Descargando modelo para traducir categorías...");

        return traductor.downloadModelIfNeeded(condiciones)
                .continueWithTask(tareaDescarga -> {
                    if (!tareaDescarga.isSuccessful()) {
                        Log.e(TAG, "Error preparando modelo de categorías",
                                tareaDescarga.getException());
                        traductor.close();
                        return Tasks.forResult(categorias);
                    }

                    List<Task<String>> tareasTraduccion =
                            new ArrayList<>();

                    for (String[] categoria : categorias) {
                        String nombre =
                                categoria.length > 1 && categoria[1] != null
                                        ? categoria[1]
                                        : "";

                        tareasTraduccion.add(
                                traducirTextoSeguro(traductor, nombre)
                        );
                    }

                    return Tasks.whenAllComplete(tareasTraduccion)
                            .continueWith(tareaFinal -> {
                                List<String[]> categoriasTraducidas =
                                        new ArrayList<>();

                                for (int i = 0; i < categorias.size(); i++) {
                                    String[] categoria = categorias.get(i);

                                    String id =
                                            categoria.length > 0 && categoria[0] != null
                                                    ? categoria[0]
                                                    : "";

                                    String nombreOriginal =
                                            categoria.length > 1 && categoria[1] != null
                                                    ? categoria[1]
                                                    : "";

                                    Task<String> tareaTexto =
                                            tareasTraduccion.get(i);

                                    String nombreFinal =
                                            tareaTexto.isSuccessful()
                                                    && tareaTexto.getResult() != null
                                                    ? tareaTexto.getResult()
                                                    : nombreOriginal;

                                    categoriasTraducidas.add(
                                            new String[]{id, nombreFinal}
                                    );
                                }

                                traductor.close();
                                Log.d(TAG, "Traducción de categorías finalizada.");

                                return categoriasTraducidas;
                            });
                });
    }

    private Task<String> traducirTextoSeguro(
            Translator traductor,
            String textoOriginal
    ) {
        String textoSeguro =
                textoOriginal != null ? textoOriginal : "";

        if (textoSeguro.trim().isEmpty()) {
            return Tasks.forResult(textoSeguro);
        }

        return traductor.translate(textoSeguro)
                .continueWith(tarea -> {
                    if (tarea.isSuccessful()
                            && tarea.getResult() != null) {
                        return tarea.getResult();
                    }

                    return textoSeguro;
                });
    }

    private List<String[]> copiarCategorias(List<String[]> categoriasOriginales) {
        List<String[]> copia = new ArrayList<>();

        if (categoriasOriginales == null) {
            return copia;
        }

        for (String[] categoria : categoriasOriginales) {
            if (categoria == null) {
                continue;
            }

            String id =
                    categoria.length > 0 && categoria[0] != null
                            ? categoria[0]
                            : "";

            String nombre =
                    categoria.length > 1 && categoria[1] != null
                            ? categoria[1]
                            : "";

            copia.add(new String[]{id, nombre});
        }

        return copia;
    }

    private String construirTextoDeteccion(List<String[]> categorias) {
        StringBuilder texto = new StringBuilder();

        for (String[] categoria : categorias) {
            if (categoria == null || categoria.length < 2) {
                continue;
            }

            String nombre = categoria[1];

            if (nombre == null || nombre.trim().isEmpty()) {
                continue;
            }

            if (texto.length() > 0) {
                texto.append("\n");
            }

            texto.append(nombre.trim());
        }

        return texto.toString();
    }
  public void cerrar() {
    identificador.close();
  }
}
