package com.clank.app.ui.feed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;

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
@Feature("Feed")
public class FeedUITest {

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
        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
    }

    @After
    public void tearDown() {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }
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

    @Test
    @Story("Carga del feed")
    @Description("Tras cargar Feed, la pantalla debe mantener un estado visual válido: lista, overlay o texto vacío.")
    @Severity(SeverityLevel.NORMAL)
    public void feed_mantieneEstadoVisualValido() {
        navegarAFeed();
        esperar(1000);

        escenario.onActivity(activity -> {
            View recycler = activity.findViewById(R.id.rvFeed);
            View overlay = activity.findViewById(R.id.overlayCargandoFeed);
            View textoVacio = activity.findViewById(R.id.tvFeedVacio);

            assertNotNull("No se encontró rvFeed.", recycler);
            assertNotNull("No se encontró overlayCargandoFeed.", overlay);
            assertNotNull("No se encontró tvFeedVacio.", textoVacio);

            boolean hayEstadoVisible =
                    recycler.isShown()
                            || overlay.isShown()
                            || textoVacio.isShown();

            assertTrue(
                    "Feed debe mostrar al menos un estado visual válido.",
                    hayEstadoVisible
            );
        });
    }
}