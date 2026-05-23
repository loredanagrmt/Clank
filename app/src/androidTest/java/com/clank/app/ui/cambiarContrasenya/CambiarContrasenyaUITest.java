package com.clank.app.ui.cambiarContrasenya;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
import com.google.firebase.auth.FirebaseUser;

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
@Feature("Cambiar contraseña")
public class CambiarContrasenyaUITest {

    private static final long TIMEOUT_MS = 8000;
    private static final long INTERVALO_MS = 250;
    private static final long TIMEOUT_AUTH_S = 10;

    private static final String NUEVA_CONTRASENYA_TEST = "Password456!";
    private static final String CONTRASENYA_INCORRECTA = "PasswordIncorrecta123!";

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

        restaurarContrasenyaOriginalSiHaceFalta();

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

        restaurarContrasenyaOriginalSiHaceFalta();

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

    private void restaurarContrasenyaOriginalSiHaceFalta()
            throws InterruptedException, TimeoutException {

        FirebaseAuth auth = FirebaseAuth.getInstance();

        try {
            auth.signOut();

            Tasks.await(
                    auth.signInWithEmailAndPassword(
                            TestDataSeeder.TEST_EMAIL,
                            TestDataSeeder.TEST_PASSWORD
                    ),
                    TIMEOUT_AUTH_S,
                    TimeUnit.SECONDS
            );

            return;
        } catch (Exception ignored) {
            // Puede no existir todavía o puede estar con la contraseña nueva.
        }

        try {
            auth.signOut();

            Tasks.await(
                    auth.signInWithEmailAndPassword(
                            TestDataSeeder.TEST_EMAIL,
                            NUEVA_CONTRASENYA_TEST
                    ),
                    TIMEOUT_AUTH_S,
                    TimeUnit.SECONDS
            );

            FirebaseUser usuario = auth.getCurrentUser();

            if (usuario != null) {
                Tasks.await(
                        usuario.updatePassword(TestDataSeeder.TEST_PASSWORD),
                        TIMEOUT_AUTH_S,
                        TimeUnit.SECONDS
                );
            }
        } catch (Exception ignored) {
            // Si tampoco existe con la nueva contraseña, el setUp lo creará después.
        }
    }

