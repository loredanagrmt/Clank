package com.clank.app.ui.perfil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.clank.app.MainActivity;
import com.clank.app.R;
import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;

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
@Feature("Bocetos en perfil")
public class BocetosPerfilUITest {

    private static final long TIMEOUT_MS = 7000;
    private static final long INTERVALO_MS = 250;

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

        seeder.crearOIniciarSesionUsuarioAuthTest();

        limpiarDatosFirestore();

        seeder.insertarUsuarioAutenticadoTest();
        seeder.insertarCategoriaTest();
        seeder.insertarBocetoAutenticadoTest();

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarDatosFirestore();

        if (seeder != null) {
            seeder.cerrarSesionAuthTest();
        }
    }

    private void limpiarDatosFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        if (seeder == null) {
            return;
        }

        seeder.eliminarBocetoAutenticadoTest();
        seeder.eliminarCategoriaTest();
        seeder.eliminarUsuarioAutenticadoFirestore();
    }

    private void navegarAPerfilPropio() {
        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.perfilFragment) {
                return;
            }

            navController.navigate(R.id.perfilFragment);
        });

        esperar(1000);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void esperarHastaPerfilCargado() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cargado = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            cargado[0] = false;

            escenario.onActivity(activity -> {
                TextView nombre = activity.findViewById(R.id.tvNombrePerfil);

                if (nombre != null
                        && TestDataSeeder.TEST_NOMBRE.equals(nombre.getText().toString())) {
                    cargado[0] = true;
                }
            });

            if (cargado[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargaron los datos del perfil propio dentro del tiempo esperado."
        );
    }

    private void esperarHastaContadorBocetos(String esperado) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] correcto = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            correcto[0] = false;

            escenario.onActivity(activity -> {
                TextView contador = activity.findViewById(R.id.tvNumBocetos);

                if (contador != null
                        && esperado.equals(contador.getText().toString())) {
                    correcto[0] = true;
                }
            });

            if (correcto[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El contador de bocetos no alcanzó el valor esperado: " + esperado
        );
    }

    private void pulsarTabBocetos() {
        escenario.onActivity(activity -> {
            View tabBocetos = activity.findViewById(R.id.tabBocetos);

            assertNotNull(
                    "No se encontró tabBocetos.",
                    tabBocetos
            );

            assertTrue(
                    "tabBocetos debe estar visible en perfil propio.",
                    tabBocetos.isShown()
            );

            assertTrue(
                    "tabBocetos debe ser clicable.",
                    tabBocetos.isClickable()
            );

            tabBocetos.performClick();
        });

        esperar(1000);
    }

    private View buscarVistaPorId(View vista, int idBuscado) {
        if (vista == null) {
            return null;
        }

        if (vista.getId() == idBuscado) {
            return vista;
        }

        if (!(vista instanceof ViewGroup)) {
            return null;
        }

        ViewGroup grupo = (ViewGroup) vista;

        for (int i = 0; i < grupo.getChildCount(); i++) {
            View encontrada = buscarVistaPorId(grupo.getChildAt(i), idBuscado);

            if (encontrada != null) {
                return encontrada;
            }
        }

        return null;
    }

    private View obtenerVistaPorId(MainActivity activity, int viewId) {
        View raiz = activity.findViewById(android.R.id.content);
        return buscarVistaPorId(raiz, viewId);
    }

    private void esperarHastaBocetoVisible() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] visible = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            visible[0] = false;

            escenario.onActivity(activity -> {
                View titulo = obtenerVistaPorId(activity, R.id.tvTituloClank);

                if (titulo instanceof TextView
                        && titulo.isShown()
                        && TestDataSeeder.TEST_BOCETO_TITULO.equals(
                        ((TextView) titulo).getText().toString()
                )) {
                    visible[0] = true;
                }
            });

            if (visible[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No apareció el boceto sembrado en el perfil dentro del tiempo esperado."
        );
    }

    private String obtenerTextoView(int viewId) {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            View vista = obtenerVistaPorId(activity, viewId);

            assertTrue(
                    "La vista no es TextView o no existe: " + viewId,
                    vista instanceof TextView
            );

            texto[0] = ((TextView) vista).getText().toString();
        });

        return texto[0];
    }

    ///////////////////////// carga de perfil /////////////////////////

    @Test
    @Story("Perfil propio")
    @Description("Al navegar al perfil propio, la pestaña de bocetos debe estar visible.")
    @Severity(SeverityLevel.BLOCKER)
    public void tabBocetos_estaVisibleEnPerfilPropio() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View tabBocetos = activity.findViewById(R.id.tabBocetos);

            assertNotNull(
                    "No se encontró tabBocetos.",
                    tabBocetos
            );

            assertTrue(
                    "tabBocetos debe estar visible en perfil propio.",
                    tabBocetos.isShown()
            );
        });
    }

    @Test
    @Story("Bocetos")
    @Description("El contador de bocetos debe reflejar el boceto sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void contadorBocetos_muestraBocetoSembrado() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaContadorBocetos("1");

        escenario.onActivity(activity -> {
            TextView contador = activity.findViewById(R.id.tvNumBocetos);

            assertEquals(
                    "1",
                    contador.getText().toString()
            );
        });
    }

    @Test
    @Story("Bocetos")
    @Description("Al pulsar la pestaña Bocetos, el RecyclerView debe tener adapter configurado.")
    @Severity(SeverityLevel.CRITICAL)
    public void tabBocetos_cargaAdapter() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();

        pulsarTabBocetos();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvClanks);

            assertNotNull(
                    "No se encontró rvClanks.",
                    recyclerView
            );

            assertNotNull(
                    "rvClanks debe tener LayoutManager.",
                    recyclerView.getLayoutManager()
            );

            assertNotNull(
                    "rvClanks debe tener Adapter.",
                    recyclerView.getAdapter()
            );
        });
    }

    @Test
    @Story("Bocetos")
    @Description("Al pulsar Bocetos, debe mostrarse el boceto sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void tabBocetos_muestraBocetoSembrado() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaContadorBocetos("1");

        pulsarTabBocetos();
        esperarHastaBocetoVisible();

        assertEquals(
                TestDataSeeder.TEST_BOCETO_TITULO,
                obtenerTextoView(R.id.tvTituloClank)
        );
    }

    @Test
    @Story("Bocetos")
    @Description("El boceto mostrado debe mantener la descripción sembrada.")
    @Severity(SeverityLevel.NORMAL)
    public void boceto_muestraDescripcionSembrada() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaContadorBocetos("1");

        pulsarTabBocetos();
        esperarHastaBocetoVisible();

        assertEquals(
                "Descripción de boceto para tests de perfil",
                obtenerTextoView(R.id.tvDescripcionClank)
        );
    }

    @Test
    @Story("Bocetos")
    @Description("En perfil propio, el boceto debe mostrar el botón de opciones.")
    @Severity(SeverityLevel.NORMAL)
    public void boceto_muestraBotonOpciones() {
        navegarAPerfilPropio();
        esperarHastaPerfilCargado();
        esperarHastaContadorBocetos("1");

        pulsarTabBocetos();
        esperarHastaBocetoVisible();

        escenario.onActivity(activity -> {
            View opciones = obtenerVistaPorId(activity, R.id.ivOpciones);

            assertNotNull(
                    "No se encontró ivOpciones en la tarjeta del boceto.",
                    opciones
            );

            assertTrue(
                    "En perfil propio, ivOpciones debe estar visible.",
                    opciones.isShown()
            );

            assertTrue(
                    "En perfil propio, ivOpciones debe ser clicable.",
                    opciones.isClickable()
            );
        });
    }
}