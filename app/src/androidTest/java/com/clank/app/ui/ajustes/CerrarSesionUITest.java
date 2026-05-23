package com.clank.app.ui.ajustes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;
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
@Feature("Cerrar sesión")
public class CerrarSesionUITest {

    private static final long TIMEOUT_MS = 7000;
    private static final long INTERVALO_MS = 250;

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    @Rule(order = 1)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 2)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    private ActivityScenario<MainActivity> escenario;
    private TestDataSeeder seeder;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        hiltRule.inject();

        seeder = new TestDataSeeder();

        seeder.crearOIniciarSesionUsuarioAuthTest();

        limpiarDatosFirestore();

        seeder.insertarUsuarioAutenticadoTest();

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarDatosFirestore();

        if (seeder != null) {
            seeder.cerrarSesionAuthTest();
        }
    }

    private void limpiarDatosFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        if (seeder == null) {
            return;
        }

        seeder.eliminarUsuarioAutenticadoFirestore();
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

    private void esperarHastaDestinoInicioSesion() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] enInicioSesion = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            enInicioSesion[0] = false;

            escenario.onActivity(activity -> {
                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment);

                enInicioSesion[0] = navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == R.id.inicioSesionFragment;
            });

            if (enInicioSesion[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se navegó a inicioSesionFragment tras cerrar sesión."
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
    @Description("Las opciones principales de ajustes deben estar visibles.")
    @Severity(SeverityLevel.CRITICAL)
    public void opcionesAjustes_estanVisibles() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View lenguaje = activity.findViewById(R.id.btn_lenguaje);
            View cambiarContrasenya = activity.findViewById(R.id.btn_cambiar_contrasenya);
            View cerrarSesion = activity.findViewById(R.id.btn_cerrar_sesion);
            View borrarCuenta = activity.findViewById(R.id.btn_borrar_cuenta);
            View switchTema = activity.findViewById(R.id.switch_tema_oscuro);

            assertNotNull("No se encontró btn_lenguaje.", lenguaje);
            assertNotNull("No se encontró btn_cambiar_contrasenya.", cambiarContrasenya);
            assertNotNull("No se encontró btn_cerrar_sesion.", cerrarSesion);
            assertNotNull("No se encontró btn_borrar_cuenta.", borrarCuenta);
            assertNotNull("No se encontró switch_tema_oscuro.", switchTema);

            assertTrue("btn_lenguaje debe estar visible.", lenguaje.isShown());
            assertTrue("btn_cambiar_contrasenya debe estar visible.", cambiarContrasenya.isShown());
            assertTrue("btn_cerrar_sesion debe estar visible.", cerrarSesion.isShown());
            assertTrue("btn_borrar_cuenta debe estar visible.", borrarCuenta.isShown());
            assertTrue("switch_tema_oscuro debe estar visible.", switchTema.isShown());
        });
    }

    @Test
    @Story("Cerrar sesión")
    @Description("La opción cerrar sesión debe estar visible, habilitada y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonCerrarSesion_estaVisibleHabilitadoYClickable() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View cerrarSesion = activity.findViewById(R.id.btn_cerrar_sesion);

            assertNotNull(
                    "No se encontró btn_cerrar_sesion.",
                    cerrarSesion
            );

            assertTrue(
                    "btn_cerrar_sesion debe estar visible.",
                    cerrarSesion.isShown()
            );

            assertTrue(
                    "btn_cerrar_sesion debe estar habilitado.",
                    cerrarSesion.isEnabled()
            );

            assertTrue(
                    "btn_cerrar_sesion debe ser clicable.",
                    cerrarSesion.isClickable()
            );
        });
    }

    ///////////////////////// navegación relacionada /////////////////////////

    @Test
    @Story("Ajustes")
    @Description("Al pulsar cambiar contraseña desde ajustes, debe navegar a cambiarContrasenyaFragment.")
    @Severity(SeverityLevel.NORMAL)
    public void cambiarContrasenya_navegaCorrectamente() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View cambiarContrasenya = activity.findViewById(R.id.btn_cambiar_contrasenya);

            assertNotNull(
                    "No se encontró btn_cambiar_contrasenya.",
                    cambiarContrasenya
            );

            cambiarContrasenya.performClick();
        });

        esperar(700);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.cambiarContrasenyaFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// cierre real /////////////////////////

    @Test
    @Story("Cerrar sesión")
    @Description("Al pulsar cerrar sesión, FirebaseAuth debe quedarse sin usuario y navegar a inicio de sesión.")
    @Severity(SeverityLevel.BLOCKER)
    public void cerrarSesion_cierraAuthYNavegaAInicioSesion() {
        navegarAAjustes();

        assertNotNull(
                "Antes de cerrar sesión debe haber usuario autenticado.",
                FirebaseAuth.getInstance().getCurrentUser()
        );

        escenario.onActivity(activity -> {
            View cerrarSesion = activity.findViewById(R.id.btn_cerrar_sesion);

            assertNotNull(
                    "No se encontró btn_cerrar_sesion.",
                    cerrarSesion
            );

            cerrarSesion.performClick();
        });

        esperarHastaDestinoInicioSesion();

        assertNull(
                "Después de cerrar sesión FirebaseAuth no debe tener usuario actual.",
                FirebaseAuth.getInstance().getCurrentUser()
        );

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.inicioSesionFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }
}