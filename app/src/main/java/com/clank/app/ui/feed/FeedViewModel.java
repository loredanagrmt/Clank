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

    private final Map<String, ListenerRegistration> listenersEstadoLike = new HashMap<>();
    private final Map<String, ListenerRegistration> listenersContadorLike = new HashMap<>();

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

        String clankIdLimpio = clankId.trim();

        MutableLiveData<Integer> liveContador = obtenerOCrearContador(clankIdLimpio);

        if (liveContador.getValue() == null) {
            liveContador.setValue(Math.max(0, numLikesInicial));
        }

        String uid = authRepository.getUid();

        if (uid == null || uid.isEmpty()) {
            obtenerOCrearEstado(clankIdLimpio).setValue(false);
        } else if (!listenersEstadoLike.containsKey(clankIdLimpio)) {
            ListenerRegistration regEstado = likeRepository.escucharEstadoLike(
                    clankIdLimpio,
                    uid,
                    haDadoLike -> {
                        if (Boolean.TRUE.equals(likeEnProceso.get(clankIdLimpio))) {
                            return;
                        }

                        obtenerOCrearEstado(clankIdLimpio).postValue(haDadoLike);
                    }
            );

            listenersEstadoLike.put(clankIdLimpio, regEstado);
        }

        if (!listenersContadorLike.containsKey(clankIdLimpio)) {
            ListenerRegistration regContador = likeRepository.escucharNumLikes(
                    clankIdLimpio,
                    cantidad -> obtenerOCrearContador(clankIdLimpio)
                            .postValue(Math.max(0, cantidad))
            );

            listenersContadorLike.put(clankIdLimpio, regContador);
        }
    }

    public void detenerListenerLike(String clankId) {
        if (clankId == null || clankId.trim().isEmpty()) {
            return;
        }

        String clankIdLimpio = clankId.trim();

        ListenerRegistration regEstado = listenersEstadoLike.remove(clankIdLimpio);

        if (regEstado != null) {
            regEstado.remove();
        }

        ListenerRegistration regContador = listenersContadorLike.remove(clankIdLimpio);

        if (regContador != null) {
            regContador.remove();
        }

        likeEnProceso.remove(clankIdLimpio);
    }

    public void toggleLike(String clankId) {
        if (clankId == null || clankId.trim().isEmpty()) {
            return;
        }

        String clankIdLimpio = clankId.trim();

        if (Boolean.TRUE.equals(likeEnProceso.get(clankIdLimpio))) {
            return;
        }

        String uid = authRepository.getUid();

        if (uid == null || uid.isEmpty()) {
            return;
        }

        MutableLiveData<Boolean> liveEstado = obtenerOCrearEstado(clankIdLimpio);
        MutableLiveData<Integer> liveContador = obtenerOCrearContador(clankIdLimpio);

        Boolean estadoActual = liveEstado.getValue();

        if (estadoActual == null) {
            return;
        }

        boolean estadoAnterior = Boolean.TRUE.equals(estadoActual);

        int contadorAnterior = liveContador.getValue() != null
                ? Math.max(0, liveContador.getValue())
                : 0;

        boolean estadoOptimista = !estadoAnterior;

        int contadorOptimista = estadoOptimista
                ? contadorAnterior + 1
                : Math.max(0, contadorAnterior - 1);

        likeEnProceso.put(clankIdLimpio, true);

        liveEstado.setValue(estadoOptimista);
        liveContador.setValue(contadorOptimista);

        likeRepository.toggleLike(clankIdLimpio, uid)
                .addOnSuccessListener(ahoraLikeado -> {
                    liveEstado.setValue(Boolean.TRUE.equals(ahoraLikeado));
                    likeEnProceso.remove(clankIdLimpio);
                })
                .addOnFailureListener(error -> {
                    liveEstado.setValue(estadoAnterior);
                    liveContador.setValue(contadorAnterior);
                    likeEnProceso.remove(clankIdLimpio);
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

        for (ListenerRegistration registration : listenersEstadoLike.values()) {
            if (registration != null) {
                registration.remove();
            }
        }

        for (ListenerRegistration registration : listenersContadorLike.values()) {
            if (registration != null) {
                registration.remove();
            }
        }

        listenersEstadoLike.clear();
        listenersContadorLike.clear();
        likeEnProceso.clear();
    }
}