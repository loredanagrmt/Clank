package com.clank.app.ui.editar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
@Feature("Eliminar Clank")
public class EliminarClankUITest {

    private static final long TIMEOUT_MS = 8000;
    private static final long INTERVALO_MS = 250;
    private static final long TIMEOUT_FIRESTORE_S = 10;

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
        seeder.insertarBocetoAutenticadoCompletoTest();

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

        seeder.eliminarBocetoAutenticadoTest();
        seeder.eliminarCategoriaTest();
        seeder.eliminarUsuarioAutenticadoFirestore();
    }

    private void navegarAEditarClank() {
        Bundle args = new Bundle();
        args.putString("clankId", TestDataSeeder.TEST_BOCETO_ID);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.editarClankFragment) {
                return;
            }

            navController.navigate(R.id.editarClankFragment, args);
        });

        esperar(1000);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void esperarHastaFormularioCargado() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cargado = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            cargado[0] = false;

            escenario.onActivity(activity -> {
                TextView titulo = activity.findViewById(R.id.etTitulo);

                if (titulo != null
                        && TestDataSeeder.TEST_BOCETO_TITULO.equals(titulo.getText().toString())) {
                    cargado[0] = true;
                }
            });

            if (cargado[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargó el boceto en edición dentro del tiempo esperado."
        );
    }

    private void esperarHastaDestinoFeed() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] enFeed = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            enFeed[0] = false;

            escenario.onActivity(activity -> {
                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment);

                enFeed[0] = navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == R.id.feedFragment;
            });

            if (enFeed[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se navegó a feedFragment tras confirmar la eliminación."
        );
    }

    private void esperarHastaDocumentoEliminado()
            throws ExecutionException, InterruptedException, TimeoutException {

        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            DocumentSnapshot doc = obtenerDocumentoBoceto();

            if (!doc.exists()) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El documento del boceto no fue eliminado dentro del tiempo esperado."
        );
    }

    private DocumentSnapshot obtenerDocumentoBoceto()
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("clanks")
                        .document(TestDataSeeder.TEST_BOCETO_ID)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    private QuerySnapshot obtenerSubcoleccion(String nombreSubcoleccion)
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("clanks")
                        .document(TestDataSeeder.TEST_BOCETO_ID)
                        .collection(nombreSubcoleccion)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// carga /////////////////////////

    @Test
    @Story("Eliminar clank")
    @Description("Al navegar a edición, el boceto debe cargarse antes de eliminar.")
    @Severity(SeverityLevel.BLOCKER)
    public void editarClank_cargaBocetoAntesDeEliminar() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.editarClankFragment,
                    navController.getCurrentDestination().getId()
            );

            TextView titulo = activity.findViewById(R.id.etTitulo);

            assertEquals(
                    TestDataSeeder.TEST_BOCETO_TITULO,
                    titulo.getText().toString()
            );
        });
    }

    @Test
    @Story("Eliminar clank")
    @Description("La acción de eliminar de la navbar debe estar visible en edición.")
    @Severity(SeverityLevel.CRITICAL)
    public void accionEliminarNavbar_estaVisible() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            View accion = activity.findViewById(R.id.btnNavbarAccion);

            assertNotNull(
                    "No se encontró btnNavbarAccion.",
                    accion
            );

            assertTrue(
                    "La acción de eliminar debe estar visible.",
                    accion.isShown()
            );

            assertTrue(
                    "La acción de eliminar debe estar habilitada.",
                    accion.isEnabled()
            );
        });
    }

    ///////////////////////// confirmación /////////////////////////

    @Test
    @Story("Eliminar clank")
    @Description("Al pulsar eliminar, debe mostrarse la hoja de confirmación.")
    @Severity(SeverityLevel.CRITICAL)
    public void pulsarEliminar_muestraHojaConfirmacion() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            View accion = activity.findViewById(R.id.btnNavbarAccion);

            assertNotNull(
                    "No se encontró btnNavbarAccion.",
                    accion
            );

            accion.performClick();
        });

        esperar(500);

        onView(withId(R.id.tituloPanelOpciones))
                .check(ViewAssertions.matches(isDisplayed()));

        onView(withId(R.id.textoConfirmacion))
                .check(ViewAssertions.matches(isDisplayed()));

        onView(withId(R.id.contenedorBotonesConfirmacion))
                .check(ViewAssertions.matches(isDisplayed()));
    }

    ///////////////////////// eliminación real /////////////////////////

    @Test
    @Story("Eliminar clank")
    @Description("Al confirmar la eliminación, debe borrarse el clank, limpiar subcolecciones y navegar a feed.")
    @Severity(SeverityLevel.BLOCKER)
    public void confirmarEliminar_borraClankYSubcoleccionesYNavegaAFeed()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarAEditarClank();
        esperarHastaFormularioCargado();

        DocumentSnapshot docAntes = obtenerDocumentoBoceto();

        assertTrue(
                "El documento debe existir antes de eliminar.",
                docAntes.exists()
        );

        assertFalse(
                "Materiales debe tener datos antes de eliminar.",
                obtenerSubcoleccion("materiales").isEmpty()
        );

        assertFalse(
                "Herramientas debe tener datos antes de eliminar.",
                obtenerSubcoleccion("herramientas").isEmpty()
        );

        assertFalse(
                "Instrucciones debe tener datos antes de eliminar.",
                obtenerSubcoleccion("instrucciones").isEmpty()
        );

        escenario.onActivity(activity -> {
            View accion = activity.findViewById(R.id.btnNavbarAccion);

            assertNotNull(
                    "No se encontró btnNavbarAccion.",
                    accion
            );

            accion.performClick();
        });

        esperar(500);

        onView(withId(R.id.includeBotonConfirmar))
                .check(ViewAssertions.matches(isDisplayed()))
                .perform(click());

        esperarHastaDestinoFeed();
        esperarHastaDocumentoEliminado();

        assertFalse(
                "El documento del boceto debe quedar eliminado.",
                obtenerDocumentoBoceto().exists()
        );

        assertTrue(
                "La subcolección materiales debe quedar vacía.",
                obtenerSubcoleccion("materiales").isEmpty()
        );

        assertTrue(
                "La subcolección herramientas debe quedar vacía.",
                obtenerSubcoleccion("herramientas").isEmpty()
        );

        assertTrue(
                "La subcolección instrucciones debe quedar vacía.",
                obtenerSubcoleccion("instrucciones").isEmpty()
        );
    }
}