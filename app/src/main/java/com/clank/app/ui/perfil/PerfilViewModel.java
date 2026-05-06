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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PerfilViewModel extends ViewModel {
  public static class PerfilData {
    public String nombre = "";
    public String correo = "";
    public String fotoPerfil = "";
  }
  private final UsuarioRepository usuarioRepository;
  private final ClankRepository clankRepository;
  private final AuthRepository authRepository;
  private final MutableLiveData<PerfilData> mPerfil = new MutableLiveData<>();
  private final MutableLiveData<Integer> mNumClanks = new MutableLiveData<>(0);
  private final MutableLiveData<Integer> mNumBocetos = new MutableLiveData<>(0);
  private ListenerRegistration mClanksListener;
  private boolean mDatosCargados = false;

  @Inject
  public PerfilViewModel(UsuarioRepository usuarioRepository, ClankRepository clankRepository, AuthRepository authRepository) {
    this.usuarioRepository = usuarioRepository;
    this.clankRepository = clankRepository;
    this.authRepository = authRepository;
  }

  /////////////////////////getters/////////////////////////
  public LiveData<PerfilData> getPerfil() {
    return mPerfil;
  }
  public LiveData<Integer> getNumClanks() {
    return mNumClanks;
  }
  public LiveData<Integer> getNumBocetos() {
    return mNumBocetos;
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
  /////////////////////////queries/////////////////////////
  public FirestoreRecyclerOptions<Clank> buildClankOptionsAcabados(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser).whereEqualTo("estadoAcabado", true);
    return new FirestoreRecyclerOptions.Builder<Clank>().setQuery(query, Clank.class).build();
  }

  public FirestoreRecyclerOptions<Clank> buildClankOptionsBocetos(String idUser) {
    Query query = clankRepository.getPorUsuario(idUser).whereEqualTo("estadoAcabado", false);
    return new FirestoreRecyclerOptions.Builder<Clank>().setQuery(query, Clank.class).build();
  }

  /////////////////////////carga de datos/////////////////////////
  public void cargarDatos(String idUser) {
    if (mDatosCargados) return;
    mDatosCargados = true;
    cargarPerfil(idUser);
    cargarContadores(idUser);
  }
  private void cargarPerfil(String idUser) {
    usuarioRepository.getUsuario(idUser).addOnSuccessListener(doc -> {
      if (!doc.exists()) return;
      PerfilData perfil = new PerfilData();
      perfil.nombre = obtenerCampo(doc, "nombre");
      perfil.correo = obtenerCampo(doc, "correo");
      perfil.fotoPerfil = obtenerCampo(doc, "fotoPerfil");
      mPerfil.setValue(perfil);
    });
  }
  private void cargarContadores(String idUser) {
    clankRepository.getPorUsuario(idUser).whereEqualTo("estadoAcabado", true).get()
      .addOnSuccessListener(snap -> mNumClanks.setValue(snap.size()));
    clankRepository.getPorUsuario(idUser).whereEqualTo("estadoAcabado", false).get()
      .addOnSuccessListener(snap -> mNumBocetos.setValue(snap.size()));
  }
  private String obtenerCampo(DocumentSnapshot doc, String campo) {
    if (!doc.contains(campo)) return "";
    String val = doc.getString(campo);
    return val != null ? val : "";
  }

  @Override
  protected void onCleared() {
    super.onCleared();
    if (mClanksListener != null) {
      mClanksListener.remove();
      mClanksListener = null;
    }
  }
}