    private void navegarACambiarContrasenya() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.cambiarContrasenyaFragment) {
                return;
            }

            navController.navigate(R.id.cambiarContrasenyaFragment);
        });

        esperar(800);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private EditText obtenerCampoDesdeContenedor(MainActivity activity, int contenedorId) {
        View contenedor = activity.findViewById(contenedorId);

        assertTrue(
                "No se encontró el contenedor del campo: " + contenedorId,
                contenedor instanceof ViewGroup
        );

        View campo = ((ViewGroup) contenedor).findViewById(R.id.customEditText);

        assertTrue(
                "No se encontró customEditText dentro del contenedor: " + contenedorId,
                campo instanceof EditText
        );

        return (EditText) campo;
    }

    private View buscarVistaClicable(View vista) {
        if (vista == null) {
            return null;
        }

        if (vista.isShown() && vista.isEnabled() && vista.isClickable()) {
            return vista;
        }

        if (!(vista instanceof ViewGroup)) {
            return null;
        }

        ViewGroup grupo = (ViewGroup) vista;

        for (int i = 0; i < grupo.getChildCount(); i++) {
            View encontrada = buscarVistaClicable(grupo.getChildAt(i));

            if (encontrada != null) {
                return encontrada;
            }
        }

        return null;
    }

    private View obtenerBotonContinuar(MainActivity activity) {
        View contenedor = activity.findViewById(R.id.contenedorBotonCambiarContrasenya);

        assertTrue(
                "No se encontró contenedorBotonCambiarContrasenya.",
                contenedor instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedor);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro de contenedorBotonCambiarContrasenya.",
                boton
        );

        return boton;
    }

    private void escribirTextoCampo(int contenedorId, String texto) {
        escenario.onActivity(activity -> {
            EditText campo = obtenerCampoDesdeContenedor(activity, contenedorId);
            campo.setText(texto);
        });

        esperar(250);
    }

    private CharSequence obtenerErrorCampo(int contenedorId) {
        final CharSequence[] error = new CharSequence[1];

        escenario.onActivity(activity -> {
            EditText campo = obtenerCampoDesdeContenedor(activity, contenedorId);
            error[0] = campo.getError();
        });

        return error[0];
    }

    private void pulsarContinuar() {
        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar habilitado antes de pulsar.",
                    boton.isEnabled()
            );

            boton.performClick();
        });

        esperar(500);
    }

    private void esperarHastaErrorEnCampo(int contenedorId) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] hayError = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            hayError[0] = false;

            escenario.onActivity(activity -> {
                EditText campo = obtenerCampoDesdeContenedor(activity, contenedorId);
                hayError[0] = campo.getError() != null
                        && campo.getError().toString().trim().length() > 0;
            });

            if (hayError[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció error en el campo esperado dentro del tiempo límite."
        );
    }

    private void esperarHastaPopupVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View popup = activity.findViewById(R.id.capaPopup);
                visible[0] = popup != null && popup.isShown();
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció el popup de contraseña actualizada dentro del tiempo esperado."
        );
    }

    private void verificarLoginConNuevaContrasenya()
            throws ExecutionException, InterruptedException, TimeoutException {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();

        Tasks.await(
                auth.signInWithEmailAndPassword(
                        TestDataSeeder.TEST_EMAIL,
                        NUEVA_CONTRASENYA_TEST
                ),
                TIMEOUT_AUTH_S,
                TimeUnit.SECONDS
        );

        FirebaseUser usuario = auth.getCurrentUser();

        assertNotNull(
                "Debe poder iniciar sesión con la nueva contraseña.",
                usuario
        );

        assertEquals(
                TestDataSeeder.TEST_EMAIL,
                usuario.getEmail()
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Cambiar contraseña")
    @Description("Al navegar a cambiar contraseña, el destino actual debe ser cambiarContrasenyaFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarACambiarContrasenya_destinoCorrecto() {
        navegarACambiarContrasenya();

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

    ///////////////////////// estructura /////////////////////////

    @Test
    @Story("Cambiar contraseña")
    @Description("Los tres campos de contraseña deben estar visibles.")
    @Severity(SeverityLevel.CRITICAL)
    public void camposContrasenya_estanVisibles() {
        navegarACambiarContrasenya();

        escenario.onActivity(activity -> {
            assertTrue(
                    "inputContrasenyaActual debe estar visible.",
                    obtenerCampoDesdeContenedor(activity, R.id.inputContrasenyaActual).isShown()
            );

            assertTrue(
                    "inputNuevaContrasenya debe estar visible.",
                    obtenerCampoDesdeContenedor(activity, R.id.inputNuevaContrasenya).isShown()
            );

            assertTrue(
                    "inputRepetirContrasenya debe estar visible.",
                    obtenerCampoDesdeContenedor(activity, R.id.inputRepetirContrasenya).isShown()
            );
        });
    }

    @Test
    @Story("Cambiar contraseña")
    @Description("El botón continuar debe estar visible y habilitado inicialmente.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonContinuar_estaVisibleYHabilitado() {
        navegarACambiarContrasenya();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible.",
                    boton.isShown()
            );

            assertTrue(
                    "El botón continuar debe estar habilitado.",
                    boton.isEnabled()
            );
        });
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación cambiar contraseña")
    @Description("Si la contraseña actual está vacía, debe mostrarse error local.")
    @Severity(SeverityLevel.CRITICAL)
    public void contrasenyaActualVacia_muestraError() {
        navegarACambiarContrasenya();

        escribirTextoCampo(R.id.inputContrasenyaActual, "");
        escribirTextoCampo(R.id.inputNuevaContrasenya, "Password999!");
        escribirTextoCampo(R.id.inputRepetirContrasenya, "Password999!");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputContrasenyaActual);

        assertNotNull(
                "Debe mostrarse error cuando la contraseña actual está vacía.",
                error
        );

        assertTrue(
                "El error de contraseña actual vacía no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación cambiar contraseña")
    @Description("Si la nueva contraseña tiene menos de 6 caracteres, debe mostrarse error local.")
    @Severity(SeverityLevel.CRITICAL)
    public void nuevaContrasenyaDebil_muestraErrorLocal() {
        navegarACambiarContrasenya();

        escribirTextoCampo(R.id.inputContrasenyaActual, TestDataSeeder.TEST_PASSWORD);
        escribirTextoCampo(R.id.inputNuevaContrasenya, "123");
        escribirTextoCampo(R.id.inputRepetirContrasenya, "123");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando la nueva contraseña es débil.",
                error
        );

        assertTrue(
                "El error de contraseña débil no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación cambiar contraseña")
    @Description("Si las contraseñas nuevas no coinciden, debe mostrarse error local.")
    @Severity(SeverityLevel.CRITICAL)
    public void contrasenyasNoCoinciden_muestraError() {
        navegarACambiarContrasenya();

        escribirTextoCampo(R.id.inputContrasenyaActual, TestDataSeeder.TEST_PASSWORD);
        escribirTextoCampo(R.id.inputNuevaContrasenya, "Password999!");
        escribirTextoCampo(R.id.inputRepetirContrasenya, "Password888!");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputRepetirContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando las contraseñas nuevas no coinciden.",
                error
        );

        assertTrue(
                "El error de contraseñas no coinciden no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    ///////////////////////// validación con Auth Emulator /////////////////////////

    @Test
    @Story("Validación cambiar contraseña")
    @Description("Si la contraseña actual no es correcta, debe mostrarse error tras consultar Auth Emulator.")
    @Severity(SeverityLevel.CRITICAL)
    public void contrasenyaActualIncorrecta_muestraErrorAuth() {
        navegarACambiarContrasenya();

        escribirTextoCampo(R.id.inputContrasenyaActual, CONTRASENYA_INCORRECTA);
        escribirTextoCampo(R.id.inputNuevaContrasenya, "Password999!");
        escribirTextoCampo(R.id.inputRepetirContrasenya, "Password999!");

        pulsarContinuar();

        esperarHastaErrorEnCampo(R.id.inputContrasenyaActual);

        CharSequence error = obtenerErrorCampo(R.id.inputContrasenyaActual);

        assertNotNull(
                "Debe mostrarse error cuando la contraseña actual es incorrecta.",
                error
        );

        assertTrue(
                "El error de contraseña actual incorrecta no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    ///////////////////////// cambio real /////////////////////////

    @Test
    @Story("Cambio real de contraseña")
    @Description("Con contraseña actual correcta y nueva válida, debe actualizarse Auth Emulator y mostrarse popup de éxito.")
    @Severity(SeverityLevel.BLOCKER)
    public void cambiarContrasenyaValida_actualizaAuthYMuestraPopup()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarACambiarContrasenya();

        escribirTextoCampo(R.id.inputContrasenyaActual, TestDataSeeder.TEST_PASSWORD);
        escribirTextoCampo(R.id.inputNuevaContrasenya, NUEVA_CONTRASENYA_TEST);
        escribirTextoCampo(R.id.inputRepetirContrasenya, NUEVA_CONTRASENYA_TEST);

        pulsarContinuar();

        esperarHastaPopupVisible();

        escenario.onActivity(activity -> {
            View popup = activity.findViewById(R.id.capaPopup);

            assertNotNull(
                    "No se encontró capaPopup.",
                    popup
            );

            assertTrue(
                    "El popup de éxito debe estar visible.",
                    popup.isShown()
            );
        });

        verificarLoginConNuevaContrasenya();

        restaurarContrasenyaOriginalSiHaceFalta();
    }
}