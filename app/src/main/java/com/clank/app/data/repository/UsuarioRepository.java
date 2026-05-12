package com.clank.app.data.repository;

import com.clank.app.data.model.Usuario;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class UsuarioRepository {

    private static final String COLECCION_USUARIOS = "usuarios";
    private static final String CAMPO_USUARIO_CLANK = "usuarioClank";

    private final FirebaseFirestore baseDatos;

    @Inject
    public UsuarioRepository() {
        this.baseDatos = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> getUsuario(String uid) {
        return baseDatos
                .collection(COLECCION_USUARIOS)
                .document(uid)
                .get();
    }

    public Task<QuerySnapshot> getUsuariosPorUsuarioClank(String usuarioClank) {
        return baseDatos
                .collection(COLECCION_USUARIOS)
                .whereEqualTo(CAMPO_USUARIO_CLANK, usuarioClank)
                .limit(1)
                .get();
    }

    public Task<Void> crear(Usuario usuario) {
        Map<String, Object> datosUsuario = new HashMap<>();

        datosUsuario.put("uid", usuario.getUid());
        datosUsuario.put("correo", usuario.getCorreo());
        datosUsuario.put("nombre", usuario.getNombre());
        datosUsuario.put("telefono", usuario.getTelefono());
        datosUsuario.put(CAMPO_USUARIO_CLANK, usuario.getUsuarioClank());
        datosUsuario.put("fotoPerfil", usuario.getFotoPerfil());
        datosUsuario.put("fechaCreacion", usuario.getFechaCreacion());
        datosUsuario.put("fechaNacimiento", usuario.getFechaNacimiento());
        datosUsuario.put("ultimaConexion", usuario.getUltimaConexion());
        datosUsuario.put("enLinea", usuario.isEnLinea());

        return baseDatos
                .collection(COLECCION_USUARIOS)
                .document(usuario.getUid())
                .set(datosUsuario);
    }

  public Task<Void> actualizar(String uid, Map<String, Object> campos) {
    return baseDatos
      .collection(COLECCION_USUARIOS)
      .document(uid)
      .update(campos);
  }
}
