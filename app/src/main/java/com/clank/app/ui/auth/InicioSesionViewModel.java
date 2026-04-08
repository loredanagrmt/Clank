package com.clank.app.ui.auth;

import androidx.lifecycle.ViewModel;
import com.clank.app.data.repository.AuthRepository;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InicioSesionViewModel extends ViewModel {

  private final AuthRepository repository;

  @Inject
  public InicioSesionViewModel(AuthRepository repository) {
    this.repository = repository;
  }
}
