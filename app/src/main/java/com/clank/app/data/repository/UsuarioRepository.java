package com.clank.app.data.repository;

import com.clank.app.data.model.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class UsuarioRepository {

    private static final String COLECCION_USUARIOS = "usuarios";

    private final FirebaseFirestore baseDatos;

    @Inject
    public UsuarioRepository() {
        this.baseDatos = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> obtenerUsuario(String uid) {
        return baseDatos
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .get();
    }

    public Task<Void> crear(Usuario usuario) {
        Map<String, Object> datosUsuario = new HashMap<>();

        datosUsuario.put("uid", usuario.obtenerUid());
        datosUsuario.put("correo", usuario.obtenerCorreo());
        datosUsuario.put("nombre", usuario.obtenerNombre());
        datosUsuario.put("telefono", usuario.obtenerTelefono());
        datosUsuario.put("fotoPerfil", usuario.obtenerFotoPerfil());
        datosUsuario.put("fechaCreacion", usuario.obtenerFechaCreacion());
        datosUsuario.put("fechaNacimiento", usuario.obtenerFechaNacimiento());
        datosUsuario.put("ultimaConexion", usuario.obtenerUltimaConexion());
        datosUsuario.put("enLinea", usuario.estaEnLinea());

        return baseDatos
                .collection(COLECCION_USUARIOS)
                .document(usuario.obtenerUid())
                .set(datosUsuario);
    }
}