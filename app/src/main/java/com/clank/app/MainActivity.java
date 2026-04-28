package com.clank.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.clank.app.util.GestorIdioma;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GestorIdioma.getInstance(this).aplicarIdiomaGuardado();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}