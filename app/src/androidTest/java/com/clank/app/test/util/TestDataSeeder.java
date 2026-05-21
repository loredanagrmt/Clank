package com.clank.app.test;

import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.data.model.Usuario;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Clase de andamiaje para tests de integración de Fase 4.
 * Uso: llamar desde @Before y @After de cada suite.
 * NUNCA usar en código de producción.
 */
public class TestDataSeeder {

    // ── Constantes de IDs controlados ─────────────────────────────────────
    public static final String TEST_CLANK_ID              = "test-clank-fase4";
    public static final String TEST_CLANK_SIN_HERR_ID     = "test-clank-sin-herr-fase4";
    public static final String TEST_EMAIL                 = "test-fase4@clank.test";
    public static final String TEST_PASSWORD              = "TestClank1234!";
    public static final String TEST_NOMBRE                = "Usuario Test Fase4";
    public static final String TEST_CLANK_TITULO          = "Clank de prueba Fase4";
    public static final int    TEST_CLANK_NUM_LIKES_INICIAL = 0;

    // ── Nombres de colecciones (deben coincidir con ClankRepository) ──────
    private static final String COL_CLANKS        = "clanks";
    private static final String COL_USUARIOS      = "usuarios";
    private static final String COL_LIKES         = "likes";
    private static final String SUB_MATERIALES    = "materiales";
    private static final String SUB_HERRAMIENTAS  = "herramientas";
    private static final String SUB_INSTRUCCIONES = "instrucciones";

    // ── Firebase ──────────────────────────────────────────────────────────
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public TestDataSeeder() {
        this.db   = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();

        this.auth.useEmulator("10.0.2.2", 9099);
        this.db.useEmulator("10.0.2.2", 8080);
    }

    private String testUid;

    public String getTestUid() {
        return testUid;
    }



    // ─────────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────────

    public FirebaseUser crearOIniciarSesionUsuarioTest()
            throws ExecutionException, InterruptedException {
        try {
            Tasks.await(auth.createUserWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD));
        } catch (Exception e) {
            Tasks.await(auth.signInWithEmailAndPassword(TEST_EMAIL, TEST_PASSWORD));
        }
        FirebaseUser user = auth.getCurrentUser();
        testUid = user.getUid();   // ← UID real, no hardcodeado
        return user;
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    public void eliminarUsuarioTest()
            throws ExecutionException, InterruptedException {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && TEST_EMAIL.equals(user.getEmail())) {
            Tasks.await(user.delete());
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // USUARIO FIRESTORE
    // ─────────────────────────────────────────────────────────────────────

    public void insertarUsuarioTest()
            throws ExecutionException, InterruptedException {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid",            testUid);
        usuario.put("nombre",         TEST_NOMBRE);
        usuario.put("correo",         TEST_EMAIL);
        usuario.put("telefono",       "600000000");
        usuario.put("fotoPerfil",     "");
        usuario.put("fotoPortada",    "");
        usuario.put("fechaCreacion",  new Date());
        usuario.put("enLinea",        false);
        Tasks.await(db.collection(COL_USUARIOS).document(testUid).set(usuario));
    }

    public void eliminarUsuarioFirestore()
            throws ExecutionException, InterruptedException {
        Tasks.await(db.collection(COL_USUARIOS).document(testUid).delete());
    }

    // ─────────────────────────────────────────────────────────────────────
    // CLANK COMPLETO
    // ─────────────────────────────────────────────────────────────────────

    public void insertarClankTest(int numLikes, boolean conSubcol)
            throws ExecutionException, InterruptedException {
        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId",          TEST_CLANK_ID);
        clank.put("usuarioId",        testUid);
        clank.put("titulo",           TEST_CLANK_TITULO);
        clank.put("descripcion",      "Descripción de prueba para tests de integración");
        clank.put("portada",          "https://example.com/portada-test.jpg");
        clank.put("tiempo",           1);   // 1=cohete
        clank.put("categorias",       Arrays.asList("test-cat"));
        clank.put("estadoAcabado",    true);
        clank.put("numLikes",         numLikes);
        clank.put("fechaPublicacion", new Date());
        Tasks.await(db.collection(COL_CLANKS).document(TEST_CLANK_ID).set(clank));
        if (conSubcol) insertarSubcoleccionesTest(TEST_CLANK_ID);
    }

    public void eliminarClankTest()
            throws ExecutionException, InterruptedException {
        borrarSubcoleccion(TEST_CLANK_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_HERRAMIENTAS);
        borrarSubcoleccion(TEST_CLANK_ID, SUB_INSTRUCCIONES);
        Tasks.await(db.collection(COL_CLANKS).document(TEST_CLANK_ID).delete());
    }

    // ─────────────────────────────────────────────────────────────────────
    // CLANK SIN HERRAMIENTAS
    // ─────────────────────────────────────────────────────────────────────

    public void insertarClankSinHerramientas()
            throws ExecutionException, InterruptedException {
        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId",          TEST_CLANK_SIN_HERR_ID);
        clank.put("usuarioId",        testUid);
        clank.put("titulo",           "Clank sin herramientas Fase4");
        clank.put("descripcion",      "Sin herramientas para verificar visibilidad GONE");
        clank.put("portada",          "https://example.com/portada-sin-herr.jpg");
        clank.put("tiempo",           2);   // 2=liebre
        clank.put("categorias",       Arrays.asList("test-cat"));
        clank.put("estadoAcabado",    true);
        clank.put("numLikes",         0);
        clank.put("fechaPublicacion", new Date());
        Tasks.await(db.collection(COL_CLANKS).document(TEST_CLANK_SIN_HERR_ID).set(clank));

        // Solo materiales e instrucciones, sin herramientas
        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId",    "test-mat-sh-01");
        mat.put("material", "Material sin herr");
        mat.put("cantidad", 1);
        batch.set(db.collection(COL_CLANKS).document(TEST_CLANK_SIN_HERR_ID)
                .collection(SUB_MATERIALES).document("test-mat-sh-01"), mat);

        Map<String, Object> inst = new HashMap<>();
        inst.put("instId",      "test-inst-sh-01");
        inst.put("orden",       1);
        inst.put("instruccion", "Paso único sin herramientas");
        inst.put("imagen",      "");
        batch.set(db.collection(COL_CLANKS).document(TEST_CLANK_SIN_HERR_ID)
                .collection(SUB_INSTRUCCIONES).document("test-inst-sh-01"), inst);

        Tasks.await(batch.commit());
    }

    public void eliminarClankSinHerramientas()
            throws ExecutionException, InterruptedException {
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_MATERIALES);
        borrarSubcoleccion(TEST_CLANK_SIN_HERR_ID, SUB_INSTRUCCIONES);
        Tasks.await(db.collection(COL_CLANKS).document(TEST_CLANK_SIN_HERR_ID).delete());
    }

