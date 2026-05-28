package com.clank.app.data.repository;

import com.clank.app.data.source.FirestoreDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LikeRepository {

  private static final String CLANKS = "clanks";
  private static final String LIKES = "likes";

  private final FirestoreDataSource dataSource;

  @Inject
  public LikeRepository(FirestoreDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Task<Boolean> toggleLike(String clankId, String uid) {
    DocumentReference clankRef = dataSource.collection(CLANKS)
            .document(clankId);

    DocumentReference likeRef = clankRef
            .collection(LIKES)
            .document(uid);

    return dataSource.getFirestore().runTransaction(transaction -> {
      DocumentSnapshot clankSnap = transaction.get(clankRef);

      if (!clankSnap.exists()) {
        throw new FirebaseFirestoreException(
                "El clank no existe",
                FirebaseFirestoreException.Code.NOT_FOUND
        );
      }

      DocumentSnapshot likeSnap = transaction.get(likeRef);
      boolean yaDioLike = likeSnap.exists();

      Long numLikesActualLong = clankSnap.getLong("numLikes");
      long numLikesActual = numLikesActualLong != null ? numLikesActualLong : 0L;

      long nuevoNumLikes;

      if (yaDioLike) {
        transaction.delete(likeRef);
        nuevoNumLikes = Math.max(0L, numLikesActual - 1L);
      } else {
        Map<String, Object> like = new HashMap<>();
        like.put("uid", uid);
        like.put("fechaLike", Timestamp.now());

        transaction.set(likeRef, like);
        nuevoNumLikes = numLikesActual + 1L;
      }

      transaction.update(clankRef, "numLikes", nuevoNumLikes);

      return !yaDioLike;
    });
  }

  public Task<Boolean> hasDadoLike(String clankId, String uid) {
    return dataSource.collection(CLANKS)
            .document(clankId)
            .collection(LIKES)
            .document(uid)
            .get()
            .continueWith(task ->
                    task.isSuccessful()
                            && task.getResult() != null
                            && task.getResult().exists()
            );
  }

  public ListenerRegistration escucharNumLikes(String clankId,
                                               OnNumLikesListener listener) {
    return dataSource.collection(CLANKS)
            .document(clankId)
            .collection(LIKES)
            .addSnapshotListener((snap, error) -> {
              if (error != null || snap == null || listener == null) {
                return;
              }

              int cantidadReal = Math.max(0, snap.size());

              listener.onNumLikes(cantidadReal);

              sincronizarNumLikes(clankId, cantidadReal);
            });
  }

  private void sincronizarNumLikes(String clankId, int cantidadReal) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    dataSource.collection(CLANKS)
            .document(clankId)
            .update("numLikes", Math.max(0, cantidadReal));
  }

  public interface OnNumLikesListener {
    void onNumLikes(int cantidad);
  }
}