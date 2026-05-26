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
    private static final String IDIOMA_BASE_CATEGORIAS = TranslateLanguage.SPANISH;

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
            return Tasks.forResult(categoriasSeguras);
        }

        String etiquetaIdiomaDestino =
                GestorIdioma.getInstance(contextoAplicacion)
                        .getIdiomaActual();

        String idiomaDestino =
                TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

        if (idiomaDestino == null) {
            Log.d(TAG, "Idioma destino no compatible. No se traducen categorías.");
            return Tasks.forResult(categoriasSeguras);
        }

        if (IDIOMA_BASE_CATEGORIAS.equals(idiomaDestino)) {
            Log.d(TAG, "Las categorías ya están en español. No se traducen.");
            return Tasks.forResult(categoriasSeguras);
        }

        String textoParaDetectar = construirTextoDeteccion(categoriasSeguras);

        if (textoParaDetectar.trim().isEmpty()) {
            Log.d(TAG, "No hay texto útil en categorías. No se traducen.");
            return Tasks.forResult(categoriasSeguras);
        }

        return identificador.identifyLanguage(textoParaDetectar)
                .continueWithTask(tareaDeteccion -> {
                    String idiomaOrigen = IDIOMA_BASE_CATEGORIAS;

                    if (tareaDeteccion.isSuccessful()) {
                        String etiquetaIdiomaOrigen = tareaDeteccion.getResult();

                        Log.d(TAG, "Idioma detectado en categorías: " + etiquetaIdiomaOrigen);

                        String idiomaDetectado =
                                resolverIdiomaOrigen(etiquetaIdiomaOrigen);

                        if (idiomaDetectado != null) {
                            idiomaOrigen = idiomaDetectado;
                        }
                    } else {
                        Log.e(
                                TAG,
                                "Error detectando idioma de categorías. Se usa español como origen.",
                                tareaDeteccion.getException()
                        );
                    }

                    if (idiomaOrigen.equals(idiomaDestino)) {
                        Log.d(TAG, "Idioma origen y destino coinciden. No se traducen categorías.");
                        return Tasks.forResult(categoriasSeguras);
                    }

                    return traducirCategorias(
                            categoriasSeguras,
                            idiomaOrigen,
                            idiomaDestino
                    );
                });
    }

    private String resolverIdiomaOrigen(String etiquetaIdiomaOrigen) {
        if (etiquetaIdiomaOrigen == null
                || etiquetaIdiomaOrigen.trim().isEmpty()
                || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {

            Log.d(TAG, "Idioma de categorías indeterminado. Se usa español como origen.");
            return IDIOMA_BASE_CATEGORIAS;
        }

        String idiomaOrigen =
                TranslateLanguage.fromLanguageTag(etiquetaIdiomaOrigen);

        if (idiomaOrigen == null) {
            Log.d(TAG, "Idioma origen no compatible. Se usa español como origen.");
            return IDIOMA_BASE_CATEGORIAS;
        }

        return idiomaOrigen;
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

        return traductor.downloadModelIfNeeded(condiciones)
                .continueWithTask(tareaDescarga -> {
                    if (!tareaDescarga.isSuccessful()) {
                        Log.e(
                                TAG,
                                "Error descargando modelo de traducción.",
                                tareaDescarga.getException()
                        );

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

                    Log.e(
                            TAG,
                            "Error traduciendo texto de categoría. Se mantiene el texto original.",
                            tarea.getException()
                    );

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
                texto.append(". ");
            }

            texto.append(nombre.trim());
        }

        return texto.toString();
    }

    public void cerrar() {
        identificador.close();
    }
}