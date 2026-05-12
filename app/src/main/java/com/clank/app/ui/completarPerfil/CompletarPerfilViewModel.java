package com.clank.app.ui.completarPerfil;

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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.google.firebase.Timestamp;

@HiltViewModel
public class CompletarPerfilViewModel extends AndroidViewModel {
    public static final String ERROR_USUARIO_CLANK_EXISTE = "ERROR_USUARIO_CLANK_EXISTE";
    public static final String ERROR_CORREO_EXISTENTE = "ERROR_CORREO_EXISTENTE";
    public static final String ERROR_DATOS_REGISTRO = "ERROR_DATOS_REGISTRO";
    public static final String ERROR_REGISTRO = "ERROR_REGISTRO";
    public static final String ERROR_SUBIR_FOTO = "ERROR_SUBIR_FOTO";
    public static final String ERROR_GUARDAR_USUARIO = "ERROR_GUARDAR_USUARIO";
    public static final String ERROR_GENERICO = "ERROR_GENERICO";
    private final AuthRepository repositorioAutenticacion;
    private final UsuarioRepository repositorioUsuario;
    private final ImagenRepository repositorioImagen;
    private static final String TAG = "CompletarPerfilVM";
    private final MutableLiveData<Recurso<Void>> estado = new MutableLiveData<>();

    @Inject
    public CompletarPerfilViewModel(@NonNull Application application, AuthRepository repositorioAutenticacion, UsuarioRepository repositorioUsuario, ImagenRepository repositorioImagen) {
        super(application);
        this.repositorioAutenticacion = repositorioAutenticacion;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioImagen = repositorioImagen;
    }

    public LiveData<Recurso<Void>> getEstado() {
        return estado;
    }

    public void completarPerfil(String nombre,
                                String correo,
                                String telefono,
                                String fechaNacimiento,
                                String contrasenya,
                                String usuarioClank,
                                Uri uriFotoPerfil) {

        if (datosRegistroIncompletos(nombre, correo, telefono, fechaNacimiento, contrasenya)) {
            estado.setValue(Recurso.error(ERROR_DATOS_REGISTRO));
            return;
        }

        String usuarioClankNormalizado = normalizarUsuarioClank(usuarioClank);

        if (usuarioClankNormalizado.isEmpty()) {
            estado.setValue(Recurso.error(ERROR_DATOS_REGISTRO));
            return;
        }

        estado.setValue(Recurso.cargando());

        repositorioUsuario.getUsuariosPorUsuarioClank(usuarioClankNormalizado)
                .addOnSuccessListener(resultado -> {
                    if (!resultado.isEmpty()) {
                        estado.setValue(Recurso.error(ERROR_USUARIO_CLANK_EXISTE));
                        return;
                    }

                    crearCuenta(
                            nombre.trim(),
                            correo.trim(),
                            telefono.trim(),
                            fechaNacimiento.trim(),
                            contrasenya,
                            usuarioClankNormalizado,
                            uriFotoPerfil
                    );
                })
                .addOnFailureListener(error -> {
                    Log.e(
                            TAG,
                            "Error comprobando usuarioClank en Firestore: " + usuarioClankNormalizado,
                            error
                    );

                    estado.setValue(Recurso.error(ERROR_GENERICO));
                });
    }

    private void crearCuenta(String nombre, String correo, String telefono, String fechaNacimiento, String contrasenya, String usuarioClank, Uri uriFotoPerfil) {
        repositorioAutenticacion.registrar(correo, contrasenya).addOnCompleteListener(tareaAutenticacion -> {
            if (!tareaAutenticacion.isSuccessful()) {
                Exception error = tareaAutenticacion.getException();
                if (error instanceof FirebaseAuthUserCollisionException) {
                    estado.setValue(Recurso.error(ERROR_CORREO_EXISTENTE));
                } else {
                    estado.setValue(Recurso.error(ERROR_REGISTRO));
                }
                return;
            }
            String uid = repositorioAutenticacion.getUid();
            if (uid == null || uid.isEmpty()) {
                estado.setValue(Recurso.error(ERROR_REGISTRO));
                return;
            }
            if (uriFotoPerfil == null) {
                guardarUsuario(uid, correo, nombre, telefono, fechaNacimiento, usuarioClank, "");
                return;
            }
            subirFotoPerfil(uid, correo, nombre, telefono, fechaNacimiento, usuarioClank, uriFotoPerfil);
        });
    }

    private void subirFotoPerfil(String uid, String correo, String nombre, String telefono, String fechaNacimiento, String usuarioClank, Uri uriFotoPerfil) {
        repositorioImagen.guardarFotoPerfil(getApplication(), uriFotoPerfil, uid).addOnSuccessListener(uriDescarga -> guardarUsuario(uid, correo, nombre, telefono, fechaNacimiento, usuarioClank, uriDescarga.toString())).addOnFailureListener(error -> estado.setValue(Recurso.error(ERROR_SUBIR_FOTO)));
    }

    private void guardarUsuario(String uid, String correo, String nombre, String telefono, String fechaNacimiento, String usuarioClank, String fotoPerfil) {
        Usuario usuario = new Usuario();
        usuario.setUid(uid);
        usuario.setCorreo(correo);
        usuario.setNombre(nombre);
        usuario.setTelefono(telefono);
        usuario.setFechaNacimiento(fechaNacimiento);
        usuario.setUsuarioClank(usuarioClank);
        usuario.setFotoPerfil(fotoPerfil);
        usuario.setFechaCreacion(obtenerFechaActual());
        usuario.setUltimaConexion(Timestamp.now());
        usuario.setEnLinea(true);
        repositorioUsuario.crear(usuario).addOnCompleteListener(tareaBaseDatos -> {
            if (tareaBaseDatos.isSuccessful()) {
                estado.setValue(Recurso.exito(null));
            } else {
                estado.setValue(Recurso.error(ERROR_GUARDAR_USUARIO));
            }
        });
    }

    private boolean datosRegistroIncompletos(String nombre, String correo, String telefono, String fechaNacimiento, String contrasenya) {
        return estaVacio(nombre) || estaVacio(correo) || estaVacio(telefono) || estaVacio(fechaNacimiento) || estaVacio(contrasenya);
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    private String normalizarUsuarioClank(String usuarioClank) {
        if (usuarioClank == null) {
            return "";
        }
        String usuarioNormalizado = usuarioClank.trim().toLowerCase(Locale.ROOT);
        if (usuarioNormalizado.isEmpty()) {
            return "";
        }
        if (!usuarioNormalizado.startsWith("@")) {
            usuarioNormalizado = "@" + usuarioNormalizado;
        }
        return usuarioNormalizado;
    }

    private String obtenerFechaActual() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return formato.format(new Date());
    }
}
