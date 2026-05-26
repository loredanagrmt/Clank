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

    private final Context contextoAplicacion;
    private final LanguageIdentifier identificador;

    @Inject
    public TraductorDetalleClank(@ApplicationContext Context contextoAplicacion) {
        this.contextoAplicacion = contextoAplicacion;
      this.identificador = LanguageIdentification.getClient();
    }

    public Task<Boolean> traducirSiProcede(DetalleClankViewModel.DetalleData datos) {
        if (datos == null) {
            return Tasks.forResult(false);
        }

        String textoParaDetectarIdioma = construirTextoDeteccion(datos);


        if (textoParaDetectarIdioma.trim().isEmpty()) {
            return Tasks.forResult(false);
        }

        String etiquetaIdiomaDestino = GestorIdioma.getInstance(contextoAplicacion).getIdiomaActual();

        String idiomaDestino = TranslateLanguage.fromLanguageTag(etiquetaIdiomaDestino);

        if (idiomaDestino == null) {
            return Tasks.forResult(false);
        }

        LanguageIdentifier identificador = LanguageIdentification.getClient();

        return identificador.identifyLanguage(textoParaDetectarIdioma).continueWithTask(tareaDeteccion -> {

            if (!tareaDeteccion.isSuccessful()) {
                return Tasks.forResult(false);
            }

            String etiquetaIdiomaOrigen = tareaDeteccion.getResult();


            if (etiquetaIdiomaOrigen == null || "und".equalsIgnoreCase(etiquetaIdiomaOrigen)) {
                return Tasks.forResult(false);
            }

            String idiomaOrigen = TranslateLanguage.fromLanguageTag(etiquetaIdiomaOrigen);


            if (idiomaOrigen == null) {
                return Tasks.forResult(false);
            }

            if (idiomaOrigen.equals(idiomaDestino)) {
                return Tasks.forResult(false);
            }


            return traducirDetalle(datos, idiomaOrigen, idiomaDestino);
        });
    }

    private Task<Boolean> traducirDetalle(DetalleClankViewModel.DetalleData datos, String idiomaOrigen, String idiomaDestino) {

        TranslatorOptions opciones = new TranslatorOptions.Builder().setSourceLanguage(idiomaOrigen).setTargetLanguage(idiomaDestino).build();

        Translator traductor = Translation.getClient(opciones);

        DownloadConditions condiciones = new DownloadConditions.Builder().build();


        return traductor.downloadModelIfNeeded(condiciones).continueWithTask(tareaDescarga -> {
            if (!tareaDescarga.isSuccessful()) {
                traductor.close();
                return Tasks.forResult(false);
            }

            return traducirCampos(datos, traductor).continueWith(tareaTraduccion -> {

                if (!tareaTraduccion.isSuccessful()) {
                }

                traductor.close();

                return tareaTraduccion.isSuccessful();
            });
        });
    }

    private Task<Void> traducirCampos(DetalleClankViewModel.DetalleData datos, Translator traductor) {
        List<Task<Void>> tareas = new ArrayList<>();

        tareas.add(traducirTextoSeguro(traductor, datos.titulo).continueWith(tarea -> {
            datos.titulo = tarea.getResult();
            return (Void) null;
        }));

        tareas.add(traducirTextoSeguro(traductor, datos.descripcion).continueWith(tarea -> {
            datos.descripcion = tarea.getResult();
            return (Void) null;
        }));

        if (datos.materiales != null) {

            for (Material material : datos.materiales) {
                if (material == null) continue;

                tareas.add(traducirTextoSeguro(traductor, material.getMaterial()).continueWith(tarea -> {
                    material.setMaterial(tarea.getResult());
                    return (Void) null;
                }));
            }
        }

        if (datos.herramientas != null) {

            for (Herramienta herramienta : datos.herramientas) {
                if (herramienta == null) continue;

                tareas.add(traducirTextoSeguro(traductor, herramienta.getHerramienta()).continueWith(tarea -> {
                    herramienta.setHerramienta(tarea.getResult());
                    return (Void) null;
                }));
            }
        }

        if (datos.instrucciones != null) {

            for (Instruccion instruccion : datos.instrucciones) {
                if (instruccion == null) continue;

                tareas.add(traducirTextoSeguro(traductor, instruccion.getInstruccion()).continueWith(tarea -> {
                    instruccion.setInstruccion(tarea.getResult());
                    return (Void) null;
                }));
            }
        }


      return Tasks.whenAll(tareas);
    }

    private Task<String> traducirTextoSeguro(Translator traductor, String texto) {
        String textoOriginal = texto != null ? texto : "";

        if (textoOriginal.trim().isEmpty()) {
            return Tasks.forResult(textoOriginal);
        }


        return traductor.translate(textoOriginal).continueWith(tarea -> {
            if (tarea.isSuccessful() && tarea.getResult() != null) {
                return tarea.getResult();
            }


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
  public void cerrar() {
    identificador.close();
  }

}
