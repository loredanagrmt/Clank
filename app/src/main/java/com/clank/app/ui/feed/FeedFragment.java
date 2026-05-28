package com.clank.app.ui.feed;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clank.app.R;
import com.clank.app.adapters.FeedAdapter;
import com.clank.app.data.model.Clank;
import com.clank.app.databinding.FragmentFeedBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FeedFragment extends Fragment {

  private FragmentFeedBinding binding;
  private FeedViewModel viewModel;
  private FeedAdapter adapter;

  private final java.util.Set<String> observadosLikes = new java.util.HashSet<>();

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view,
                            @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

    configurarRecyclerView();
    cargarAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();

    ((NavbarHost) requireActivity()).mostrarNavbarConAccionYFiltro(
            getString(R.string.app_name),
            R.drawable.ic_buscar,
            v -> Navigation.findNavController(requireView())
                    .navigate(R.id.action_feed_a_busqueda),
            v -> Navigation.findNavController(requireView())
                    .navigate(R.id.action_feedFragment_to_filtrosFragment)
    );
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (adapter != null) {
      adapter.cerrar();
    }

    binding = null;
  }

  private void configurarRecyclerView() {
    binding.rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvFeed.setHasFixedSize(false);

    int spacing = (int) getResources().getDimension(R.dimen.feed_item_spacing);

    binding.rvFeed.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(@NonNull Rect outRect,
                                 @NonNull View view,
                                 @NonNull RecyclerView parent,
                                 @NonNull RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) > 0) {
          outRect.top = spacing;
        }
      }
    });
    binding.rvFeed.setVisibility(View.INVISIBLE);
    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
  }

  ///////////////////////// adapter /////////////////////////

  private void cargarAdapter() {
    FirestoreRecyclerOptions<Clank> options =
            new FirestoreRecyclerOptions.Builder<Clank>()
                    .setQuery(viewModel.getFeedQuery(), Clank.class)
                    .setLifecycleOwner(getViewLifecycleOwner())
                    .build();

    adapter = new FeedAdapter(
            options,
            requireContext(),
            viewModel.getUsuarioRepository(),
            viewModel.getCurrentUserId(),

            new FeedAdapter.OnClankClickListener() {
              @Override
              public void onClankClick(String clankId) {
                comprobarYEntrarDetalle(clankId);
              }

              @Override
              public void onUsuarioClick(String usuarioId) {
                Bundle args = new Bundle();
                args.putString("usuarioId", usuarioId);

                Navigation.findNavController(requireView())
                        .navigate(R.id.action_feed_a_perfil, args);
              }
            },

            clankId -> viewModel.toggleLike(clankId),

            (clankIds, numLikesIniciales) ->
                    arrancarObservadoresLikes(clankIds, numLikesIniciales),

            new FeedAdapter.OnPreparacionTarjetasListener() {
              @Override
              public void alIniciarPreparacion() {
                mostrarCargandoFeed();
              }

              @Override
              public void alFinalizarPreparacion() {
                ocultarCargandoFeed();
              }
            }
    );

    binding.rvFeed.setAdapter(adapter);
  }

  private void arrancarObservadoresLikes(List<String> clankIds,
                                         List<Integer> numLikesIniciales) {
    java.util.Set<String> idsActuales = new java.util.HashSet<>(clankIds);

    java.util.Iterator<String> iterador = observadosLikes.iterator();

    while (iterador.hasNext()) {
      String clankIdObservado = iterador.next();

      if (!idsActuales.contains(clankIdObservado)) {
        viewModel.detenerListenerLike(clankIdObservado);
        iterador.remove();
      }
    }

    for (int i = 0; i < clankIds.size(); i++) {
      String clankId = clankIds.get(i);
      int likesInicial = numLikesIniciales.get(i);

      viewModel.iniciarListenerLike(clankId, likesInicial);

      if (observadosLikes.contains(clankId)) {
        continue;
      }

      observadosLikes.add(clankId);
      observarEstadoLike(clankId);
      observarContadorLike(clankId);
    }
  }

  private void observarEstadoLike(String clankId) {
    viewModel.getEstadoLike(clankId).observe(getViewLifecycleOwner(), isLiked -> {
      if (isLiked == null || adapter == null) {
        return;
      }

      adapter.actualizarEstadoLike(clankId, isLiked);

      int pos = encontrarPosicion(clankId);

      if (pos >= 0) {
        adapter.notifyItemChanged(pos, FeedAdapter.PAYLOAD_LIKE);
      }
    });
  }

  private void observarContadorLike(String clankId) {
    viewModel.getContadorLikes(clankId).observe(getViewLifecycleOwner(), contador -> {
      if (contador == null || adapter == null) {
        return;
      }

      adapter.actualizarContadorLike(clankId, contador);

      int pos = encontrarPosicion(clankId);

      if (pos >= 0) {
        adapter.notifyItemChanged(pos, FeedAdapter.PAYLOAD_LIKE);
      }
    });
  }

  private int encontrarPosicion(String clankId) {
    if (adapter == null) {
      return -1;
    }

    for (int i = 0; i < adapter.getItemCount(); i++) {
      if (clankId.equals(adapter.getSnapshots().getSnapshot(i).getId())) {
        return i;
      }
    }

    return -1;
  }

  ///////////////////////// overlay cargando /////////////////////////

  private void mostrarCargandoFeed() {
    if (binding == null) {
      return;
    }

    binding.tvFeedVacio.setVisibility(View.GONE);
    binding.rvFeed.setVisibility(View.INVISIBLE);
    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
  }

  private void ocultarCargandoFeed() {
    if (binding == null) {
      return;
    }

    binding.overlayCargandoFeed.setVisibility(View.GONE);
    actualizarEstadoContenido();
  }

  private void actualizarEstadoContenido() {
    if (binding == null || adapter == null) {
      return;
    }

    if (!adapter.estaPreparado()) {
      binding.tvFeedVacio.setVisibility(View.GONE);
      binding.rvFeed.setVisibility(View.INVISIBLE);
      binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
      return;
    }

    boolean hayClanks = adapter.getCantidadRealFirestore() > 0;

    binding.overlayCargandoFeed.setVisibility(View.GONE);
    binding.rvFeed.setVisibility(hayClanks ? View.VISIBLE : View.GONE);
    binding.tvFeedVacio.setVisibility(hayClanks ? View.GONE : View.VISIBLE);
  }

  private void comprobarYEntrarDetalle(String clankId) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    viewModel.getClankRepository()
            .getPorIdServidor(clankId)
            .addOnSuccessListener(documento -> {
              if (binding == null) {
                return;
              }

              if (documento == null || !documento.exists()) {
                if (adapter != null) {
                  adapter.notifyDataSetChanged();
                }

                actualizarEstadoContenido();
                return;
              }

              Bundle args = new Bundle();
              args.putString("clankId", clankId);

              Navigation.findNavController(requireView())
                      .navigate(R.id.action_feed_a_detalle_clank, args);
            })
            .addOnFailureListener(error -> {
              if (binding == null) {
                return;
              }

              actualizarEstadoContenido();
            });
  }
}