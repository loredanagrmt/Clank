package com.clank.app.data.source;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FirestoreDataSource {
  private final FirebaseFirestore db = FirebaseFirestore.getInstance();

  public void getClanks(OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
    db.collection("clanks")
      .orderBy("fechaCreacion", Query.Direction.DESCENDING)
      .get()
      .addOnSuccessListener(onSuccess)
      .addOnFailureListener(onFailure);
  }
}
