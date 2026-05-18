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
  private final MutableLiveData<PerfilData> perfil = new MutableLiveData<>();
  private final MutableLiveData<Integer> numClanks = new MutableLiveData<>(0);
  private final MutableLiveData<Integer> numBocetos = new MutableLiveData<>(0);
  private boolean datosCargados = false;
  private String idUser;

  //listeners en tiempo real
  private ListenerRegistration listenerClanks;
  private ListenerRegistration listenerBocetos;

  @Inject
  public PerfilViewModel(UsuarioRepository usuarioRepository, ClankRepository clankRepository, AuthRepository authRepository) {
    this.usuarioRepository = usuarioRepository;
    this.clankRepository = clankRepository;
    this.authRepository = authRepository;
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

  /////////////////////////queries/////////////////////////
  public FirestoreRecyclerOptions<Clank> buildClankOptionsAcabados(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", true);
    return new FirestoreRecyclerOptions.Builder<Clank>()
            .setQuery(query, Clank.class)
            .build();
  }

  public FirestoreRecyclerOptions<Clank> buildClankOptionsBocetos(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser)
            .whereEqualTo("estadoAcabado", false);
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
    usuarioRepository.getUsuario(idUser).addOnSuccessListener(doc -> {
      if (!doc.exists()) return;
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

  @Override
  protected void onCleared() {
    super.onCleared();
    if (listenerClanks  != null) listenerClanks.remove();
    if (listenerBocetos != null) listenerBocetos.remove();
  }
}
