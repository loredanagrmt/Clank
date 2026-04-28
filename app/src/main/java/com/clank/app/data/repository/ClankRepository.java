package com.clank.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.clank.app.data.model.Clank;
import com.clank.app.data.source.FirestoreDataSource;
import com.clank.app.util.Recurso;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClankRepository {

  private final FirestoreDataSource dataSource;

  @Inject
  public ClankRepository(FirestoreDataSource dataSource) {
    this.dataSource = dataSource;
  }

  public MutableLiveData<Recurso<List<Clank>>> getClanks() {
    MutableLiveData<Recurso<List<Clank>>> liveData = new MutableLiveData<>();
    liveData.setValue(Recurso.loading());

    dataSource.getClanks(
      querySnapshot -> {
        List<Clank> lista = querySnapshot.toObjects(Clank.class);
        liveData.setValue(Recurso.success(lista));
      },
      e -> liveData.setValue(Recurso.error(e.getMessage()))
    );
    return liveData;
  }
}
