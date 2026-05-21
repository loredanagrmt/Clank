package com.clank.app.test;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TestDataSeeder {

    public static final String TEST_EMAIL = "test-fase4@clank.test";
    public static final String TEST_PASSWORD = "TestClank1234!";
    public static final String TEST_NOMBRE = "Usuario Test Fase4";
    public static final String TEST_USUARIO_CLANK = "usuario_test_fase4";

    public static final String TEST_CLANK_ID = "test-clank-fase4";
    public static final String TEST_CLANK_SIN_HERR_ID = "test-clank-sin-herr-fase4";
    public static final String TEST_CLANK_TITULO = "Clank de prueba Fase4";
    public static final int TEST_CLANK_NUM_LIKES_INICIAL = 0;

    private static final String COL_CLANKS = "clanks";
    private static final String COL_USUARIOS = "usuarios";

    private static final String SUB_MATERIALES = "materiales";
    private static final String SUB_HERRAMIENTAS = "herramientas";
    private static final String SUB_INSTRUCCIONES = "instrucciones";
    private static final String SUB_LIKES = "likes";

    private static final long TIMEOUT_S = 10;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private String testUid;

    public TestDataSeeder() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public FirebaseUser crearOIniciarSesionUsuarioTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        try {
            Tasks.await(
                    auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD),
                    TIMEOUT_S,
                    TimeUnit.SECONDS
            );
        } catch (Exception ignored) {
            Tasks.await(
                    auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD),
                    TIMEOUT_S,
                    TimeUnit.SECONDS
            );
        }

        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            throw new IllegalStateException("No se pudo obtener el usuario de prueba autenticado.");
        }

        testUid = usuario.getUid();
        return usuario;
    }

    public String getTestUid() {
        if (testUid != null && !testUid.trim().isEmpty()) {
            return testUid;
        }

        FirebaseUser usuario = auth.getCurrentUser();

        if (usuario == null) {
            throw new IllegalStateException("No hay usuario autenticado para obtener UID de test.");
        }

        testUid = usuario.getUid();
        return testUid;
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    public void insertarUsuarioTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getTestUid();

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", uid);
        usuario.put("nombre", TEST_NOMBRE);
        usuario.put("correo", TEST_EMAIL);
        usuario.put("telefono", "600000000");
        usuario.put("usuarioClank", TEST_USUARIO_CLANK);
        usuario.put("fotoPerfil", "");
        usuario.put("fotoPortada", "");
        usuario.put("fechaCreacion", new Date());
        usuario.put("enLinea", false);

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(uid)
                        .set(usuario),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuarioFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(getTestUid())
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuarioAuth()
            throws ExecutionException, InterruptedException, TimeoutException {

        FirebaseUser user = auth.getCurrentUser();

        if (user != null && TEST_EMAIL.equals(user.getEmail())) {
            Tasks.await(user.delete(), TIMEOUT_S, TimeUnit.SECONDS);
        }
    }

    public void insertarClankTest(int numLikes, boolean conSubcol)
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getTestUid();

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_ID);
        clank.put("usuarioId", uid);
        clank.put("titulo", TEST_CLANK_TITULO);
        clank.put("descripcion", "Descripción de prueba para tests de integración");
        clank.put("portada", "");
        clank.put("tiempo", 1);
        clank.put("categorias", Arrays.asList("test-cat"));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", numLikes);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (conSubcol) {
            insertarSubcoleccionesTest(TEST_CLANK_ID);
        }
    }

    public void eliminarClankTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_CLANK_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarClankSinHerramientas()
            throws ExecutionException, InterruptedException, TimeoutException {

        String uid = getTestUid();

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_SIN_HERR_ID);
        clank.put("usuarioId", uid);
        clank.put("titulo", "Clank sin herramientas Fase4");
        clank.put("descripcion", "Sin herramientas para verificar visibilidad GONE");
        clank.put("portada", "");
        clank.put("tiempo", 2);
        clank.put("categorias", Arrays.asList("test-cat"));
        clank.put("estadoAcabado", true);
        clank.put("numLikes", 0);
        clank.put("fechaPublicacion", new Date());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .set(clank),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId", "test-mat-sh-01");
        mat.put("material", "Material sin herr");
        mat.put("cantidad", 1);

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .collection(SUB_MATERIALES)
                        .document("test-mat-sh-01"),
                mat
        );

        Map<String, Object> inst = new HashMap<>();
        inst.put("instId", "test-inst-sh-01");
        inst.put("orden", 1);
        inst.put("instruccion", "Paso único sin herramientas");
        inst.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-sh-01"),
                inst
        );

        Tasks.await(batch.commit(), TIMEOUT_S, TimeUnit.SECONDS);
    }

    public void eliminarClankSinHerramientas()
            throws ExecutionException, InterruptedException, TimeoutException {

        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_INSTRUCCIONES);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_LIKES);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_SIN_HERR_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarSubcoleccionesTest(String clankId)
            throws ExecutionException, InterruptedException, TimeoutException {

        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId", "test-mat-01");
        mat.put("material", "Lana de prueba");
        mat.put("cantidad", 2);

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_MATERIALES)
                        .document("test-mat-01"),
                mat
        );

        Map<String, Object> herr = new HashMap<>();
        herr.put("herrId", "test-herr-01");
        herr.put("herramienta", "Agujas de prueba");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_HERRAMIENTAS)
                        .document("test-herr-01"),
                herr
        );

        Map<String, Object> inst1 = new HashMap<>();
        inst1.put("instId", "test-inst-01");
        inst1.put("orden", 1);
        inst1.put("instruccion", "Primer paso de prueba");
        inst1.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-01"),
                inst1
        );

        Map<String, Object> inst2 = new HashMap<>();
        inst2.put("instId", "test-inst-02");
        inst2.put("orden", 2);
        inst2.put("instruccion", "Segundo paso de prueba");
        inst2.put("imagen", "");

        batch.set(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(SUB_INSTRUCCIONES)
                        .document("test-inst-02"),
                inst2
        );

        Tasks.await(batch.commit(), TIMEOUT_S, TimeUnit.SECONDS);
    }

    public void insertarLikeTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> like = new HashMap<>();
        like.put("uid", getTestUid());

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .collection(SUB_LIKES)
                        .document(getTestUid())
                        .set(like),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarLikeTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .collection(SUB_LIKES)
                        .document(getTestUid())
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    private void borrarSubcoleccion(String clankId, String subcoleccion)
            throws ExecutionException, InterruptedException, TimeoutException {

        QuerySnapshot docs = Tasks.await(
                db.collection(COL_CLANKS)
                        .document(clankId)
                        .collection(subcoleccion)
                        .get(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );

        if (docs.isEmpty()) {
            return;
        }

        WriteBatch batch = db.batch();

        for (DocumentSnapshot doc : docs.getDocuments()) {
            batch.delete(doc.getReference());
        }

        Tasks.await(batch.commit(), TIMEOUT_S, TimeUnit.SECONDS);
    }
}