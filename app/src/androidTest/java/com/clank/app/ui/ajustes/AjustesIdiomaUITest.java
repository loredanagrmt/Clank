package com.clank.app.ui.ajustes;

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
@Feature("Ajustes - idioma")
public class AjustesIdiomaUITest {

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

    private void guardarIdiomaSinAplicarLocale(String codigoIdioma) {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .putString(KEY_IDIOMA, codigoIdioma)
                .commit();
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

    private void pulsarBotonIdioma() {
        escenario.onActivity(activity -> {
            View botonIdioma = activity.findViewById(R.id.btn_lenguaje);

            assertNotNull(
                    "No se encontró btn_lenguaje.",
                    botonIdioma
            );

            assertTrue(
                    "btn_lenguaje debe estar visible.",
                    botonIdioma.isShown()
            );

            assertTrue(
                    "btn_lenguaje debe ser clicable.",
                    botonIdioma.isClickable()
            );

            botonIdioma.performClick();
        });

        esperar(700);
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
    @Description("La opción de idioma debe estar visible, habilitada y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonIdioma_estaVisibleHabilitadoYClickable() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View botonIdioma = activity.findViewById(R.id.btn_lenguaje);

            assertNotNull(
                    "No se encontró btn_lenguaje.",
                    botonIdioma
            );

            assertTrue(
                    "btn_lenguaje debe estar visible.",
                    botonIdioma.isShown()
            );

            assertTrue(
                    "btn_lenguaje debe estar habilitado.",
                    botonIdioma.isEnabled()
            );

            assertTrue(
                    "btn_lenguaje debe ser clicable.",
                    botonIdioma.isClickable()
            );
        });
    }

    @Test
    @Story("Idioma")
    @Description("Con preferencias limpias, GestorIdioma debe devolver español como idioma por defecto.")
    @Severity(SeverityLevel.CRITICAL)
    public void idiomaPorDefecto_esEspanol() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            String idiomaActual = GestorIdioma.getInstance(activity).getIdiomaActual();

            assertEquals(
                    "es",
                    idiomaActual
            );
        });

        assertEquals(
                "es",
                obtenerIdiomaGuardado()
        );
    }

    ///////////////////////// hoja de idiomas /////////////////////////

    @Test
    @Story("Idioma")
    @Description("Al pulsar idioma, debe mostrarse la hoja de opciones con la lista de idiomas.")
    @Severity(SeverityLevel.CRITICAL)
    public void pulsarIdioma_muestraHojaIdiomas() {
        navegarAAjustes();

        pulsarBotonIdioma();

        onView(withId(R.id.tituloPanelOpciones))
                .check(matches(isDisplayed()));

        onView(withId(R.id.listaOpciones))
                .check(matches(isDisplayed()));

        onView(withText("Español"))
                .check(matches(isDisplayed()));

        onView(withText("English"))
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Idioma")
    @Description("La hoja de idioma debe mostrar las opciones de idioma disponibles.")
    @Severity(SeverityLevel.NORMAL)
    public void hojaIdiomas_muestraOpcionesDisponibles() {
        navegarAAjustes();

        pulsarBotonIdioma();

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

    ///////////////////////// persistencia /////////////////////////

    @Test
    @Story("Idioma")
    @Description("Al seleccionar English desde ajustes, debe guardarse el código en.")
    @Severity(SeverityLevel.CRITICAL)
    public void seleccionarEnglish_guardaIdiomaEn() {
        navegarAAjustes();

        pulsarBotonIdioma();

        onView(withText("English"))
                .check(matches(isDisplayed()))
                .perform(click());

        esperarHastaIdiomaGuardado("en");

        assertEquals(
                "en",
                obtenerIdiomaGuardado()
        );
    }

    @Test
    @Story("Idioma")
    @Description("Al seleccionar Français desde ajustes, debe guardarse el código fr.")
    @Severity(SeverityLevel.CRITICAL)
    public void seleccionarFrances_guardaIdiomaFr() {
        navegarAAjustes();

        pulsarBotonIdioma();

        onView(withText("Français"))
                .check(matches(isDisplayed()))
                .perform(click());

        esperarHastaIdiomaGuardado("fr");

        assertEquals(
                "fr",
                obtenerIdiomaGuardado()
        );
    }

    @Test
    @Story("Idioma")
    @Description("Si ya hay un idioma guardado, GestorIdioma debe devolver ese idioma.")
    @Severity(SeverityLevel.NORMAL)
    public void gestorIdioma_devuelveIdiomaGuardado() {
        guardarIdiomaSinAplicarLocale("it");

        escenario.onActivity(activity -> {
            String idiomaActual = GestorIdioma.getInstance(activity).getIdiomaActual();

            assertEquals(
                    "it",
                    idiomaActual
            );
        });

        assertEquals(
                "it",
                obtenerIdiomaGuardado()
        );
    }
}