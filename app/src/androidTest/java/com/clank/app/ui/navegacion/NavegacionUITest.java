package com.clank.app.ui.navegacion;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
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
@Feature("Navegación")
public class NavegacionUITest {

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

  private NavController getNavController() {
    final NavController[] nav = new NavController[1];

    escenario.onActivity(activity ->
            nav[0] = Navigation.findNavController(
                    activity,
                    R.id.nav_host_fragment
            )
    );

    return nav[0];
  }

  private void navegarA(int destinoId) {
    escenario.onActivity(activity -> {
      NavController navController =
              Navigation.findNavController(activity, R.id.nav_host_fragment);

      if (navController.getCurrentDestination() != null
              && navController.getCurrentDestination().getId() == destinoId) {
        return;
      }

      navController.navigate(destinoId);
    });

    esperar(700);
  }

  private void esperar(long millis) {
    onView(isRoot()).perform(new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isRoot();
      }

      @Override
      public String getDescription() {
        return "Esperar " + millis + " ms";
      }

      @Override
      public void perform(UiController uiController, View view) {
        uiController.loopMainThreadForAtLeast(millis);
      }
    });
  }

  private boolean esDestinoFlujoInicialValido(int destino) {
    return destino == R.id.logoFragment
            || destino == R.id.feedFragment
            || destino == R.id.inspirarFragment
            || destino == R.id.elegirIdiomaFragment
            || destino == R.id.bienvenidaFragment
            || destino == R.id.inicioSesionFragment;
  }

  ///////////////////////// destino inicial /////////////////////////

  @Test
  @Story("Inicio de la aplicación")
  @Description("Al arrancar la app, el destino inicial debe ser uno de los destinos válidos del flujo inicial.")
  @Severity(SeverityLevel.BLOCKER)
  public void appArranca_destinoEsValido() {
    NavController nav = getNavController();

    int destino = nav.getCurrentDestination() != null
            ? nav.getCurrentDestination().getId()
            : -1;

    assertTrue(
            "El destino inicial debe pertenecer al flujo inicial de la app.",
            esDestinoFlujoInicialValido(destino)
    );
  }

  ///////////////////////// desde inicio sesión /////////////////////////

  @Test
  @Story("Flujo de autenticación")
  @Description("Desde inicio de sesión, pulsar el botón de registro debe navegar a registroFragment.")
  @Severity(SeverityLevel.CRITICAL)
  public void desdeSesion_navegarARegistro_destinoCorrecto() {
    navegarA(R.id.inicioSesionFragment);

    onView(withId(R.id.btnRegistrarse))
            .perform(click());

    esperar(700);

    NavController nav = getNavController();

    assertEquals(
            R.id.registroFragment,
            nav.getCurrentDestination().getId()
    );
  }

  @Test
  @Story("Flujo de recuperación de contraseña")
  @Description("Desde inicio de sesión, pulsar olvidé contraseña debe navegar a olvideContrasenyaFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void desdeSesion_navegarAOlvideContrasenya_destinoCorrecto() {
    navegarA(R.id.inicioSesionFragment);

    onView(withId(R.id.tvOlvidoContrasenya))
            .perform(click());

    esperar(700);

    NavController nav = getNavController();

    assertEquals(
            R.id.olvideContrasenyaFragment,
            nav.getCurrentDestination().getId()
    );
  }

  ///////////////////////// desde registro /////////////////////////

  @Test
  @Story("Flujo de autenticación")
  @Description("Desde registro, pulsar el enlace de inicio de sesión debe volver a inicioSesionFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void desdeRegistro_navegarAInicioSesion_destinoCorrecto() {
    navegarA(R.id.registroFragment);

    onView(withId(R.id.tvIrInicioSesion))
            .perform(click());

    esperar(700);

    NavController nav = getNavController();

    assertEquals(
            R.id.inicioSesionFragment,
            nav.getCurrentDestination().getId()
    );
  }

  ///////////////////////// volver desde navbar /////////////////////////

  @Test
  @Story("Navegación hacia atrás")
  @Description("Desde registro, el botón volver de la navbar debe salir de registro y dejar la app en un destino válido del flujo inicial.")
  @Severity(SeverityLevel.CRITICAL)
  public void desdeRegistro_botonVolver_saleDeRegistro() {
    navegarA(R.id.inicioSesionFragment);

    onView(withId(R.id.btnRegistrarse))
            .perform(click());

    esperar(700);

    NavController navAntes = getNavController();

    assertEquals(
            R.id.registroFragment,
            navAntes.getCurrentDestination().getId()
    );

    onView(allOf(
            withId(R.id.btnNavbarVolver),
            isDisplayed()
    )).perform(click());

    esperar(700);

    NavController navDespues = getNavController();

    int destino = navDespues.getCurrentDestination() != null
            ? navDespues.getCurrentDestination().getId()
            : -1;

    assertNotEquals(
            "El botón volver debe sacar al usuario de registroFragment.",
            R.id.registroFragment,
            destino
    );

    assertTrue(
            "Tras volver desde registro, el destino debe ser válido dentro del flujo inicial.",
            esDestinoFlujoInicialValido(destino)
    );
  }

  @Test
  @Story("Navegación hacia atrás")
  @Description("Desde olvidé contraseña, el botón volver de la navbar debe regresar a inicioSesionFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void desdeOlvideContrasenya_botonVolver_vuelveAInicioSesion() {
    navegarA(R.id.inicioSesionFragment);

    onView(withId(R.id.tvOlvidoContrasenya))
            .perform(click());

    esperar(700);

    onView(allOf(
            withId(R.id.btnNavbarVolver),
            isDisplayed()
    )).perform(click());

    esperar(700);

    NavController nav = getNavController();

    assertEquals(
            R.id.inicioSesionFragment,
            nav.getCurrentDestination().getId()
    );
  }

  ///////////////////////// navegación directa /////////////////////////

  @Test
  @Story("Navegación entre pantallas principales")
  @Description("Navegar directamente a crearFragment debe dejar el NavController en crearFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void navegarACrear_destinoCorrecto() {
    navegarA(R.id.crearFragment);

    NavController nav = getNavController();

    assertEquals(
            R.id.crearFragment,
            nav.getCurrentDestination().getId()
    );
  }

  @Test
  @Story("Navegación entre pantallas principales")
  @Description("Navegar directamente a feedFragment debe dejar el NavController en feedFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void navegarAFeed_destinoCorrecto() {
    navegarA(R.id.feedFragment);

    NavController nav = getNavController();

    assertEquals(
            R.id.feedFragment,
            nav.getCurrentDestination().getId()
    );
  }

  @Test
  @Story("Navegación entre pantallas principales")
  @Description("Navegar directamente a ajustesFragment debe dejar el NavController en ajustesFragment.")
  @Severity(SeverityLevel.NORMAL)
  public void navegarAAjustes_destinoCorrecto() {
    navegarA(R.id.ajustesFragment);

    NavController nav = getNavController();

    assertEquals(
            R.id.ajustesFragment,
            nav.getCurrentDestination().getId()
    );
  }
}