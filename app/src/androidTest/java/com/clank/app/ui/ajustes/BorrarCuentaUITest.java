package com.clank.app.ui.ajustes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Dialog;
import android.os.SystemClock;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
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
@Feature("Borrar cuenta")
public class BorrarCuentaUITest {

    private static final long TIMEOUT_MS = 60000;
    private static final long INTERVALO_MS = 300;
    private static final long TIMEOUT_FIRESTORE_S = 10;
    private static final long TIMEOUT_AUTH_S = 10;

    private static final String TAG_CONFIRMACION_BORRAR_CUENTA =
            "confirmacion_borrar_cuenta";

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    @Rule(order = 1)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 2)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    private ActivityScenario<MainActivity> escenario;
    private TestDataSeeder seeder;
    private String uidAutenticado;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        hiltRule.inject();

        seeder = new TestDataSeeder();

        uidAutenticado = seeder.crearOIniciarSesionUsuarioAuthTest();

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

        if (seeder != null) {
            limpiarDatosFirestore();
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

    private void esperarHastaDestinoBienvenida() {
        long inicio = SystemClock.elapsedRealtime();
        final int[] destinoActual = new int[]{-1};

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            escenario.onActivity(activity -> {
                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment);

                destinoActual[0] = navController.getCurrentDestination() != null
                        ? navController.getCurrentDestination().getId()
                        : -1;
            });

            if (destinoActual[0] == R.id.bienvenidaFragment) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se navegó a bienvenidaFragment tras borrar cuenta. Destino actual: "
                        + destinoActual[0]
        );
    }

    private void esperarHastaHojaConfirmacionCerrada() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cerrada = new boolean[]{false};

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            escenario.onActivity(activity -> {
                Fragment hoja = obtenerHojaConfirmacionBorrarCuenta(activity);
                cerrada[0] = hoja == null || !hoja.isAdded();
            });

            if (cerrada[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "La hoja de confirmación de borrar cuenta no se cerró dentro del tiempo esperado."
        );
    }

    private void esperarHastaVistaHojaVisible(int idVista, String descripcionVista) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[]{false};

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            escenario.onActivity(activity -> {
                Fragment hoja = obtenerHojaConfirmacionBorrarCuenta(activity);
                View vista = obtenerVistaDeHoja(hoja, idVista);
                visible[0] = vista != null && vista.isShown();
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se mostró la vista de la hoja de confirmación: " + descripcionVista
        );
    }

    private void pulsarVistaHoja(int idVista, String descripcionVista) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] pulsada = new boolean[]{false};

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            escenario.onActivity(activity -> {
                Fragment hoja = obtenerHojaConfirmacionBorrarCuenta(activity);
                View vista = obtenerVistaDeHoja(hoja, idVista);

                if (vista != null && vista.isShown() && vista.isEnabled()) {
                    vista.performClick();
                    pulsada[0] = true;
                }
            });

            if (pulsada[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se pudo pulsar la vista de la hoja de confirmación: " + descripcionVista
        );
    }

    private Fragment obtenerHojaConfirmacionBorrarCuenta(MainActivity activity) {
        Fragment fragmentoActual = obtenerFragmentoActualPrincipal(activity);

        if (fragmentoActual == null) {
            return null;
        }

        return fragmentoActual
                .getChildFragmentManager()
                .findFragmentByTag(TAG_CONFIRMACION_BORRAR_CUENTA);
    }

    private Fragment obtenerFragmentoActualPrincipal(MainActivity activity) {
        Fragment navHostFragment =
                activity.getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) {
            return null;
        }

        List<Fragment> fragmentos =
                navHostFragment.getChildFragmentManager().getFragments();

        for (Fragment fragmento : fragmentos) {
            if (fragmento != null && fragmento.isAdded()) {
                return fragmento;
            }
        }

        return null;
    }

    private View obtenerVistaDeHoja(Fragment hoja, int idVista) {
        if (hoja == null) {
            return null;
        }

        View vistaHoja = hoja.getView();

        if (vistaHoja != null) {
            View vista = vistaHoja.findViewById(idVista);

            if (vista != null) {
                return vista;
            }
        }

        if (hoja instanceof DialogFragment) {
            Dialog dialogo = ((DialogFragment) hoja).getDialog();

            if (dialogo != null) {
                return dialogo.findViewById(idVista);
            }
        }

        return null;
    }

    private DocumentSnapshot obtenerUsuarioFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uidAutenticado)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    private DocumentSnapshot obtenerBocetoFirestore()
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

    private QuerySnapshot obtenerSubcoleccionBoceto(String subcoleccion)
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("clanks")
                        .document(TestDataSeeder.TEST_BOCETO_ID)
                        .collection(subcoleccion)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    private void esperarHastaUsuarioFirestoreEliminado()
            throws ExecutionException, InterruptedException, TimeoutException {

        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            if (!obtenerUsuarioFirestore().exists()) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El documento del usuario no fue eliminado dentro del tiempo esperado."
        );
    }

    private void esperarHastaBocetoEliminado()
            throws ExecutionException, InterruptedException, TimeoutException {

        long inicio = SystemClock.elapsedRealtime();

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            if (!obtenerBocetoFirestore().exists()) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El boceto del usuario no fue eliminado dentro del tiempo esperado."
        );
    }

    private void verificarUsuarioAuthEliminado()
            throws InterruptedException, TimeoutException {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();

        try {
            Tasks.await(
                    auth.signInWithEmailAndPassword(
                            TestDataSeeder.TEST_EMAIL,
                            TestDataSeeder.TEST_PASSWORD
                    ),
                    TIMEOUT_AUTH_S,
                    TimeUnit.SECONDS
            );

            fail("No debería poder iniciar sesión tras borrar la cuenta.");
        } catch (ExecutionException esperado) {
            assertTrue(
                    "El login debe fallar porque el usuario Auth fue eliminado.",
                    esperado.getCause() != null
            );
        }
    }

    ///////////////////////// estructura /////////////////////////

    @Test
    @Story("Borrar cuenta")
    @Description("La opción borrar cuenta debe estar visible y ser clicable.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonBorrarCuenta_estaVisibleYClickable() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View borrarCuenta = activity.findViewById(R.id.btn_borrar_cuenta);

            assertNotNull(
                    "No se encontró btn_borrar_cuenta.",
                    borrarCuenta
            );

            assertTrue(
                    "btn_borrar_cuenta debe estar visible.",
                    borrarCuenta.isShown()
            );

            assertTrue(
                    "btn_borrar_cuenta debe ser clicable.",
                    borrarCuenta.isClickable()
            );
        });
    }

    @Test
    @Story("Borrar cuenta")
    @Description("Al pulsar borrar cuenta, debe mostrarse la hoja de confirmación.")
    @Severity(SeverityLevel.CRITICAL)
    public void pulsarBorrarCuenta_muestraConfirmacion() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View borrarCuenta = activity.findViewById(R.id.btn_borrar_cuenta);

            assertNotNull(
                    "No se encontró btn_borrar_cuenta.",
                    borrarCuenta
            );

            borrarCuenta.performClick();
        });

        esperarHastaVistaHojaVisible(
                R.id.tituloPanelOpciones,
                "título de confirmación"
        );

        esperarHastaVistaHojaVisible(
                R.id.textoConfirmacion,
                "texto de confirmación"
        );

        esperarHastaVistaHojaVisible(
                R.id.contenedorBotonesConfirmacion,
                "contenedor de botones de confirmación"
        );
    }

    @Test
    @Story("Borrar cuenta")
    @Description("Al cancelar el borrado, debe mantenerse la sesión y seguir en ajustes.")
    @Severity(SeverityLevel.NORMAL)
    public void cancelarBorrarCuenta_mantieneSesionYAjustes() {
        navegarAAjustes();

        escenario.onActivity(activity -> {
            View borrarCuenta = activity.findViewById(R.id.btn_borrar_cuenta);
            assertNotNull("No se encontró btn_borrar_cuenta.", borrarCuenta);
            borrarCuenta.performClick();
        });

        esperarHastaVistaHojaVisible(
                R.id.includeBotonCancelar,
                "botón cancelar"
        );

        pulsarVistaHoja(
                R.id.includeBotonCancelar,
                "botón cancelar"
        );

        esperarHastaHojaConfirmacionCerrada();

        assertNotNull(
                "Al cancelar, la sesión debe seguir activa.",
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
                    R.id.ajustesFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// borrado real /////////////////////////

    @Test
    @Story("Borrar cuenta")
    @Description("Al confirmar el borrado, debe eliminar usuario, clanks y subcolecciones, cerrar sesión y navegar a bienvenida.")
    @Severity(SeverityLevel.BLOCKER)
    public void confirmarBorrarCuenta_eliminaDatosAuthYFirestoreYNavegaBienvenida()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarAAjustes();

        assertTrue(
                "El usuario Firestore debe existir antes de borrar.",
                obtenerUsuarioFirestore().exists()
        );

        assertTrue(
                "El boceto debe existir antes de borrar.",
                obtenerBocetoFirestore().exists()
        );

        assertFalse(
                "Materiales debe tener datos antes de borrar.",
                obtenerSubcoleccionBoceto("materiales").isEmpty()
        );

        assertFalse(
                "Herramientas debe tener datos antes de borrar.",
                obtenerSubcoleccionBoceto("herramientas").isEmpty()
        );

        assertFalse(
                "Instrucciones debe tener datos antes de borrar.",
                obtenerSubcoleccionBoceto("instrucciones").isEmpty()
        );

        escenario.onActivity(activity -> {
            View borrarCuenta = activity.findViewById(R.id.btn_borrar_cuenta);
            assertNotNull("No se encontró btn_borrar_cuenta.", borrarCuenta);
            borrarCuenta.performClick();
        });

        esperarHastaVistaHojaVisible(
                R.id.includeBotonConfirmar,
                "botón confirmar"
        );

        pulsarVistaHoja(
                R.id.includeBotonConfirmar,
                "botón confirmar"
        );

        esperarHastaUsuarioFirestoreEliminado();
        esperarHastaBocetoEliminado();
        esperarHastaDestinoBienvenida();

        assertFalse(
                "El usuario Firestore debe quedar eliminado.",
                obtenerUsuarioFirestore().exists()
        );

        assertFalse(
                "El boceto debe quedar eliminado.",
                obtenerBocetoFirestore().exists()
        );

        assertTrue(
                "Materiales debe quedar vacío.",
                obtenerSubcoleccionBoceto("materiales").isEmpty()
        );

        assertTrue(
                "Herramientas debe quedar vacío.",
                obtenerSubcoleccionBoceto("herramientas").isEmpty()
        );

        assertTrue(
                "Instrucciones debe quedar vacío.",
                obtenerSubcoleccionBoceto("instrucciones").isEmpty()
        );

        assertNull(
                "Después de borrar cuenta no debe quedar usuario autenticado.",
                FirebaseAuth.getInstance().getCurrentUser()
        );

        verificarUsuarioAuthEliminado();
    }
}