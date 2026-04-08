package com.clank.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.clank.app.data.model.Clank;
import com.clank.app.data.source.FirestoreDataSource;
import com.clank.app.util.Resource;

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

  public MutableLiveData<Resource<List<Clank>>> getClanks() {
    MutableLiveData<Resource<List<Clank>>> liveData = new MutableLiveData<>();
    liveData.setValue(Resource.loading());

    dataSource.getClanks(
      querySnapshot -> {
        List<Clank> lista = querySnapshot.toObjects(Clank.class);
        liveData.setValue(Resource.success(lista));
      },
      e -> liveData.setValue(Resource.error(e.getMessage()))
    );
    return liveData;
  }
}
