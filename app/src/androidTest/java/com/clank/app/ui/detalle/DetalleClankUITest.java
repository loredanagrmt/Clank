package com.clank.app.ui.detalle;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import android.os.Bundle;
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
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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
public class DetalleClankUITest {

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    @Rule(order = 1)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 2)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    private ActivityScenario<MainActivity> escenario;
    private TestDataSeeder seeder;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        hiltRule.inject();

        seeder = new TestDataSeeder();
        seeder.insertarUsuarioTest();
        seeder.insertarCategoriaTest();
        seeder.insertarClankTest(0, true);
        seeder.insertarClankSinHerramientas();

        escenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        if (seeder != null) {
            seeder.eliminarClankTest();
            seeder.eliminarClankSinHerramientas();
            seeder.eliminarCategoriaTest();
            seeder.eliminarUsuarioFirestore();
        }
    }

    private void navegarADetalle(String clankId) {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            navController.navigate(R.id.detalleClankFragment, args);
        });

        esperar(1800);
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

    @Test
    @Story("Visualización del detalle")
    @Description("Al cargar el detalle, el overlay de carga desaparece y el título y la descripción son visibles.")
    @Severity(SeverityLevel.BLOCKER)
    public void cargarDetalleRellenaVista() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.overlayCargando))
                .check(matches(withEffectiveVisibility(GONE)));

        onView(withId(R.id.tvTitulo))
                .check(matches(isDisplayed()));

        onView(withId(R.id.tvDescripcion))
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Visualización del detalle")
    @Description("El título mostrado debe coincidir exactamente con el dato sembrado en Firestore.")
    @Severity(SeverityLevel.CRITICAL)
    public void tituloCoincideConDatoSembrado() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.tvTitulo))
                .check(matches(withText(TestDataSeeder.TEST_CLANK_TITULO)));
    }

    @Test
    @Story("Likes")
    @Description("El contador inicial de likes debe coincidir con el valor sembrado para el clank.")
    @Severity(SeverityLevel.NORMAL)
    public void likeInicialSeRellena() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.tvNumLikesDetalle))
                .check(matches(isDisplayed()))
                .check(matches(withText(
                        String.valueOf(TestDataSeeder.TEST_CLANK_NUM_LIKES_INICIAL)
                )));
    }

    @Test
    @Story("Likes")
    @Description("El botón de like debe mostrarse y ser clickable desde el detalle del clank.")
    @Severity(SeverityLevel.NORMAL)
    public void botonLikeEsVisibleYClickable() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.btnLikeDetalle))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));
    }

    @Test
    @Story("Likes")
    @Description("Al pulsar el botón de like, la pantalla debe mantenerse estable y el botón debe seguir visible.")
    @Severity(SeverityLevel.NORMAL)
    public void toggleLikeMantieneBotonVisible() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.btnLikeDetalle))
                .check(matches(isDisplayed()))
                .perform(click());

        esperar(800);

        onView(withId(R.id.btnLikeDetalle))
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Contenido del clank")
    @Description("La sección de materiales debe mostrar al menos un elemento sembrado.")
    @Severity(SeverityLevel.NORMAL)
    public void materialesVisibles() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.llContenedorMateriales))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));
    }

    @Test
    @Story("Contenido del clank")
    @Description("La sección de herramientas debe ocultarse cuando el clank no tiene herramientas.")
    @Severity(SeverityLevel.NORMAL)
    public void herramientasOcultasSinDatos() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_SIN_HERR_ID);

        onView(withId(R.id.llContenedorHerramientas))
                .check(matches(withEffectiveVisibility(GONE)));
    }

    @Test
    @Story("Contenido del clank")
    @Description("Las instrucciones deben mostrarse ordenadas y comenzar por el índice 1.")
    @Severity(SeverityLevel.NORMAL)
    public void instruccionesOrdenadas() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.llContenedorInstrucciones))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));

        onView(withText("1."))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    @Test
    @Story("Contenido del clank")
    @Description("Las categorías sembradas deben mostrarse en el contenedor flexible de categorías.")
    @Severity(SeverityLevel.NORMAL)
    public void categoriasVisibles() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.flexboxCategorias))
                .perform(scrollTo())
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));
    }

    @Test
    @Story("Navegación desde detalle")
    @Description("Al pulsar el avatar del autor, se debe navegar al perfil del usuario.")
    @Severity(SeverityLevel.NORMAL)
    public void autorNavegaAPerfil() {
        navegarADetalle(TestDataSeeder.TEST_CLANK_ID);

        onView(withId(R.id.civAvatarUsuario))
                .perform(click());

        esperar(800);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertEquals(
                    R.id.perfilFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }
}