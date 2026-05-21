package com.clank.app.ui.crear;

import androidx.test.core.app.ActivityScenario;
import androidx.navigation.Navigation;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
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
@Feature("Clanks")
public class CrearClankUITest {

  @Rule(order = 0)
  public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Rule(order = 1)
  public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

  private ActivityScenario<MainActivity> escenario;

  @Before
  public void setUp() {
    hiltRule.inject();
    escenario = ActivityScenario.launch(MainActivity.class);
  }

  @After
  public void tearDown() {
    if (escenario != null) {
      escenario.close();
      escenario = null;
    }
  }

  private void navegarACrear() {
    escenario.onActivity(activity ->
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                    .navigate(R.id.crearFragment)
    );
  }

  @Test
  @Story("Visualización del formulario de creación")
  @Description("El área de portada del clank debe ser visible al cargar la pantalla.")
  @Severity(SeverityLevel.NORMAL)
  public void framePortada_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.framePortada))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Visualización del formulario de creación")
  @Description("El campo de título debe estar visible al cargar la pantalla.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoTitulo_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Visualización del formulario de creación")
  @Description("El campo de descripción debe estar visible al cargar la pantalla.")
  @Severity(SeverityLevel.NORMAL)
  public void campoDescripcion_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Selector de tiempo")
  @Description("El selector rápido representado por el cohete debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnTiempoCohete_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoCohete))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Selector de tiempo")
  @Description("El selector de dificultad/tiempo medio representado por la liebre debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnTiempoLiebre_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoLiebre))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Selector de tiempo")
  @Description("El selector lento representado por la tortuga debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnTiempoTortuga_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoTortuga))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Secciones del clank")
  @Description("El botón para añadir material debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnAnyadirMaterial_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirMaterial))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Secciones del clank")
  @Description("El botón para añadir herramienta debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnAnyadirHerramienta_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirHerramienta))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Secciones del clank")
  @Description("El botón para añadir instrucción debe estar visible.")
  @Severity(SeverityLevel.NORMAL)
  public void btnAnyadirInstruccion_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirInstruccion))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Publicación y guardado")
  @Description("El botón de publicar debe estar visible dentro del formulario de creación.")
  @Severity(SeverityLevel.CRITICAL)
  public void btnPublicar_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnPublicar))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Publicación y guardado")
  @Description("El botón de guardar como boceto debe estar visible dentro del formulario de creación.")
  @Severity(SeverityLevel.NORMAL)
  public void btnGuardarBoceto_estaVisible() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnGuardarBoceto))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
  }

  @Test
  @Story("Introducción de datos del clank")
  @Description("Al escribir en el campo título, el texto introducido debe mostrarse correctamente.")
  @Severity(SeverityLevel.CRITICAL)
  public void campoTitulo_escribirTexto_muestraTextoEscrito() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
            .perform(
                    ViewActions.replaceText("Maceta de arcilla"),
                    ViewActions.closeSoftKeyboard()
            );

    Espresso.onView(ViewMatchers.withId(R.id.etTitulo))
            .check(ViewAssertions.matches(ViewMatchers.withText("Maceta de arcilla")));
  }

  @Test
  @Story("Introducción de datos del clank")
  @Description("Al escribir en el campo descripción, el texto introducido debe mostrarse correctamente.")
  @Severity(SeverityLevel.NORMAL)
  public void campoDescripcion_escribirTexto_muestraTextoEscrito() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
            .perform(
                    ViewActions.replaceText("Una bonita maceta hecha a mano"),
                    ViewActions.closeSoftKeyboard()
            );

    Espresso.onView(ViewMatchers.withId(R.id.etDescripcion))
            .check(ViewAssertions.matches(ViewMatchers.withText("Una bonita maceta hecha a mano")));
  }

  @Test
  @Story("Selector de tiempo")
  @Description("El selector rápido representado por el cohete debe ser clickable.")
  @Severity(SeverityLevel.NORMAL)
  public void btnTiempoCohete_esClickable() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnTiempoCohete))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isClickable()));
  }

  @Test
  @Story("Secciones del clank")
  @Description("El botón de añadir material debe ser clickable.")
  @Severity(SeverityLevel.NORMAL)
  public void btnAnyadirMaterial_esClickable() {
    navegarACrear();

    Espresso.onView(ViewMatchers.withId(R.id.btnAnyadirMaterial))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isClickable()));
  }
}