package com.clank.app.ui;

import androidx.navigation.Navigation;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.clank.app.MainActivity;
import com.clank.app.R;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegistroUITest {

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
    new ActivityScenarioRule<>(MainActivity.class);

  private void navegarARegistro() {
    activityRule.getScenario().onActivity(activity ->
      Navigation.findNavController(activity, R.id.nav_host_fragment)
        .navigate(R.id.registroFragment)
    );
  }

  /////////////////////////campos del formulario/////////////////////////

  @Test
  public void campoNombreCompleto_estaVisible() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputNombreCompleto))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoCorreo_estaVisible() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoTelefono_estaVisible() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputTelefono))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoFechaNacimiento_estaVisible() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputFechaNacimiento))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoContrasenya_estaVisible() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputContrasenya))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoConfirmarContrasenya_estaVisible() {
    navegarARegistro();

    // Este campo puede quedar fuera de pantalla: hay que hacer scroll
    Espresso.onView(ViewMatchers.withId(R.id.scrollRegistro))
      .perform(ViewActions.swipeUp());

    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputConfirmarContrasenya))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////botón y enlaces/////////////////////////

  @Test
  public void botonRegistrarme_estaVisible() {
    navegarARegistro();

    Espresso.onView(ViewMatchers.withId(R.id.scrollRegistro))
      .perform(ViewActions.swipeUp());

    Espresso.onView(ViewMatchers.withId(R.id.btnRegistrarme))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void tvYaTienesCuenta_estaVisible() {
    navegarARegistro();

    Espresso.onView(ViewMatchers.withId(R.id.scrollRegistro))
      .perform(ViewActions.swipeUp());

    Espresso.onView(ViewMatchers.withId(R.id.tvYaTienesCuenta))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void tvIrInicioSesion_estaVisible() {
    navegarARegistro();

    Espresso.onView(ViewMatchers.withId(R.id.scrollRegistro))
      .perform(ViewActions.swipeUp());

    Espresso.onView(ViewMatchers.withId(R.id.tvIrInicioSesion))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////interscción con campos/////////////////////////

  // ── Interacción con campos ───────────────────────────────────

  @Test
  public void campoNombre_escribirTexto_muestraTextoEscrito() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputNombreCompleto))
    )).perform(ViewActions.replaceText("Ana Garcia"), ViewActions.closeSoftKeyboard());

    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputNombreCompleto))
    )).check(ViewAssertions.matches(ViewMatchers.withText("Ana Garcia")));
  }

  @Test
  public void campoCorreo_escribirTexto_muestraTextoEscrito() {
    navegarARegistro();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).perform(ViewActions.replaceText("ana@clank.com"), ViewActions.closeSoftKeyboard());

    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).check(ViewAssertions.matches(ViewMatchers.withText("ana@clank.com")));
  }
}
