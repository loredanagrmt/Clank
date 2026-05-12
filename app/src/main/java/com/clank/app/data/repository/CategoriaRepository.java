package com.clank.app.data.repository;

import com.clank.app.data.source.FirestoreDataSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CategoriaRepository {

  private static final String COLLECTION = "categorias";
  private final FirestoreDataSource dataSource;

  @Inject
  public CategoriaRepository(FirestoreDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Task<QuerySnapshot> getTodas() {
    return dataSource.collection(COLLECTION).get();
  }
}
