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
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ResultadosFragment extends Fragment {

    private FragmentResultadosBinding binding;
    private ResultadosViewModel viewModel;
    private FeedAdapter adapter;
    private String categoria;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResultadosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavbarHost) requireActivity())
                .mostrarNavbarConVolver(categoria != null ? categoria : "");
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ResultadosViewModel.class);

        categoria = getArguments() != null
                ? getArguments().getString("categoria", "")
                : "";

        // RecyclerView igual que FeedFragment
        binding.rvResultados.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvResultados.setHasFixedSize(false);

        int spacing = (int) getResources().getDimension(R.dimen.feed_item_spacing);
        binding.rvResultados.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View v,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                if (parent.getChildAdapterPosition(v) > 0) outRect.top = spacing;
            }
        });

        FirestoreRecyclerOptions<Clank> options = new FirestoreRecyclerOptions.Builder<Clank>().setQuery(viewModel.getQueryPorCategoria(categoria), Clank.class)
                .setLifecycleOwner(getViewLifecycleOwner()).build();

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
              .navigate(R.id.action_resultadosFragment_to_detalleClankFragment, args);
          }
          @Override
          public void onUsuarioClick(String usuarioId) {
            Bundle args = new Bundle();
            args.putString("usuarioId", usuarioId);
            Navigation.findNavController(requireView())
              .navigate(R.id.action_resultadosFragment_to_perfilFragment, args);
          }
        },
        new FeedAdapter.OnPreparacionTarjetasListener() {
          @Override
          public void alIniciarPreparacion() {
            binding.overlayCargandoResultados.setVisibility(View.VISIBLE);
          }
          @Override
          public void alFinalizarPreparacion() {
            if (binding != null)
              binding.overlayCargandoResultados.setVisibility(View.GONE);
          }
        }
      );

        binding.rvResultados.setAdapter(adapter);
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
}
