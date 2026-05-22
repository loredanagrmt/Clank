package com.clank.app.ui.perfil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
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
@Feature("Perfil")
public class PerfilUITest {

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

        limpiarDatos();
        seeder.insertarUsuarioTest();
        seeder.insertarCategoriaTest();
        seeder.insertarClankTest(0, false);

        escenario = ActivityScenario.launch(MainActivity.class);
        esperar(600);
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (escenario != null) {
            escenario.close();
            escenario = null;
        }

        limpiarDatos();
    }

    private void limpiarDatos() throws ExecutionException, InterruptedException, TimeoutException {
        if (seeder == null) {
            return;
        }

        seeder.eliminarLikeTest();
        seeder.eliminarClankTest();
        seeder.eliminarCategoriaTest();
        seeder.eliminarUsuarioFirestore();
    }

    private void navegarAPerfilAjeno() {
        Bundle args = new Bundle();
        args.putString("usuarioId", TestDataSeeder.TEST_UID);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.perfilFragment) {
                return;
            }

            navController.navigate(R.id.perfilFragment, args);
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
                "No se cargaron los datos del perfil dentro del tiempo esperado."
        );
    }

    private void esperarHastaContadorClanks(String esperado) {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] correcto = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            correcto[0] = false;

            escenario.onActivity(activity -> {
                TextView contador = activity.findViewById(R.id.tvNumClanks);

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
                "El contador de clanks no alcanzó el valor esperado: " + esperado
        );
    }

    private void esperarHastaAdapterConItems() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] tieneItems = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            tieneItems[0] = false;

            escenario.onActivity(activity -> {
                RecyclerView recyclerView = activity.findViewById(R.id.rvClanks);

                if (recyclerView != null
                        && recyclerView.getAdapter() != null
                        && recyclerView.getAdapter().getItemCount() > 0) {
                    tieneItems[0] = true;
                }
            });

            if (tieneItems[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "El RecyclerView de perfil no recibió clanks dentro del tiempo esperado."
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Carga de perfil")
    @Description("Al navegar a perfil con usuarioId, el destino actual debe ser perfilFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAPerfil_destinoCorrecto() {
        navegarAPerfilAjeno();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.perfilFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura principal /////////////////////////

    @Test
    @Story("Estructura de perfil")
    @Description("La cabecera del perfil debe estar visible.")
    @Severity(SeverityLevel.CRITICAL)
    public void cabeceraPerfil_estaVisible() {
        navegarAPerfilAjeno();

        escenario.onActivity(activity -> {
            View cabecera = activity.findViewById(R.id.llCabeceraPerfil);

            assertNotNull(
                    "No se encontró llCabeceraPerfil.",
                    cabecera
            );

            assertTrue(
                    "La cabecera del perfil debe estar visible.",
                    cabecera.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de perfil")
    @Description("La foto de perfil debe estar visible.")
    @Severity(SeverityLevel.NORMAL)
    public void fotoPerfil_estaVisible() {
        navegarAPerfilAjeno();

        escenario.onActivity(activity -> {
            View foto = activity.findViewById(R.id.civFotoPerfil);

            assertNotNull(
                    "No se encontró civFotoPerfil.",
                    foto
            );

            assertTrue(
                    "La foto de perfil debe estar visible.",
                    foto.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de perfil")
    @Description("El RecyclerView de clanks debe existir y tener adapter.")
    @Severity(SeverityLevel.CRITICAL)
    public void recyclerClanks_tieneAdapter() {
        navegarAPerfilAjeno();

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
    @Story("Estructura de perfil")
    @Description("El overlay de carga de perfil debe existir.")
    @Severity(SeverityLevel.NORMAL)
    public void overlayCargandoPerfil_existe() {
        navegarAPerfilAjeno();

        escenario.onActivity(activity -> {
            View overlay = activity.findViewById(R.id.overlayCargandoPerfil);

            assertNotNull(
                    "No se encontró overlayCargandoPerfil.",
                    overlay
            );
        });
    }

    ///////////////////////// datos del usuario /////////////////////////

    @Test
    @Story("Datos de perfil")
    @Description("El perfil debe mostrar el nombre del usuario sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfil_muestraNombreUsuario() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            TextView nombre = activity.findViewById(R.id.tvNombrePerfil);

            assertEquals(
                    TestDataSeeder.TEST_NOMBRE,
                    nombre.getText().toString()
            );
        });
    }

    @Test
    @Story("Datos de perfil")
    @Description("El perfil debe mostrar el usuarioClank del usuario sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfil_muestraUsuarioClank() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            TextView usuarioClank = activity.findViewById(R.id.tvUidPerfil);

            assertEquals(
                    "@" + TestDataSeeder.TEST_USUARIO_CLANK.replace("@", "").trim(),
                    usuarioClank.getText().toString()
            );
        });
    }

    ///////////////////////// perfil ajeno /////////////////////////

    @Test
    @Story("Perfil ajeno")
    @Description("En un perfil ajeno, el botón de ajustes debe estar oculto.")
    @Severity(SeverityLevel.NORMAL)
    public void perfilAjeno_ocultaBotonAjustes() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View ajustes = activity.findViewById(R.id.btnAjustes);

            assertNotNull(
                    "No se encontró btnAjustes.",
                    ajustes
            );

            assertTrue(
                    "En perfil ajeno, btnAjustes no debe estar visible.",
                    !ajustes.isShown()
            );
        });
    }

    @Test
    @Story("Perfil ajeno")
    @Description("En un perfil ajeno, el enlace de editar perfil debe estar oculto.")
    @Severity(SeverityLevel.NORMAL)
    public void perfilAjeno_ocultaEditarPerfil() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View editar = activity.findViewById(R.id.tvEditarPerfil);

            assertNotNull(
                    "No se encontró tvEditarPerfil.",
                    editar
            );

            assertTrue(
                    "En perfil ajeno, tvEditarPerfil no debe estar visible.",
                    !editar.isShown()
            );
        });
    }

    @Test
    @Story("Perfil ajeno")
    @Description("En un perfil ajeno, la pestaña de bocetos debe estar oculta.")
    @Severity(SeverityLevel.NORMAL)
    public void perfilAjeno_ocultaTabBocetos() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();

        escenario.onActivity(activity -> {
            View tabBocetos = activity.findViewById(R.id.tabBocetos);

            assertNotNull(
                    "No se encontró tabBocetos.",
                    tabBocetos
            );

            assertTrue(
                    "En perfil ajeno, tabBocetos no debe estar visible.",
                    !tabBocetos.isShown()
            );
        });
    }

    ///////////////////////// clanks del usuario /////////////////////////

    @Test
    @Story("Clanks del perfil")
    @Description("El contador de clanks debe mostrar el clank sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfil_muestraContadorClanks() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();
        esperarHastaContadorClanks("1");

        escenario.onActivity(activity -> {
            TextView contador = activity.findViewById(R.id.tvNumClanks);

            assertEquals(
                    "1",
                    contador.getText().toString()
            );
        });
    }

    @Test
    @Story("Clanks del perfil")
    @Description("El RecyclerView debe recibir al menos un clank acabado del usuario sembrado.")
    @Severity(SeverityLevel.CRITICAL)
    public void perfil_muestraClankSembradoEnRecycler() {
        navegarAPerfilAjeno();
        esperarHastaPerfilCargado();
        esperarHastaAdapterConItems();

        escenario.onActivity(activity -> {
            RecyclerView recyclerView = activity.findViewById(R.id.rvClanks);

            assertTrue(
                    "El adapter debe tener al menos un clank.",
                    recyclerView.getAdapter() != null
                            && recyclerView.getAdapter().getItemCount() > 0
            );
        });
    }
}