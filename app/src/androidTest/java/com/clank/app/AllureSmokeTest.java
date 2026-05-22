package com.clank.app;

import static io.qameta.allure.kotlin.Allure.step;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.qameta.allure.android.runners.AllureAndroidJUnit4;

@RunWith(AllureAndroidJUnit4.class)
public class AllureSmokeTest {

    @Test
    public void pruebaMinimaAllure() {
        step("Paso minimo para comprobar que Allure genera resultados", contexto -> {
            // Test intencionadamente simple para validar la integracion de Allure.
            return null;
        });
    }
}