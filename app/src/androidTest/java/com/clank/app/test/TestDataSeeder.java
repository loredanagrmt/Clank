package com.clank.app.test;

import com.google.android.gms.tasks.Tasks;
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

    public static final String TEST_UID = "test-uid-fase4";
    public static final String TEST_EMAIL = "test-fase4@clank.test";
    public static final String TEST_NOMBRE = "Usuario Test Fase4";
    public static final String TEST_USUARIO_CLANK = "usuario_test_fase4";

    public static final String TEST_CLANK_ID = "test-clank-fase4";
    public static final String TEST_CLANK_SIN_HERR_ID = "test-clank-sin-herr-fase4";
    public static final String TEST_CLANK_TITULO = "Clank de prueba Fase4";
    public static final int TEST_CLANK_NUM_LIKES_INICIAL = 0;

    public static final String TEST_CATEGORIA_ID = "test-cat";
    public static final String TEST_CATEGORIA_NOMBRE = "Categoría de prueba";

    private static final String COL_CLANKS = "clanks";
    private static final String COL_USUARIOS = "usuarios";
    private static final String COL_CATEGORIAS = "categorias";

    private static final String SUB_MATERIALES = "materiales";
    private static final String SUB_HERRAMIENTAS = "herramientas";
    private static final String SUB_INSTRUCCIONES = "instrucciones";
    private static final String SUB_LIKES = "likes";

    private static final long TIMEOUT_S = 10;

    private final FirebaseFirestore db;

    public TestDataSeeder() {
        this.db = FirebaseFirestore.getInstance();
    }

    public String getTestUid() {
        return TEST_UID;
    }

    public void insertarUsuarioTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> usuario = new HashMap<>();
        usuario.put("uid", TEST_UID);
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
                        .document(TEST_UID)
                        .set(usuario),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarUsuarioFirestore()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_USUARIOS)
                        .document(TEST_UID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarCategoriaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> categoria = new HashMap<>();
        categoria.put("categoria", TEST_CATEGORIA_NOMBRE);

        Tasks.await(
                db.collection(COL_CATEGORIAS)
                        .document(TEST_CATEGORIA_ID)
                        .set(categoria),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void eliminarCategoriaTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Tasks.await(
                db.collection(COL_CATEGORIAS)
                        .document(TEST_CATEGORIA_ID)
                        .delete(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarClankTest(int numLikes, boolean conSubcol)
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_ID);
        clank.put("usuarioId", TEST_UID);
        clank.put("titulo", TEST_CLANK_TITULO);
        clank.put("descripcion", "Descripción de prueba para tests de integración");
        clank.put("portada", "");
        clank.put("tiempo", 1);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
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

        Map<String, Object> clank = new HashMap<>();
        clank.put("clankId", TEST_CLANK_SIN_HERR_ID);
        clank.put("usuarioId", TEST_UID);
        clank.put("titulo", "Clank sin herramientas Fase4");
        clank.put("descripcion", "Sin herramientas para verificar visibilidad GONE");
        clank.put("portada", "");
        clank.put("tiempo", 2);
        clank.put("categorias", Arrays.asList(TEST_CATEGORIA_ID));
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

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
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

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }

    public void insertarLikeTest()
            throws ExecutionException, InterruptedException, TimeoutException {

        Map<String, Object> like = new HashMap<>();
        like.put("uid", TEST_UID);

        Tasks.await(
                db.collection(COL_CLANKS)
                        .document(TEST_CLANK_ID)
                        .collection(SUB_LIKES)
                        .document(TEST_UID)
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
                        .document(TEST_UID)
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

        Tasks.await(
                batch.commit(),
                TIMEOUT_S,
                TimeUnit.SECONDS
        );
    }
}