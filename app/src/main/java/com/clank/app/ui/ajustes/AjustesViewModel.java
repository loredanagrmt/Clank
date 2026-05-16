package com.clank.app.ui.ajustes;

import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AjustesViewModel extends ViewModel {

    private final AuthRepository repositorioAutenticacion;

    @Inject
    public AjustesViewModel(AuthRepository repositorioAutenticacion) {
        this.repositorioAutenticacion = repositorioAutenticacion;
    }

    public void cerrarSesion() {
        repositorioAutenticacion.cerrarSesion();
    }
}