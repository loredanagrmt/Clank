package com.clank.app.ui.idioma;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
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
import com.clank.app.util.GestorIdioma;

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
@Feature("Elegir idioma inicial")
public class ElegirIdiomaUITest {

    private static final long TIMEOUT_MS = 7000;
    private static final long INTERVALO_MS = 250;

    private static final String PREFS_NAME = "clank_prefs";
    private static final String KEY_IDIOMA = "idioma_seleccionado";

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

        limpiarPreferenciasIdioma();

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("es")
        );

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(800);
    }

    @After
    public void tearDown() {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarPreferenciasIdioma();

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("es")
        );
    }

    private void limpiarPreferenciasIdioma() {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .clear()
                .commit();
    }

    private String obtenerIdiomaGuardado() {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        return preferencias.getString(KEY_IDIOMA, "es");
    }

    private void navegarAElegirIdioma() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.elegirIdiomaFragment) {
                return;
            }

            navController.navigate(R.id.elegirIdiomaFragment);
        });

        esperar(900);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void esperarHastaIdiomaGuardado(String codigoEsperado) {
        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            if (codigoEsperado.equals(obtenerIdiomaGuardado())) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El idioma guardado no alcanzó el valor esperado: " + codigoEsperado
        );
    }

    private void esperarHastaDestinoInspirar() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] enInspirar = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            enInspirar[0] = false;

            escenario.onActivity(activity -> {
                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment);

                enInspirar[0] = navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == R.id.inspirarFragment;
            });

            if (enInspirar[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se navegó a inspirarFragment tras seleccionar idioma."
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Elegir idioma")
    @Description("Al navegar a elegir idioma, el destino actual debe ser elegirIdiomaFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAElegirIdioma_destinoCorrecto() {
        navegarAElegirIdioma();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.elegirIdiomaFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// hoja automática /////////////////////////

    @Test
    @Story("Elegir idioma")
    @Description("Al entrar en elegir idioma, debe mostrarse automáticamente la hoja de idiomas.")
    @Severity(SeverityLevel.CRITICAL)
    public void entrarElegirIdioma_muestraHojaIdiomas() {
        navegarAElegirIdioma();

        onView(withId(R.id.tituloPanelOpciones))
                .check(matches(isDisplayed()));

        onView(withId(R.id.listaOpciones))
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Elegir idioma")
    @Description("La hoja inicial debe mostrar el título siempre en español.")
    @Severity(SeverityLevel.CRITICAL)
    public void hojaIdiomas_muestraTituloEnEspanol() {
        navegarAElegirIdioma();

        onView(withText("Elige tu idioma"))
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Elegir idioma")
    @Description("La hoja inicial debe mostrar las opciones de idioma disponibles.")
    @Severity(SeverityLevel.CRITICAL)
    public void hojaIdiomas_muestraOpcionesDisponibles() {
        navegarAElegirIdioma();

        onView(withText("Español"))
                .check(matches(isDisplayed()));

        onView(withText("English"))
                .check(matches(isDisplayed()));

        onView(withText("Français"))
                .check(matches(isDisplayed()));

        onView(withText("Deutsch"))
                .check(matches(isDisplayed()));

        onView(withText("Português"))
                .check(matches(isDisplayed()));

        onView(withText("Italiano"))
                .check(matches(isDisplayed()));
    }

    ///////////////////////// selección y navegación /////////////////////////

    @Test
    @Story("Elegir idioma")
    @Description("Al seleccionar English, debe guardarse en y navegar a inspirar.")
    @Severity(SeverityLevel.BLOCKER)
    public void seleccionarEnglish_guardaIdiomaYNavegaAInspirar() {
        navegarAElegirIdioma();

        onView(withText("English"))
                .check(matches(isDisplayed()))
                .perform(click());

        esperarHastaIdiomaGuardado("en");
        esperarHastaDestinoInspirar();

        assertEquals(
                "en",
                obtenerIdiomaGuardado()
        );

        escenario.onActivity(activity -> {
            String idiomaActual =
                    GestorIdioma.getInstance(activity).getIdiomaActual();

            assertEquals(
                    "en",
                    idiomaActual
            );
        });
    }

    @Test
    @Story("Elegir idioma")
    @Description("Al seleccionar Français, debe guardarse fr y navegar a inspirar.")
    @Severity(SeverityLevel.BLOCKER)
    public void seleccionarFrances_guardaIdiomaYNavegaAInspirar() {
        navegarAElegirIdioma();

        onView(withText("Français"))
                .check(matches(isDisplayed()))
                .perform(click());

        esperarHastaIdiomaGuardado("fr");
        esperarHastaDestinoInspirar();

        assertEquals(
                "fr",
                obtenerIdiomaGuardado()
        );

        escenario.onActivity(activity -> {
            String idiomaActual =
                    GestorIdioma.getInstance(activity).getIdiomaActual();

            assertEquals(
                    "fr",
                    idiomaActual
            );
        });
    }

    @Test
    @Story("Elegir idioma")
    @Description("Si no se selecciona idioma, la preferencia debe permanecer en español por defecto.")
    @Severity(SeverityLevel.NORMAL)
    public void sinSeleccion_mantieneEspanolPorDefecto() {
        navegarAElegirIdioma();

        assertEquals(
                "es",
                obtenerIdiomaGuardado()
        );

        escenario.onActivity(activity -> {
            assertEquals(
                    "es",
                    GestorIdioma.getInstance(activity).getIdiomaActual()
            );
        });
    }
}