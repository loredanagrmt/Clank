package com.clank.app.ui.inicioSesion;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.util.Recurso;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class InicioSesionViewModel extends ViewModel {

    public enum DestinoNavegacion {
        PRINCIPAL,
        REGISTRO
    }

    private final MutableLiveData<Recurso<DestinoNavegacion>> resultadoInicioSesion =
            new MutableLiveData<>();

    private final AuthRepository repositorioAutenticacion;
    private final UsuarioRepository repositorioUsuario;

    @Inject
    public InicioSesionViewModel(
            AuthRepository repositorioAutenticacion,
            UsuarioRepository repositorioUsuario
    ) {
        this.repositorioAutenticacion = repositorioAutenticacion;
        this.repositorioUsuario = repositorioUsuario;
    }

    public LiveData<Recurso<DestinoNavegacion>> obtenerResultadoInicioSesion() {
        return resultadoInicioSesion;
    }

  public boolean haySesionIniciada() {
    return repositorioAutenticacion.getSesionUsuario() != null;
  }

    public void iniciarSesion(String correo, String contrasenya) {
        if (correo == null || contrasenya == null
                || correo.trim().isEmpty()
                || contrasenya.trim().isEmpty()) {

            resultadoInicioSesion.setValue(
                    Recurso.error("Rellena todos los campos")
            );
            return;
        }

        resultadoInicioSesion.setValue(Recurso.cargando());

        repositorioAutenticacion.iniciarSesion(correo.trim(), contrasenya.trim())
                .addOnCompleteListener(tarea -> {
                    if (tarea.isSuccessful()) {
                        resultadoInicioSesion.postValue(
                                Recurso.exito(DestinoNavegacion.PRINCIPAL)
                        );
                    } else {
                        resultadoInicioSesion.postValue(
                                Recurso.error("El correo o la contrasenya son incorrectos")
                        );
                    }
                });
    }

    public void iniciarSesionGoogle(GoogleSignInAccount cuenta) {
        if (cuenta == null) {
            resultadoInicioSesion.setValue(
                    Recurso.error("Cuenta de Google no válida")
            );
            return;
        }

        resultadoInicioSesion.setValue(Recurso.cargando());

        repositorioAutenticacion.iniciarSesionGoogle(cuenta)
                .addOnCompleteListener(tarea -> {
                    if (tarea.isSuccessful()) {
                        comprobarUsuarioExiste(repositorioAutenticacion.getUid());
                    } else {
                        resultadoInicioSesion.postValue(
                                Recurso.error("No se pudo iniciar sesión con Google")
                        );
                    }
                });
    }

  private void comprobarUsuarioExiste(String uid) {
    if (uid == null || uid.isEmpty()) {
      resultadoInicioSesion.postValue(
        Recurso.error("No se pudo obtener el usuario autenticado")
      );
      return;
    }

    repositorioUsuario.getUsuario(uid)
      .addOnSuccessListener(documento -> {
        if (documento.exists()) {
          resultadoInicioSesion.postValue(
            Recurso.exito(DestinoNavegacion.PRINCIPAL)
          );
        } else {
          resultadoInicioSesion.postValue(
            Recurso.exito(DestinoNavegacion.REGISTRO)
          );
        }
      })
      .addOnFailureListener(error ->
        resultadoInicioSesion.postValue(
          Recurso.error("Error accediendo al usuario: " + error.getMessage())
        )
      );
  }
}
