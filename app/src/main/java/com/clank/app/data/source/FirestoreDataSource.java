package com.clank.app.data.source;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import javax.inject.Inject;

public class FirestoreDataSource {

  private final FirebaseFirestore db;

  @Inject
  public FirestoreDataSource(FirebaseFirestore db) {
    this.db = db;
  }

  public CollectionReference collection(String name) {
    return db.collection(name);
  }

  public DocumentReference document(String collection, String id) {
    return db.collection(collection).document(id);
  }

  public Task<Void> set(String collection, String id, Object data) {
    return db.collection(collection).document(id).set(data);
  }

  public Task<Void> update(String collection, String id, Map<String, Object> fields) {
    return db.collection(collection).document(id).update(fields);
  }

  public Task<DocumentSnapshot> get(String collection, String id) {
    return db.collection(collection).document(id).get();
  }

  public Task<Void> delete(String collection, String id) {
    return db.collection(collection).document(id).delete();
  }

  public Query queryByField(String collection, String field, Object value) {
    return db.collection(collection).whereEqualTo(field, value);
  }

  public Query queryOrderedDesc(String collection, String orderField) {
    return db.collection(collection).orderBy(orderField, Query.Direction.DESCENDING);
  }

  public FirebaseFirestore getFirestore() {
    return db;
  }
}