    // ─────────────────────────────────────────────────────────────────────
    // SUBCOLECCIONES (clank completo)
    // ─────────────────────────────────────────────────────────────────────

    public void insertarSubcoleccionesTest(String clankId)
            throws ExecutionException, InterruptedException {
        WriteBatch batch = db.batch();

        Map<String, Object> mat = new HashMap<>();
        mat.put("matId",    "test-mat-01");
        mat.put("material", "Lana de prueba");
        mat.put("cantidad", 2);
        batch.set(db.collection(COL_CLANKS).document(clankId)
                .collection(SUB_MATERIALES).document("test-mat-01"), mat);

        Map<String, Object> herr = new HashMap<>();
        herr.put("herrId",      "test-herr-01");
        herr.put("herramienta", "Agujas de prueba");
        batch.set(db.collection(COL_CLANKS).document(clankId)
                .collection(SUB_HERRAMIENTAS).document("test-herr-01"), herr);

        Map<String, Object> inst1 = new HashMap<>();
        inst1.put("instId",      "test-inst-01");
        inst1.put("orden",       1);
        inst1.put("instruccion", "Primer paso de prueba");
        inst1.put("imagen",      "");
        batch.set(db.collection(COL_CLANKS).document(clankId)
                .collection(SUB_INSTRUCCIONES).document("test-inst-01"), inst1);

        Map<String, Object> inst2 = new HashMap<>();
        inst2.put("instId",      "test-inst-02");
        inst2.put("orden",       2);
        inst2.put("instruccion", "Segundo paso de prueba");
        inst2.put("imagen",      "");
        batch.set(db.collection(COL_CLANKS).document(clankId)
                .collection(SUB_INSTRUCCIONES).document("test-inst-02"), inst2);

        Tasks.await(batch.commit());
    }

    // ─────────────────────────────────────────────────────────────────────
    // LIKES
    // ─────────────────────────────────────────────────────────────────────

    public void insertarLikeTest()
            throws ExecutionException, InterruptedException {
        Tasks.await(db.collection(COL_LIKES)
                .document(TEST_CLANK_ID)
                .collection("usuarios")
                .document(testUid)
                .set(new HashMap<>()));
    }

    public void eliminarLikeTest()
            throws ExecutionException, InterruptedException {
        Tasks.await(db.collection(COL_LIKES)
                .document(TEST_CLANK_ID)
                .collection("usuarios")
                .document(testUid)
                .delete());
    }

    // ─────────────────────────────────────────────────────────────────────
    // UTILIDADES INTERNAS
    // ─────────────────────────────────────────────────────────────────────

    private void borrarSubcoleccion(String clankId, String subcoleccion)
            throws ExecutionException, InterruptedException {
        var docs = Tasks.await(db.collection(COL_CLANKS).document(clankId)
                .collection(subcoleccion).get());
        if (docs.isEmpty()) return;
        WriteBatch batch = db.batch();
        for (var doc : docs.getDocuments()) batch.delete(doc.getReference());
        Tasks.await(batch.commit());
    }
}
