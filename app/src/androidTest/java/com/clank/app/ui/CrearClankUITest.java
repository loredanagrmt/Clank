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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CrearClankUITest {

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
    new ActivityScenarioRule<>(MainActivity.class);

  private void navegarACrear() {
    activityRule.getScenario().onActivity(activity ->
      Navigation.findNavController(activity, R.id.nav_host_fragment)
        .navigate(R.id.crearFragment)
    );
  }

  /////////////////////////estructura principal/////////////////////////

  @Test
  public void framePortada_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.framePortada))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoTitulo_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void campoDescripcion_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////botones tiempo/////////////////////////

  @Test
  public void btnTiempoCohete_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoCohete))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void btnTiempoLiebre_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoLiebre))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void btnTiempoTortuga_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoTortuga))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////botones de seccion/////////////////////////

  @Test
  public void btnAnyadirMaterial_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirMaterial))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void btnAnyadirHerramienta_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirHerramienta))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void btnAnyadirInstruccion_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirInstruccion))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////botones principales/////////////////////////

  @Test
  public void btnPublicar_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnPublicar))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  public void btnGuardarBoceto_estaVisible() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnGuardarBoceto))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  /////////////////////////interaccion con campos/////////////////////////

  @Test
  public void campoTitulo_escribirTexto_muestraTextoEscrito() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
      .perform(ViewActions.replaceText("Maceta de arcilla"), ViewActions.closeSoftKeyboard());

    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
      .check(ViewAssertions.matches(ViewMatchers.withText("Maceta de arcilla")));
  }

  @Test
  public void campoDescripcion_escribirTexto_muestraTextoEscrito() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
      .perform(ViewActions.replaceText("Una bonita maceta hecha a mano"), ViewActions.closeSoftKeyboard());

    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
      .check(ViewAssertions.matches(ViewMatchers.withText("Una bonita maceta hecha a mano")));
  }

  /////////////////////////botones de tiempo/////////////////////////

  @Test
  public void btnTiempoCohete_esClickable() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoCohete))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isClickable()));
  }

  @Test
  public void btnAnyadirMaterial_esClickable() {
    navegarACrear();
    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirMaterial))
      .perform(ViewActions.scrollTo())
      .check(ViewAssertions.matches(ViewMatchers.isClickable()));
  }
}
