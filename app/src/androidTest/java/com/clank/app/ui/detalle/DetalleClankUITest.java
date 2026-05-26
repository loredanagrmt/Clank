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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;

/**
 * TC-18 · DetalleClankUITest
 * CU-13 — Visualizar detalle de un clank
 *
 * Prerequisito: TestDataSeeder siembra en Firebase Emulator
 * un clank completo (con materiales, herramientas, instrucciones
 * y categorías) y un clank sin herramientas.
 *
 * Fase: 3 — UI Espresso sobre Fragment real + Firebase Emulator
 * Tests: 10 · Resultado esperado: todos PASS
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DetalleClankUITest {

    // ── IDs de datos de prueba ────────────────────────────────────────────
    private static final String CLANK_COMPLETO_ID    = TestDataSeeder.TEST_CLANK_ID;
    private static final String CLANK_SIN_HERR_ID    = TestDataSeeder.TEST_CLANK_SIN_HERR_ID;
    private static final String CLANK_ID_INVALIDO    = "";

    // ── Auxiliares ────────────────────────────────────────────────────────
    private CountingIdlingResource idlingResource;
    private NavController navController;
    private TestDataSeeder seeder;

    // ─────────────────────────────────────────────────────────────────────
    // SETUP / TEARDOWN
    // ─────────────────────────────────────────────────────────────────────

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        seeder = new TestDataSeeder();
        seeder.crearOIniciarSesionUsuarioTest();
        seeder.insertarUsuarioTest();
        seeder.insertarClankTest(0, true);           // clank completo, numLikes=0
        seeder.insertarClankSinHerramientas();       // clank sin herramientas

        idlingResource = EspressoIdlingResource.getIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);
        navController = Mockito.mock(NavController.class);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        IdlingRegistry.getInstance().unregister(idlingResource);
        seeder.eliminarClankTest();
        seeder.eliminarClankSinHerramientas();
        seeder.eliminarUsuarioFirestore();
        seeder.cerrarSesion();
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────

    private FragmentScenario<DetalleClankFragment> lanzarFragment(String clankId) {
        Bundle args = new Bundle();
        args.putString("clankId", clankId);
        return FragmentScenario.launchInContainer(
                DetalleClankFragment.class,
                args,
                R.style.Theme_Clank
        );
    }

    // ─────────────────────────────────────────────────────────────────────
    // TESTS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * TC-18-01
     * El overlay desaparece y el título y descripción son visibles.
     */
    @Test
    public void cargarDetalleRellenaVista() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.overlayCargando))
                .check(matches(not(isDisplayed())));

        onView(withId(R.id.tvTitulo))
                .check(matches(isDisplayed()));

        onView(withId(R.id.tvDescripcion))
                .check(matches(isDisplayed()));
    }

    /**
     * TC-18-02
     * El título coincide con el valor sembrado por TestDataSeeder.
     */
    @Test
    public void tituloCoincidesConDatoSembrado() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.tvTitulo))
                .check(matches(withText(TestDataSeeder.TEST_CLANK_TITULO)));
    }

    /**
     * TC-18-03
     * El contador de likes muestra el valor inicial sembrado (0).
     */
    @Test
    public void likeInicialSeRellena() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.tvNumLikesDetalle))
                .check(matches(isDisplayed()))
                .check(matches(withText(
                        String.valueOf(TestDataSeeder.TEST_CLANK_NUM_LIKES_INICIAL)
                )));
    }

    /**
     * TC-18-04
     * Al pulsar el botón de like el botón sigue visible (toggle básico;
     * la verificación completa de colección y contador está en TC-20).
     */
    @Test
    public void toggleLikeActualizaIcono() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.btnLikeDetalle))
                .check(matches(isDisplayed()))
                .perform(click());

        onView(withId(R.id.btnLikeDetalle))
                .check(matches(isDisplayed()));
    }

    /**
     * TC-18-05
     * El contenedor de materiales tiene al menos una fila.
     */
    @Test
    public void materialesVisibles() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.llContenedorMateriales))
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));
    }

    /**
     * TC-18-06
     * El contenedor de herramientas está GONE cuando el clank
     * no tiene herramientas.
     */
    @Test
    public void herramientasOcultasSinDatos() {
        lanzarFragment(CLANK_SIN_HERR_ID);

        onView(withId(R.id.llContenedorHerramientas))
                .check(matches(not(isDisplayed())));
    }

    /**
     * TC-18-07
     * El contenedor de instrucciones tiene al menos una fila
     * y el primer número de orden "1." es visible.
     */
    @Test
    public void instruccionesOrdenadas() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.llContenedorInstrucciones))
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));

        onView(withText("1."))
                .check(matches(isDisplayed()));
    }

    /**
     * TC-18-08
     * El FlexboxLayout de categorías contiene al menos un chip.
     */
    @Test
    public void categoriasVisibles() {
        lanzarFragment(CLANK_COMPLETO_ID);

        onView(withId(R.id.flexboxCategorias))
                .check(matches(isDisplayed()))
                .check(matches(hasMinimumChildCount(1)));
    }

    /**
     * TC-18-09
     * Al pulsar el avatar del autor se navega a action_detalle_a_perfil.
     */
    @Test
    public void autorNavegaAPerfil() {
        FragmentScenario<DetalleClankFragment> scenario = lanzarFragment(CLANK_COMPLETO_ID);

        scenario.onFragment(fragment ->
                Navigation.setViewNavController(
                        fragment.requireView(),
                        navController
                )
        );

        onView(withId(R.id.civAvatarUsuario))
                .perform(click());

        Mockito.verify(navController).navigate(
                Mockito.eq(R.id.action_detalle_a_perfil),
                Mockito.any(Bundle.class)
        );
    }

    /**
     * TC-18-10
     * Con clankId vacío el overlay desaparece y el título queda vacío
     * (el ViewModel emite cancelarCarga sin datos).
     */
    @Test
    public void clankIdInvalidoMuestraError() {
        lanzarFragment(CLANK_ID_INVALIDO);

        onView(withId(R.id.overlayCargando))
                .check(matches(not(isDisplayed())));

        onView(withId(R.id.tvTitulo))
                .check(matches(withText("")));
    }
}
