package com.clank.app.ui.logo;

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
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;
import com.clank.app.util.GestorIdioma;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
@Feature("Arranque de sesión")
public class ArranqueSesionUITest {

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
    private TestDataSeeder seeder;

    @Before
    public void setUp() {
        hiltRule.inject();

        seeder = new TestDataSeeder();

        cerrarSesion();
        limpiarPreferenciasIdioma();

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("es")
        );
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        if (seeder != null) {
            seeder.eliminarUsuarioAutenticadoFirestore();
            seeder.cerrarSesionAuthTest();
        }

        cerrarSesion();
        limpiarPreferenciasIdioma();

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("es")
        );
    }

    private void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
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

    private void guardarIdioma(String codigoIdioma) {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        preferencias.edit()
                .putString(KEY_IDIOMA, codigoIdioma)
                .commit();

        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(codigoIdioma)
        );
    }

    private String obtenerIdiomaGuardado() {
        Context contexto = ApplicationProvider.getApplicationContext();

        SharedPreferences preferencias = contexto.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );

        return preferencias.getString(KEY_IDIOMA, "es");
    }

    private void lanzarActivity() {
        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(300);
    }

    private void prepararSesionAutenticada()
            throws ExecutionException, InterruptedException, TimeoutException {

        seeder.crearOIniciarSesionUsuarioAuthTest();
        seeder.insertarUsuarioAutenticadoTest();
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private int obtenerDestinoActual() {
        final int[] destino = new int[]{-1};

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null) {
                destino[0] = navController.getCurrentDestination().getId();
            }
        });

        return destino[0];
    }

    private void esperarHastaDestino(int destinoEsperado) {
        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            if (obtenerDestinoActual() == destinoEsperado) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se llegó al destino esperado: " + destinoEsperado
                        + ". Destino actual: " + obtenerDestinoActual()
        );
    }

    private void comprobarVistaOculta(int viewId, String nombreVista) {
        escenario.onActivity(activity -> {
            View vista = activity.findViewById(viewId);

            assertNotNull(
                    "No se encontró " + nombreVista + ".",
                    vista
            );

            assertTrue(
                    nombreVista + " debe estar oculta.",
                    vista.getVisibility() != View.VISIBLE
            );
        });
    }

    private void comprobarVistaVisible(int viewId, String nombreVista) {
        escenario.onActivity(activity -> {
            View vista = activity.findViewById(viewId);

            assertNotNull(
                    "No se encontró " + nombreVista + ".",
                    vista
            );

            assertTrue(
                    nombreVista + " debe estar visible.",
                    vista.getVisibility() == View.VISIBLE
            );
        });
    }

    ///////////////////////// estado inicial /////////////////////////

    @Test
    @Story("Arranque")
    @Description("Al lanzar la app sin sesión, debe iniciar el flujo público y terminar en elegirIdiomaFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void lanzarAppSinSesion_iniciaFlujoPublico() {
        cerrarSesion();

        lanzarActivity();

        esperarHastaDestino(R.id.elegirIdiomaFragment);

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

    @Test
    @Story("Arranque")
    @Description("En logoFragment deben estar ocultas navbar y bottom bar.")
    @Severity(SeverityLevel.CRITICAL)
    public void logo_ocultaNavbarYBottomBar() {
        lanzarActivity();

        comprobarVistaOculta(R.id.navbar, "navbar");
        comprobarVistaOculta(R.id.frameBottomBar, "frameBottomBar");
    }

    ///////////////////////// sin sesión /////////////////////////

    @Test
    @Story("Arranque sin sesión")
    @Description("Si no hay sesión iniciada, LogoFragment debe navegar a elegirIdiomaFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void sinSesion_navegaAElegirIdioma() {
        cerrarSesion();

        lanzarActivity();

        esperarHastaDestino(R.id.elegirIdiomaFragment);
    }

    @Test
    @Story("Arranque sin sesión")
    @Description("En elegirIdiomaFragment, la bottom bar debe seguir oculta.")
    @Severity(SeverityLevel.CRITICAL)
    public void elegirIdioma_ocultaBottomBar() {
        cerrarSesion();

        lanzarActivity();

        esperarHastaDestino(R.id.elegirIdiomaFragment);

        comprobarVistaOculta(R.id.frameBottomBar, "frameBottomBar");
    }

    ///////////////////////// con sesión /////////////////////////

    @Test
    @Story("Arranque con sesión")
    @Description("Si hay sesión iniciada, LogoFragment debe navegar directamente a feedFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void conSesion_navegaAFeed()
            throws ExecutionException, InterruptedException, TimeoutException {

        prepararSesionAutenticada();

        lanzarActivity();

        esperarHastaDestino(R.id.feedFragment);
    }

    @Test
    @Story("Arranque con sesión")
    @Description("Al llegar al feed desde sesión iniciada, la bottom bar debe mostrarse.")
    @Severity(SeverityLevel.CRITICAL)
    public void feed_muestraBottomBarConSesion()
            throws ExecutionException, InterruptedException, TimeoutException {

        prepararSesionAutenticada();

        lanzarActivity();

        esperarHastaDestino(R.id.feedFragment);

        comprobarVistaVisible(R.id.frameBottomBar, "frameBottomBar");
    }

    ///////////////////////// idioma guardado /////////////////////////

    @Test
    @Story("Arranque con idioma")
    @Description("Si hay un idioma guardado, GestorIdioma debe mantenerlo al arrancar.")
    @Severity(SeverityLevel.NORMAL)
    public void arranque_mantieneIdiomaGuardado() {
        guardarIdioma("it");

        lanzarActivity();

        assertEquals(
                "it",
                obtenerIdiomaGuardado()
        );

        escenario.onActivity(activity -> {
            String idiomaActual =
                    GestorIdioma.getInstance(activity).getIdiomaActual();

            assertEquals(
                    "it",
                    idiomaActual
            );
        });
    }
}