package com.clank.app.ui.olvideContrasenya;

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
public class OlvideContrasenyaUITest {

    @Rule(order = 0)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 1)
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

    private void navegarAOlvideContrasenya() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.olvideContrasenyaFragment) {
                return;
            }

            navController.navigate(R.id.olvideContrasenyaFragment);
        });

        esperar(700);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private TextView obtenerCampoCorreo(MainActivity activity) {
        View contenedor = activity.findViewById(R.id.inputCorreoOlvideContrasenya);

        assertTrue(
                "No se encontró el contenedor inputCorreoOlvideContrasenya.",
                contenedor instanceof ViewGroup
        );

        View campo = ((ViewGroup) contenedor).findViewById(R.id.customEditText);

        assertTrue(
                "No se encontró customEditText dentro de inputCorreoOlvideContrasenya.",
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
        View contenedorFooter = activity.findViewById(R.id.contenedorBotonOlvideContrasenya);

        assertTrue(
                "No se encontró el contenedor footer contenedorBotonOlvideContrasenya.",
                contenedorFooter instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedorFooter);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro del footer de recuperación.",
                boton
        );

        return boton;
    }

    private void escribirCorreo(String correo) {
        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoCorreo(activity);
            campo.setText(correo);
        });

        esperar(300);
    }

    private String obtenerTextoCorreo() {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoCorreo(activity);
            texto[0] = campo.getText().toString();
        });

        return texto[0];
    }

    private CharSequence obtenerErrorCorreo() {
        final CharSequence[] error = new CharSequence[1];

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoCorreo(activity);
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
    @Story("Recuperación de contraseña")
    @Description("El campo de correo debe estar visible en la pantalla de recuperación.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoCorreo_estaVisible() {
        navegarAOlvideContrasenya();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoCorreo(activity);

            assertTrue(
                    "El campo correo debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Recuperación de contraseña")
    @Description("El botón continuar debe estar visible en la pantalla de recuperación.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonContinuar_estaVisible() {
        navegarAOlvideContrasenya();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Recuperación de contraseña")
    @Description("El botón continuar debe estar habilitado inicialmente.")
    @Severity(SeverityLevel.NORMAL)
    public void botonContinuar_estaHabilitadoInicialmente() {
        navegarAOlvideContrasenya();

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
    @Story("Introducción de correo")
    @Description("Al escribir un correo, el texto debe reflejarse en el campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoCorreo_escribirTexto_muestraTextoEscrito() {
        navegarAOlvideContrasenya();

        escribirCorreo("ana@clank.com");

        assertEquals(
                "ana@clank.com",
                obtenerTextoCorreo()
        );
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación de correo")
    @Description("Si el correo está vacío, el campo debe mostrar error local y no llamar al backend.")
    @Severity(SeverityLevel.CRITICAL)
    public void correoVacio_muestraError() {
        navegarAOlvideContrasenya();

        escribirCorreo("");
        pulsarContinuar();

        CharSequence error = obtenerErrorCorreo();

        assertNotNull(
                "El campo correo debe mostrar error cuando está vacío.",
                error
        );

        assertTrue(
                "El error de correo vacío no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de correo")
    @Description("Si el correo tiene formato inválido, el campo debe mostrar error local y no llamar al backend.")
    @Severity(SeverityLevel.CRITICAL)
    public void correoInvalido_muestraError() {
        navegarAOlvideContrasenya();

        escribirCorreo("correo-invalido");
        pulsarContinuar();

        CharSequence error = obtenerErrorCorreo();

        assertNotNull(
                "El campo correo debe mostrar error cuando el formato es inválido.",
                error
        );

        assertTrue(
                "El error de correo inválido no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }
}