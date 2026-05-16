package com.clank.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public final class GestorTema {

    private static final String PREFERENCIAS_TEMA = "preferencias_tema";
    private static final String CLAVE_MODO_OSCURO = "modo_oscuro";

    private GestorTema() {
    }

    public static void aplicarTemaGuardado(Context contexto) {
        boolean modoOscuro = obtenerModoOscuroGuardado(contexto);

        AppCompatDelegate.setDefaultNightMode(
                modoOscuro
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean obtenerModoOscuroGuardado(Context contexto) {
        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFERENCIAS_TEMA,
                Context.MODE_PRIVATE
        );

        return preferencias.getBoolean(CLAVE_MODO_OSCURO, false);
    }

    public static void cambiarModoOscuro(Context contexto, boolean activarModoOscuro) {
        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFERENCIAS_TEMA,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .putBoolean(CLAVE_MODO_OSCURO, activarModoOscuro)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                activarModoOscuro
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}