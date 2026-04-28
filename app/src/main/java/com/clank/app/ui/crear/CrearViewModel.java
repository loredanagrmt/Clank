package com.clank.app.ui.crear;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.util.Recurso;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CrearViewModel extends ViewModel {

  public final MutableLiveData<Recurso<Void>> guardarEstado = new MutableLiveData<>();

  @Inject
  public CrearViewModel() {}

  public void guardarClank(Clank clank) {

  }
}
