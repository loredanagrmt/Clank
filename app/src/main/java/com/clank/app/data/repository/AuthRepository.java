package com.clank.app.data.repository;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AuthRepository {

    private final FirebaseAuth autenticacion;

    @Inject
    public AuthRepository() {
        this.autenticacion = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> iniciarSesion(String correo, String contrasenya) {
        return autenticacion.signInWithEmailAndPassword(correo, contrasenya);
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
}