package com.clank.app.data.repository;

import com.clank.app.data.model.Clank;
import com.clank.app.data.source.FirestoreDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClankRepository {

    private static final String COLLECTION = "clanks";
    private static final String MATERIALES = "materiales";
    private static final String HERRAMIENTAS = "herramientas";
    private static final String INSTRUCCIONES = "instrucciones";

    private final FirestoreDataSource dataSource;
    private final FirebaseStorage storage;

    @Inject
    public ClankRepository(FirestoreDataSource dataSource,
                           FirebaseStorage storage) {
        this.dataSource = dataSource;
        this.storage = storage;
    }

    public DocumentReference crear(Clank clank) {
        CollectionReference col = dataSource.collection(COLLECTION);
        DocumentReference ref = col.document();
        ref.set(clank);
        return ref;
    }

    public DocumentReference nuevaReferencia() {
        return dataSource.collection(COLLECTION).document();
    }

    public Query getPorUsuario(String usuarioId) {
        return dataSource.collection(COLLECTION)
                .whereEqualTo("usuarioId", usuarioId);
    }

    /// ////////////////////// lectura de un clank por id /////////////////////////
    public Task<DocumentSnapshot> getPorId(String clankId) {
        return dataSource.collection(COLLECTION)
                .document(clankId)
                .get();
    }

    // Subcolecciones de un clank
    public Task<QuerySnapshot> getMateriales(String clankId) {
        return dataSource.collection(COLLECTION)
                .document(clankId)
                .collection(MATERIALES)
                .get();
    }

    public Task<QuerySnapshot> getHerramientas(String clankId) {
        return dataSource.collection(COLLECTION)
                .document(clankId)
                .collection(HERRAMIENTAS)
                .get();
    }

    public Task<QuerySnapshot> getInstrucciones(String clankId) {
        return dataSource.collection(COLLECTION)
                .document(clankId)
                .collection(INSTRUCCIONES)
                .orderBy("orden")
                .get();
    }

    public Query getTodosAcabados() {
        return dataSource.collection(COLLECTION)
                .whereEqualTo("estadoAcabado", true)
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING);
    }

    /// ////////////////////// eliminar clank completo /////////////////////////
    public Task<Void> eliminarCompletoPorId(String clankId) {
        TaskCompletionSource<Void> resultado = new TaskCompletionSource<>();

        Task<DocumentSnapshot> tareaClank = getPorId(clankId);
        Task<QuerySnapshot> tareaMateriales = getMateriales(clankId);
        Task<QuerySnapshot> tareaHerramientas = getHerramientas(clankId);
        Task<QuerySnapshot> tareaInstrucciones = getInstrucciones(clankId);

        Tasks.whenAllSuccess(
                        tareaClank,
                        tareaMateriales,
                        tareaHerramientas,
                        tareaInstrucciones
                )
                .addOnSuccessListener(resultados -> {
                    DocumentSnapshot docClank =
                            (DocumentSnapshot) resultados.get(0);

                    QuerySnapshot snapMateriales =
                            (QuerySnapshot) resultados.get(1);

                    QuerySnapshot snapHerramientas =
                            (QuerySnapshot) resultados.get(2);

                    QuerySnapshot snapInstrucciones =
                            (QuerySnapshot) resultados.get(3);

                    eliminarArchivosStorage(docClank, snapInstrucciones)
                            .addOnSuccessListener(unused ->
                                    eliminarSubcoleccionesYDocumento(
                                            clankId,
                                            snapMateriales,
                                            snapHerramientas,
                                            snapInstrucciones
                                    )
                                            .addOnSuccessListener(v ->
                                                    resultado.setResult(null))
                                            .addOnFailureListener(
                                                    resultado::setException
                                            )
                            )
                            .addOnFailureListener(resultado::setException);
                })
                .addOnFailureListener(resultado::setException);

        return resultado.getTask();
    }

    private Task<Void> eliminarArchivosStorage(DocumentSnapshot docClank,
                                               QuerySnapshot snapInstrucciones) {
        List<Task<Void>> tareas = new ArrayList<>();

        if (docClank != null && docClank.exists()) {
            String portada = docClank.getString("portada");

            if (portada != null && !portada.trim().isEmpty()) {
                tareas.add(eliminarArchivoStorage(portada));
            }
        }

        for (DocumentSnapshot doc : snapInstrucciones.getDocuments()) {
            String imagen = doc.getString("imagen");

            if (imagen != null && !imagen.trim().isEmpty()) {
                tareas.add(eliminarArchivoStorage(imagen));
            }
        }

        return esperarTareasVoid(tareas);
    }

    private Task<Void> eliminarSubcoleccionesYDocumento(
            String clankId,
            QuerySnapshot snapMateriales,
            QuerySnapshot snapHerramientas,
            QuerySnapshot snapInstrucciones
    ) {
        List<Task<Void>> tareasBorrado = new ArrayList<>();

        anyadirBorradosSubcoleccion(tareasBorrado, snapMateriales);
        anyadirBorradosSubcoleccion(tareasBorrado, snapHerramientas);
        anyadirBorradosSubcoleccion(tareasBorrado, snapInstrucciones);

        return esperarTareasVoid(tareasBorrado)
                .continueWithTask(tarea -> {
                    if (!tarea.isSuccessful()) {
                        Exception error = tarea.getException() != null
                                ? tarea.getException()
                                : new Exception("Error borrando subcolecciones");
                        return Tasks.forException(error);
                    }

                    return dataSource.collection(COLLECTION)
                            .document(clankId)
                            .delete();
                });
    }

    private void anyadirBorradosSubcoleccion(List<Task<Void>> tareas,
                                             QuerySnapshot snapshot) {
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            tareas.add(doc.getReference().delete());
        }
    }

    private Task<Void> esperarTareasVoid(List<Task<Void>> tareas) {
        if (tareas.isEmpty()) {
            return Tasks.forResult(null);
        }

        return Tasks.whenAllSuccess(tareas)
                .continueWith(tarea -> {
                    if (!tarea.isSuccessful()) {
                        Exception error = tarea.getException() != null
                                ? tarea.getException()
                                : new Exception("Error completando tareas");
                        throw error;
                    }

                    return null;
                });
    }

    private Task<Void> eliminarArchivoStorage(String urlArchivo) {
        TaskCompletionSource<Void> resultado = new TaskCompletionSource<>();

        try {
            storage.getReferenceFromUrl(urlArchivo)
                    .delete()
                    .addOnSuccessListener(unused ->
                            resultado.setResult(null))
                    .addOnFailureListener(error -> {
                        StorageException storageError =
                                StorageException.fromException(error);

                        if (storageError != null
                                && storageError.getErrorCode()
                                == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            resultado.setResult(null);
                        } else {
                            resultado.setException(error);
                        }
                    });

        } catch (IllegalArgumentException e) {
            resultado.setException(e);
        }

        return resultado.getTask();
    }
  public Task<QuerySnapshot> getClanksAcabadosRecientes(int limite) {
    return dataSource.collection(COLLECTION).whereEqualTo("estadoAcabado", true)
      .orderBy("fechaPublicacion", Query.Direction.DESCENDING).limit(limite).get();
  }
}
