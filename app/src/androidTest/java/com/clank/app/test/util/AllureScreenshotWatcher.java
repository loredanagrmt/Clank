package com.clank.app.test.util;

import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.ByteArrayOutputStream;

import io.qameta.allure.kotlin.Attachment;

public class AllureScreenshotWatcher extends TestWatcher {

    @Override
    protected void succeeded(Description description) {
        capturarYAdjuntar("PASS", description);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        capturarYAdjuntar("FAIL", description);
    }

    private void capturarYAdjuntar(String estado, Description description) {
        try {
            Bitmap captura = InstrumentationRegistry
                    .getInstrumentation()
                    .getUiAutomation()
                    .takeScreenshot();

            if (captura == null) {
                return;
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            captura.compress(Bitmap.CompressFormat.PNG, 100, salida);
            captura.recycle();

            String nombre = estado + " - "
                    + description.getClassName()
                    + "."
                    + description.getMethodName();

            adjuntarCaptura(nombre, salida.toByteArray());

        } catch (Exception ignored) {
        }
    }

    @Attachment(value = "{nombre}", type = "image/png", fileExtension = ".png")
    private byte[] adjuntarCaptura(String nombre, byte[] captura) {
        return captura;
    }
}