package com.clank.app.ui.navegacion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import android.util.Log;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.clank.app.MainActivity;
import com.clank.app.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class NavegacionUITest {

  private static final String TAG = "TEST_NAV";

  @Rule
  public ActivityScenarioRule<MainActivity> activityRule =
    new ActivityScenarioRule<>(MainActivity.class);

  private NavController getNavController() {
    final NavController[] nav = new NavController[1];
    activityRule.getScenario().onActivity(activity ->
      nav[0] = Navigation.findNavController(activity, R.id.nav_host_fragment)
    );
    return nav[0];
  }

  private void navegarA(int destinoId) {
    activityRule.getScenario().onActivity(activity ->
      Navigation.findNavController(activity, R.id.nav_host_fragment)
        .navigate(destinoId)
    );
  }

  private void logDestino(String momento) {
    activityRule.getScenario().onActivity(activity -> {
      NavController nav = Navigation.findNavController(activity, R.id.nav_host_fragment);
      if (nav.getCurrentDestination() != null) {
        Log.d(TAG, momento
          + " | id=" + nav.getCurrentDestination().getId()
          + " | name=" + nav.getCurrentDestination().getDisplayName());
      } else {
        Log.d(TAG, momento + " | destino=NULL");
      }
    });
  }

  /////////////////////////destino inicial/////////////////////////

  @Test
  public void appArranca_destinoEsValido() {
    logDestino("appArranca · arranque");

    NavController nav = getNavController();
    int destino = nav.getCurrentDestination().getId();
    boolean esValido = destino == R.id.logoFragment
      || destino == R.id.feedFragment
      || destino == R.id.inspirarFragment
      || destino == R.id.elegirIdiomaFragment
      || destino == R.id.bienvenidaFragment;

    Log.d(TAG, "appArranca · esValido=" + esValido + " id=" + destino);
    assertEquals(true, esValido);
  }

  /////////////////////////desde inicio sesión/////////////////////////

  @Test
  public void desdeSesionNavegarARegistroDestinoCorrecto() {
    logDestino("desdeSesion·Registro · antes de navegar");
    navegarA(R.id.inicioSesionFragment);
    logDestino("desdeSesion·Registro · tras navegar a inicioSesion");

    onView(withId(R.id.btnRegistrarse)).check(matches(isDisplayed()));

    onView(allOf(withId(R.id.btnRegistrarse), isDisplayed(), isClickable()))
      .perform(click());

    logDestino("desdeSesion·Registro · tras click btnRegistrarse");
    onView(withId(R.id.scrollRegistro)).check(matches(isDisplayed()));
  }

  @Test
  public void desdeInicioSesionNavegarAOlvideContrasenya() {
    logDestino("desdeSesion·Olvide · antes de navegar");
    navegarA(R.id.inicioSesionFragment);
    logDestino("desdeSesion·Olvide · tras navegar a inicioSesion");

    onView(allOf(withId(R.id.tvOlvidoContrasenya), isDisplayed()))
      .perform(click());

    logDestino("desdeSesion·Olvide · tras click tvOlvidoContrasenya");
    onView(withId(R.id.scrollOlvideContrasenya)).check(matches(isDisplayed()));
  }

  /////////////////////////desde registro/////////////////////////
  @Test
  public void desdeRegistro_navegarAInicioSesion_destinoCorrecto() {
    logDestino("desdeRegistro·InicioSesion · antes de navegar");
    navegarA(R.id.registroFragment);
    logDestino("desdeRegistro·InicioSesion · tras navegar a registro");

    onView(allOf(withId(R.id.tvIrInicioSesion), isDisplayed()))
      .perform(click());

    logDestino("desdeRegistro·InicioSesion · tras click tvIrInicioSesion");
    NavController nav = getNavController();
    Log.d(TAG, "desdeRegistro·InicioSesion · assertEquals esperado=inicioSesionFragment ("
      + R.id.inicioSesionFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.inicioSesionFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void desdeRegistro_botonVolver_vuelveABienvenida() {
    logDestino("desdeRegistro·Volver · antes de navegar");
    navegarA(R.id.bienvenidaFragment);
    logDestino("desdeRegistro·Volver · tras navegar a bienvenida");

    activityRule.getScenario().onActivity(activity ->
      Navigation.findNavController(activity, R.id.nav_host_fragment)
        .navigate(R.id.action_bienvenida_a_registro)
    );
    logDestino("desdeRegistro·Volver · tras action_bienvenida_a_registro");

    onView(allOf(withId(R.id.btnNavbarVolver), isDisplayed()))
      .perform(click());
    logDestino("desdeRegistro·Volver · tras click btnNavbarVolver");

    NavController nav = getNavController();
    Log.d(TAG, "desdeRegistro·Volver · assertEquals esperado=bienvenidaFragment ("
      + R.id.bienvenidaFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.bienvenidaFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void desdeRegistroViaInicioSesion_botonVolver_vuelveABienvenida() {
    logDestino("desdeRegistroViaSesion · antes de navegar");
    navegarA(R.id.bienvenidaFragment);
    logDestino("desdeRegistroViaSesion · tras navegar a bienvenida");

    activityRule.getScenario().onActivity(activity -> {
      NavController nav = Navigation.findNavController(activity, R.id.nav_host_fragment);
      nav.navigate(R.id.action_bienvenida_a_inicio_sesion);
    });
    logDestino("desdeRegistroViaSesion · tras action_bienvenida_a_inicio_sesion");

    activityRule.getScenario().onActivity(activity -> {
      NavController nav = Navigation.findNavController(activity, R.id.nav_host_fragment);
      nav.navigate(R.id.action_inicio_sesion_a_registro);
    });
    logDestino("desdeRegistroViaSesion · tras action_inicio_sesion_a_registro");

    onView(allOf(withId(R.id.btnNavbarVolver), isDisplayed()))
      .perform(click());
    logDestino("desdeRegistroViaSesion · tras click btnNavbarVolver");

    NavController nav = getNavController();
    Log.d(TAG, "desdeRegistroViaSesion · assertEquals esperado=bienvenidaFragment ("
      + R.id.bienvenidaFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.bienvenidaFragment, nav.getCurrentDestination().getId());
  }
  /////////////////////////desde olvidé contraseña/////////////////////////
  @Test
  public void desdeOlvideContrasenya_botonVolver_vuelveAInicioSesion() {
    logDestino("desdeOlvide · antes de navegar");
    navegarA(R.id.inicioSesionFragment);
    logDestino("desdeOlvide · tras navegar a inicioSesion");

    onView(allOf(withId(R.id.tvOlvidoContrasenya), isDisplayed()))
      .perform(click());
    logDestino("desdeOlvide · tras click tvOlvidoContrasenya");

    onView(allOf(withId(R.id.btnNavbarVolver), isDisplayed()))
      .perform(click());
    logDestino("desdeOlvide · tras click btnNavbarVolver");

    NavController nav = getNavController();
    Log.d(TAG, "desdeOlvide · assertEquals esperado=inicioSesionFragment ("
      + R.id.inicioSesionFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.inicioSesionFragment, nav.getCurrentDestination().getId());
  }

  /////////////////////////navegación directa/////////////////////////

  @Test
  public void navegarACrear_destinoCorrecto() {
    navegarA(R.id.crearFragment);
    logDestino("navegarACrear · tras navigate");
    NavController nav = getNavController();
    Log.d(TAG, "navegarACrear · assertEquals esperado=crearFragment ("
      + R.id.crearFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.crearFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void navegarAFeed_destinoCorrecto() {
    navegarA(R.id.feedFragment);
    logDestino("navegarAFeed · tras navigate");
    NavController nav = getNavController();
    Log.d(TAG, "navegarAFeed · assertEquals esperado=feedFragment ("
      + R.id.feedFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.feedFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void navegarAAjustes_destinoCorrecto() {
    navegarA(R.id.ajustesFragment);
    logDestino("navegarAAjustes · tras navigate");
    NavController nav = getNavController();
    Log.d(TAG, "navegarAAjustes · assertEquals esperado=ajustesFragment ("
      + R.id.ajustesFragment + ") obtenido=" + nav.getCurrentDestination().getId());
    assertEquals(R.id.ajustesFragment, nav.getCurrentDestination().getId());
  }
}
