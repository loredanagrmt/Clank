package com.clank.app.ui.resultados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
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
@Feature("Resultados")
public class ResultadosUITest {

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

        limpiarDatos();
        seeder.insertarUsuarioTest();
        seeder.insertarCategoriaTest();
        seeder.insertarClankTest(0, false);

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
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
        seeder.eliminarUsuarioFirestore();
    }

    private void navegarAResultados() {
        navegarAResultadosConCategoria(
                TestDataSeeder.TEST_CATEGORIA_ID,
                TestDataSeeder.TEST_CATEGORIA_NOMBRE
        );
    }

    private void navegarAResultadosConCategoria(String categoriaId,
                                                String nombreCategoria) {
        Bundle args = new Bundle();
        args.putString("categoria", categoriaId);
        args.putString("nombreCategoria", nombreCategoria);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.resultadosFragment) {
                return;
            }

            navController.navigate(R.id.resultadosFragment, args);
        });

        esperar(1000);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private View buscarVistaPorId(View vista, int idBuscado) {
        if (vista == null) {
            return null;
        }

        if (vista.getId() == idBuscado) {
            return vista;
        }

        if (!(vista instanceof ViewGroup)) {
            return null;
        }

        ViewGroup grupo = (ViewGroup) vista;

        for (int i = 0; i < grupo.getChildCount(); i++) {
            View encontrada = buscarVistaPorId(grupo.getChildAt(i), idBuscado);

            if (encontrada != null) {
                return encontrada;
            }
        }

        return null;
    }

    private View obtenerVistaPorId(MainActivity activity, int viewId) {
        View raiz = activity.findViewById(android.R.id.content);
        return buscarVistaPorId(raiz, viewId);
    }

    private void esperarHastaTarjetaVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View titulo = obtenerVistaPorId(activity, R.id.tvTituloClank);

                if (titulo instanceof TextView
                        && titulo.isShown()
                        && TestDataSeeder.TEST_CLANK_TITULO.equals(
                        ((TextView) titulo).getText().toString()
                )) {
                    visible[0] = true;
                }
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció la tarjeta del clank sembrado en Resultados dentro del tiempo esperado."
        );
    }

    private void esperarHastaEstadoVacioVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View textoVacio = activity.findViewById(R.id.tvResultadosVacio);
                View recycler = activity.findViewById(R.id.rvResultados);
                View overlay = activity.findViewById(R.id.overlayCargandoResultados);

                assertNotNull(
                        "No se encontró tvResultadosVacio.",
                        textoVacio
                );

                assertNotNull(
                        "No se encontró rvResultados.",
                        recycler
                );

                assertNotNull(
                        "No se encontró overlayCargandoResultados.",
                        overlay
                );

                visible[0] = textoVacio.isShown()
                        && recycler.getVisibility() == View.GONE
                        && overlay.getVisibility() == View.GONE;
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció el estado vacío de resultados dentro del tiempo esperado."
        );
    }

    private String obtenerTextoView(int viewId) {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            View vista = obtenerVistaPorId(activity, viewId);

            assertTrue(
                    "La vista no es TextView o no existe: " + viewId,
                    vista instanceof TextView
            );

            texto[0] = ((TextView) vista).getText().toString();
        });

        return texto[0];
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Carga de resultados")
    @Description("Al navegar a resultados, el destino actual debe ser resultadosFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAResultados_destinoCorrecto() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.resultadosFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Estructura de resultados")
    @Description("El RecyclerView principal de resultados debe existir.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerResultados_existe() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            View vista = activity.findViewById(R.id.rvResultados);

            assertNotNull(
                    "No se encontró rvResultados.",
                    vista
            );

            assertTrue(
                    "rvResultados debe ser un RecyclerView.",
                    vista instanceof RecyclerView
            );
        });
    }

    @Test
    @Story("Estructura de resultados")
    @Description("El RecyclerView de resultados debe tener LayoutManager configurado.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerResultados_tieneLayoutManager() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvResultados);

            assertNotNull(
                    "No se encontró rvResultados.",
                    recyclerView
            );

            assertNotNull(
                    "rvResultados debe tener LayoutManager.",
                    recyclerView.getLayoutManager()
            );
        });
    }

    @Test
    @Story("Estructura de resultados")
    @Description("El RecyclerView de resultados debe tener adapter configurado.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerResultados_tieneAdapter() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvResultados);

            assertNotNull(
                    "No se encontró rvResultados.",
                    recyclerView
            );

            assertNotNull(
                    "rvResultados debe tener Adapter.",
                    recyclerView.getAdapter()
            );
        });
    }

    @Test
    @Story("Estructura de resultados")
    @Description("El overlay de carga de resultados debe existir.")
    @Severity(SeverityLevel.NORMAL)
    public void overlayCargandoResultados_existe() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            View overlay = activity.findViewById(R.id.overlayCargandoResultados);

            assertNotNull(
                    "No se encontró overlayCargandoResultados.",
                    overlay
            );
        });
    }

    @Test
    @Story("Estructura de resultados")
    @Description("El texto de resultados vacíos debe existir.")
    @Severity(SeverityLevel.NORMAL)
    public void textoResultadosVacio_existe() {
        navegarAResultados();

        escenario.onActivity(activity -> {
            View textoVacio = activity.findViewById(R.id.tvResultadosVacio);

            assertNotNull(
                    "No se encontró tvResultadosVacio.",
                    textoVacio
            );
        });
    }

    ///////////////////////// estado vacío /////////////////////////

    @Test
    @Story("Estado vacío de resultados")
    @Description("Si no hay clanks para la categoría indicada, debe mostrarse el estado vacío.")
    @Severity(SeverityLevel.CRITICAL)
    public void resultadosSinClanks_muestraEstadoVacio() {
        navegarAResultadosConCategoria(
                "categoria-sin-clanks-test",
                "Categoría sin clanks"
        );

        esperarHastaEstadoVacioVisible();

        escenario.onActivity(activity -> {
            View textoVacio = activity.findViewById(R.id.tvResultadosVacio);
            View recycler = activity.findViewById(R.id.rvResultados);
            View overlay = activity.findViewById(R.id.overlayCargandoResultados);

            assertTrue(
                    "tvResultadosVacio debe estar visible cuando no hay resultados.",
                    textoVacio.isShown()
            );

            assertEquals(
                    View.GONE,
                    recycler.getVisibility()
            );

            assertEquals(
                    View.GONE,
                    overlay.getVisibility()
            );
        });
    }

    ///////////////////////// resultados filtrados /////////////////////////

    @Test
    @Story("Contenido de resultados")
    @Description("Con un clank sembrado en la categoría indicada, debe mostrarse al menos una tarjeta.")
    @Severity(SeverityLevel.CRITICAL)
    public void resultadosConCategoriaSembrada_muestranTarjeta() {
        navegarAResultados();
        esperarHastaTarjetaVisible();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvResultados);

            assertNotNull(
                    "No se encontró rvResultados.",
                    recyclerView
            );

            assertTrue(
                    "El adapter debe tener al menos una tarjeta.",
                    recyclerView.getAdapter() != null
                            && recyclerView.getAdapter().getItemCount() > 0
            );
        });
    }

    @Test
    @Story("Contenido de resultados")
    @Description("La tarjeta debe mostrar el título del clank sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void tarjetaResultados_muestraTituloSembrado() {
        navegarAResultados();
        esperarHastaTarjetaVisible();

        assertEquals(
                TestDataSeeder.TEST_CLANK_TITULO,
                obtenerTextoView(R.id.tvTituloClank)
        );
    }

    @Test
    @Story("Contenido de resultados")
    @Description("La tarjeta debe mostrar la descripción del clank sembrado.")
    @Severity(SeverityLevel.NORMAL)
    public void tarjetaResultados_muestraDescripcionSembrada() {
        navegarAResultados();
        esperarHastaTarjetaVisible();

        assertEquals(
                "Descripción de prueba para tests de integración",
                obtenerTextoView(R.id.tvDescripcionClank)
        );
    }

    @Test
    @Story("Contenido de resultados")
    @Description("La tarjeta debe mostrar el contador inicial de likes.")
    @Severity(SeverityLevel.NORMAL)
    public void tarjetaResultados_muestraLikesIniciales() {
        navegarAResultados();
        esperarHastaTarjetaVisible();

        assertEquals(
                String.valueOf(TestDataSeeder.TEST_CLANK_NUM_LIKES_INICIAL),
                obtenerTextoView(R.id.tvNumLikes)
        );
    }

    @Test
    @Story("Navegación desde resultados")
    @Description("Al pulsar una tarjeta de resultados, debe navegarse al detalle del clank.")
    @Severity(SeverityLevel.CRITICAL)
    public void clickTarjeta_navegaADetalle() {
        navegarAResultados();
        esperarHastaTarjetaVisible();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvResultados);

            assertNotNull(
                    "No se encontró rvResultados.",
                    recyclerView
            );

            assertNotNull(
                    "rvResultados debe tener adapter.",
                    recyclerView.getAdapter()
            );

            assertTrue(
                    "El adapter debe tener al menos una tarjeta antes de hacer click.",
                    recyclerView.getAdapter().getItemCount() > 0
            );

            assertTrue(
                    "El RecyclerView debe tener al menos una vista hija visible.",
                    recyclerView.getChildCount() > 0
            );

            View primeraTarjeta = recyclerView.getChildAt(0);

            assertNotNull(
                    "No se pudo obtener la primera tarjeta visible de resultados.",
                    primeraTarjeta
            );

            primeraTarjeta.performClick();
        });

        esperar(1000);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.detalleClankFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }
}