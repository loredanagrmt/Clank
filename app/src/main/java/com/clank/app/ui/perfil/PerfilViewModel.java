package com.clank.app.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.clank.app.data.repository.LikeRepository;
import java.util.HashMap;
import java.util.Map;

@HiltViewModel
public class PerfilViewModel extends ViewModel {
  public static class PerfilData {
    public String nombre = "";
    public String correo = "";
    public String usuarioClank = "";
    public String fotoPerfil = "";
  }
  private final UsuarioRepository usuarioRepository;
  private final ClankRepository clankRepository;
  private final AuthRepository authRepository;
  private final LikeRepository likeRepository;
  private final MutableLiveData<PerfilData> perfil = new MutableLiveData<>();
  private final MutableLiveData<Integer> numClanks = new MutableLiveData<>(0);
  private final MutableLiveData<Integer> numBocetos = new MutableLiveData<>(0);
  private boolean datosCargados = false;
  private String idUser;
  private final Map<String, MutableLiveData<Boolean>> estadoLikes   = new HashMap<>();
  private final Map<String, MutableLiveData<Integer>>contadorLikes = new HashMap<>();
  private final Map<String, Boolean> likeEnProceso = new HashMap<>();
  private final Map<String, ListenerRegistration> listenersLikes = new HashMap<>();

  //listeners en tiempo real
  private ListenerRegistration listenerClanks;
  private ListenerRegistration listenerBocetos;
  private ListenerRegistration listenerPerfil;

  @Inject
  public PerfilViewModel(UsuarioRepository usuarioRepository, ClankRepository clankRepository, AuthRepository authRepository,
                         LikeRepository likeRepository) {
    this.usuarioRepository = usuarioRepository;
    this.clankRepository = clankRepository;
    this.authRepository = authRepository;
    this.likeRepository    = likeRepository;
  }

  /////////////////////////getters/////////////////////////
  public LiveData<PerfilData> getPerfil() {
    return perfil;
  }
  public LiveData<Integer> getNumClanks() {
    return numClanks;
  }
  public LiveData<Integer> getNumBocetos() {
    return numBocetos;
  }
  public UsuarioRepository getUsuarioRepository() {
    return usuarioRepository;
  }
  public String getCurrentUserId() {
    return authRepository.getUid();
  }
  public boolean esPerfilPropio(String idUser) {
    String uid = authRepository.getUid();
    return uid != null && uid.equals(idUser);
  }
  public boolean esPerfilPropio() {
    return idUser != null && esPerfilPropio(idUser);
  }

  public ClankRepository getClankRepository() {
    return clankRepository;
  }
  public LikeRepository getLikeRepository() {return likeRepository;}

  /////////////////////////queries/////////////////////////
  public FirestoreRecyclerOptions<Clank> buildClankOptionsAcabados(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", true)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING);

    return new FirestoreRecyclerOptions.Builder<Clank>()
            .setQuery(query, Clank.class)
            .build();
  }

  public FirestoreRecyclerOptions<Clank> buildClankOptionsBocetos(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", false)
            .orderBy("fechaPublicacion", Query.Direction.DESCENDING);

    return new FirestoreRecyclerOptions.Builder<Clank>()
            .setQuery(query, Clank.class)
            .build();
  }

  /// //////////////////////eliminar clank/////////////////////////
  public Task<Void> eliminarClank(String clankId) {
    return clankRepository.eliminarCompletoPorId(clankId);
  }

