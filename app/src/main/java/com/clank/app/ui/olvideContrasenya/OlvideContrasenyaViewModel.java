package com.clank.app.ui.olvideContrasenya;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class OlvideContrasenyaViewModel extends ViewModel {

    public enum EstadoSolicitudCodigo {
        EXITO,
        CORREO_INVALIDO,
        CORREO_NO_REGISTRADO,
        ERROR_GENERAL
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> cargando =
            new MutableLiveData<>(false);

    private final MutableLiveData<EstadoSolicitudCodigo> estadoSolicitudCodigo =
            new MutableLiveData<>();

    @Inject
    public OlvideContrasenyaViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<EstadoSolicitudCodigo> getEstadoSolicitudCodigo() {
        return estadoSolicitudCodigo;
    }

    public void solicitarCodigoRecuperacion(String correo) {
        cargando.setValue(true);

        authRepository.solicitarCodigoRecuperacion(correo)
                .addOnSuccessListener(resultado -> {
                    cargando.setValue(false);

                    Object datosBrutos = resultado.getData();

                    if (!(datosBrutos instanceof Map)) {
                        estadoSolicitudCodigo.setValue(
                                EstadoSolicitudCodigo.ERROR_GENERAL
                        );
                        return;
                    }

                    Map<?, ?> datos = (Map<?, ?>) datosBrutos;

                    boolean correcto = Boolean.TRUE.equals(datos.get("correcto"));

                    if (correcto) {
                        estadoSolicitudCodigo.setValue(
                                EstadoSolicitudCodigo.EXITO
                        );
                        return;
                    }

                    Object motivo = datos.get("motivo");

                    if ("CORREO_NO_REGISTRADO".equals(motivo)) {
                        estadoSolicitudCodigo.setValue(
                                EstadoSolicitudCodigo.CORREO_NO_REGISTRADO
                        );
                        return;
                    }

                    estadoSolicitudCodigo.setValue(
                            EstadoSolicitudCodigo.ERROR_GENERAL
                    );
                })
                .addOnFailureListener(error -> {
                    cargando.setValue(false);

                    if (error instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException errorFunctions =
                                (FirebaseFunctionsException) error;

                        if (errorFunctions.getCode()
                                == FirebaseFunctionsException.Code.INVALID_ARGUMENT) {
                            estadoSolicitudCodigo.setValue(
                                    EstadoSolicitudCodigo.CORREO_INVALIDO
                            );
                            return;
                        }
                    }

                    estadoSolicitudCodigo.setValue(
                            EstadoSolicitudCodigo.ERROR_GENERAL
                    );
                });
    }

    public void limpiarEstadoSolicitudCodigo() {
        estadoSolicitudCodigo.setValue(null);
    }
}