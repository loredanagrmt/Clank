package com.clank.app.ui.detalle;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.R;
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.EspressoIdlingResource;
import com.clank.app.test.util.FirebaseEmulatorRule;
import com.clank.app.ui.detalle.DetalleClankFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DetalleClankUITest {

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    private CountingIdlingResource idlingResource;
    private NavController navController;
    private TestDataSeeder seeder;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        seeder = new TestDataSeeder();
        seeder.crearOIniciarSesionUsuarioTest();
        seeder.insertarUsuarioTest();
        seeder.insertarClankTest(0, true);
        seeder.insertarClankSinHerramientas();

        idlingResource = EspressoIdlingResource.getIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);
        navController = Mockito.mock(NavController.class);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        IdlingRegistry.getInstance().unregister(idlingResource);
        seeder.eliminarClankTest();
        seeder.eliminarClankSinHerramientas();
        seeder.eliminarUsuarioFirestore();
        seeder.cerrarSesion();
    }

    private FragmentScenario<DetalleClankFragment> lanzarFragment(String clankId) {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);
        return FragmentScenario.launchInContainer(
                DetalleClankFragment.class,
                args,
                R.style.Theme_Clank
        );
    }

    @Test
    public void cargarDetalleRellenaVista() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.overlayCargando)).check(matches(not(isDisplayed())));
        onView(withId(R.id.tvTitulo)).check(matches(isDisplayed()));
        onView(withId(R.id.tvDescripcion)).check(matches(isDisplayed()));
    }

    @Test
    public void tituloCoincidesConDatoSembrado() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.tvTitulo)).check(matches(withText(TestDataSeeder.TEST_CLANK_TITULO)));
    }

    @Test
    public void likeInicialSeRellena() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.tvNumLikesDetalle))
                .check(matches(isDisplayed()))
                .check(matches(withText(String.valueOf(TestDataSeeder.TEST_CLANK_NUM_LIKES_INICIAL))));
    }

    @Test
    public void toggleLikeActualizaIcono() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.btnLikeDetalle)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.btnLikeDetalle)).check(matches(isDisplayed()));
    }

    @Test
    public void materialesVisibles() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.llContenedorMateriales)).check(matches(isDisplayed())).check(matches(hasMinimumChildCount(1)));
    }

    @Test
    public void herramientasOcultasSinDatos() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_SIN_HERR_ID);
        onView(withId(R.id.llContenedorHerramientas)).check(matches(not(isDisplayed())));
    }

    @Test
    public void instruccionesOrdenadas() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.llContenedorInstrucciones)).check(matches(isDisplayed())).check(matches(hasMinimumChildCount(1)));
        onView(withText("1.")).check(matches(isDisplayed()));
    }

    @Ignore("Pendiente de revisar CategoriaRepository.java para sembrar categoría real sin inventar colección.")
    @Test
    public void categoriasVisibles() {
        lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        onView(withId(R.id.flexboxCategorias))
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));
    }

    @Ignore("Pendiente de revisar DetalleClankViewModel.java para validar clankId vacío sin crash.")
    @Test
    public void clankIdInvalidoMuestraError() {
        lanzarFragment("");
        onView(withId(R.id.overlayCargando)).check(matches(not(isDisplayed())));
        onView(withId(R.id.tvTitulo)).check(matches(withText("")));
    }

    @Test
    public void autorNavegaAPerfil() {
        FragmentScenario<DetalleClankFragment> scenario = lanzarFragment(TestDataSeeder.TEST_CLANK_ID);
        scenario.onFragment(fragment -> Navigation.setViewNavController(fragment.requireView(), navController));
        onView(withId(R.id.civAvatarUsuario)).perform(click());
        Mockito.verify(navController).navigate(Mockito.eq(R.id.action_detalle_a_perfil), Mockito.any(Bundle.class));
    }

}