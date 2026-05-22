package com.clank.app.ui.nuevaContrasenya;

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
public class NuevaContrasenyaUITest {

    private static final String CORREO_TEST = "ana@clank.com";
    private static final String TOKEN_TEST = "token-test";

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

    private void navegarANuevaContrasenya() {
        navegarANuevaContrasenya(CORREO_TEST, TOKEN_TEST);
    }

    private void navegarANuevaContrasenya(String correo, String token) {
        Bundle args = new Bundle();
        args.putString("correoRecuperacion", correo);
        args.putString("tokenRecuperacion", token);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.nuevaContrasenyaFragment) {
                return;
            }

            navController.navigate(R.id.nuevaContrasenyaFragment, args);
        });

        esperar(700);
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

    private View obtenerBotonContinuar(MainActivity activity) {
        View contenedorFooter = activity.findViewById(R.id.contenedorBotonNuevaContrasenya);

        assertTrue(
                "No se encontró el footer contenedorBotonNuevaContrasenya.",
                contenedorFooter instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedorFooter);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro del footer de nueva contraseña.",
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
    @Story("Nueva contraseña")
    @Description("El campo de nueva contraseña debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoNuevaContrasenya_estaVisible() {
        navegarANuevaContrasenya();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(
                    activity,
                    R.id.inputNuevaContrasenya
            );

            assertTrue(
                    "El campo nueva contraseña debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Nueva contraseña")
    @Description("El campo de repetir nueva contraseña debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoRepetirNuevaContrasenya_estaVisible() {
        navegarANuevaContrasenya();

        escenario.onActivity(activity -> {
            TextView campo = obtenerCampoDesdeContenedor(
                    activity,
                    R.id.inputRepetirNuevaContrasenya
            );

            assertTrue(
                    "El campo repetir nueva contraseña debe estar visible.",
                    campo.isShown()
            );
        });
    }

    @Test
    @Story("Nueva contraseña")
    @Description("El botón continuar debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonContinuar_estaVisible() {
        navegarANuevaContrasenya();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Nueva contraseña")
    @Description("El botón continuar debe estar habilitado inicialmente.")
    @Severity(SeverityLevel.NORMAL)
    public void botonContinuar_estaHabilitadoInicialmente() {
        navegarANuevaContrasenya();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar habilitado inicialmente.",
                    boton.isEnabled()
            );
        });
    }

    ///////////////////////// interacción con campos /////////////////////////

    @Test
    @Story("Introducción de nueva contraseña")
    @Description("Al escribir una nueva contraseña, el texto debe quedar almacenado en el campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoNuevaContrasenya_escribirTexto_muestraTextoEscrito() {
        navegarANuevaContrasenya();

        escribirTextoCampo(
                R.id.inputNuevaContrasenya,
                "Nueva123"
        );

        assertEquals(
                "Nueva123",
                obtenerTextoCampo(R.id.inputNuevaContrasenya)
        );
    }

    @Test
    @Story("Introducción de nueva contraseña")
    @Description("Al escribir la repetición de contraseña, el texto debe quedar almacenado en el campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void campoRepetirNuevaContrasenya_escribirTexto_muestraTextoEscrito() {
        navegarANuevaContrasenya();

        escribirTextoCampo(
                R.id.inputRepetirNuevaContrasenya,
                "Nueva123"
        );

        assertEquals(
                "Nueva123",
                obtenerTextoCampo(R.id.inputRepetirNuevaContrasenya)
        );
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación de nueva contraseña")
    @Description("Si la nueva contraseña está vacía, se muestra error local en el primer campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void nuevaContrasenyaVacia_muestraError() {
        navegarANuevaContrasenya();

        escribirTextoCampo(R.id.inputNuevaContrasenya, "");
        escribirTextoCampo(R.id.inputRepetirNuevaContrasenya, "");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando la nueva contraseña está vacía.",
                error
        );

        assertTrue(
                "El error de contraseña vacía no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de nueva contraseña")
    @Description("Si la nueva contraseña tiene menos de seis caracteres, se muestra error local.")
    @Severity(SeverityLevel.CRITICAL)
    public void nuevaContrasenyaCorta_muestraError() {
        navegarANuevaContrasenya();

        escribirTextoCampo(R.id.inputNuevaContrasenya, "123");
        escribirTextoCampo(R.id.inputRepetirNuevaContrasenya, "123");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando la contraseña es demasiado corta.",
                error
        );

        assertTrue(
                "El error de contraseña corta no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de nueva contraseña")
    @Description("Si la repetición está vacía, se muestra error local en el segundo campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void repetirContrasenyaVacia_muestraError() {
        navegarANuevaContrasenya();

        escribirTextoCampo(R.id.inputNuevaContrasenya, "Nueva123");
        escribirTextoCampo(R.id.inputRepetirNuevaContrasenya, "");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputRepetirNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando la repetición de contraseña está vacía.",
                error
        );

        assertTrue(
                "El error de repetición vacía no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de nueva contraseña")
    @Description("Si las contraseñas no coinciden, se muestra error local en el segundo campo.")
    @Severity(SeverityLevel.CRITICAL)
    public void contrasenyasNoCoinciden_muestraError() {
        navegarANuevaContrasenya();

        escribirTextoCampo(R.id.inputNuevaContrasenya, "Nueva123");
        escribirTextoCampo(R.id.inputRepetirNuevaContrasenya, "Otra123");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputRepetirNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando las contraseñas no coinciden.",
                error
        );

        assertTrue(
                "El error de contraseñas no coincidentes no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de nueva contraseña")
    @Description("Si faltan los datos de recuperación, se muestra error local antes de llamar al backend.")
    @Severity(SeverityLevel.NORMAL)
    public void datosRecuperacionVacios_muestraError() {
        navegarANuevaContrasenya("", "");

        escribirTextoCampo(R.id.inputNuevaContrasenya, "Nueva123");
        escribirTextoCampo(R.id.inputRepetirNuevaContrasenya, "Nueva123");

        pulsarContinuar();

        CharSequence error = obtenerErrorCampo(R.id.inputNuevaContrasenya);

        assertNotNull(
                "Debe mostrarse error cuando faltan correo o token de recuperación.",
                error
        );

        assertTrue(
                "El error de datos de recuperación no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }
}