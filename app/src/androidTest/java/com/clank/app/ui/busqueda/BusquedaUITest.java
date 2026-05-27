package com.clank.app.ui.busqueda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;

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
@Feature("Búsqueda")
public class BusquedaUITest {

    private static final long TIMEOUT_MS = 30000;
    private static final long INTERVALO_MS = 300;

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

        limpiarDatos();

        seeder.insertarUsuarioTest();
        seeder.insertarCategoriaTest();
        seeder.insertarClankTest(0, false);

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(800);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarDatos();
    }

    private void limpiarDatos() throws ExecutionException, InterruptedException, TimeoutException {
        if (seeder == null) {
            return;
        }

        seeder.eliminarLikeTest();
        seeder.eliminarClankTest();
        seeder.eliminarCategoriaTest();
        seeder.eliminarUsuariosConUsuarioClankTest();
    }

    private void navegarABusqueda() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.busquedaFragment) {
                return;
            }

            navController.navigate(R.id.busquedaFragment);
        });

        esperar(900);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void escribirBusqueda(String texto) {
        escenario.onActivity(activity -> {
            EditText campo = activity.findViewById(R.id.etBusqueda);

            assertNotNull(
                    "No se encontró etBusqueda.",
                    campo
            );

            campo.requestFocus();
            campo.setText("");
            campo.setText(texto);
            campo.setSelection(campo.getText().length());
        });

        esperar(700);
    }

    private int obtenerItemCountRecycler() {
        final int[] cantidad = new int[1];

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvBusqueda);

            assertNotNull(
                    "No se encontró rvBusqueda.",
                    recyclerView
            );

            assertNotNull(
                    "rvBusqueda debe tener adapter.",
                    recyclerView.getAdapter()
            );

            cantidad[0] = recyclerView.getAdapter().getItemCount();
        });

        return cantidad[0];
    }

    private void esperarHastaResultados(int minimoResultados) {
        long inicio = SystemClock.elapsedRealtime();
        int ultimaCantidad = 0;

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            ultimaCantidad = obtenerItemCountRecycler();

            if (ultimaCantidad >= minimoResultados) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No aparecieron resultados de búsqueda dentro del tiempo esperado. "
                        + "Última cantidad en RecyclerView: " + ultimaCantidad
        );
    }

    private void esperarHastaSinResultadosVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View textoVacio = activity.findViewById(R.id.tvBusquedaVacio);

                assertNotNull(
                        "No se encontró tvBusquedaVacio.",
                        textoVacio
                );

                visible[0] = textoVacio.isShown();
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció el estado de búsqueda sin resultados dentro del tiempo esperado."
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Carga de búsqueda")
    @Description("Al navegar a búsqueda, el destino actual debe ser busquedaFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarABusqueda_destinoCorrecto() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.busquedaFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Estructura de búsqueda")
    @Description("La barra de búsqueda debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void barraBusqueda_estaVisible() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            View vista = activity.findViewById(R.id.barraBusqueda);

            assertNotNull(
                    "No se encontró barraBusqueda.",
                    vista
            );

            assertTrue(
                    "barraBusqueda debe estar visible.",
                    vista.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de búsqueda")
    @Description("El botón volver debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void botonVolver_estaVisible() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            View boton = activity.findViewById(R.id.btnBusquedaVolver);

            assertNotNull(
                    "No se encontró btnBusquedaVolver.",
                    boton
            );

            assertTrue(
                    "btnBusquedaVolver debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de búsqueda")
    @Description("El campo de búsqueda debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoBusqueda_estaVisible() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            EditText campo = activity.findViewById(R.id.etBusqueda);

            assertNotNull(
                    "No se encontró etBusqueda.",
                    campo
            );

            assertTrue(
                    "etBusqueda debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de búsqueda")
    @Description("El RecyclerView de resultados debe existir y tener adapter.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerBusqueda_tieneAdapter() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvBusqueda);

            assertNotNull(
                    "No se encontró rvBusqueda.",
                    recyclerView
            );

            assertNotNull(
                    "rvBusqueda debe tener LayoutManager.",
                    recyclerView.getLayoutManager()
            );

            assertNotNull(
                    "rvBusqueda debe tener Adapter.",
                    recyclerView.getAdapter()
            );
        });
    }

    @Test
    @Story("Estructura de búsqueda")
    @Description("El ProgressBar y el texto de estado vacío deben existir.")
    @Severity(SeverityLevel.NORMAL)
    public void estadosBusqueda_existen() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            View progreso = activity.findViewById(R.id.progressBusqueda);
            View vacio = activity.findViewById(R.id.tvBusquedaVacio);

            assertNotNull(
                    "No se encontró progressBusqueda.",
                    progreso
            );

            assertNotNull(
                    "No se encontró tvBusquedaVacio.",
                    vacio
            );
        });
    }

    ///////////////////////// interacción /////////////////////////

    @Test
    @Story("Campo de búsqueda")
    @Description("Al escribir en el campo de búsqueda, el texto debe conservarse.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoBusqueda_escribirTexto_muestraTextoEscrito() {
        navegarABusqueda();

        escribirBusqueda("lana");

        escenario.onActivity(activity -> {
            EditText campo = activity.findViewById(R.id.etBusqueda);

            assertNotNull(
                    "No se encontró etBusqueda.",
                    campo
            );

            assertEquals(
                    "lana",
                    campo.getText().toString()
            );
        });
    }

    @Test
    @Story("Resultados de búsqueda")
    @Description("Al buscar por el título del clank sembrado, debe aparecer al menos un resultado.")
    @Severity(SeverityLevel.CRITICAL)
    public void buscarPorTitulo_muestraResultados() {
        navegarABusqueda();

        escribirBusqueda(TestDataSeeder.TEST_CLANK_TITULO);

        esperarHastaResultados(1);

        assertTrue(
                "La búsqueda por título debe devolver al menos un resultado.",
                obtenerItemCountRecycler() > 0
        );
    }

    @Test
    @Story("Resultados de búsqueda")
    @Description("Al buscar por descripción del clank sembrado, debe aparecer al menos un resultado.")
    @Severity(SeverityLevel.NORMAL)
    public void buscarPorDescripcion_muestraResultados() {
        navegarABusqueda();

        escribirBusqueda("Descripción de prueba");

        esperarHastaResultados(1);

        assertTrue(
                "La búsqueda por descripción debe devolver al menos un resultado.",
                obtenerItemCountRecycler() > 0
        );
    }

    @Test
    @Story("Resultados de búsqueda")
    @Description("Al buscar por usuarioClank del usuario sembrado, debe aparecer al menos un resultado.")
    @Severity(SeverityLevel.NORMAL)
    public void buscarPorUsuario_muestraResultados() {
        navegarABusqueda();

        escribirBusqueda(TestDataSeeder.TEST_USUARIO_CLANK);

        esperarHastaResultados(1);

        assertTrue(
                "La búsqueda por usuario debe devolver al menos un resultado.",
                obtenerItemCountRecycler() > 0
        );
    }

    @Test
    @Story("Resultados de búsqueda")
    @Description("Al buscar un término inexistente, debe mostrarse el estado sin resultados.")
    @Severity(SeverityLevel.NORMAL)
    public void buscarTerminoInexistente_muestraSinResultados() {
        navegarABusqueda();

        escribirBusqueda("termino_que_no_deberia_existir_999");

        esperarHastaSinResultadosVisible();

        escenario.onActivity(activity -> {
            View textoVacio = activity.findViewById(R.id.tvBusquedaVacio);
            View recycler = activity.findViewById(R.id.rvBusqueda);

            assertTrue(
                    "tvBusquedaVacio debe estar visible cuando no hay resultados.",
                    textoVacio.isShown()
            );

            assertTrue(
                    "rvBusqueda debe ocultarse cuando no hay resultados.",
                    recycler.getVisibility() == View.GONE
            );
        });
    }

    @Test
    @Story("Navegación de búsqueda")
    @Description("Al pulsar volver, la pantalla debe salir de busquedaFragment.")
    @Severity(SeverityLevel.NORMAL)
    public void botonVolver_saleDeBusqueda() {
        navegarABusqueda();

        escenario.onActivity(activity -> {
            View boton = activity.findViewById(R.id.btnBusquedaVolver);

            assertNotNull(
                    "No se encontró btnBusquedaVolver.",
                    boton
            );

            boton.performClick();
        });

        esperar(700);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertTrue(
                    "El destino actual no debe seguir siendo busquedaFragment.",
                    navController.getCurrentDestination().getId() != R.id.busquedaFragment
            );
        });
    }
}