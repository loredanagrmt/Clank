package com.clank.app.data.repository;

import com.clank.app.data.model.Clank;
import com.clank.app.data.source.FirestoreDataSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClankRepository {

  private static final String COLLECTION = "clanks";
  private static final String MATERIALES = "materiales";
  private static final String HERRAMIENTAS = "herramientas";
  private static final String INSTRUCCIONES = "instrucciones";

  private final FirestoreDataSource dataSource;

  @Inject
  public ClankRepository(FirestoreDataSource dataSource) {
    this.dataSource = dataSource;
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

  /////////////////////////lectura de un clank por id/////////////////////////
  public Task<DocumentSnapshot> getPorId(String clankId) {
    return dataSource.collection(COLLECTION).document(clankId).get();
  }

  //subcolecciones de un clank
  public Task<QuerySnapshot> getMateriales(String clankId) {
    return dataSource.collection(COLLECTION).document(clankId)
      .collection(MATERIALES).orderBy("matId").get();
  }

  public Task<QuerySnapshot> getHerramientas(String clankId) {
    return dataSource.collection(COLLECTION).document(clankId)
      .collection(HERRAMIENTAS).orderBy("herrId").get();
  }

  public Task<QuerySnapshot> getInstrucciones(String clankId) {
    return dataSource.collection(COLLECTION).document(clankId)
      .collection(INSTRUCCIONES).orderBy("orden").get();
  }
}
