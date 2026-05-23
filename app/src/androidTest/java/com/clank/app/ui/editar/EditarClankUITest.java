package com.clank.app.ui.editar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
@Feature("Editar Clank")
public class EditarClankUITest {

    private static final long TIMEOUT_MS = 8000;
    private static final long INTERVALO_MS = 250;
    private static final long TIMEOUT_FIRESTORE_S = 10;

    private static final String TITULO_EDITADO = "Boceto editado desde test";
    private static final String DESCRIPCION_EDITADA = "Descripción editada desde test";

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
        seeder.insertarBocetoAutenticadoCompletoTest();

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

    private void navegarAEditarClank() {
        Bundle args = new Bundle();
        args.putString("clankId", TestDataSeeder.TEST_BOCETO_ID);

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.editarClankFragment) {
                return;
            }

            navController.navigate(R.id.editarClankFragment, args);
        });

        esperar(1000);
    }

    private void esperar(long millis) {
        SystemClock.sleep(millis);
    }

    private void esperarHastaFormularioCargado() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] cargado = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            cargado[0] = false;

            escenario.onActivity(activity -> {
                EditText titulo = activity.findViewById(R.id.etTitulo);
                EditText descripcion = activity.findViewById(R.id.etDescripcion);

                if (titulo != null
                        && descripcion != null
                        && TestDataSeeder.TEST_BOCETO_TITULO.equals(titulo.getText().toString())
                        && "Descripción de boceto para tests de edición".equals(descripcion.getText().toString())) {
                    cargado[0] = true;
                }
            });

            if (cargado[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se cargaron los datos del boceto en EditarClankFragment dentro del tiempo esperado."
        );
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

    private String obtenerTextoEditText(int viewId) {
        final String[] texto = new String[1];

        escenario.onActivity(activity -> {
            View vista = obtenerVistaPorId(activity, viewId);

            assertTrue(
                    "La vista no es EditText o no existe: " + viewId,
                    vista instanceof EditText
            );

            texto[0] = ((EditText) vista).getText().toString();
        });

        return texto[0];
    }

    private void escribirTexto(int viewId, String texto) {
        escenario.onActivity(activity -> {
            View vista = obtenerVistaPorId(activity, viewId);

            assertTrue(
                    "La vista no es EditText o no existe: " + viewId,
                    vista instanceof EditText
            );

            ((EditText) vista).setText(texto);
        });

        esperar(300);
    }

    private EditText obtenerPrimerEditTextDentroDe(MainActivity activity, int contenedorId) {
        View contenedor = activity.findViewById(contenedorId);

        assertTrue(
                "No se encontró el contenedor indicado o no es ViewGroup: " + contenedorId,
                contenedor instanceof ViewGroup
        );

        return buscarPrimerEditText((ViewGroup) contenedor);
    }

    private EditText buscarPrimerEditText(ViewGroup grupo) {
        for (int i = 0; i < grupo.getChildCount(); i++) {
            View hija = grupo.getChildAt(i);

            if (hija instanceof EditText) {
                return (EditText) hija;
            }

            if (hija instanceof ViewGroup) {
                EditText encontrada = buscarPrimerEditText((ViewGroup) hija);

                if (encontrada != null) {
                    return encontrada;
                }
            }
        }

        return null;
    }

    private DocumentSnapshot obtenerDocumentoBoceto()
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection("clanks")
                        .document(TestDataSeeder.TEST_BOCETO_ID)
                        .get(),
                TIMEOUT_FIRESTORE_S,
                TimeUnit.SECONDS
        );
    }

    private void esperarHastaDestinoPerfil() {
        long inicio = SystemClock.elapsedRealtime();
        final boolean[] enPerfil = new boolean[1];

        while (SystemClock.elapsedRealtime() - inicio < TIMEOUT_MS) {
            enPerfil[0] = false;

            escenario.onActivity(activity -> {
                NavController navController =
                        Navigation.findNavController(activity, R.id.nav_host_fragment);

                enPerfil[0] = navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == R.id.perfilFragment;
            });

            if (enPerfil[0]) {
                return;
            }

            esperar(INTERVALO_MS);
        }

        throw new AssertionError(
                "No se navegó a perfil tras guardar/publicar el clank."
        );
    }

    ///////////////////////// navegación /////////////////////////

    @Test
    @Story("Carga de edición")
    @Description("Al navegar a editar clank con clankId, el destino actual debe ser editarClankFragment.")
    @Severity(SeverityLevel.BLOCKER)
    public void navegarAEditarClank_destinoCorrecto() {
        navegarAEditarClank();

        escenario.onActivity(activity -> {
            NavController navController =
                    Navigation.findNavController(activity, R.id.nav_host_fragment);

            assertNotNull(
                    "El NavController debe tener destino actual.",
                    navController.getCurrentDestination()
            );

            assertEquals(
                    R.id.editarClankFragment,
                    navController.getCurrentDestination().getId()
            );
        });
    }

    ///////////////////////// estructura /////////////////////////

    @Test
    @Story("Estructura de edición")
    @Description("Los botones Publicar y Guardar boceto deben existir.")
    @Severity(SeverityLevel.CRITICAL)
    public void botonesPublicarYGuardarBoceto_existen() {
        navegarAEditarClank();

        escenario.onActivity(activity -> {
            View publicar = activity.findViewById(R.id.btnPublicar);
            View guardar = activity.findViewById(R.id.btnGuardarBoceto);

            assertNotNull(
                    "No se encontró btnPublicar.",
                    publicar
            );

            assertNotNull(
                    "No se encontró btnGuardarBoceto.",
                    guardar
            );

            assertTrue(
                    "btnGuardarBoceto debe estar visible.",
                    guardar.isShown()
            );
        });
    }

    @Test
    @Story("Estructura de edición")
    @Description("El formulario debe mostrar los contenedores principales de materiales, herramientas, instrucciones y categorías.")
    @Severity(SeverityLevel.NORMAL)
    public void contenedoresPrincipales_existen() {
        navegarAEditarClank();

        escenario.onActivity(activity -> {
            assertNotNull(
                    "No se encontró llContenedorMateriales.",
                    activity.findViewById(R.id.llContenedorMateriales)
            );

            assertNotNull(
                    "No se encontró llContenedorHerramientas.",
                    activity.findViewById(R.id.llContenedorHerramientas)
            );

            assertNotNull(
                    "No se encontró llContenedorInstrucciones.",
                    activity.findViewById(R.id.llContenedorInstrucciones)
            );

            assertNotNull(
                    "No se encontró flexboxCategorias.",
                    activity.findViewById(R.id.flexboxCategorias)
            );
        });
    }

    ///////////////////////// carga de datos /////////////////////////

    @Test
    @Story("Carga de edición")
    @Description("El formulario debe precargar el título del boceto.")
    @Severity(SeverityLevel.CRITICAL)
    public void formularioCargaTituloBoceto() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        assertEquals(
                TestDataSeeder.TEST_BOCETO_TITULO,
                obtenerTextoEditText(R.id.etTitulo)
        );
    }

    @Test
    @Story("Carga de edición")
    @Description("El formulario debe precargar la descripción del boceto.")
    @Severity(SeverityLevel.CRITICAL)
    public void formularioCargaDescripcionBoceto() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        assertEquals(
                "Descripción de boceto para tests de edición",
                obtenerTextoEditText(R.id.etDescripcion)
        );
    }

    @Test
    @Story("Carga de edición")
    @Description("El formulario debe precargar al menos un material del boceto.")
    @Severity(SeverityLevel.CRITICAL)
    public void formularioCargaMaterialSembrado() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            EditText primerMaterial =
                    obtenerPrimerEditTextDentroDe(activity, R.id.llContenedorMateriales);

            assertNotNull(
                    "Debe existir al menos un EditText en materiales.",
                    primerMaterial
            );

            assertTrue(
                    "El material sembrado debe estar cargado en el formulario.",
                    primerMaterial.getText().toString().trim().length() > 0
            );
        });
    }

    @Test
    @Story("Carga de edición")
    @Description("El formulario debe precargar al menos una instrucción del boceto.")
    @Severity(SeverityLevel.CRITICAL)
    public void formularioCargaInstruccionSembrada() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            View contenedor = activity.findViewById(R.id.llContenedorInstrucciones);

            assertTrue(
                    "llContenedorInstrucciones debe ser ViewGroup.",
                    contenedor instanceof ViewGroup
            );

            View textoInstruccion = buscarVistaPorId(contenedor, R.id.etTextoInstruccion);

            assertTrue(
                    "Debe existir etTextoInstruccion.",
                    textoInstruccion instanceof EditText
            );

            assertEquals(
                    "Primer paso del boceto",
                    ((EditText) textoInstruccion).getText().toString()
            );
        });
    }

    @Test
    @Story("Carga de edición")
    @Description("Debe cargarse al menos una categoría en el formulario de edición.")
    @Severity(SeverityLevel.NORMAL)
    public void formularioCargaCategorias() {
        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escenario.onActivity(activity -> {
            View flexbox = activity.findViewById(R.id.flexboxCategorias);

            assertTrue(
                    "flexboxCategorias debe ser ViewGroup.",
                    flexbox instanceof ViewGroup
            );

            assertTrue(
                    "Debe cargarse al menos una categoría.",
                    ((ViewGroup) flexbox).getChildCount() > 0
            );
        });
    }

    ///////////////////////// guardado real /////////////////////////

    @Test
    @Story("Guardado de edición")
    @Description("Al guardar como boceto con datos editados, el documento debe persistir como estadoAcabado=false y volver al perfil.")
    @Severity(SeverityLevel.CRITICAL)
    public void guardarBocetoEditado_persisteEnFirestoreYVuelvePerfil()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escribirTexto(R.id.etTitulo, TITULO_EDITADO);
        escribirTexto(R.id.etDescripcion, DESCRIPCION_EDITADA);

        escenario.onActivity(activity -> {
            View guardar = activity.findViewById(R.id.btnGuardarBoceto);

            assertNotNull(
                    "No se encontró btnGuardarBoceto.",
                    guardar
            );

            assertTrue(
                    "btnGuardarBoceto debe estar habilitado.",
                    guardar.isEnabled()
            );

            guardar.performClick();
        });

        esperarHastaDestinoPerfil();

        DocumentSnapshot doc = obtenerDocumentoBoceto();

        assertTrue(
                "El documento del boceto debe existir.",
                doc.exists()
        );

        assertEquals(
                TITULO_EDITADO,
                doc.getString("titulo")
        );

        assertEquals(
                DESCRIPCION_EDITADA,
                doc.getString("descripcion")
        );

        assertEquals(
                false,
                doc.getBoolean("estadoAcabado")
        );
    }

    @Test
    @Story("Publicación de edición")
    @Description("Al publicar el boceto editado, el documento debe persistir como estadoAcabado=true y volver al perfil.")
    @Severity(SeverityLevel.CRITICAL)
    public void publicarBocetoEditado_persisteComoClankAcabadoYVuelvePerfil()
            throws ExecutionException, InterruptedException, TimeoutException {

        navegarAEditarClank();
        esperarHastaFormularioCargado();

        escribirTexto(R.id.etTitulo, TITULO_EDITADO);
        escribirTexto(R.id.etDescripcion, DESCRIPCION_EDITADA);

        escenario.onActivity(activity -> {
            View publicar = activity.findViewById(R.id.btnPublicar);

            assertNotNull(
                    "No se encontró btnPublicar.",
                    publicar
            );

            assertTrue(
                    "btnPublicar debe estar habilitado tras editar título/descripción.",
                    publicar.isEnabled()
            );

            publicar.performClick();
        });

        esperarHastaDestinoPerfil();

        DocumentSnapshot doc = obtenerDocumentoBoceto();

        assertTrue(
                "El documento publicado debe existir.",
                doc.exists()
        );

        assertEquals(
                TITULO_EDITADO,
                doc.getString("titulo")
        );

        assertEquals(
                DESCRIPCION_EDITADA,
                doc.getString("descripcion")
        );

        assertEquals(
                true,
                doc.getBoolean("estadoAcabado")
        );
    }
}