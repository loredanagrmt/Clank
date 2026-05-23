package com.clank.app.ui.ajustes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;
import com.clank.app.util.GestorTema;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

import io.qameta.allure.kotlin.Description;
import io.qameta.allure.kotlin.Epic;
import io.qameta.allure.kotlin.Feature;
import io.qameta.allure.kotlin.Severity;
import io.qameta.allure.kotlin.SeverityLevel;
import io.qameta.allure.kotlin.Story;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
@LargeTest
@Epic("UI Tests")
@Feature("Ajustes - tema")
public class AjustesTemaUITest {

    private static final long TIMEOUT_MS = 7000;
    private static final long INTERVALO_MS = 250;

    private static final String PREFERENCIAS_TEMA = "preferencias_tema";
    private static final String CLAVE_MODO_OSCURO = "modo_oscuro";

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    @Rule(order = 1)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 2)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    private ActivityScenario<MainActivity> escenario;

    @Before
    public void setUp() {
        hiltRule.inject();

        limpiarPreferenciasTema();

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
    }

    @After
    public void tearDown() {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarPreferenciasTema();

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void limpiarPreferenciasTema() {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFERENCIAS_TEMA,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .clear()
                .commit();
    }

    private boolean obtenerPreferenciaModoOscuro() {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFERENCIAS_TEMA,
                Context.MODE_PRIVATE
        );

        return preferencias.getBoolean(CLAVE_MODO_OSCURO, false);
    }

    private void guardarPreferenciaModoOscuro(boolean activado) {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFERENCIAS_TEMA,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .putBoolean(CLAVE_MODO_OSCURO, activado)
                .commit();
    }

    private void relanzarActivity() {
        if (escenario != null) {
            escenario.close();
        }

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(1000);
    }

    private void navegarAAjustes() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.ajustesFragment) {
                return;
            }

            navController.navigate(R.id.ajustesFragment);
        });

        esperar(800);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private SwitchCompat obtenerSwitchTema(MainActivity activity) {
        View vista = activity.findViewById(R.id.switch_tema_oscuro);

        assertTrue(
                "No se encontró switch_tema_oscuro o no es SwitchCompat.",
                vista instanceof SwitchCompat
        );

        return (SwitchCompat) vista;
    }

    private void pulsarSwitchTema() {
        escenario.onActivity(activity -> {
            SwitchCompat switchTema = obtenerSwitchTema(activity);

            assertTrue(
                    "switch_tema_oscuro debe estar habilitado antes de pulsarlo.",
                    switchTema.isEnabled()
            );

            switchTema.performClick();
        });

        esperar(700);
    }

    private void esperarHastaPreferencia(boolean esperado) {
        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            if (obtenerPreferenciaModoOscuro() == esperado) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "La preferencia modo_oscuro no alcanzó el valor esperado: " + esperado
        );
    }

    private void esperarHastaSwitchEstado(boolean esperado) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] correcto = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            correcto[0] = false;

            escenario.onActivity(activity -> {
                SwitchCompat switchTema = obtenerSwitchTema(activity);
                correcto[0] = switchTema.isChecked() == esperado;
            });

            if (correcto[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El switch de tema no alcanzó el estado esperado: " + esperado
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Ajustes")
    @Description("Al navegar a ajustes, el destino actual debe ser ajustesFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAAjustes_destinoCorrecto() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.ajustesFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura /////////////////////////

    @Test
    @Story("Ajustes")
    @Description("El switch de tema oscuro debe estar visible y habilitado.")
    @Severity(SeverityLevel.CRITICAL)
    public void switchTemaOscuro_estaVisibleYHabilitado() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            SwitchCompat switchTema = obtenerSwitchTema(activity);

            assertTrue(
                    "switch_tema_oscuro debe estar visible.",
                    switchTema.isShown()
            );

            assertTrue(
                    "switch_tema_oscuro debe estar habilitado.",
                    switchTema.isEnabled()
            );
        });
    }

    @Test
    @Story("Ajustes")
    @Description("Con preferencias limpias, el tema oscuro debe estar desactivado inicialmente.")
    @Severity(SeverityLevel.CRITICAL)
    public void temaOscuro_inicialmenteDesactivado() {
        navegarAAjustes();

        assertTrue(
                "La preferencia modo oscuro debe iniciar desactivada.",
                !obtenerPreferenciaModoOscuro()
        );

        escenario.onActivity(activity -> {
            SwitchCompat switchTema = obtenerSwitchTema(activity);

            assertTrue(
                    "El switch debe iniciar desactivado.",
                    !switchTema.isChecked()
            );

            assertTrue(
                    "GestorTema debe devolver modo oscuro desactivado.",
                    !GestorTema.obtenerModoOscuroGuardado(activity)
            );
        });
    }

    ///////////////////////// persistencia /////////////////////////

    @Test
    @Story("Tema oscuro")
    @Description("Al activar el switch, debe guardarse modo_oscuro=true en SharedPreferences.")
    @Severity(SeverityLevel.CRITICAL)
    public void activarTemaOscuro_guardaPreferencia() {
        navegarAAjustes();

        pulsarSwitchTema();

        esperarHastaPreferencia(true);

        assertTrue(
                "La preferencia modo_oscuro debe quedar activada.",
                obtenerPreferenciaModoOscuro()
        );
    }

    @Test
    @Story("Tema oscuro")
    @Description("Al desactivar el switch, debe guardarse modo_oscuro=false en SharedPreferences.")
    @Severity(SeverityLevel.CRITICAL)
    public void desactivarTemaOscuro_guardaPreferencia() {
        guardarPreferenciaModoOscuro(true);

        relanzarActivity();

        navegarAAjustes();
        esperarHastaSwitchEstado(true);

        pulsarSwitchTema();

        esperarHastaPreferencia(false);

        assertTrue(
                "La preferencia modo_oscuro debe quedar desactivada.",
                !obtenerPreferenciaModoOscuro()
        );
    }

    @Test
    @Story("Tema oscuro")
    @Description("Tras relanzar la Activity, el switch debe mantener el estado guardado.")
    @Severity(SeverityLevel.NORMAL)
    public void relanzarActivity_mantieneEstadoDelSwitch() {
        guardarPreferenciaModoOscuro(true);

        relanzarActivity();

        navegarAAjustes();

        esperarHastaSwitchEstado(true);

        escenario.onActivity(activity -> {
            SwitchCompat switchTema = obtenerSwitchTema(activity);

            assertTrue(
                    "Tras relanzar, el switch debe seguir activado.",
                    switchTema.isChecked()
            );

            assertTrue(
                    "GestorTema debe leer modo oscuro activado.",
                    GestorTema.obtenerModoOscuroGuardado(activity)
            );
        });
    }
}