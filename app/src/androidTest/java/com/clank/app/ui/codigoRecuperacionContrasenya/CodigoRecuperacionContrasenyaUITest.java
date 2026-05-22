package com.clank.app.ui.codigoRecuperacionContrasenya;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
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
public class CodigoRecuperacionContrasenyaUITest {

    private static final String CORREO_TEST = "ana@clank.com";

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

    private void navegarACodigoRecuperacion() {
        navegarACodigoRecuperacion(CORREO_TEST);
    }

    private void navegarACodigoRecuperacion(String correoRecuperacion) {
        Bundle args = new Bundle();
        args.putString("correoRecuperacion", correoRecuperacion);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId()
                    == R.id.codigoRecuperacionContrasenyaFragment) {
                return;
            }

            navController.navigate(
                    R.id.codigoRecuperacionContrasenyaFragment,
                    args
            );
        });

        esperar(700);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private EditText[] obtenerCasillas(MainActivity activity) {
        return new EditText[]{
                activity.findViewById(R.id.etCodigoUno),
                activity.findViewById(R.id.etCodigoDos),
                activity.findViewById(R.id.etCodigoTres),
                activity.findViewById(R.id.etCodigoCuatro),
                activity.findViewById(R.id.etCodigoCinco),
                activity.findViewById(R.id.etCodigoSeis)
        };
    }

    private void comprobarCasillasExisten(EditText[] casillas) {
        assertEquals(
                "Deben existir 6 casillas para el código de recuperación.",
                6,
                casillas.length
        );

        for (int i = 0; i < casillas.length; i++) {
            assertNotNull(
                    "No se encontró la casilla de código número " + (i + 1),
                    casillas[i]
            );
        }
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
        View contenedorFooter =
                activity.findViewById(R.id.contenedorBotonCodigoRecuperacionContrasenya);

        assertTrue(
                "No se encontró el footer contenedorBotonCodigoRecuperacionContrasenya.",
                contenedorFooter instanceof ViewGroup
        );

        View boton = buscarVistaClicable(contenedorFooter);

        assertNotNull(
                "No se encontró ninguna vista clicable dentro del footer del código.",
                boton
        );

        return boton;
    }

    private void escribirCodigo(String codigo) {
        escenario.onActivity(activity -> {
            EditText[] casillas = obtenerCasillas(activity);
            comprobarCasillasExisten(casillas);

            for (int i = 0; i < casillas.length; i++) {
                String caracter = "";

                if (codigo != null && i < codigo.length()) {
                    caracter = String.valueOf(codigo.charAt(i));
                }

                casillas[i].setText(caracter);
            }
        });

        esperar(300);
    }

    private String obtenerCodigoEscrito() {
        final String[] codigo = new String[1];

        escenario.onActivity(activity -> {
            EditText[] casillas = obtenerCasillas(activity);
            comprobarCasillasExisten(casillas);

            StringBuilder builder = new StringBuilder();

            for (EditText casilla : casillas) {
                builder.append(casilla.getText().toString());
            }

            codigo[0] = builder.toString();
        });

        return codigo[0];
    }

    private CharSequence obtenerErrorPrimeraCasilla() {
        final CharSequence[] error = new CharSequence[1];

        escenario.onActivity(activity -> {
            EditText primeraCasilla = activity.findViewById(R.id.etCodigoUno);

            assertNotNull(
                    "No se encontró etCodigoUno.",
                    primeraCasilla
            );

            error[0] = primeraCasilla.getError();
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
    @Story("Código de recuperación")
    @Description("La pantalla debe mostrar las seis casillas del código de recuperación.")
    @Severity(SeverityLevel.CRITICAL)
    public void casillasCodigo_estanVisibles() {
        navegarACodigoRecuperacion();

        escenario.onActivity(activity -> {
            EditText[] casillas = obtenerCasillas(activity);
            comprobarCasillasExisten(casillas);

            for (EditText casilla : casillas) {
                assertTrue(
                        "Cada casilla de código debe estar visible.",
                        casilla.isShown()
                );
            }
        });
    }

    @Test
    @Story("Código de recuperación")
    @Description("El botón continuar debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonContinuar_estaVisible() {
        navegarACodigoRecuperacion();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar visible.",
                    boton.isShown()
            );
        });
    }

    @Test
    @Story("Código de recuperación")
    @Description("El botón continuar debe estar habilitado inicialmente.")
    @Severity(SeverityLevel.NORMAL)
    public void botonContinuar_estaHabilitadoInicialmente() {
        navegarACodigoRecuperacion();

        escenario.onActivity(activity -> {
            View boton = obtenerBotonContinuar(activity);

            assertTrue(
                    "El botón continuar debe estar habilitado inicialmente.",
                    boton.isEnabled()
            );
        });
    }

    ///////////////////////// interacción con código /////////////////////////

    @Test
    @Story("Introducción de código")
    @Description("Al introducir seis dígitos, las casillas deben conservar el código escrito.")
    @Severity(SeverityLevel.CRITICAL)
    public void escribirCodigoCompleto_muestraCodigoEscrito() {
        navegarACodigoRecuperacion();

        escribirCodigo("123456");

        assertEquals(
                "123456",
                obtenerCodigoEscrito()
        );
    }

    @Test
    @Story("Introducción de código")
    @Description("Al introducir un código parcial, las casillas deben conservar los dígitos escritos.")
    @Severity(SeverityLevel.NORMAL)
    public void escribirCodigoParcial_muestraCodigoEscrito() {
        navegarACodigoRecuperacion();

        escribirCodigo("123");

        assertEquals(
                "123",
                obtenerCodigoEscrito()
        );
    }

    ///////////////////////// validaciones locales /////////////////////////

    @Test
    @Story("Validación de código")
    @Description("Si el código está incompleto, se debe mostrar error local antes de llamar al backend.")
    @Severity(SeverityLevel.CRITICAL)
    public void codigoIncompleto_muestraError() {
        navegarACodigoRecuperacion();

        escribirCodigo("123");
        pulsarContinuar();

        CharSequence error = obtenerErrorPrimeraCasilla();

        assertNotNull(
                "La primera casilla debe mostrar error cuando el código está incompleto.",
                error
        );

        assertTrue(
                "El error de código incompleto no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }

    @Test
    @Story("Validación de código")
    @Description("Si falta el correo de recuperación, se debe mostrar error general antes de llamar al backend.")
    @Severity(SeverityLevel.NORMAL)
    public void correoRecuperacionVacio_muestraError() {
        navegarACodigoRecuperacion("");

        escribirCodigo("123456");
        pulsarContinuar();

        CharSequence error = obtenerErrorPrimeraCasilla();

        assertNotNull(
                "La primera casilla debe mostrar error si no hay correo de recuperación.",
                error
        );

        assertTrue(
                "El error por correo de recuperación vacío no debe estar vacío.",
                error.toString().trim().length() > 0
        );
    }
}