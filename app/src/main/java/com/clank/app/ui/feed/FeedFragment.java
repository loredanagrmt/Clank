package com.clank.app.ui.feed;

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
import com.clank.app.databinding.FragmentFeedBinding;
import com.clank.app.ui.comun.NavbarHost;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.clank.app.data.model.Clank;

import dagger.hilt.android.AndroidEntryPoint;
import android.graphics.Rect;
import android.view.View;

@AndroidEntryPoint
public class FeedFragment extends Fragment {

  private FragmentFeedBinding binding;
  private FeedViewModel viewModel;
  private FeedAdapter adapter;

  /////////////////////////ciclo de vida/////////////////////////
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFeedBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

    configurarNavbar();
    configurarRecyclerView();
    cargarAdapter();
  }

  @Override
  public void onResume() {
    super.onResume();
    ((NavbarHost) requireActivity()).ocultarNavbar();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (adapter != null) adapter.startListening();
  }

  @Override
  public void onStop() {
    super.onStop();
    if (adapter != null) adapter.stopListening();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  /////////////////////////navbar/////////////////////////
  private void configurarNavbar() {
    binding.navbar.tvNavbarTitulo.setText(getString(R.string.app_name));
    binding.navbar.btnNavbarVolver.setVisibility(View.GONE);

    // botón buscar solo en feed
    binding.navbar.btnNavbarAccion.setVisibility(View.VISIBLE);
    binding.navbar.btnNavbarAccion.setImageResource(R.drawable.ic_buscar);
    binding.navbar.btnNavbarAccion.setOnClickListener(v -> Navigation.findNavController(requireView()).navigate(R.id.action_feed_a_busqueda));

    //botón filtrar solo en feed
    binding.navbar.btnNavbarFiltrar.setVisibility(View.VISIBLE);
    binding.navbar.btnNavbarFiltrar.setOnClickListener(v ->
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_feedFragment_to_filtrosFragment)
    );
  }

  /////////////////////////recyclerView/////////////////////////
  private void configurarRecyclerView() {
    binding.rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvFeed.setHasFixedSize(false);

    int spacing = (int) getResources().getDimension(R.dimen.feed_item_spacing);
    binding.rvFeed.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override
      public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos > 0) outRect.top = spacing;
      }
    });
  }

  /////////////////////////adapter/////////////////////////
  private void cargarAdapter() {
    FirestoreRecyclerOptions<Clank> options = viewModel.buildFeedOptions();

    adapter = new FeedAdapter(
            options,
            requireContext(),
            viewModel.getUsuarioRepository(),
            viewModel.getClankRepository(),
            viewModel.getCurrentUserId(),
      new FeedAdapter.OnClankClickListener() {
              @Override
              public void onClankClick(String clankId) {
              Bundle args = new Bundle();
              args.putString("clankId", clankId);
              Navigation.findNavController(requireView())
                      .navigate(R.id.action_feed_a_detalle_clank, args);
              }

              @Override
              public void onUsuarioClick(String usuarioId) {
                Bundle args = new Bundle();
                args.putString("usuarioId", usuarioId);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_feed_a_perfil, args);
              }
            },
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
  private void mostrarCargandoFeed() {
    if (binding == null) {
      return;
    }

    binding.overlayCargandoFeed.setVisibility(View.VISIBLE);
  }

  private void ocultarCargandoFeed() {
    if (binding == null) {
      return;
    }

    binding.overlayCargandoFeed.setVisibility(View.GONE);
  }
}
