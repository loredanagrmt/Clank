package com.clank.app.ui.perfil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;

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
@Feature("Perfil propio")
public class PerfilPropioUITest {

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
        seeder.insertarCategoriaTest();
        seeder.insertarClankAutenticadoTest(0, false);

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

        seeder.eliminarLikeTest();
        seeder.eliminarClankTest();
        seeder.eliminarCategoriaTest();
        seeder.eliminarUsuarioAutenticadoFirestore();
    }

    private void navegarAPerfilPropio() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.perfilFragment) {
                return;
            }

            navController.navigate(R.id.perfilFragment);
        });

        esperar(1000);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void esperarHastaPerfilCargado() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cargado = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            cargado[0] = false;

            escenario.onActivity(activity -> {
                TextView nombre = activity.findViewById(R.id.tvNombrePerfil);

                if (nombre != null
                        && TestDataSeeder.TEST_NOMBRE.equals(nombre.getText().toString())) {
                    cargado[0] = true;
                }
            });

            if (cargado[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargaron los datos del perfil propio dentro del tiempo esperado."
        );
    }

    private void esperarHastaContadorClanks(String esperado) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] correcto = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            correcto[0] = false;

            escenario.onActivity(activity -> {
                TextView contador = activity.findViewById(R.id.tvNumClanks);

                if (contador != null
                        && esperado.equals(contador.getText().toString())) {
                    correcto[0] = true;
                }
            });

            if (correcto[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El contador de clanks del perfil propio no alcanzó el valor esperado: " + esperado
        );
    }

    private void esperarHastaAdapterConItems() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] tieneItems = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            tieneItems[0] = false;

            escenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvClanks);

                if (recyclerView != null
                        && recyclerView.getAdapter() != null
                        && recyclerView.getAdapter().getItemCount() > 0) {
                    tieneItems[0] = true;
                }
            });

            if (tieneItems[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El RecyclerView de perfil propio no recibió clanks dentro del tiempo esperado."
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("Al navegar al perfil sin usuarioId, debe cargarse el perfil del usuario autenticado.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAPerfilPropio_destinoCorrecto() {
        navegarAPerfilPropio();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.perfilFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// datos /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("El perfil propio debe mostrar el nombre del usuario autenticado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraNombreUsuario() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            TextView nombre = activity.findViewById(R.id.tvNombrePerfil);

            assertEquals(
                    TestDataSeeder.TEST_NOMBRE,
                    nombre.getText().toString()
            );
        });
    }

    @Test
    @Story("Perfil propio")
    @Description("El perfil propio debe mostrar el usuarioClank del usuario autenticado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraUsuarioClank() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            TextView usuarioClank = activity.findViewById(R.id.tvUidPerfil);

            assertEquals(
                    "@" + TestDataSeeder.TEST_USUARIO_CLANK.replace("@", "").trim(),
                    usuarioClank.getText().toString()
            );
        });
    }

    ///////////////////////// controles propios /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("En perfil propio, el botón de ajustes debe estar visible y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraBotonAjustes() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View ajustes = activity.findViewById(R.id.btnAjustes);

            assertNotNull(
                    "No se encontró btnAjustes.",
                    ajustes
            );

            assertTrue(
                    "En perfil propio, btnAjustes debe estar visible.",
                    ajustes.isShown()
            );

            assertTrue(
                    "En perfil propio, btnAjustes debe ser clicable.",
                    ajustes.isClickable()
            );
        });
    }

    @Test
    @Story("Perfil propio")
    @Description("En perfil propio, editar perfil debe estar visible y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraEditarPerfil() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View editar = activity.findViewById(R.id.tvEditarPerfil);

            assertNotNull(
                    "No se encontró tvEditarPerfil.",
                    editar
            );

            assertTrue(
                    "En perfil propio, tvEditarPerfil debe estar visible.",
                    editar.isShown()
            );

            assertTrue(
                    "En perfil propio, tvEditarPerfil debe ser clicable.",
                    editar.isClickable()
            );
        });
    }

    @Test
    @Story("Perfil propio")
    @Description("En perfil propio, la pestaña de bocetos debe estar visible y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraTabBocetos() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View tabBocetos = activity.findViewById(R.id.tabBocetos);

            assertNotNull(
                    "No se encontró tabBocetos.",
                    tabBocetos
            );

            assertTrue(
                    "En perfil propio, tabBocetos debe estar visible.",
                    tabBocetos.isShown()
            );

            assertTrue(
                    "En perfil propio, tabBocetos debe ser clicable.",
                    tabBocetos.isClickable()
            );
        });
    }

    ///////////////////////// clanks /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("El contador de clanks debe mostrar el clank sembrado del usuario autenticado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraContadorClanks() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaContadorClanks("1");

        escenario.onActivity(activity -> {
            TextView contador = activity.findViewById(R.id.tvNumClanks);

            assertEquals(
                    "1",
                    contador.getText().toString()
            );
        });
    }

    @Test
    @Story("Perfil propio")
    @Description("El RecyclerView debe recibir al menos un clank acabado del usuario autenticado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_muestraClankSembradoEnRecycler() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaAdapterConItems();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvClanks);

            assertTrue(
                    "El adapter debe tener al menos un clank.",
                    recyclerView.getAdapter() != null
                            && recyclerView.getAdapter().getItemCount() > 0
            );
        });
    }

    ///////////////////////// navegación desde perfil propio /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("Al pulsar editar perfil desde perfil propio, debe navegar a editarPerfilFragment.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfilPropio_editarPerfilNavegaCorrectamente() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View editar = activity.findViewById(R.id.tvEditarPerfil);

            assertNotNull(
                    "No se encontró tvEditarPerfil.",
                    editar
            );

            editar.performClick();
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
                    R.id.editarPerfilFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    @Test
    @Story("Perfil propio")
    @Description("Al pulsar ajustes desde perfil propio, debe navegar a ajustesFragment.")
    @Severity(SeverityLevel.NORMAL)
    public void perfilPropio_ajustesNavegaCorrectamente() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View ajustes = activity.findViewById(R.id.btnAjustes);

            assertNotNull(
                    "No se encontró btnAjustes.",
                    ajustes
            );

            ajustes.performClick();
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
                    R.id.ajustesFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }
}