package com.clank.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class GestorIdioma {

    private static final String PREFS_NAME = "clank_prefs";
    private static final String KEY_IDIOMA = "idioma_seleccionado";
    private static final String IDIOMA_DEFAULT = "es";

    private static GestorIdioma instancia;
    private final SharedPreferences prefs;

    private GestorIdioma(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static GestorIdioma getInstance(Context context) {
        if (instancia == null) {
            instancia = new GestorIdioma(context);
        }
        return instancia;
    }

    public void aplicarIdioma(String codigoIdioma) {
        prefs.edit().putString(KEY_IDIOMA, codigoIdioma).apply();
        LocaleListCompat localeList = LocaleListCompat.forLanguageTags(codigoIdioma);
        AppCompatDelegate.setApplicationLocales(localeList);
    }

    public String getIdiomaActual() {
        return prefs.getString(KEY_IDIOMA, IDIOMA_DEFAULT);
    }

    public Locale getLocaleActual() {
        return new Locale(getIdiomaActual());
    }

    public void aplicarIdiomaGuardado() {
        String idioma = getIdiomaActual();
        LocaleListCompat localeList = LocaleListCompat.forLanguageTags(idioma);
        AppCompatDelegate.setApplicationLocales(localeList);
    }
}