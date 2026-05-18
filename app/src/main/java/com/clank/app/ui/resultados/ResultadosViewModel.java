package com.clank.app.ui.resultados;

import androidx.lifecycle.ViewModel;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.google.firebase.firestore.Query;
import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.clank.app.data.repository.AuthRepository;


@HiltViewModel
public class ResultadosViewModel extends ViewModel {

    private final ClankRepository clankRepository;
    private final UsuarioRepository usuarioRepository;
  private final AuthRepository authRepository;

    @Inject
    public ResultadosViewModel(ClankRepository clankRepository,
                               UsuarioRepository usuarioRepository, AuthRepository authRepository) {
        this.clankRepository = clankRepository;
        this.usuarioRepository = usuarioRepository;
      this.authRepository = authRepository;
    }

    public Query getQueryPorCategoria(String categoria) {
        Query q = clankRepository.getPorCategoria(categoria);
        return q;
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
}

