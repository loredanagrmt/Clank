package com.clank.app.ui.ajustes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AjustesViewModel extends ViewModel {

    public enum EstadoEliminacionCuenta {
        EXITO,
        ERROR_GENERAL
    }

    private final AuthRepository repositorioAutenticacion;

    private final MutableLiveData<Boolean> eliminandoCuenta =
            new MutableLiveData<>(false);

    private final MutableLiveData<EstadoEliminacionCuenta> estadoEliminacionCuenta =
            new MutableLiveData<>();

    @Inject
    public AjustesViewModel(AuthRepository repositorioAutenticacion) {
        this.repositorioAutenticacion = repositorioAutenticacion;
    }

    public void cerrarSesion() {
        repositorioAutenticacion.cerrarSesion();
    }

    public LiveData<Boolean> getEliminandoCuenta() {
        return eliminandoCuenta;
    }

    public LiveData<EstadoEliminacionCuenta> getEstadoEliminacionCuenta() {
        return estadoEliminacionCuenta;
    }

    public void eliminarCuentaCompleta() {
        eliminandoCuenta.setValue(true);

        repositorioAutenticacion.eliminarCuentaCompleta()
                .addOnSuccessListener(resultado -> {
                    eliminandoCuenta.setValue(false);

                    Object datosBrutos = resultado.getData();

                    if (!(datosBrutos instanceof Map)) {
                        estadoEliminacionCuenta.setValue(
                                EstadoEliminacionCuenta.ERROR_GENERAL
                        );
                        return;
                    }

                    Map<?, ?> datos = (Map<?, ?>) datosBrutos;
                    boolean correcto = Boolean.TRUE.equals(datos.get("correcto"));

                    if (correcto) {
                        estadoEliminacionCuenta.setValue(
                                EstadoEliminacionCuenta.EXITO
                        );
                    } else {
                        estadoEliminacionCuenta.setValue(
                                EstadoEliminacionCuenta.ERROR_GENERAL
                        );
                    }
                })
                .addOnFailureListener(error -> {
                    eliminandoCuenta.setValue(false);
                    estadoEliminacionCuenta.setValue(
                            EstadoEliminacionCuenta.ERROR_GENERAL
                    );
                });
    }

    public boolean puedeCambiarContrasenya() {
        return repositorioAutenticacion.puedeCambiarContrasenya();
    }
    public void limpiarEstadoEliminacionCuenta() {
        estadoEliminacionCuenta.setValue(null);
    }
}