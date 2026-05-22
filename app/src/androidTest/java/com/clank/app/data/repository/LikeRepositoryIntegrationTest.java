package com.clank.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.clank.app.test.TestDataSeeder;
import com.clank.app.test.util.AllureScreenshotWatcher;
import com.clank.app.test.util.FirebaseEmulatorRule;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
@Epic("Integration Tests")
@Feature("Likes")
public class LikeRepositoryIntegrationTest {

    private static final long TIMEOUT_S = 10;

    @Rule(order = 0)
    public FirebaseEmulatorRule emulatorRule = new FirebaseEmulatorRule();

    @Rule(order = 1)
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Rule(order = 2)
    public AllureScreenshotWatcher screenshotWatcher = new AllureScreenshotWatcher();

    @Inject
    LikeRepository likeRepository;

    private FirebaseFirestore db;
    private TestDataSeeder seeder;

    @Before
    public void setUp() throws ExecutionException, InterruptedException, TimeoutException {
        hiltRule.inject();

        db = FirebaseFirestore.getInstance();
        seeder = new TestDataSeeder();

        seeder.insertarUsuarioTest();
        seeder.insertarCategoriaTest();
        seeder.insertarClankTest(0, false);

        seeder.eliminarLikeTest();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        if (seeder != null) {
            seeder.eliminarLikeTest();
            seeder.eliminarClankTest();
            seeder.eliminarCategoriaTest();
            seeder.eliminarUsuarioFirestore();
        }
    }

    @Test
    @Story("Dar like")
    @Description("Cuando el usuario no ha dado like, toggleLike debe crear el documento de like y devolver true.")
    @Severity(SeverityLevel.CRITICAL)
    public void darLike_creaDocumentoYDevuelveTrue()
            throws ExecutionException, InterruptedException, TimeoutException {

        Boolean resultado = Tasks.await(
                likeRepository.toggleLike(
                        TestDataSeeder.TEST_CLANK_ID,
                        TestDataSeeder.TEST_UID
                ),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        DocumentSnapshot likeDoc = obtenerDocumentoLike();

        assertTrue("toggleLike debe devolver true al crear un like.", resultado);
        assertTrue("El documento de like debe existir.", likeDoc.exists());
        assertEquals(
                "El uid guardado en el documento debe coincidir con el usuario de prueba.",
                TestDataSeeder.TEST_UID,
                likeDoc.getString("uid")
        );
    }

    @Test
    @Story("Quitar like")
    @Description("Cuando el usuario ya había dado like, toggleLike debe eliminar el documento y devolver false.")
    @Severity(SeverityLevel.CRITICAL)
    public void quitarLike_eliminaDocumentoYDevuelveFalse()
            throws ExecutionException, InterruptedException, TimeoutException {

        seeder.insertarLikeTest();

        Boolean resultado = Tasks.await(
                likeRepository.toggleLike(
                        TestDataSeeder.TEST_CLANK_ID,
                        TestDataSeeder.TEST_UID
                ),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        DocumentSnapshot likeDoc = obtenerDocumentoLike();

        assertFalse("toggleLike debe devolver false al quitar un like.", resultado);
        assertFalse("El documento de like debe dejar de existir.", likeDoc.exists());
    }

    @Test
    @Story("Consultar like")
    @Description("hasDadoLike debe devolver false cuando no existe documento de like para el usuario.")
    @Severity(SeverityLevel.NORMAL)
    public void hasDadoLike_sinDocumento_devuelveFalse()
            throws ExecutionException, InterruptedException, TimeoutException {

        Boolean resultado = Tasks.await(
                likeRepository.hasDadoLike(
                        TestDataSeeder.TEST_CLANK_ID,
                        TestDataSeeder.TEST_UID
                ),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        assertFalse("hasDadoLike debe devolver false si no existe like.", resultado);
    }

    @Test
    @Story("Consultar like")
    @Description("hasDadoLike debe devolver true cuando existe documento de like para el usuario.")
    @Severity(SeverityLevel.NORMAL)
    public void hasDadoLike_conDocumento_devuelveTrue()
            throws ExecutionException, InterruptedException, TimeoutException {

        seeder.insertarLikeTest();

        Boolean resultado = Tasks.await(
                likeRepository.hasDadoLike(
                        TestDataSeeder.TEST_CLANK_ID,
                        TestDataSeeder.TEST_UID
                ),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        assertTrue("hasDadoLike debe devolver true si existe like.", resultado);
    }

    @Test
    @Story("Contador de likes")
    @Description("escucharNumLikes debe emitir la cantidad real de documentos en clanks/{clankId}/likes.")
    @Severity(SeverityLevel.NORMAL)
    public void escucharNumLikes_emiteCantidadCorrecta()
            throws ExecutionException, InterruptedException, TimeoutException {

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger cantidadRecibida = new AtomicInteger(-1);

        ListenerRegistration listener = likeRepository.escucharNumLikes(
                TestDataSeeder.TEST_CLANK_ID,
                cantidad -> {
                    cantidadRecibida.set(cantidad);

                    if (cantidad == 1) {
                        latch.countDown();
                    }
                }
        );

        try {
            seeder.insertarLikeTest();

            boolean emitido = latch.await(TIMEOUT_S, TimeUnit.SECONDS);

            assertTrue("El listener debe emitir la cantidad esperada.", emitido);
            assertEquals(
                    "Debe existir exactamente 1 like en la subcolección.",
                    1,
                    cantidadRecibida.get()
            );

        } finally {
            listener.remove();
        }
    }

    private DocumentSnapshot obtenerDocumentoLike()
            throws ExecutionException, InterruptedException, TimeoutException {

        return Tasks.await(
                db.collection("clanks")
                        .document(TestDataSeeder.TEST_CLANK_ID)
                        .collection("likes")
                        .document(TestDataSeeder.TEST_UID)
                        .get(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }
}