package com.clank.app.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.LikeRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class FeedViewModel extends ViewModel {

  private final ClankRepository clankRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuthRepository authRepository;
  private final LikeRepository likeRepository;

  private final Map<String, MutableLiveData<Boolean>> estadoLikes = new HashMap<>();
  private final Map<String, MutableLiveData<Integer>> contadorLikes = new HashMap<>();
  private final Map<String, Boolean> likeEnProceso = new HashMap<>();
  private final Map<String, ListenerRegistration> listeners = new HashMap<>();

  @Inject
  public FeedViewModel(ClankRepository clankRepository,
                       UsuarioRepository usuarioRepository,
                       AuthRepository authRepository,
                       LikeRepository likeRepository) {
    this.clankRepository = clankRepository;
    this.usuarioRepository = usuarioRepository;
    this.authRepository = authRepository;
    this.likeRepository = likeRepository;
  }

  public UsuarioRepository getUsuarioRepository() {
    return usuarioRepository;
  }

  public ClankRepository getClankRepository() {
    return clankRepository;
  }

  public String getCurrentUserId() {
    return authRepository.getUid();
  }

  public Query getFeedQuery() {
    return clankRepository.getTodosAcabados();
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

    if (listeners.containsKey(clankId)) {
      return;
    }

    ListenerRegistration registration = likeRepository.escucharNumLikes(
            clankId,
            cantidad -> obtenerOCrearContador(clankId).postValue(Math.max(0, cantidad))
    );

    listeners.put(clankId, registration);
  }

  public void detenerListenerLike(String clankId) {
    ListenerRegistration registration = listeners.remove(clankId);

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

    for (ListenerRegistration registration : listeners.values()) {
      if (registration != null) {
        registration.remove();
      }
    }

    listeners.clear();
    likeEnProceso.clear();
  }
}