package com.clank.app.ui.inicio;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.util.AllureScreenshotWatcher;

import org.hamcrest.Matcher;
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
public class InicioSesionUITest {

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

  private void navegarAInicioSesion() {
    escenario.onActivity(activity -> {
      NavController navController =
              Navigation.findNavController(activity, R.id.nav_host_fragment);

      if (navController.getCurrentDestination() != null
              && navController.getCurrentDestination().getId() == R.id.inicioSesionFragment) {
        return;
      }

      navController.navigate(R.id.inicioSesionFragment);
    });

    esperar(700);
  }

  private void esperar(long millis) {
    SystemClock.sleep(millis);
  }

  private Matcher<View> campoCorreo() {
    return allOf(
            withId(R.id.customEditText),
            ViewMatchers.isDescendantOfA(withId(R.id.inputCorreo))
    );
  }

  private Matcher<View> campoContrasenya() {
    return allOf(
            withId(R.id.customEditText),
            ViewMatchers.isDescendantOfA(withId(R.id.inputContrasenya))
    );
  }

  private void escribirTextoCampo(int contenedorId, String texto) {
    escenario.onActivity(activity -> {
      View contenedor = activity.findViewById(contenedorId);

      if (!(contenedor instanceof ViewGroup)) {
        throw new AssertionError("No se encontró el contenedor del campo.");
      }

      View campo = contenedor.findViewById(R.id.customEditText);

      if (!(campo instanceof TextView)) {
        throw new AssertionError("El campo customEditText no es un TextView/EditText válido.");
      }

      ((TextView) campo).setText(texto);
    });

    esperar(300);
  }

  private String obtenerTextoCampo(int contenedorId) {
    final String[] texto = new String[1];

    escenario.onActivity(activity -> {
      View contenedor = activity.findViewById(contenedorId);

      if (!(contenedor instanceof ViewGroup)) {
        throw new AssertionError("No se encontró el contenedor del campo.");
      }

      View campo = contenedor.findViewById(R.id.customEditText);

      if (!(campo instanceof TextView)) {
        throw new AssertionError("El campo customEditText no es un TextView/EditText válido.");
      }

      texto[0] = ((TextView) campo).getText().toString();
    });

    return texto[0];
  }

  @Test
  @Story("Visualización del formulario")
  @Description("El campo de correo electrónico debe estar visible al acceder a inicio de sesión.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoCorreo_estaVisible() {
    navegarAInicioSesion();

    onView(campoCorreo())
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Visualización del formulario")
  @Description("El campo de contraseña debe estar visible al acceder a inicio de sesión.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoContrasenya_estaVisible() {
    navegarAInicioSesion();

    onView(campoContrasenya())
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Elementos de acción")
  @Description("El botón de iniciar sesión debe estar visible en el formulario.")
  @Severity(SeverityLevel.CRITICAL)
  public void botonIniciarSesion_estaVisible() {
    navegarAInicioSesion();

    onView(withId(R.id.btnIniciarSesion))
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Elementos de acción")
  @Description("El botón de registro debe estar visible para usuarios sin cuenta.")
  @Severity(SeverityLevel.NORMAL)
  public void botonRegistrarse_estaVisible() {
    navegarAInicioSesion();

    onView(withId(R.id.btnRegistrarse))
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Elementos auxiliares")
  @Description("El enlace de recuperación de contraseña debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void tvOlvidoContrasenya_estaVisible() {
    navegarAInicioSesion();

    onView(withId(R.id.tvOlvidoContrasenya))
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Elementos auxiliares")
  @Description("El icono de inicio de sesión con Google debe estar visible.")
  @Severity(SeverityLevel.MINOR)
  public void imgGoogle_estaVisible() {
    navegarAInicioSesion();

    onView(withId(R.id.imgGoogle))
            .check(matches(isDisplayed()));
  }

  @Test
  @Story("Introducción de credenciales")
  @Description("Al escribir en el campo correo, el texto debe reflejarse correctamente.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoCorreo_escribirTexto_muestraTextoEscrito() {
    navegarAInicioSesion();

    escribirTextoCampo(R.id.inputCorreo, "test@clank.com");

    assertEquals(
            "test@clank.com",
            obtenerTextoCampo(R.id.inputCorreo)
    );
  }

  @Test
  @Story("Introducción de credenciales")
  @Description("El campo contraseña debe aceptar entrada de texto sin romper la interfaz.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoContrasenya_escribirTexto_aceptaInput() {
    navegarAInicioSesion();

    escribirTextoCampo(R.id.inputContrasenya, "pass123");

    assertEquals(
            "pass123",
            obtenerTextoCampo(R.id.inputContrasenya)
    );
  }
}