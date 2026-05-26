package com.clank.app.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
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

  //estado de likes por clankId
  private final Map<String, MutableLiveData<Boolean>> estadoLikes   = new HashMap<>();
  private final Map<String, MutableLiveData<Integer>> contadorLikes = new HashMap<>();
  private final Map<String, Boolean>                  likeEnProceso = new HashMap<>();
  private final Map<String, ListenerRegistration>     listeners     = new HashMap<>();

  @Inject
  public FeedViewModel(ClankRepository clankRepository,
                       UsuarioRepository usuarioRepository,
                       AuthRepository authRepository,
                       LikeRepository likeRepository) {
    this.clankRepository  = clankRepository;
    this.usuarioRepository = usuarioRepository;
    this.authRepository   = authRepository;
    this.likeRepository   = likeRepository;
  }

  /////////////////////////getters/////////////////////////
  public UsuarioRepository getUsuarioRepository() { return usuarioRepository; }
  public ClankRepository   getClankRepository()   { return clankRepository; }
  public String            getCurrentUserId()     { return authRepository.getUid(); }

  /////////////////////////guery/////////////////////////
  public Query getFeedQuery() {
    return clankRepository.getTodosAcabados();
  }

  /////////////////////////likes/////////////////////////
  public LiveData<Boolean> getEstadoLike(String clankId) {
    return obtenerOCrearEstado(clankId);
  }

  public LiveData<Integer> getContadorLikes(String clankId) {
    return obtenerOCrearContador(clankId);
  }
  public void iniciarListenerLike(String clankId, int numLikesInicial) {
    if (listeners.containsKey(clankId)) return;

    //valor inicial de likes
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
    listeners.put(clankId, reg);
  }

  public void detenerListenerLike(String clankId) {
    ListenerRegistration reg = listeners.remove(clankId);
    if (reg != null) reg.remove();
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

  //////////////////////////limpieza/////////////////////////

  @Override
  protected void onCleared() {
    super.onCleared();
    for (ListenerRegistration reg : listeners.values()) {
      if (reg != null) reg.remove();
    }
    listeners.clear();
  }
}
