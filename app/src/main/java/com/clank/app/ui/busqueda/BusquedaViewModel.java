package com.clank.app.ui.busqueda;

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
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class BusquedaViewModel extends ViewModel {
  private static final int LIMITE_CLANKS = 200;
  private final ClankRepository clankRepository;
  private final UsuarioRepository usuarioRepository;
  private final MutableLiveData<List<Clank>> resultados = new MutableLiveData<>();
  private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
  private final MutableLiveData<String> error = new MutableLiveData<>();
  private final Map<String, Usuario> cacheUsuarios = new HashMap<>();

  @Inject
  public BusquedaViewModel(ClankRepository clankRepository, UsuarioRepository usuarioRepository) {
    this.clankRepository = clankRepository;
    this.usuarioRepository = usuarioRepository;
  }

  public LiveData<List<Clank>> getResultados() { return resultados; }
  public LiveData<Boolean> getCargando(){ return cargando; }
  public LiveData<String> getError() { return error; }

  public void buscar(String queryRaw) {
    String query = queryRaw != null ? queryRaw.trim().toLowerCase() : "";
    if (query.isEmpty()) {
      resultados.setValue(new ArrayList<>());
      return;
    }
    cargando.setValue(true);
    error.setValue(null);

    //query de usuarios
    Task<QuerySnapshot> tareaUsuarios = usuarioRepository.getTodosUsuarios();

    //query de clanks
    Task<QuerySnapshot> tareaClanks = clankRepository.getTodosAcabados().limit(LIMITE_CLANKS).get();

    Tasks.whenAllComplete(tareaClanks, tareaUsuarios).addOnSuccessListener(tareas -> {

        //procesar usiarios
        cacheUsuarios.clear();
        List<String> uidUsuariosCoincidentes = new ArrayList<>();
        String queryHandle = query.startsWith("@") ? query.substring(1) : query;

        if (tareaUsuarios.isSuccessful() && tareaUsuarios.getResult() != null) {
          for (DocumentSnapshot doc : tareaUsuarios.getResult().getDocuments()) {
            try {
              String uid = doc.getId();
              String handle = doc.contains("usuarioClank")
                ? doc.getString("usuarioClank") : "";
              String nombre = doc.contains("nombre")
                ? doc.getString("nombre") : "";

              handle = handle != null ? handle.replace("@", "").trim().toLowerCase() : "";
              nombre = nombre != null ? nombre.toLowerCase() : "";

              // guardar en cache para el adapter
              Usuario u = doc.toObject(Usuario.class);
              if (u != null) cacheUsuarios.put(uid, u);

              if (handle.contains(queryHandle) || nombre.contains(query)) {
                uidUsuariosCoincidentes.add(uid);
              }
            } catch (Exception e) {}
          }
        }

        //procesar clanks
        List<Clank> filtrados = new ArrayList<>();

        if (tareaClanks.isSuccessful() && tareaClanks.getResult() != null) {
          for (DocumentSnapshot doc : tareaClanks.getResult().getDocuments()) {
            try {
              Clank clank = doc.toObject(Clank.class);
              if (clank == null) continue;
              clank.setClankId(doc.getId());

              String titulo = clank.getTitulo() != null
                ? clank.getTitulo().toLowerCase() : "";
              String descripcion = clank.getDescripcion() != null
                ? clank.getDescripcion().toLowerCase() : "";

              boolean coincideContenido = titulo.contains(query) || descripcion.contains(query);
              boolean coincideUsuario = clank.getUsuarioId() != null && uidUsuariosCoincidentes.contains(clank.getUsuarioId());

              if (coincideContenido || coincideUsuario) {
                filtrados.add(clank);
              }
            } catch (Exception e) {}
          }
        }

        cargando.setValue(false);
        resultados.setValue(filtrados);
      })
      .addOnFailureListener(e -> {
        cargando.setValue(false);
        error.setValue(e.getMessage());
      });
  }

  public void limpiar() {
    resultados.setValue(new ArrayList<>());
    error.setValue(null);
  }

  public Usuario getUsuarioCacheado(String uid) {
    return cacheUsuarios.get(uid);
  }
}
