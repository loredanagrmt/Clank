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
public class InicioSesionUITest {

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
    new ActivityScenarioRule<>(MainActivity.class);

  private void navegarAInicioSesion() {
    activityRule.getScenario().onActivity(activity ->
      Navigation.findNavController(activity, R.id.nav_host_fragment)
        .navigate(R.id.inicioSesionFragment)
    );
  }

  /////////////////////////campos del formulario/////////////////////////

  @Test
  public void campoCorreo_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoContrasenya_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputContrasenya))
    )).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////botones/////////////////////////

  @Test
  public void botonIniciarSesion_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(ViewMatchers.withId(R.id.btnIniciarSesion))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void botonRegistrarse_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(ViewMatchers.withId(R.id.btnRegistrarse))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  //////////////////////////auxiliares/////////////////////////

  @Test
  public void tvOlvidoContrasenya_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(ViewMatchers.withId(R.id.tvOlvidoContrasenya))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void imgGoogle_estaVisible() {
    navegarAInicioSesion();
    Espresso.onView(ViewMatchers.withId(R.id.imgGoogle))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////interaccion/////////////////////////

  @Test
  public void campoCorreo_escribirTexto_muestraTextoEscrito() {
    navegarAInicioSesion();
    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).perform(ViewActions.typeText("test@clank.com"), ViewActions.closeSoftKeyboard());

    Espresso.onView(Matchers.allOf(
      ViewMatchers.withId(R.id.customEditText),
      ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputCorreo))
    )).check(ViewAssertions.matches(ViewMatchers.withText("test@clank.com")));
  }

  @Test
  public void campoContrasenya_escribirTexto_aceptaInput() {
    navegarAInicioSesion();
    Espresso.onView(Matchers.allOf(
        ViewMatchers.withId(R.id.customEditText),
        ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.inputContrasenya))
      )).perform(ViewActions.typeText("pass123"), ViewActions.closeSoftKeyboard())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }
}
