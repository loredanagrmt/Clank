package com.clank.app.ui.registro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
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
public class RegistroUITest {

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

  private void navegarARegistro() {
    escenario.onActivity(activity -> {
      NavController navController =
              Navigation.findNavController(activity, R.id.nav_host_fragment);

      if (navController.getCurrentDestination() != null
              && navController.getCurrentDestination().getId() == R.id.registroFragment) {
        return;
      }

      navController.navigate(R.id.registroFragment);
    });

    esperar(700);
  }

  private void esperar(long millis) {
    SystemClock.sleep(millis);
  }

  private void hacerScrollAbajo() {
    escenario.onActivity(activity -> {
      View scroll = activity.findViewById(R.id.scrollRegistro);

      if (scroll instanceof NestedScrollView) {
        ((NestedScrollView) scroll).fullScroll(View.FOCUS_DOWN);
      } else if (scroll instanceof ScrollView) {
        ((ScrollView) scroll).fullScroll(View.FOCUS_DOWN);
      } else if (scroll != null) {
        scroll.scrollTo(0, scroll.getBottom());
      }
    });

    esperar(500);
  }

  private void comprobarVisible(int viewId, String nombreVista) {
    escenario.onActivity(activity -> {
      View vista = activity.findViewById(viewId);

      assertTrue(
              "No se encontró la vista: " + nombreVista,
              vista != null
      );

      assertTrue(
              "La vista no está visible: " + nombreVista,
              vista.isShown()
      );
    });
  }

  private TextView obtenerCampoDesdeContenedor(ViewGroup contenedor) {
    View campo = contenedor.findViewById(R.id.customEditText);

    assertTrue(
            "El campo customEditText no existe dentro del contenedor.",
            campo != null
    );

    assertTrue(
            "El campo customEditText no es un TextView/EditText válido.",
            campo instanceof TextView
    );

    return (TextView) campo;
  }

  private void comprobarCampoVisible(int contenedorId, String nombreCampo) {
    escenario.onActivity(activity -> {
      View contenedor = activity.findViewById(contenedorId);

      assertTrue(
              "No se encontró el contenedor del campo: " + nombreCampo,
              contenedor instanceof ViewGroup
      );

      TextView campo = obtenerCampoDesdeContenedor((ViewGroup) contenedor);

      assertTrue(
              "El contenedor no está visible: " + nombreCampo,
              contenedor.isShown()
      );

      assertTrue(
              "El campo no está visible: " + nombreCampo,
              campo.isShown()
      );
    });
  }

  private void escribirTextoCampo(int contenedorId, String texto) {
    escenario.onActivity(activity -> {
      View contenedor = activity.findViewById(contenedorId);

      assertTrue(
              "No se encontró el contenedor del campo.",
              contenedor instanceof ViewGroup
      );

      TextView campo = obtenerCampoDesdeContenedor((ViewGroup) contenedor);
      campo.setText(texto);
    });

    esperar(300);
  }

  private String obtenerTextoCampo(int contenedorId) {
    final String[] texto = new String[1];

    escenario.onActivity(activity -> {
      View contenedor = activity.findViewById(contenedorId);

      assertTrue(
              "No se encontró el contenedor del campo.",
              contenedor instanceof ViewGroup
      );

      TextView campo = obtenerCampoDesdeContenedor((ViewGroup) contenedor);
      texto[0] = campo.getText().toString();
    });

    return texto[0];
  }

  ///////////////////////// campos del formulario /////////////////////////

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de nombre completo debe estar visible al abrir el formulario.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoNombreCompleto_estaVisible() {
    navegarARegistro();

    comprobarCampoVisible(
            R.id.inputNombreCompleto,
            "nombre completo"
    );
  }

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de correo electrónico debe estar visible al abrir el formulario.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoCorreo_estaVisible() {
    navegarARegistro();

    comprobarCampoVisible(
            R.id.inputCorreo,
            "correo"
    );
  }

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de teléfono debe estar visible al abrir el formulario.")
  @Severity(SeverityLevel.NORMAL)
  public void campoTelefono_estaVisible() {
    navegarARegistro();

    comprobarCampoVisible(
            R.id.inputTelefono,
            "teléfono"
    );
  }

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de fecha de nacimiento debe estar visible al abrir el formulario.")
  @Severity(SeverityLevel.NORMAL)
  public void campoFechaNacimiento_estaVisible() {
    navegarARegistro();

    comprobarCampoVisible(
            R.id.inputFechaNacimiento,
            "fecha de nacimiento"
    );
  }

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de contraseña debe estar visible en registro con correo y contraseña.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoContrasenya_estaVisible() {
    navegarARegistro();

    comprobarCampoVisible(
            R.id.inputContrasenya,
            "contraseña"
    );
  }

  @Test
  @Story("Visualización del formulario de registro")
  @Description("El campo de confirmar contraseña debe estar visible al hacer scroll en el formulario.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoConfirmarContrasenya_estaVisible() {
    navegarARegistro();
    hacerScrollAbajo();

    comprobarCampoVisible(
            R.id.inputConfirmarContrasenya,
            "confirmar contraseña"
    );
  }

  ///////////////////////// botón y enlaces /////////////////////////

  @Test
  @Story("Acciones del formulario de registro")
  @Description("El botón de registrarme debe estar visible al final del formulario.")
  @Severity(SeverityLevel.CRITICAL)
  public void botonRegistrarme_estaVisible() {
    navegarARegistro();
    hacerScrollAbajo();

    comprobarVisible(
            R.id.btnRegistrarme,
            "botón registrarme"
    );
  }

  @Test
  @Story("Acciones del formulario de registro")
  @Description("El texto de ya tienes cuenta debe estar visible al final del formulario.")
  @Severity(SeverityLevel.NORMAL)
  public void tvYaTienesCuenta_estaVisible() {
    navegarARegistro();
    hacerScrollAbajo();

    comprobarVisible(
            R.id.tvYaTienesCuenta,
            "texto ya tienes cuenta"
    );
  }

  @Test
  @Story("Acciones del formulario de registro")
  @Description("El enlace para volver al inicio de sesión debe estar visible al final del formulario.")
  @Severity(SeverityLevel.NORMAL)
  public void tvIrInicioSesion_estaVisible() {
    navegarARegistro();
    hacerScrollAbajo();

    comprobarVisible(
            R.id.tvIrInicioSesion,
            "enlace ir a inicio sesión"
    );
  }

  ///////////////////////// interacción con campos /////////////////////////

  @Test
  @Story("Introducción de datos de registro")
  @Description("Al escribir en el campo nombre, el texto debe reflejarse correctamente.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoNombre_escribirTexto_muestraTextoEscrito() {
    navegarARegistro();

    escribirTextoCampo(
            R.id.inputNombreCompleto,
            "Ana Garcia"
    );

    assertEquals(
            "Ana Garcia",
            obtenerTextoCampo(R.id.inputNombreCompleto)
    );
  }

  @Test
  @Story("Introducción de datos de registro")
  @Description("Al escribir en el campo correo, el texto debe reflejarse correctamente.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoCorreo_escribirTexto_muestraTextoEscrito() {
    navegarARegistro();

    escribirTextoCampo(
            R.id.inputCorreo,
            "ana@clank.com"
    );

    assertEquals(
            "ana@clank.com",
            obtenerTextoCampo(R.id.inputCorreo)
    );
  }
}