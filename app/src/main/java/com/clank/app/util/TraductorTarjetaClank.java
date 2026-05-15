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

public class TraductorTarjetaClank {

    private final Context contextoAplicacion;

    public TraductorTarjetaClank(Context context) {
        this.contextoAplicacion = context.getApplicationContext();
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
        String tituloSeguro =
                tituloOriginal != null ? tituloOriginal : "";

        String descripcionSegura =
                descripcionOriginal != null ? descripcionOriginal : "";

        String textoParaDetectar =
                construirTextoDeteccion(
                        tituloSeguro,
                        descripcionSegura
                );

        if (textoParaDetectar.trim().isEmpty()) {
            return Tasks.forResult(
                    new TextoTarjetaTraducido(
                            tituloSeguro,
                            descripcionSegura
                    )
            );
        }

        String etiquetaIdiomaDestino =
                GestorIdioma.getInstance(contextoAplicacion)
                        .getIdiomaActual();

        String idiomaDestino =
                TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

        if (idiomaDestino == null) {
            return Tasks.forResult(
                    new TextoTarjetaTraducido(
                            tituloSeguro,
                            descripcionSegura
                    )
            );
        }

        LanguageIdentifier identificador =
                LanguageIdentification.getClient();

        return identificador.identifyLanguage(textoParaDetectar)
                .continueWithTask(tareaDeteccion -> {
                    if (!tareaDeteccion.isSuccessful()) {
                        return Tasks.forResult(
                                new TextoTarjetaTraducido(
                                        tituloSeguro,
                                        descripcionSegura
                                )
                        );
                    }

                    String etiquetaIdiomaOrigen =
                            tareaDeteccion.getResult();

                    if (etiquetaIdiomaOrigen == null
                            || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {
                        return Tasks.forResult(
                                new TextoTarjetaTraducido(
                                        tituloSeguro,
                                        descripcionSegura
                                )
                        );
                    }

                    String idiomaOrigen =
                            TranslateLanguage.fromLanguageTag(
                                    etiquetaIdiomaOrigen
                            );

                    if (idiomaOrigen == null
                            || idiomaOrigen.equals(idiomaDestino)) {
                        return Tasks.forResult(
                                new TextoTarjetaTraducido(
                                        tituloSeguro,
                                        descripcionSegura
                                )
                        );
                    }

                    return traducirTarjeta(
                            tituloSeguro,
                            descripcionSegura,
                            idiomaOrigen,
                            idiomaDestino
                    );
                })
                .addOnCompleteListener(tarea -> identificador.close());
    }

    private Task<TextoTarjetaTraducido> traducirTarjeta(
            String tituloOriginal,
            String descripcionOriginal,
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
                        traductor.close();

                        return Tasks.forResult(
                                new TextoTarjetaTraducido(
                                        tituloOriginal,
                                        descripcionOriginal
                                )
                        );
                    }

                    Task<String> tareaTitulo =
                            traducirTextoSeguro(
                                    traductor,
                                    tituloOriginal
                            );

                    Task<String> tareaDescripcion =
                            traducirTextoSeguro(
                                    traductor,
                                    descripcionOriginal
                            );

                    return Tasks.whenAllComplete(
                                    tareaTitulo,
                                    tareaDescripcion
                            )
                            .continueWith(tareaTraduccion -> {
                                String tituloTraducido =
                                        tareaTitulo.isSuccessful()
                                                && tareaTitulo.getResult() != null
                                                ? tareaTitulo.getResult()
                                                : tituloOriginal;

                                String descripcionTraducida =
                                        tareaDescripcion.isSuccessful()
                                                && tareaDescripcion.getResult() != null
                                                ? tareaDescripcion.getResult()
                                                : descripcionOriginal;

                                traductor.close();

                                return new TextoTarjetaTraducido(
                                        tituloTraducido,
                                        descripcionTraducida
                                );
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

    private String construirTextoDeteccion(
            String titulo,
            String descripcion
    ) {
        StringBuilder texto = new StringBuilder();

        if (titulo != null && !titulo.trim().isEmpty()) {
            texto.append(titulo.trim());
        }

        if (descripcion != null && !descripcion.trim().isEmpty()) {
            if (texto.length() > 0) {
                texto.append("\n");
            }

            texto.append(descripcion.trim());
        }

        return texto.toString();
    }
}