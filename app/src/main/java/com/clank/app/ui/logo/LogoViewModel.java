package com.clank.app.ui.logo;

import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LogoViewModel extends ViewModel {

    private final AuthRepository repositorioAutenticacion;

    @Inject
    public LogoViewModel(AuthRepository repositorioAutenticacion) {
        this.repositorioAutenticacion = repositorioAutenticacion;
    }

    public boolean haySesionIniciada() {
        return repositorioAutenticacion.getSesionUsuario() != null;
    }
}