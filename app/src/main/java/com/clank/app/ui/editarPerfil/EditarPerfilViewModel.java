package com.clank.app.ui.editarPerfil;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.clank.app.data.model.Usuario;
import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.ImagenRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.util.Recurso;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditarPerfilViewModel extends AndroidViewModel {

  public static final String ERROR_USUARIO_CLANK_EXISTE = "ERROR_USUARIO_CLANK_EXISTE";
  public static final String ERROR_SUBIR_FOTO = "ERROR_SUBIR_FOTO";
  public static final String ERROR_GUARDAR_USUARIO = "ERROR_GUARDAR_USUARIO";
  public static final String ERROR_CARGAR_USUARIO = "ERROR_CARGAR_USUARIO";
  public static final String ERROR_GENERICO = "ERROR_GENERICO";
  private final AuthRepository repositorioAutenticacion;
  private final UsuarioRepository repositorioUsuario;
  private final ImagenRepository repositorioImagen;

  private final MutableLiveData<Usuario> perfil = new MutableLiveData<>();
  private final MutableLiveData<Recurso<Void>> estado = new MutableLiveData<>();

  @Inject
  public EditarPerfilViewModel(@NonNull Application application, AuthRepository repositorioAutenticacion, UsuarioRepository repositorioUsuario, ImagenRepository repositorioImagen) {
    super(application);
    this.repositorioAutenticacion = repositorioAutenticacion;
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioImagen = repositorioImagen;
  }

  public LiveData<Usuario> getPerfil() { return perfil; }
  public LiveData<Recurso<Void>> getEstado() { return estado; }


  /////////////////////////cargar usuario/////////////////////////
  public void cargarUsuario() {
    String uid = repositorioAutenticacion.getUid();
    if (uid == null || uid.isEmpty()) return;

    repositorioUsuario.getUsuario(uid)
      .addOnSuccessListener(doc -> {
        if (doc.exists()) {
          perfil.setValue(doc.toObject(Usuario.class));
        }
      })
      .addOnFailureListener(e ->
        estado.setValue(Recurso.error(ERROR_CARGAR_USUARIO)));
  }

  /////////////////////////guardar cambios/////////////////////////
  public void guardarCambios(String nombre, String usuarioClank, String telefono, Uri uriFotoNueva) {
    String uid = repositorioAutenticacion.getUid();
    if (uid == null || uid.isEmpty()) {
      estado.setValue(Recurso.error(ERROR_GENERICO));
      return;
    }

    estado.setValue(Recurso.cargando());

    String usuarioClankNormalizado = normalizarUsuarioClank(usuarioClank);

    //comprueba si el usuarioClank ha cambiado para evitar validación innecesaria
    Usuario actual = perfil.getValue();
    String usuarioClankActual = actual != null ? actual.getUsuarioClank() : "";

    if (usuarioClankNormalizado.equals(usuarioClankActual)) {
      continuarGuardado(uid, nombre, usuarioClankNormalizado, telefono, uriFotoNueva);
    } else {
      repositorioUsuario.getUsuariosPorUsuarioClank(usuarioClankNormalizado)
        .addOnSuccessListener(snapshot -> {
          if (!snapshot.isEmpty()) {
            estado.setValue(Recurso.error(ERROR_USUARIO_CLANK_EXISTE));
            return;
          }
          continuarGuardado(uid, nombre, usuarioClankNormalizado, telefono, uriFotoNueva);
        })
        .addOnFailureListener(e ->
          estado.setValue(Recurso.error(ERROR_GENERICO)));
    }
  }

  private void continuarGuardado(String uid, String nombre, String usuarioClank, String telefono, Uri uriFotoNueva) {
    if (uriFotoNueva != null) {
      repositorioImagen.guardarFotoPerfil(getApplication(), uriFotoNueva, uid)
        .addOnSuccessListener(uriDescarga ->
          persistirCambios(uid, nombre, usuarioClank, telefono,
            uriDescarga.toString()))
        .addOnFailureListener(e ->
          estado.setValue(Recurso.error(ERROR_SUBIR_FOTO)));
    } else {
      persistirCambios(uid, nombre, usuarioClank, telefono, null);
    }
  }

  private void persistirCambios(String uid, String nombre,String usuarioClank, String telefono, String nuevaUrlFoto) {
    Map<String, Object> campos = new HashMap<>();
    campos.put("nombre", nombre);
    campos.put("usuarioClank", usuarioClank);
    campos.put("telefono", telefono);

    if (nuevaUrlFoto != null) {
      campos.put("fotoPerfil", nuevaUrlFoto);
    }

    repositorioUsuario.actualizar(uid, campos)
      .addOnSuccessListener(v ->
        estado.setValue(Recurso.exito(null)))
      .addOnFailureListener(e ->
        estado.setValue(Recurso.error(ERROR_GUARDAR_USUARIO)));
  }

  /////////////////////////utilidades/////////////////////////
  private String normalizarUsuarioClank(String usuarioClank) {
    if (usuarioClank == null) return "";
    String normalizado = usuarioClank.trim().toLowerCase();
    if (normalizado.isEmpty()) return "";
    if (!normalizado.startsWith("@")) normalizado = "@" + normalizado;
    return normalizado;
  }
}
