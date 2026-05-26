package com.clank.app.ui.cambiarContrasenya;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CambiarContrasenyaViewModel extends ViewModel {

    public enum EstadoCambioContrasenya {
        EXITO,
        CONTRASENYA_ACTUAL_INCORRECTA,
        CONTRASENYA_DEBIL,
        ERROR_GENERAL
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<EstadoCambioContrasenya> estadoCambio =
            new MutableLiveData<>();

    @Inject
    public CambiarContrasenyaViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<EstadoCambioContrasenya> getEstadoCambio() {
        return estadoCambio;
    }

    public void cambiarContrasenya(String contrasenyaActual, String nuevaContrasenya) {
        cargando.setValue(true);

        authRepository.comprobarContrasenyaActual(contrasenyaActual)
                .addOnSuccessListener(resultado -> actualizarContrasenya(nuevaContrasenya))
                .addOnFailureListener(error -> {
                    cargando.setValue(false);

                    if (error instanceof FirebaseAuthInvalidCredentialsException) {
                        estadoCambio.setValue(
                                EstadoCambioContrasenya.CONTRASENYA_ACTUAL_INCORRECTA
                        );
                    } else {
                        estadoCambio.setValue(
                                EstadoCambioContrasenya.ERROR_GENERAL
                        );
                    }
                });
    }

    private void actualizarContrasenya(String nuevaContrasenya) {
        authRepository.actualizarContrasenya(nuevaContrasenya)
                .addOnSuccessListener(resultado -> {
                    cargando.setValue(false);
                    estadoCambio.setValue(
                            EstadoCambioContrasenya.EXITO
                    );
                })
                .addOnFailureListener(error -> {
                    cargando.setValue(false);

                    if (error instanceof FirebaseAuthWeakPasswordException) {
                        estadoCambio.setValue(
                                EstadoCambioContrasenya.CONTRASENYA_DEBIL
                        );
                    } else {
                        estadoCambio.setValue(
                                EstadoCambioContrasenya.ERROR_GENERAL
                        );
                    }
                });
    }

    public void limpiarEstadoCambio() {
        estadoCambio.setValue(null);
    }
}