package com.clank.app.ui.completarPerfil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.util.AllureScreenshotWatcher;

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
@Feature("Autenticación")
public class CompletarPerfilUITest {

    private static final String PREFERENCIAS_REGISTRO = "registro_temporal";

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    private ActivityScenario<MainActivity> escenario;

    @Before
    public void setUp() {
        limpiarPreferenciasRegistro();

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

        limpiarPreferenciasRegistro();
    }

    private void limpiarPreferenciasRegistro() {
        Context contexto = ApplicationProvider.getApplicationContext();

        contexto.getSharedPreferences(PREFERENCIAS_REGISTRO, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    private void navegarACompletarPerfil() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.completarPerfilFragment) {
                return;
            }

            navController.navigate(R.id.completarPerfilFragment);
        });

        esperar(700);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void comprobarVistaVisible(int viewId, String nombreVista) {
        escenario.onActivity(activity -> {
            View vista = activity.findViewById(viewId);

            assertNotNull(
                    "No se encontró la vista: " + nombreVista,
                    vista
            );

            assertTrue(
                    "La vista no está visible: " + nombreVista,
                    vista.isShown()
            );
        });
    }

    private TextView obtenerCampoUsuarioClank(MainActivity activity) {
        View contenedor = activity.findViewById(R.id.inputUsuarioClank);

        assertTrue(
                "No se encontró el contenedor inputUsuarioClank.",
                contenedor instanceof ViewGroup
        );

        View campo = ((ViewGroup) contenedor).findViewById(R.id.customEditText);

        assertTrue(
                "No se encontró customEditText dentro de inputUsuarioClank.",
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

    private View obtenerBotonContinuar(MainActivity activity) {
        View contenedorFooter = activity.findViewById(R.id.contenedorBotonContinuar);

        assertTrue(
                "No se encontró el contenedor contenedorBotonContinuar.",
                contenedorFooter instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedorFooter);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro del footer de completar perfil.",
                boton
        );

        return boton;
    }

    private void escribirUsuarioClank(String usuarioClank) {
        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoUsuarioClank(activity);
            campo.setText(usuarioClank);
        });

        esperar(300);
    }

    private String obtenerTextoUsuarioClank() {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoUsuarioClank(activity);
            texto[0] = campo.getText().toString();
        });

        return texto[0];
    }

    private CharSequence obtenerErrorUsuarioClank() {
        final CharSequence[] error = new CharSequence[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoUsuarioClank(activity);
            error[0] = campo.getError();
        });

        return error[0];
    }

    private void pulsarContinuar() {
        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible antes de pulsarlo.",
                    boton.isShown()
            );

            assertTrue(
                    "El botón continuar debe estar habilitado antes de pulsarlo.",
                    boton.isEnabled()
            );

            boton.performClick();
        });

        esperar(500);
    }

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Completar perfil")
    @Description("El título de completar perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void tituloCompletarPerfil_estaVisible() {
        navegarACompletarPerfil();

        comprobarVistaVisible(
                R.id.tvTituloCompletarPerfil,
                "título completar perfil"
        );
    }

    @Test
    @Story("Completar perfil")
    @Description("La imagen de perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void imagenPerfil_estaVisible() {
        navegarACompletarPerfil();

        comprobarVistaVisible(
                R.id.imgFotoPerfil,
                "imagen foto perfil"
        );
    }

    @Test
    @Story("Completar perfil")
    @Description("El botón de editar foto de perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void botonEditarFotoPerfil_estaVisible() {
        navegarACompletarPerfil();

        comprobarVistaVisible(
                R.id.btnEditarFotoPerfil,
                "botón editar foto perfil"
        );
    }

    @Test
    @Story("Completar perfil")
    @Description("El campo usuarioClank debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoUsuarioClank_estaVisible() {
        navegarACompletarPerfil();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoUsuarioClank(activity);

            assertTrue(
                    "El campo usuarioClank debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Completar perfil")
    @Description("El botón continuar debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonContinuar_estaVisible() {
        navegarACompletarPerfil();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Completar perfil")
    @Description("El botón continuar debe estar habilitado inicialmente.")
    @Severity(SeverityLevel.NORMAL)
    public void botonContinuar_estaHabilitadoInicialmente() {
        navegarACompletarPerfil();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar habilitado inicialmente.",
                    boton.isEnabled()
            );
        });
    }

    ///////////////////////// interacción con campo /////////////////////////

    @Test
    @Story("Completar perfil")
    @Description("Al escribir el usuario Clank, el campo debe conservar el texto escrito.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoUsuarioClank_escribirTexto_muestraTextoEscrito() {
        navegarACompletarPerfil();

        escribirUsuarioClank("@ana_clank");

        assertEquals(
                "@ana_clank",
                obtenerTextoUsuarioClank()
        );
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación de completar perfil")
    @Description("Si usuarioClank está vacío, se muestra error local sin llamar al backend.")
    @Severity(SeverityLevel.CRITICAL)
    public void usuarioClankVacio_muestraError() {
        navegarACompletarPerfil();

        escribirUsuarioClank("");
        pulsarContinuar();

        CharSequence error = obtenerErrorUsuarioClank();

        assertNotNull(
                "Debe mostrarse error cuando usuarioClank está vacío.",
                error
        );

        assertTrue(
                "El error de usuarioClank vacío no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de completar perfil")
    @Description("Si hay usuarioClank pero faltan los datos temporales de registro, se muestra error local.")
    @Severity(SeverityLevel.CRITICAL)
    public void datosRegistroFaltantes_muestraError() {
        navegarACompletarPerfil();

        escribirUsuarioClank("@ana_clank");
        pulsarContinuar();

        CharSequence error = obtenerErrorUsuarioClank();

        assertNotNull(
                "Debe mostrarse error cuando faltan datos temporales de registro.",
                error
        );

        assertTrue(
                "El error de datos de registro faltantes no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }
}