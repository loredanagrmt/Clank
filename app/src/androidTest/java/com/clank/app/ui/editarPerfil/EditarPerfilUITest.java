package com.clank.app.ui.editarPerfil;

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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
@Feature("Perfil")
public class EditarPerfilUITest {

    private static final long TIMEOUT_MS = 7000;
    private static final long INTERVALO_MS = 250;
    private static final long TIMEOUT_FIRESTORE_S = 10;

    private static final String NOMBRE_EDITADO = "Nombre Editado Test";
    private static final String USUARIO_CLANK_EDITADO = "@perfil_editado_test";
    private static final String TELEFONO_EDITADO = "699111222";

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

    private void navegarAEditarPerfil() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.editarPerfilFragment) {
                return;
            }

            navController.navigate(R.id.editarPerfilFragment);
        });

        esperar(800);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private TextView obtenerCampoDesdeContenedor(MainActivity activity, int contenedorId) {
        View contenedor = activity.findViewById(contenedorId);

        assertTrue(
                "No se encontró el contenedor del campo: " + contenedorId,
                contenedor instanceof ViewGroup
        );

        View campo = ((ViewGroup) contenedor).findViewById(R.id.customEditText);

        assertTrue(
                "No se encontró customEditText dentro del contenedor: " + contenedorId,
                campo instanceof TextView
        );

        return (TextView) campo;
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

    private View obtenerBotonGuardar(MainActivity activity) {
        View contenedor = activity.findViewById(R.id.contenedorBotonGuardar);

        assertTrue(
                "No se encontró contenedorBotonGuardar.",
                contenedor instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedor);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro de contenedorBotonGuardar.",
                boton
        );

        return boton;
    }

    private void escribirTextoCampo(int contenedorId, String texto) {
        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, contenedorId);
            campo.setText(texto);
        });

        esperar(300);
    }

    private String obtenerTextoCampo(int contenedorId) {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, contenedorId);
            texto[0] = campo.getText().toString();
        });

        return texto[0];
    }

    private CharSequence obtenerErrorCampo(int contenedorId) {
        final CharSequence[] error = new CharSequence[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, contenedorId);
            error[0] = campo.getError();
        });

        return error[0];
    }

    private void pulsarGuardar() {
        escenario.onActivity(activity -> {
            View boton = obtenerBotonGuardar(activity);

            assertTrue(
                    "El botón guardar debe estar habilitado antes de pulsarlo.",
                    boton.isEnabled()
            );

            boton.performClick();
        });

        esperar(500);
    }

    private void esperarHastaEditarPerfilCargado() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cargado = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            cargado[0] = false;

            escenario.onActivity(activity -> {
                TextView nombre =
                        obtenerCampoDesdeContenedor(activity, R.id.inputNombre);

                TextView correo =
                        obtenerCampoDesdeContenedor(activity, R.id.inputCorreo);

                if (TestDataSeeder.TEST_NOMBRE.equals(nombre.getText().toString())
                        && TestDataSeeder.TEST_EMAIL.equals(correo.getText().toString())) {
                    cargado[0] = true;
                }
            });

            if (cargado[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargaron los datos del usuario autenticado en editar perfil."
        );
    }

    private void esperarHastaPopupExitoVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View capaPopup = activity.findViewById(R.id.capaPopup);

                assertNotNull(
                        "No se encontró capaPopup.",
                        capaPopup
                );

                visible[0] = capaPopup.isShown();
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció el popup de éxito tras guardar cambios."
        );
    }

    private DocumentSnapshot obtenerDocumentoUsuarioAutenticado()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = seeder.getUidAutenticadoTest();

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("usuarios")
                        .document(uid)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Editar perfil")
    @Description("Al navegar a editar perfil, el destino actual debe ser editarPerfilFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAEditarPerfil_destinoCorrecto() {
        navegarAEditarPerfil();

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

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Editar perfil")
    @Description("La imagen de perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void imagenPerfil_estaVisible() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            View imagen = activity.findViewById(R.id.imgFotoPerfil);

            assertNotNull(
                    "No se encontró imgFotoPerfil.",
                    imagen
            );

            assertTrue(
                    "imgFotoPerfil debe estar visible.",
                    imagen.isShown()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El botón para editar la foto de perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void botonEditarFotoPerfil_estaVisible() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            View boton = activity.findViewById(R.id.btnEditarFotoPerfil);

            assertNotNull(
                    "No se encontró btnEditarFotoPerfil.",
                    boton
            );

            assertTrue(
                    "btnEditarFotoPerfil debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El botón guardar debe estar visible y habilitado.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonGuardar_estaVisibleYHabilitado() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonGuardar(activity);

            assertTrue(
                    "El botón guardar debe estar visible.",
                    boton.isShown()
            );

            assertTrue(
                    "El botón guardar debe estar habilitado.",
                    boton.isEnabled()
            );
        });
    }

    ///////////////////////// campos visibles /////////////////////////

    @Test
    @Story("Editar perfil")
    @Description("El campo nombre debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoNombre_estaVisible() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, R.id.inputNombre);

            assertTrue(
                    "El campo nombre debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El campo usuarioClank debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoUsuarioClank_estaVisible() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, R.id.inputUsuarioClank);

            assertTrue(
                    "El campo usuarioClank debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El campo teléfono debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void campoTelefono_estaVisible() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, R.id.inputTelefono);

            assertTrue(
                    "El campo teléfono debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El campo correo debe estar visible pero no editable.")
    @Severity(SeverityLevel.NORMAL)
    public void campoCorreo_estaVisibleYDeshabilitado() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, R.id.inputCorreo);

            assertTrue(
                    "El campo correo debe estar visible.",
                    campo.isShown()
            );

            assertTrue(
                    "El campo correo no debe estar habilitado para edición.",
                    !campo.isEnabled()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("El campo fecha de nacimiento debe estar visible pero no editable.")
    @Severity(SeverityLevel.NORMAL)
    public void campoFechaNacimiento_estaVisibleYDeshabilitado() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(activity, R.id.inputFechaNacimiento);

            assertTrue(
                    "El campo fecha de nacimiento debe estar visible.",
                    campo.isShown()
            );

            assertTrue(
                    "El campo fecha de nacimiento no debe estar habilitado para edición.",
                    !campo.isEnabled()
            );
        });
    }

    ///////////////////////// interacción con campos /////////////////////////

    @Test
    @Story("Editar perfil")
    @Description("Al escribir el nombre, el texto debe conservarse en el campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoNombre_escribirTexto_muestraTextoEscrito() {
        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputNombre, "Ana García");

        assertEquals(
                "Ana García",
                obtenerTextoCampo(R.id.inputNombre)
        );
    }

    @Test
    @Story("Editar perfil")
    @Description("Al escribir usuarioClank, el texto debe conservarse en el campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoUsuarioClank_escribirTexto_muestraTextoEscrito() {
        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputUsuarioClank, "@ana_clank");

        assertEquals(
                "@ana_clank",
                obtenerTextoCampo(R.id.inputUsuarioClank)
        );
    }

    @Test
    @Story("Editar perfil")
    @Description("Al escribir teléfono, el texto debe conservarse en el campo.")
    @Severity(SeverityLevel.NORMAL)
    public void campoTelefono_escribirTexto_muestraTextoEscrito() {
        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputTelefono, "600123123");

        assertEquals(
                "600123123",
                obtenerTextoCampo(R.id.inputTelefono)
        );
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación de editar perfil")
    @Description("Si el nombre está vacío, se debe mostrar error local antes de guardar.")
    @Severity(SeverityLevel.CRITICAL)
    public void nombreVacio_muestraError() {
        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputNombre, "");
        escribirTextoCampo(R.id.inputUsuarioClank, "@ana_clank");
        escribirTextoCampo(R.id.inputTelefono, "600123123");

        pulsarGuardar();

        CharSequence error = obtenerErrorCampo(R.id.inputNombre);

        assertNotNull(
                "Debe mostrarse error cuando el nombre está vacío.",
                error
        );

        assertTrue(
                "El error de nombre vacío no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de editar perfil")
    @Description("Si usuarioClank está vacío, se debe mostrar error local antes de guardar.")
    @Severity(SeverityLevel.CRITICAL)
    public void usuarioClankVacio_muestraError() {
        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputNombre, "Ana García");
        escribirTextoCampo(R.id.inputUsuarioClank, "");
        escribirTextoCampo(R.id.inputTelefono, "600123123");

        pulsarGuardar();

        CharSequence error = obtenerErrorCampo(R.id.inputUsuarioClank);

        assertNotNull(
                "Debe mostrarse error cuando usuarioClank está vacío.",
                error
        );

        assertTrue(
                "El error de usuarioClank vacío no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    ///////////////////////// guardado real /////////////////////////

    @Test
    @Story("Guardado real de editar perfil")
    @Description("Al guardar datos válidos, debe actualizarse el documento del usuario autenticado en Firestore Emulator y mostrarse popup de éxito.")
    @Severity(SeverityLevel.CRITICAL)
    public void guardarCambiosValidos_persisteEnFirestoreYMuestraPopup()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarAEditarPerfil();
        esperarHastaEditarPerfilCargado();

        escribirTextoCampo(R.id.inputNombre, NOMBRE_EDITADO);
        escribirTextoCampo(R.id.inputUsuarioClank, USUARIO_CLANK_EDITADO);
        escribirTextoCampo(R.id.inputTelefono, TELEFONO_EDITADO);

        pulsarGuardar();

        esperarHastaPopupExitoVisible();

        DocumentSnapshot doc = obtenerDocumentoUsuarioAutenticado();

        assertTrue(
                "El documento del usuario autenticado debe existir.",
                doc.exists()
        );

        assertEquals(
                NOMBRE_EDITADO,
                doc.getString("nombre")
        );

        assertEquals(
                USUARIO_CLANK_EDITADO,
                doc.getString("usuarioClank")
        );

        assertEquals(
                TELEFONO_EDITADO,
                doc.getString("telefono")
        );
    }

    ///////////////////////// navegación secundaria /////////////////////////

    @Test
    @Story("Editar perfil")
    @Description("El enlace cambiar contraseña debe estar visible y ser clicable.")
    @Severity(SeverityLevel.NORMAL)
    public void cambiarContrasenya_estaVisibleYClickable() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            View enlace = activity.findViewById(R.id.tvCambiarContrasenya);

            assertNotNull(
                    "No se encontró tvCambiarContrasenya.",
                    enlace
            );

            assertTrue(
                    "tvCambiarContrasenya debe estar visible.",
                    enlace.isShown()
            );

            assertTrue(
                    "tvCambiarContrasenya debe ser clicable.",
                    enlace.isClickable()
            );
        });
    }

    @Test
    @Story("Editar perfil")
    @Description("Al pulsar cambiar contraseña, debe navegarse a cambiarContrasenyaFragment.")
    @Severity(SeverityLevel.NORMAL)
    public void cambiarContrasenya_navegaCorrectamente() {
        navegarAEditarPerfil();

        escenario.onActivity(activity -> {
            View enlace = activity.findViewById(R.id.tvCambiarContrasenya);

            assertNotNull(
                    "No se encontró tvCambiarContrasenya.",
                    enlace
            );

            enlace.performClick();
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
}