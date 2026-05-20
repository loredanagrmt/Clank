package com.clank.app.ui.resultados;

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
import com.clank.app.databinding.FragmentResultadosBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResultadosFragment extends Fragment {

  private FragmentResultadosBinding binding;
  private ResultadosViewModel viewModel;
  private FeedAdapter adapter;

  private String categoria;
  private String nombreCategoria;

  private final java.util.Set<String> observadosLikes =
          new java.util.HashSet<>();

  ///////////////////////// ciclo de vida /////////////////////////

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    categoria = getArguments() != null
            ? getArguments().getString("categoria", "")
            : "";

    nombreCategoria = getArguments() != null
            ? getArguments().getString("nombreCategoria", "")
            : "";
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentResultadosBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view,
                            Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(ResultadosViewModel.class);

    configurarRecyclerView();
    cargarAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();
    configurarNavbar();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (adapter != null) {
      adapter.cerrar();
    }

    binding = null;
  }

  ///////////////////////// navbar /////////////////////////

  private void configurarNavbar() {
    ((NavbarHost) requireActivity())
            .mostrarNavbarConVolver(
                    nombreCategoria != null
                            ? nombreCategoria
                            : ""
            );
  }

  ///////////////////////// recyclerView /////////////////////////

  private void configurarRecyclerView() {
    binding.rvResultados.setLayoutManager(
            new LinearLayoutManager(requireContext())
    );

    binding.rvResultados.setHasFixedSize(false);

    int spacing = (int) getResources().getDimension(
            R.dimen.feed_item_spacing
    );

    binding.rvResultados.addItemDecoration(new RecyclerView.ItemDecoration() {
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
  }

  ///////////////////////// adapter /////////////////////////

  private void cargarAdapter() {
    FirestoreRecyclerOptions<Clank> options =
            new FirestoreRecyclerOptions.Builder<Clank>()
                    .setQuery(
                            viewModel.getQueryPorCategoria(categoria),
                            Clank.class
                    )
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
                Bundle args = new Bundle();
                args.putString("clankId", clankId);

                Navigation.findNavController(requireView())
                        .navigate(
                                R.id.action_resultadosFragment_to_detalleClankFragment,
                                args
                        );
              }

              @Override
              public void onUsuarioClick(String usuarioId) {
                Bundle args = new Bundle();
                args.putString("usuarioId", usuarioId);

                Navigation.findNavController(requireView())
                        .navigate(
                                R.id.action_resultadosFragment_to_perfilFragment,
                                args
                        );
              }
            },

            clankId -> viewModel.toggleLike(clankId),

            (clankIds, numLikesIniciales) ->
                    arrancarObservadoresLikes(
                            clankIds,
                            numLikesIniciales
                    ),

            new FeedAdapter.OnPreparacionTarjetasListener() {
              @Override
              public void alIniciarPreparacion() {
                if (binding != null) {
                  binding.overlayCargandoResultados.setVisibility(View.VISIBLE);
                }
              }

              @Override
              public void alFinalizarPreparacion() {
                if (binding != null) {
                  binding.overlayCargandoResultados.setVisibility(View.GONE);
                }
              }
            }
    );

    binding.rvResultados.setAdapter(adapter);
  }

  private void arrancarObservadoresLikes(List<String> clankIds,
                                         List<Integer> numLikesIniciales) {
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
    viewModel.getEstadoLike(clankId)
            .observe(getViewLifecycleOwner(), isLiked -> {
              if (isLiked == null || adapter == null) {
                return;
              }

              Integer contador =
                      viewModel.getContadorLikes(clankId).getValue();

              adapter.actualizarLike(
                      clankId,
                      isLiked,
                      contador != null ? contador : 0
              );

              int pos = encontrarPosicion(clankId);

              if (pos >= 0) {
                adapter.notifyItemChanged(
                        pos,
                        FeedAdapter.PAYLOAD_LIKE
                );
              }
            });
  }

  private void observarContadorLike(String clankId) {
    viewModel.getContadorLikes(clankId)
            .observe(getViewLifecycleOwner(), contador -> {
              if (contador == null || adapter == null) {
                return;
              }

              Boolean isLiked =
                      viewModel.getEstadoLike(clankId).getValue();

              adapter.actualizarLike(
                      clankId,
                      Boolean.TRUE.equals(isLiked),
                      contador
              );

              int pos = encontrarPosicion(clankId);

              if (pos >= 0) {
                adapter.notifyItemChanged(
                        pos,
                        FeedAdapter.PAYLOAD_LIKE
                );
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
}