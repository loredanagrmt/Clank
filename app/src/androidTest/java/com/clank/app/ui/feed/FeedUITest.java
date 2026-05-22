package com.clank.app.ui.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
@Feature("Feed")
public class FeedUITest {

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

    private void navegarAFeed() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.feedFragment) {
                return;
            }

            navController.navigate(R.id.feedFragment);
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
                "No apareció la tarjeta del clank sembrado en el Feed dentro del tiempo esperado."
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
    @Story("Carga del feed")
    @Description("Al navegar a Feed, el destino actual del NavController debe ser feedFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAFeed_destinoCorrecto() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.feedFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Estructura del feed")
    @Description("El RecyclerView principal del feed debe existir.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerFeed_existe() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            View vista = activity.findViewById(R.id.rvFeed);

            assertNotNull(
                    "No se encontró rvFeed.",
                    vista
            );

            assertTrue(
                    "rvFeed debe ser un RecyclerView.",
                    vista instanceof RecyclerView
            );
        });
    }

    @Test
    @Story("Estructura del feed")
    @Description("El RecyclerView del feed debe tener LayoutManager configurado.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerFeed_tieneLayoutManager() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvFeed);

            assertNotNull(
                    "No se encontró rvFeed.",
                    recyclerView
            );

            assertNotNull(
                    "rvFeed debe tener LayoutManager configurado.",
                    recyclerView.getLayoutManager()
            );
        });
    }

    @Test
    @Story("Estructura del feed")
    @Description("El RecyclerView del feed debe tener adapter configurado.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerFeed_tieneAdapter() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvFeed);

            assertNotNull(
                    "No se encontró rvFeed.",
                    recyclerView
            );

            assertNotNull(
                    "rvFeed debe tener Adapter configurado.",
                    recyclerView.getAdapter()
            );
        });
    }

    @Test
    @Story("Estructura del feed")
    @Description("El overlay de carga del feed debe existir.")
    @Severity(SeverityLevel.NORMAL)
    public void overlayCargandoFeed_existe() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            View overlay = activity.findViewById(R.id.overlayCargandoFeed);

            assertNotNull(
                    "No se encontró overlayCargandoFeed.",
                    overlay
            );
        });
    }

    @Test
    @Story("Estructura del feed")
    @Description("El texto de feed vacío debe existir para poder mostrarse cuando no haya clanks.")
    @Severity(SeverityLevel.NORMAL)
    public void textoFeedVacio_existe() {
        navegarAFeed();

        escenario.onActivity(activity -> {
            View textoVacio = activity.findViewById(R.id.tvFeedVacio);

            assertNotNull(
                    "No se encontró tvFeedVacio.",
                    textoVacio
            );
        });
    }

    ///////////////////////// tarjeta sembrada /////////////////////////

    @Test
    @Story("Contenido del feed")
    @Description("Con un clank sembrado en Firestore Emulator, Feed debe mostrar al menos una tarjeta.")
    @Severity(SeverityLevel.CRITICAL)
    public void feedConClankSembrado_muestraTarjeta() {
        navegarAFeed();
        esperarHastaTarjetaVisible();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvFeed);

            assertNotNull(
                    "No se encontró rvFeed.",
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
    @Story("Contenido del feed")
    @Description("La tarjeta del feed debe mostrar el título del clank sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void tarjetaFeed_muestraTituloSembrado() {
        navegarAFeed();
        esperarHastaTarjetaVisible();

        assertEquals(
                TestDataSeeder.TEST_CLANK_TITULO,
                obtenerTextoView(R.id.tvTituloClank)
        );
    }

    @Test
    @Story("Contenido del feed")
    @Description("La tarjeta del feed debe mostrar la descripción del clank sembrado.")
    @Severity(SeverityLevel.NORMAL)
    public void tarjetaFeed_muestraDescripcionSembrada() {
        navegarAFeed();
        esperarHastaTarjetaVisible();

        assertEquals(
                "Descripción de prueba para tests de integración",
                obtenerTextoView(R.id.tvDescripcionClank)
        );
    }

    @Test
    @Story("Contenido del feed")
    @Description("La tarjeta del feed debe mostrar el contador inicial de likes.")
    @Severity(SeverityLevel.NORMAL)
    public void tarjetaFeed_muestraLikesIniciales() {
        navegarAFeed();
        esperarHastaTarjetaVisible();

        assertEquals(
                String.valueOf(TestDataSeeder.TEST_CLANK_NUM_LIKES_INICIAL),
                obtenerTextoView(R.id.tvNumLikes)
        );
    }

    @Test
    @Story("Navegación desde feed")
    @Description("Al pulsar una tarjeta del feed, debe navegarse al detalle del clank.")
    @Severity(SeverityLevel.CRITICAL)
    public void clickTarjeta_navegaADetalle() {
        navegarAFeed();
        esperarHastaTarjetaVisible();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvFeed);

            assertNotNull(
                    "No se encontró rvFeed.",
                    recyclerView
            );

            assertNotNull(
                    "rvFeed debe tener adapter.",
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
                    "No se pudo obtener la primera tarjeta visible del feed.",
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