package com.clank.app.ui.perfil;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.LikeRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

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

  private final Map<String, MutableLiveData<Boolean>> estadoLikes = new HashMap<>();
  private final Map<String, MutableLiveData<Integer>> contadorLikes = new HashMap<>();
  private final Map<String, Boolean> likeEnProceso = new HashMap<>();
  private final Map<String, ListenerRegistration> listenersLikes = new HashMap<>();

  private ListenerRegistration listenerClanks;
  private ListenerRegistration listenerBocetos;
  private ListenerRegistration listenerPerfil;

  @Inject
  public PerfilViewModel(UsuarioRepository usuarioRepository,
                         ClankRepository clankRepository,
                         AuthRepository authRepository,
                         LikeRepository likeRepository) {
    this.usuarioRepository = usuarioRepository;
    this.clankRepository = clankRepository;
    this.authRepository = authRepository;
    this.likeRepository = likeRepository;
  }

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

  public LikeRepository getLikeRepository() {
    return likeRepository;
  }

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

  public Task<Void> eliminarClank(String clankId) {
    return clankRepository.eliminarCompletoPorId(clankId);
  }

  public void cargarDatos(String idUser) {
    if (datosCargados) {
      return;
    }

    datosCargados = true;
    this.idUser = idUser;

    cargarPerfil(idUser);
    cargarContadores(idUser);
  }

  private void cargarPerfil(String idUser) {
    listenerPerfil = usuarioRepository.escucharUsuario(idUser, (doc, error) -> {
      if (doc == null || !doc.exists()) {
        return;
      }

      PerfilData datos = new PerfilData();
      datos.nombre = obtenerCampo(doc, "nombre");
      datos.correo = obtenerCampo(doc, "correo");
      datos.fotoPerfil = obtenerCampo(doc, "fotoPerfil");
      datos.usuarioClank = obtenerCampo(doc, "usuarioClank");

      perfil.setValue(datos);
    });
  }

  private void cargarContadores(String idUser) {
    listenerClanks = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", true)
            .addSnapshotListener((snap, error) -> {
              if (snap != null) {
                numClanks.setValue(snap.size());
              }
            });

    listenerBocetos = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", false)
            .addSnapshotListener((snap, error) -> {
              if (snap != null) {
                numBocetos.setValue(snap.size());
              }
            });
  }

  private String obtenerCampo(DocumentSnapshot doc, String campo) {
    if (!doc.contains(campo)) {
      return "";
    }

    String valor = doc.getString(campo);
    return valor != null ? valor : "";
  }

  public void invalidarDatos() {
    datosCargados = false;

    if (listenerPerfil != null) {
      listenerPerfil.remove();
      listenerPerfil = null;
    }

    if (listenerClanks != null) {
      listenerClanks.remove();
      listenerClanks = null;
    }

    if (listenerBocetos != null) {
      listenerBocetos.remove();
      listenerBocetos = null;
    }
  }

  public LiveData<Boolean> getEstadoLike(String clankId) {
    return obtenerOCrearEstado(clankId);
  }

  public LiveData<Integer> getContadorLikes(String clankId) {
    return obtenerOCrearContador(clankId);
  }

  public void iniciarListenerLike(String clankId, int numLikesInicial) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    obtenerOCrearEstado(clankId);
    obtenerOCrearContador(clankId);

    String uid = authRepository.getUid();

    if (uid != null && !uid.isEmpty()) {
      likeRepository.hasDadoLike(clankId, uid)
              .addOnSuccessListener(dioLike -> {
                if (Boolean.TRUE.equals(likeEnProceso.get(clankId))) {
                  return;
                }

                obtenerOCrearEstado(clankId).setValue(Boolean.TRUE.equals(dioLike));
              })
              .addOnFailureListener(error -> {
                if (Boolean.TRUE.equals(likeEnProceso.get(clankId))) {
                  return;
                }

                obtenerOCrearEstado(clankId).setValue(false);
              });
    } else {
      obtenerOCrearEstado(clankId).setValue(false);
    }

    if (listenersLikes.containsKey(clankId)) {
      return;
    }

    ListenerRegistration registration = likeRepository.escucharNumLikes(
            clankId,
            cantidad -> obtenerOCrearContador(clankId).postValue(Math.max(0, cantidad))
    );

    listenersLikes.put(clankId, registration);
  }

  public void detenerListenerLike(String clankId) {
    ListenerRegistration registration = listenersLikes.remove(clankId);

    if (registration != null) {
      registration.remove();
    }
  }

  public void toggleLike(String clankId) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    if (Boolean.TRUE.equals(likeEnProceso.get(clankId))) {
      return;
    }

    String uid = authRepository.getUid();

    if (uid == null || uid.isEmpty()) {
      return;
    }

    MutableLiveData<Boolean> liveEstado = obtenerOCrearEstado(clankId);
    MutableLiveData<Integer> liveContador = obtenerOCrearContador(clankId);

    Boolean estadoActual = liveEstado.getValue();
    Integer contadorActual = liveContador.getValue();

    if (estadoActual == null || contadorActual == null) {
      return;
    }

    likeEnProceso.put(clankId, true);

    boolean estadoAnterior = Boolean.TRUE.equals(estadoActual);
    int contadorAnterior = Math.max(0, contadorActual);

    boolean estadoOptimista = !estadoAnterior;

    int contadorOptimista = estadoOptimista
            ? contadorAnterior + 1
            : Math.max(0, contadorAnterior - 1);

    liveEstado.setValue(estadoOptimista);
    liveContador.setValue(contadorOptimista);

    likeRepository.toggleLike(clankId, uid)
            .addOnSuccessListener(ahoraLikeado -> {
              liveEstado.setValue(Boolean.TRUE.equals(ahoraLikeado));
              likeEnProceso.remove(clankId);
            })
            .addOnFailureListener(error -> {
              liveEstado.setValue(estadoAnterior);
              liveContador.setValue(contadorAnterior);
              likeEnProceso.remove(clankId);
            });
  }

  private MutableLiveData<Boolean> obtenerOCrearEstado(String clankId) {
    if (!estadoLikes.containsKey(clankId)) {
      estadoLikes.put(clankId, new MutableLiveData<>(null));
    }

    return estadoLikes.get(clankId);
  }

  private MutableLiveData<Integer> obtenerOCrearContador(String clankId) {
    if (!contadorLikes.containsKey(clankId)) {
      contadorLikes.put(clankId, new MutableLiveData<>(null));
    }

    return contadorLikes.get(clankId);
  }

  @Override
  protected void onCleared() {
    super.onCleared();

    if (listenerPerfil != null) {
      listenerPerfil.remove();
    }

    if (listenerClanks != null) {
      listenerClanks.remove();
    }

    if (listenerBocetos != null) {
      listenerBocetos.remove();
    }

    for (ListenerRegistration registration : listenersLikes.values()) {
      if (registration != null) {
        registration.remove();
      }
    }

    listenersLikes.clear();
    likeEnProceso.clear();
  }
}