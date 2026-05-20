package com.clank.app.data.repository;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.google.firebase.auth.UserInfo;

@Singleton
public class AuthRepository {

    private final FirebaseAuth autenticacion;
    private final FirebaseFunctions funciones;

    @Inject
    public AuthRepository() {
        this.autenticacion = FirebaseAuth.getInstance();
        this.funciones = FirebaseFunctions.getInstance("us-central1");
    }

    public Task<AuthResult> iniciarSesion(String correo, String contrasenya) {
        return autenticacion.signInWithEmailAndPassword(correo, contrasenya);
    }

    public void cerrarSesion() {
        autenticacion.signOut();
    }

    public Task<AuthResult> registrar(String correo, String contrasenya) {
        return autenticacion.createUserWithEmailAndPassword(correo, contrasenya);
    }

    public Task<AuthResult> iniciarSesionGoogle(GoogleSignInAccount cuenta) {
        AuthCredential credencial = GoogleAuthProvider.getCredential(cuenta.getIdToken(), null);
        return autenticacion.signInWithCredential(credencial);
    }

    public FirebaseUser getSesionUsuario() {
        return autenticacion.getCurrentUser();
    }

    public String getUid() {
        FirebaseUser usuarioActual = autenticacion.getCurrentUser();

        if (usuarioActual == null) {
            return null;
        }

        return usuarioActual.getUid();
    }

    public String getCorreo() {
        FirebaseUser usuarioActual = autenticacion.getCurrentUser();

        if (usuarioActual == null) {
            return null;
        }

        return usuarioActual.getEmail();
    }

    public boolean puedeCambiarContrasenya() {
        FirebaseUser usuarioActual = autenticacion.getCurrentUser();

        if (usuarioActual == null) {
            return false;
        }

        for (UserInfo proveedor : usuarioActual.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(proveedor.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    public Task<HttpsCallableResult> solicitarCodigoRecuperacion(String correo) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", correo);

        return funciones
                .getHttpsCallable("solicitarCodigoRecuperacion")
                .call(datos);
    }

    public Task<HttpsCallableResult> verificarCodigoRecuperacion(
            String correo,
            String codigo
    ) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", correo);
        datos.put("codigo", codigo);

        return funciones
                .getHttpsCallable("verificarCodigoRecuperacion")
                .call(datos);
    }

    public Task<HttpsCallableResult> actualizarContrasenyaRecuperacion(
            String correo,
            String tokenRecuperacion,
            String nuevaContrasenya
    ) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("correo", correo);
        datos.put("tokenRecuperacion", tokenRecuperacion);
        datos.put("nuevaContrasenya", nuevaContrasenya);

        return funciones
                .getHttpsCallable("actualizarContrasenyaRecuperacion")
                .call(datos);
    }

    public Task<HttpsCallableResult> eliminarCuentaCompleta() {
        Map<String, Object> datos = new HashMap<>();

        return funciones
                .getHttpsCallable("eliminarCuentaCompleta")
                .call(datos);
    }

    public Task<Void> comprobarContrasenyaActual(String contrasenyaActual) {
        FirebaseUser usuarioActual = autenticacion.getCurrentUser();
        String correo = getCorreo();

        if (usuarioActual == null || correo == null || correo.trim().isEmpty()) {
            return Tasks.forException(
                    new IllegalStateException("No se ha podido obtener el usuario o su correo.")
            );
        }

        AuthCredential credencial = EmailAuthProvider.getCredential(
                correo,
                contrasenyaActual
        );

        return usuarioActual.reauthenticate(credencial);
    }

    public Task<Void> actualizarContrasenya(String nuevaContrasenya) {
        FirebaseUser usuarioActual = autenticacion.getCurrentUser();

        if (usuarioActual == null) {
            return Tasks.forException(
                    new IllegalStateException("No hay ningún usuario autenticado.")
            );
        }

        return usuarioActual.updatePassword(nuevaContrasenya);
    }
}