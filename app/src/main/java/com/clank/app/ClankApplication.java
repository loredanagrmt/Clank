package com.clank.app;

import android.app.Application;

import com.clank.app.util.GestorTema;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ClankApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        GestorTema.aplicarTemaGuardado(this);
    }

}
