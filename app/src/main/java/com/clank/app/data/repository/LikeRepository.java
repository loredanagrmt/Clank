package com.clank.app.data.repository;

import com.clank.app.data.source.FirestoreDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LikeRepository {

  private static final String CLANKS = "clanks";
  private static final String LIKES  = "likes";

  private final FirestoreDataSource dataSource;

  @Inject
  public LikeRepository(FirestoreDataSource dataSource) {
    this.dataSource = dataSource;
  }
  public Task<Boolean> toggleLike(String clankId, String uid) {
    DocumentReference likeRef = dataSource.collection(CLANKS)
      .document(clankId)
      .collection(LIKES)
      .document(uid);

    return dataSource.getFirestore().runTransaction(transaction -> {
      DocumentSnapshot likeSnap = transaction.get(likeRef);
      boolean yaDioLike = likeSnap.exists();

      if (yaDioLike) {
        transaction.delete(likeRef);
      } else {
        transaction.set(likeRef, Collections.singletonMap("uid", uid));
      }

      return !yaDioLike;
    });
  }
  public Task<Boolean> hasDadoLike(String clankId, String uid) {
    return dataSource.collection(CLANKS)
      .document(clankId)
      .collection(LIKES)
      .document(uid)
      .get()
      .continueWith(task -> task.isSuccessful()
        && task.getResult() != null
        && task.getResult().exists());
  }
  public ListenerRegistration escucharNumLikes(String clankId,
                                               OnNumLikesListener listener) {
    return dataSource.collection(CLANKS)
      .document(clankId)
      .collection(LIKES)
      .addSnapshotListener((snap, e) -> {
        if (e != null || snap == null) return;
        listener.onNumLikes(snap.size());
      });
  }

  public interface OnNumLikesListener {
    void onNumLikes(int cantidad);
  }
}
