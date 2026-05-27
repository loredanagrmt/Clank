package com.clank.app.ui.busqueda;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.data.model.Usuario;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class BusquedaViewModel extends ViewModel {

  private static final String TAG = "BusquedaViewModel";
  private static final int LIMITE_CLANKS = 200;

  private final ClankRepository clankRepository;
  private final UsuarioRepository usuarioRepository;

  private final MutableLiveData<List<Clank>> resultados =
          new MutableLiveData<>(new ArrayList<>());

  private final MutableLiveData<Boolean> cargando =
          new MutableLiveData<>(false);

  private final MutableLiveData<String> error =
          new MutableLiveData<>();

  private final Map<String, Usuario> cacheUsuarios = new HashMap<>();

  @Inject
  public BusquedaViewModel(ClankRepository clankRepository,
                           UsuarioRepository usuarioRepository) {
    this.clankRepository = clankRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public LiveData<List<Clank>> getResultados() {
    return resultados;
  }

  public LiveData<Boolean> getCargando() {
    return cargando;
  }

  public LiveData<String> getError() {
    return error;
  }

  public void buscar(String queryRaw) {
    String query = normalizarTexto(queryRaw);

    if (query.isEmpty()) {
      limpiar();
      return;
    }

    cargando.setValue(true);
    error.setValue(null);

    Task<QuerySnapshot> tareaUsuarios = usuarioRepository.getTodosUsuarios();

    Task<QuerySnapshot> tareaClanks = clankRepository
            .getTodosAcabados()
            .limit(LIMITE_CLANKS)
            .get();

    Tasks.whenAllComplete(tareaClanks, tareaUsuarios)
            .addOnSuccessListener(tareas -> {
              HashSet<String> uidUsuariosCoincidentes =
                      obtenerUsuariosCoincidentes(tareaUsuarios, query);

              List<Clank> clanksFiltrados =
                      obtenerClanksCoincidentes(tareaClanks, query, uidUsuariosCoincidentes);

              cargando.setValue(false);
              resultados.setValue(clanksFiltrados);
            })
            .addOnFailureListener(e -> {
              Log.e(TAG, "Error en búsqueda: " + e.getMessage(), e);
              cargando.setValue(false);
              error.setValue(e.getMessage());
            });
  }

  private HashSet<String> obtenerUsuariosCoincidentes(Task<QuerySnapshot> tareaUsuarios,
                                                      String query) {
    cacheUsuarios.clear();

    HashSet<String> uidUsuariosCoincidentes = new HashSet<>();
    String queryHandle = normalizarHandle(query);

    if (!tareaUsuarios.isSuccessful() || tareaUsuarios.getResult() == null) {
      Log.w(
              TAG,
              "Query usuarios falló: "
                      + (tareaUsuarios.getException() != null
                      ? tareaUsuarios.getException().getMessage()
                      : "null")
      );

      return uidUsuariosCoincidentes;
    }

    for (DocumentSnapshot doc : tareaUsuarios.getResult().getDocuments()) {
      String uidDocumento = doc.getId();
      String uidCampo = doc.getString("uid");
      String handle = normalizarHandle(doc.getString("usuarioClank"));
      String nombre = normalizarTexto(doc.getString("nombre"));

      boolean coincideHandle =
              !handle.isEmpty() && handle.contains(queryHandle);

      boolean coincideNombre =
              !nombre.isEmpty() && nombre.contains(query);

      if (coincideHandle || coincideNombre) {
        if (uidDocumento != null && !uidDocumento.trim().isEmpty()) {
          uidUsuariosCoincidentes.add(uidDocumento);
        }

        if (uidCampo != null && !uidCampo.trim().isEmpty()) {
          uidUsuariosCoincidentes.add(uidCampo);
        }
      }

      guardarUsuarioEnCacheSinRomperBusqueda(doc, uidDocumento, uidCampo);
    }

    return uidUsuariosCoincidentes;
  }

  private void guardarUsuarioEnCacheSinRomperBusqueda(DocumentSnapshot doc,
                                                      String uidDocumento,
                                                      String uidCampo) {
    try {
      Usuario usuario = doc.toObject(Usuario.class);

      if (usuario == null) {
        return;
      }

      if (uidDocumento != null && !uidDocumento.trim().isEmpty()) {
        cacheUsuarios.put(uidDocumento, usuario);
      }

      if (uidCampo != null && !uidCampo.trim().isEmpty()) {
        cacheUsuarios.put(uidCampo, usuario);
      }

    } catch (Exception e) {
      Log.w(
              TAG,
              "No se pudo mapear Usuario para cache, pero la búsqueda continúa: "
                      + e.getMessage()
      );
    }
  }

  private List<Clank> obtenerClanksCoincidentes(Task<QuerySnapshot> tareaClanks,
                                                String query,
                                                HashSet<String> uidUsuariosCoincidentes) {
    List<Clank> filtrados = new ArrayList<>();

    if (!tareaClanks.isSuccessful() || tareaClanks.getResult() == null) {
      Log.e(
              TAG,
              "Query clanks falló: "
                      + (tareaClanks.getException() != null
                      ? tareaClanks.getException().getMessage()
                      : "null")
      );

      return filtrados;
    }

    for (DocumentSnapshot doc : tareaClanks.getResult().getDocuments()) {
      try {
        Clank clank = doc.toObject(Clank.class);

        if (clank == null) {
          continue;
        }

        clank.setClankId(doc.getId());

        String titulo = normalizarTexto(clank.getTitulo());
        String descripcion = normalizarTexto(clank.getDescripcion());
        String usuarioId = clank.getUsuarioId();

        boolean coincideContenido =
                titulo.contains(query) || descripcion.contains(query);

        boolean coincideUsuario =
                usuarioId != null && uidUsuariosCoincidentes.contains(usuarioId);

        if (coincideContenido || coincideUsuario) {
          filtrados.add(clank);
        }

      } catch (Exception e) {
        Log.w(TAG, "Error procesando clank: " + e.getMessage());
      }
    }

    return filtrados;
  }

  public void limpiar() {
    cargando.setValue(false);
    resultados.setValue(new ArrayList<>());
    error.setValue(null);
  }

  public Usuario getUsuarioCacheado(String uid) {
    return cacheUsuarios.get(uid);
  }

  private String normalizarTexto(String texto) {
    return texto != null
            ? texto.trim().toLowerCase()
            : "";
  }

  private String normalizarHandle(String texto) {
    return normalizarTexto(texto)
            .replace("@", "");
  }
}