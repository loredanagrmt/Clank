package com.clank.app.ui.navegacion;

import androidx.navigation.Navigation;
import androidx.navigation.NavController;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.clank.app.MainActivity;
import com.clank.app.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class NavegacionUITest {

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

  /////////////////////////destno inicial/////////////////////////

  @Test
  public void appArranca_destinoEsValido() {
    NavController nav = getNavController();
    int destino = nav.getCurrentDestination().getId();
    boolean esValido = destino == R.id.logoFragment
      || destino == R.id.feedFragment
      || destino == R.id.inspirarFragment
      || destino == R.id.elegirIdiomaFragment
      || destino == R.id.bienvenidaFragment;
    assertEquals(true, esValido);
  }

  /////////////////////////desde inicio sesion/////////////////////////

  @Test
  public void desdeSesion_navegarARegistro_destinoCorrecto() {
    navegarA(R.id.inicioSesionFragment);

    Espresso.onView(ViewMatchers.withId(R.id.btnRegistrarse))
      .perform(ViewActions.click());

    NavController nav = getNavController();
    assertEquals(R.id.registroFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void desdeSesion_navegarAOlvideContrasenya_destinoCorrecto() {
    navegarA(R.id.inicioSesionFragment);

    Espresso.onView(ViewMatchers.withId(R.id.tvOlvidoContrasenya))
      .perform(ViewActions.click());

    NavController nav = getNavController();
    assertEquals(R.id.olvideContrasenyaFragment, nav.getCurrentDestination().getId());
  }

  /////////////////////////desde registro/////////////////////////

  @Test
  public void desdeRegistro_navegarAInicioSesion_destinoCorrecto() {
    navegarA(R.id.registroFragment);

    Espresso.onView(ViewMatchers.withId(R.id.tvIrInicioSesion))
      .perform(ViewActions.click());

    NavController nav = getNavController();
    assertEquals(R.id.inicioSesionFragment, nav.getCurrentDestination().getId());
  }

  /////////////////////////volver desde navbar/////////////////////////

  @Test
  public void desdeRegistro_botonVolver_vuelveAtras() {
    navegarA(R.id.inicioSesionFragment);
    navegarA(R.id.registroFragment);

    Espresso.onView(ViewMatchers.withId(R.id.btnNavbarVolver))
      .perform(ViewActions.click());

    NavController nav = getNavController();
    assertEquals(R.id.inicioSesionFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void desdeOlvideContrasenya_botonVolver_vuelveAInicioSesion() {
    navegarA(R.id.inicioSesionFragment);

    Espresso.onView(ViewMatchers.withId(R.id.tvOlvidoContrasenya))
      .perform(ViewActions.click());

    Espresso.onView(ViewMatchers.withId(R.id.btnNavbarVolver))
      .perform(ViewActions.click());

    NavController nav = getNavController();
    assertEquals(R.id.inicioSesionFragment, nav.getCurrentDestination().getId());
  }

  /////////////////////////navegación/////////////////////////

  @Test
  public void navegarACrear_destinoCorrecto() {
    navegarA(R.id.crearFragment);
    NavController nav = getNavController();
    assertEquals(R.id.crearFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void navegarAFeed_destinoCorrecto() {
    navegarA(R.id.feedFragment);
    NavController nav = getNavController();
    assertEquals(R.id.feedFragment, nav.getCurrentDestination().getId());
  }

  @Test
  public void navegarAAjustes_destinoCorrecto() {
    navegarA(R.id.ajustesFragment);
    NavController nav = getNavController();
    assertEquals(R.id.ajustesFragment, nav.getCurrentDestination().getId());
  }
}