  /////////////////////////carga de datos/////////////////////////
  public void cargarDatos(String idUser) {
    if (datosCargados) return;
    datosCargados = true;
    this.idUser = idUser;
    cargarPerfil(idUser);
    cargarContadores(idUser);
  }
  private void cargarPerfil(String idUser) {
    listenerPerfil = usuarioRepository.escucharUsuario(idUser, (doc, e) -> {
      if (doc == null || !doc.exists()) return;
      PerfilData datos = new PerfilData();
      datos.nombre = obtenerCampo(doc, "nombre");
      datos.correo = obtenerCampo(doc, "correo");
      datos.fotoPerfil = obtenerCampo(doc, "fotoPerfil");
      datos.usuarioClank = obtenerCampo(doc, "usuarioClank");
      perfil.setValue(datos);
    });
  }
  //carga en tiempo real
  private void cargarContadores(String idUser) {
    listenerClanks = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", true)
            .addSnapshotListener((snap, e) -> {
              if (snap != null) numClanks.setValue(snap.size());
            });

    listenerBocetos = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", false)
            .addSnapshotListener((snap, e) -> {
              if (snap != null) numBocetos.setValue(snap.size());
            });
  }
  private String obtenerCampo(DocumentSnapshot doc, String campo) {
    if (!doc.contains(campo)) return "";
    String val = doc.getString(campo);
    return val != null ? val : "";
  }
  public void invalidarDatos() {
    datosCargados = false;
    if (listenerPerfil  != null) { listenerPerfil.remove();  listenerPerfil  = null; }
    if (listenerClanks  != null) { listenerClanks.remove();  listenerClanks  = null; }
    if (listenerBocetos != null) { listenerBocetos.remove(); listenerBocetos = null; }
  }
  public LiveData<Boolean> getEstadoLike(String clankId) {
    return obtenerOCrearEstado(clankId);
  }

  public LiveData<Integer> getContadorLikes(String clankId) {
    return obtenerOCrearContador(clankId);
  }

  public void iniciarListenerLike(String clankId, int numLikesInicial) {
    if (listenersLikes.containsKey(clankId)) return;

    MutableLiveData<Integer> liveContador = obtenerOCrearContador(clankId);
    if (liveContador.getValue() == null || liveContador.getValue() == 0) {
      liveContador.setValue(numLikesInicial);
    }

    String uid = authRepository.getUid();
    if (uid != null && !uid.isEmpty()) {
      likeRepository.hasDadoLike(clankId, uid).addOnSuccessListener(dioLike -> {
        MutableLiveData<Boolean> liveEstado = obtenerOCrearEstado(clankId);
        if (liveEstado.getValue() == null) {
          liveEstado.setValue(dioLike);
        }
      });
    }

    ListenerRegistration reg = likeRepository.escucharNumLikes(
      clankId,
      cantidad -> obtenerOCrearContador(clankId).postValue(cantidad)
    );
    listenersLikes.put(clankId, reg);
  }

  public void toggleLike(String clankId) {
    if (Boolean.TRUE.equals(likeEnProceso.get(clankId))) return;

    String uid = authRepository.getUid();
    if (uid == null || uid.isEmpty()) return;

    likeEnProceso.put(clankId, true);

    likeRepository.toggleLike(clankId, uid)
      .addOnSuccessListener(ahoraLikeado -> {
        obtenerOCrearEstado(clankId).setValue(ahoraLikeado);
        likeEnProceso.put(clankId, false);
      })
      .addOnFailureListener(e -> likeEnProceso.put(clankId, false));
  }

  private MutableLiveData<Boolean> obtenerOCrearEstado(String clankId) {
    if (!estadoLikes.containsKey(clankId)) {
      estadoLikes.put(clankId, new MutableLiveData<>(null));
    }
    return estadoLikes.get(clankId);
  }

  private MutableLiveData<Integer> obtenerOCrearContador(String clankId) {
    if (!contadorLikes.containsKey(clankId)) {
      contadorLikes.put(clankId, new MutableLiveData<>(0));
    }
    return contadorLikes.get(clankId);
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    if (listenerPerfil  != null) listenerPerfil.remove();
    if (listenerClanks  != null) listenerClanks.remove();
    if (listenerBocetos != null) listenerBocetos.remove();
    for (ListenerRegistration reg : listenersLikes.values()) {
      if (reg != null) reg.remove();
    }
    listenersLikes.clear();
  }
}
