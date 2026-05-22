package com.clank.app.ui.filtros;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
@Feature("Filtros")
public class FiltrosUITest {

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
        seeder.insertarCategoriaTest();

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

        seeder.eliminarCategoriaTest();
    }

    private void navegarAFiltros() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.filtrosFragment) {
                return;
            }

            navController.navigate(R.id.filtrosFragment);
        });

        esperar(800);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private ViewGroup obtenerContenedorCategorias(MainActivity activity) {
        View vista = activity.findViewById(R.id.chipGroupCategorias);

        assertTrue(
                "No se encontró chipGroupCategorias o no es ViewGroup.",
                vista instanceof ViewGroup
        );

        return (ViewGroup) vista;
    }

    private void esperarHastaCategoriasCargadas() {
        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            final boolean[] hayCategorias = new boolean[1];

            escenario.onActivity(activity -> {
                ViewGroup contenedor = obtenerContenedorCategorias(activity);
                hayCategorias[0] = contenedor.getChildCount() > 0;
            });

            if (hayCategorias[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargaron categorías en filtros dentro del tiempo esperado."
        );
    }

    private TextView obtenerPrimerChip(MainActivity activity) {
        ViewGroup contenedor = obtenerContenedorCategorias(activity);

        assertTrue(
                "Debe existir al menos un chip de categoría.",
                contenedor.getChildCount() > 0
        );

        View chip = contenedor.getChildAt(0);

        assertTrue(
                "El primer chip debe ser un TextView/Button.",
                chip instanceof TextView
        );

        return (TextView) chip;
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Carga de filtros")
    @Description("Al navegar a filtros, el destino actual debe ser filtrosFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAFiltros_destinoCorrecto() {
        navegarAFiltros();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.filtrosFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura /////////////////////////

    @Test
    @Story("Estructura de filtros")
    @Description("El contenedor de categorías debe existir.")
    @Severity(SeverityLevel.CRITICAL)
    public void contenedorCategorias_existe() {
        navegarAFiltros();

        escenario.onActivity(activity -> {
            ViewGroup contenedor = obtenerContenedorCategorias(activity);

            assertNotNull(
                    "chipGroupCategorias no debe ser null.",
                    contenedor
            );
        });
    }

    @Test
    @Story("Estructura de filtros")
    @Description("El contenedor de categorías debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void contenedorCategorias_estaVisible() {
        navegarAFiltros();

        escenario.onActivity(activity -> {
            ViewGroup contenedor = obtenerContenedorCategorias(activity);

            assertTrue(
                    "chipGroupCategorias debe estar visible.",
                    contenedor.isShown()
            );
        });
    }

    ///////////////////////// categorías /////////////////////////

    @Test
    @Story("Carga de categorías")
    @Description("Con una categoría sembrada en Firestore Emulator, debe pintarse al menos un chip.")
    @Severity(SeverityLevel.CRITICAL)
    public void categoriasSembradas_muestranChips() {
        navegarAFiltros();
        esperarHastaCategoriasCargadas();

        escenario.onActivity(activity -> {
            ViewGroup contenedor = obtenerContenedorCategorias(activity);

            assertTrue(
                    "Debe mostrarse al menos una categoría.",
                    contenedor.getChildCount() > 0
            );
        });
    }

    @Test
    @Story("Carga de categorías")
    @Description("El chip debe mostrar el nombre de la categoría sembrada.")
    @Severity(SeverityLevel.CRITICAL)
    public void chipCategoria_muestraNombreSembrado() {
        navegarAFiltros();
        esperarHastaCategoriasCargadas();

        escenario.onActivity(activity -> {
            TextView chip = obtenerPrimerChip(activity);

            assertEquals(
                    TestDataSeeder.TEST_CATEGORIA_NOMBRE,
                    chip.getText().toString()
            );
        });
    }

    @Test
    @Story("Interacción de filtros")
    @Description("El chip de categoría debe estar visible y ser clicable.")
    @Severity(SeverityLevel.NORMAL)
    public void chipCategoria_estaVisibleYClickable() {
        navegarAFiltros();
        esperarHastaCategoriasCargadas();

        escenario.onActivity(activity -> {
            TextView chip = obtenerPrimerChip(activity);

            assertTrue(
                    "El chip de categoría debe estar visible.",
                    chip.isShown()
            );

            assertTrue(
                    "El chip de categoría debe ser clicable.",
                    chip.isClickable()
            );
        });
    }
}