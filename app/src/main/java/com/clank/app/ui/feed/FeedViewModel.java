package com.clank.app.ui.feed;

import androidx.lifecycle.ViewModel;
import com.clank.app.data.model.Clank;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import com.clank.app.data.repository.AuthRepository;

@HiltViewModel
public class FeedViewModel extends ViewModel {

  private final ClankRepository clankRepository;
  private final UsuarioRepository usuarioRepository;
  private final AuthRepository authRepository;

  @Inject
  public FeedViewModel(ClankRepository clankRepository, UsuarioRepository usuarioRepository,  AuthRepository authRepository) {
    this.clankRepository = clankRepository;
    this.usuarioRepository = usuarioRepository;
    this.authRepository = authRepository;
  }

  /////////////////////////getter/////////////////////////
  public UsuarioRepository getUsuarioRepository() {
    return usuarioRepository;
  }

  public ClankRepository getClankRepository() {
    return clankRepository;
  }

  public String getCurrentUserId() {
    return authRepository.getUid();
  }

  /////////////////////////query feed/////////////////////////
  public FirestoreRecyclerOptions<Clank> buildFeedOptions() {
    return new FirestoreRecyclerOptions.Builder<Clank>()
      .setQuery(clankRepository.getTodosAcabados(), Clank.class)
      .build();
  }
}
