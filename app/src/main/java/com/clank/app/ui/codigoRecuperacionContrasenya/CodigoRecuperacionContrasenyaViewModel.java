package com.clank.app.ui.codigoRecuperacionContrasenya;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CodigoRecuperacionContrasenyaViewModel extends ViewModel {

    public enum EstadoVerificacionCodigo {
        EXITO,
        CODIGO_INVALIDO,
        CODIGO_CADUCADO,
        DEMASIADOS_INTENTOS,
        CORREO_INVALIDO,
        ERROR_GENERAL
    }

    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
    private final MutableLiveData<EstadoVerificacionCodigo> estadoVerificacion =
            new MutableLiveData<>();
    private final MutableLiveData<String> tokenRecuperacion = new MutableLiveData<>();

    @Inject
    public CodigoRecuperacionContrasenyaViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    public LiveData<Boolean> getCargando() {
        return cargando;
    }

    public LiveData<EstadoVerificacionCodigo> getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public LiveData<String> getTokenRecuperacion() {
        return tokenRecuperacion;
    }

    public void verificarCodigo(String correo, String codigo) {
        cargando.setValue(true);

        authRepository.verificarCodigoRecuperacion(correo, codigo)
                .addOnSuccessListener(resultado -> {
                    cargando.setValue(false);

                    Object datosBrutos = resultado.getData();

                    if (!(datosBrutos instanceof Map)) {
                        estadoVerificacion.setValue(
                                EstadoVerificacionCodigo.ERROR_GENERAL
                        );
                        return;
                    }

                    Map<?, ?> datos = (Map<?, ?>) datosBrutos;

                    boolean correcto = Boolean.TRUE.equals(datos.get("correcto"));

                    if (correcto) {
                        Object token = datos.get("tokenRecuperacion");

                        if (token instanceof String && !((String) token).trim().isEmpty()) {
                            tokenRecuperacion.setValue((String) token);
                            estadoVerificacion.setValue(
                                    EstadoVerificacionCodigo.EXITO
                            );
                        } else {
                            estadoVerificacion.setValue(
                                    EstadoVerificacionCodigo.ERROR_GENERAL
                            );
                        }

                        return;
                    }

                    Object motivo = datos.get("motivo");

                    if (!(motivo instanceof String)) {
                        estadoVerificacion.setValue(
                                EstadoVerificacionCodigo.ERROR_GENERAL
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
                            estadoVerificacion.setValue(
                                    EstadoVerificacionCodigo.CORREO_INVALIDO
                            );
                            return;
                        }
                    }

                    estadoVerificacion.setValue(
                            EstadoVerificacionCodigo.ERROR_GENERAL
                    );
                });
    }

    private void gestionarMotivoError(String motivo) {
        switch (motivo) {
            case "CODIGO_INVALIDO":
                estadoVerificacion.setValue(
                        EstadoVerificacionCodigo.CODIGO_INVALIDO
                );
                break;

            case "CODIGO_CADUCADO":
                estadoVerificacion.setValue(
                        EstadoVerificacionCodigo.CODIGO_CADUCADO
                );
                break;

            case "DEMASIADOS_INTENTOS":
                estadoVerificacion.setValue(
                        EstadoVerificacionCodigo.DEMASIADOS_INTENTOS
                );
                break;

            default:
                estadoVerificacion.setValue(
                        EstadoVerificacionCodigo.ERROR_GENERAL
                );
                break;
        }
    }

    public void limpiarEstadoVerificacion() {
        estadoVerificacion.setValue(null);
    }
}