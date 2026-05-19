package com.clank.app.ui.nuevaContrasenya;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NuevaContrasenyaViewModel extends ViewModel {

    public enum EstadoActualizacionContrasenya {
        EXITO,
        CONTRASENYA_DEBIL,
        TOKEN_INVALIDO,
        TOKEN_CADUCADO,
        DATOS_INVALIDOS,
        ERROR_GENERAL
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<EstadoActualizacionContrasenya> estadoActualizacion =
            new MutableLiveData<>();

    @Inject
    public NuevaContrasenyaViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<EstadoActualizacionContrasenya> getEstadoActualizacion() {
        return estadoActualizacion;
    }

    public void actualizarContrasenya(
            String correo,
            String tokenRecuperacion,
            String nuevaContrasenya
    ) {
        cargando.setValue(true);

        authRepository.actualizarContrasenyaRecuperacion(
                        correo,
                        tokenRecuperacion,
                        nuevaContrasenya
                )
                .addOnSuccessListener(resultado -> {
                    cargando.setValue(false);

                    Object datosBrutos = resultado.getData();

                    if (!(datosBrutos instanceof Map)) {
                        estadoActualizacion.setValue(
                                EstadoActualizacionContrasenya.ERROR_GENERAL
                        );
                        return;
                    }

                    Map<?, ?> datos = (Map<?, ?>) datosBrutos;
                    boolean correcto = Boolean.TRUE.equals(datos.get("correcto"));

                    if (correcto) {
                        estadoActualizacion.setValue(
                                EstadoActualizacionContrasenya.EXITO
                        );
                        return;
                    }

                    Object motivo = datos.get("motivo");

                    if (!(motivo instanceof String)) {
                        estadoActualizacion.setValue(
                                EstadoActualizacionContrasenya.ERROR_GENERAL
                        );
                        return;
                    }

                    gestionarMotivoError((String) motivo);
                })
                .addOnFailureListener(error -> {
                    cargando.setValue(false);

                    if (error instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException errorFunctions =
                                (FirebaseFunctionsException) error;

                        if (errorFunctions.getCode()
                                == FirebaseFunctionsException.Code.INVALID_ARGUMENT) {
                            estadoActualizacion.setValue(
                                    EstadoActualizacionContrasenya.DATOS_INVALIDOS
                            );
                            return;
                        }
                    }

                    estadoActualizacion.setValue(
                            EstadoActualizacionContrasenya.ERROR_GENERAL
                    );
                });
    }

    private void gestionarMotivoError(String motivo) {
        switch (motivo) {
            case "CONTRASENYA_DEBIL":
                estadoActualizacion.setValue(
                        EstadoActualizacionContrasenya.CONTRASENYA_DEBIL
                );
                break;

            case "TOKEN_INVALIDO":
                estadoActualizacion.setValue(
                        EstadoActualizacionContrasenya.TOKEN_INVALIDO
                );
                break;

            case "TOKEN_CADUCADO":
                estadoActualizacion.setValue(
                        EstadoActualizacionContrasenya.TOKEN_CADUCADO
                );
                break;

            default:
                estadoActualizacion.setValue(
                        EstadoActualizacionContrasenya.ERROR_GENERAL
                );
                break;
        }
    }

    public void limpiarEstadoActualizacion() {
        estadoActualizacion.setValue(null);
    }
}