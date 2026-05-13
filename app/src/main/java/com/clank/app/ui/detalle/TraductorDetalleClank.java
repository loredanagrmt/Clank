package com.clank.app.ui.detalle;

import android.content.Context;
import android.util.Log;

import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.util.GestorIdioma;
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

public class TraductorDetalleClank {

    private static final String TAG = "TraductorClank";

    private final Context contextoAplicacion;

    @Inject
    public TraductorDetalleClank(@ApplicationContext Context contextoAplicacion) {
        this.contextoAplicacion = contextoAplicacion;
    }

    public Task<Boolean> traducirSiProcede(DetalleClankViewModel.DetalleData datos) {
        Log.d(TAG, "Inicio de traducirSiProcede()");

        if (datos == null) {
            Log.d(TAG, "DetalleData es null. No se traduce.");
            return Tasks.forResult(false);
        }

        String textoParaDetectarIdioma = construirTextoDeteccion(datos);

        Log.d(TAG, "Texto preparado para detección. Longitud: " + textoParaDetectarIdioma.length());

        if (textoParaDetectarIdioma.trim().isEmpty()) {
            Log.d(TAG, "No hay texto útil para detectar idioma.");
            return Tasks.forResult(false);
        }

        String etiquetaIdiomaDestino = GestorIdioma.getInstance(contextoAplicacion).getIdiomaActual();

        Log.d(TAG, "Idioma elegido por el usuario: " + etiquetaIdiomaDestino);

        String idiomaDestino = TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

        Log.d(TAG, "Idioma destino compatible con ML Kit: " + idiomaDestino);

        if (idiomaDestino == null) {
            Log.d(TAG, "El idioma destino no es compatible con traducción.");
            return Tasks.forResult(false);
        }

        LanguageIdentifier identificador = LanguageIdentification.getClient();

        Log.d(TAG, "Iniciando detección de idioma...");

        return identificador.identifyLanguage(textoParaDetectarIdioma).continueWithTask(tareaDeteccion -> {
            Log.d(TAG, "Detección finalizada. Éxito: " + tareaDeteccion.isSuccessful());

            if (!tareaDeteccion.isSuccessful()) {
                Log.e(TAG, "Error detectando idioma", tareaDeteccion.getException());
                return Tasks.forResult(false);
            }

            String etiquetaIdiomaOrigen = tareaDeteccion.getResult();

            Log.d(TAG, "Idioma detectado: " + etiquetaIdiomaOrigen);

            if (etiquetaIdiomaOrigen == null || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {
                Log.d(TAG, "Idioma no detectado con suficiente confianza.");
                return Tasks.forResult(false);
            }

            String idiomaOrigen = TranslateLanguage.fromLanguageTag(etiquetaIdiomaOrigen);

            Log.d(TAG, "Idioma origen compatible con ML Kit: " + idiomaOrigen);

            if (idiomaOrigen == null) {
                Log.d(TAG, "El idioma origen no es traducible por ML Kit.");
                return Tasks.forResult(false);
            }

            if (idiomaOrigen.equals(idiomaDestino)) {
                Log.d(TAG, "Idioma origen y destino coinciden. No se traduce.");
                return Tasks.forResult(false);
            }

            Log.d(TAG, "Se traducirá el detalle de " + idiomaOrigen + " a " + idiomaDestino);

            return traducirDetalle(datos, idiomaOrigen, idiomaDestino);
        }).addOnCompleteListener(tarea -> {
            identificador.close();
            Log.d(TAG, "LanguageIdentifier cerrado.");
            Log.d(TAG, "traducirSiProcede() completado. Éxito tarea: " + tarea.isSuccessful());
        });
    }

    private Task<Boolean> traducirDetalle(DetalleClankViewModel.DetalleData datos, String idiomaOrigen, String idiomaDestino) {
        Log.d(TAG, "Creando Translator...");

        TranslatorOptions opciones = new TranslatorOptions.Builder().setSourceLanguage(idiomaOrigen).setTargetLanguage(idiomaDestino).build();

        Translator traductor = Translation.getClient(opciones);

        DownloadConditions condiciones = new DownloadConditions.Builder().build();

        Log.d(TAG, "Iniciando downloadModelIfNeeded(). " + "Si es la primera vez, puede tardar.");

        return traductor.downloadModelIfNeeded(condiciones).addOnSuccessListener(unused -> Log.d(TAG, "Modelo de traducción disponible.")).addOnFailureListener(error -> Log.e(TAG, "Error descargando/comprobando modelo de traducción", error)).continueWithTask(tareaDescarga -> {
            Log.d(TAG, "downloadModelIfNeeded() terminado. Éxito: " + tareaDescarga.isSuccessful());

            if (!tareaDescarga.isSuccessful()) {
                traductor.close();
                Log.d(TAG, "Translator cerrado tras fallo de descarga.");
                return Tasks.forResult(false);
            }

            Log.d(TAG, "Iniciando traducción de campos del detalle...");

            return traducirCampos(datos, traductor).continueWith(tareaTraduccion -> {
                Log.d(TAG, "Traducción de campos finalizada. Éxito: " + tareaTraduccion.isSuccessful());

                if (!tareaTraduccion.isSuccessful()) {
                    Log.e(TAG, "Error general traduciendo campos", tareaTraduccion.getException());
                }

                traductor.close();
                Log.d(TAG, "Translator cerrado.");

                return tareaTraduccion.isSuccessful();
            });
        });
    }

    private Task<Void> traducirCampos(DetalleClankViewModel.DetalleData datos, Translator traductor) {
        List<Task<Void>> tareas = new ArrayList<>();

        Log.d(TAG, "Preparando traducción de título...");
        tareas.add(traducirTextoSeguro(traductor, datos.titulo).continueWith(tarea -> {
            datos.titulo = tarea.getResult();
            return (Void) null;
        }));

        Log.d(TAG, "Preparando traducción de descripción...");
        tareas.add(traducirTextoSeguro(traductor, datos.descripcion).continueWith(tarea -> {
            datos.descripcion = tarea.getResult();
            return (Void) null;
        }));

        if (datos.materiales != null) {
            Log.d(TAG, "Materiales a traducir: " + datos.materiales.size());

            for (Material material : datos.materiales) {
                if (material == null) continue;

                tareas.add(traducirTextoSeguro(traductor, material.getMaterial()).continueWith(tarea -> {
                    material.setMaterial(tarea.getResult());
                    return (Void) null;
                }));
            }
        }

        if (datos.herramientas != null) {
            Log.d(TAG, "Herramientas a traducir: " + datos.herramientas.size());

            for (Herramienta herramienta : datos.herramientas) {
                if (herramienta == null) continue;

                tareas.add(traducirTextoSeguro(traductor, herramienta.getHerramienta()).continueWith(tarea -> {
                    herramienta.setHerramienta(tarea.getResult());
                    return (Void) null;
                }));
            }
        }

        if (datos.instrucciones != null) {
            Log.d(TAG, "Instrucciones a traducir: " + datos.instrucciones.size());

            for (Instruccion instruccion : datos.instrucciones) {
                if (instruccion == null) continue;

                tareas.add(traducirTextoSeguro(traductor, instruccion.getInstruccion()).continueWith(tarea -> {
                    instruccion.setInstruccion(tarea.getResult());
                    return (Void) null;
                }));
            }
        }

        Log.d(TAG, "Total de tareas de traducción preparadas: " + tareas.size());

        return Tasks.whenAll(tareas).addOnSuccessListener(unused -> Log.d(TAG, "Todas las tareas de traducción han terminado.")).addOnFailureListener(error -> Log.e(TAG, "Alguna tarea de traducción falló", error));
    }

    private Task<String> traducirTextoSeguro(Translator traductor, String texto) {
        String textoOriginal = texto != null ? texto : "";

        if (textoOriginal.trim().isEmpty()) {
            Log.d(TAG, "Texto vacío. Se omite traducción.");
            return Tasks.forResult(textoOriginal);
        }

        Log.d(TAG, "Traduciendo texto. Longitud original: " + textoOriginal.length());

        return traductor.translate(textoOriginal).continueWith(tarea -> {
            if (tarea.isSuccessful() && tarea.getResult() != null) {
                Log.d(TAG, "Texto traducido correctamente. Longitud resultado: " + tarea.getResult().length());
                return tarea.getResult();
            }

            Log.e(TAG, "Falló la traducción de un texto. Se conserva original.", tarea.getException());

            return textoOriginal;
        });
    }

    private String construirTextoDeteccion(DetalleClankViewModel.DetalleData datos) {
        StringBuilder texto = new StringBuilder();

        anyadirTexto(texto, datos.titulo);
        anyadirTexto(texto, datos.descripcion);

        if (datos.instrucciones != null) {
            for (Instruccion instruccion : datos.instrucciones) {
                if (instruccion != null) {
                    anyadirTexto(texto, instruccion.getInstruccion());
                }
            }
        }

        if (datos.materiales != null) {
            for (Material material : datos.materiales) {
                if (material != null) {
                    anyadirTexto(texto, material.getMaterial());
                }
            }
        }

        if (datos.herramientas != null) {
            for (Herramienta herramienta : datos.herramientas) {
                if (herramienta != null) {
                    anyadirTexto(texto, herramienta.getHerramienta());
                }
            }
        }

        return texto.toString().trim();
    }

    private void anyadirTexto(StringBuilder acumulador, String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return;
        }

        if (acumulador.length() > 0) {
            acumulador.append("\n");
        }

        acumulador.append(texto.trim());
    }
}