package com.clank.app.ui.filtros;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.clank.app.R;
import com.clank.app.data.model.Categoria;
import com.clank.app.databinding.FragmentFiltrosBinding;
import com.clank.app.ui.comun.NavbarHost;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FiltrosFragment extends Fragment {

    private FragmentFiltrosBinding binding;
    private FiltrosViewModel viewModel;

    ///////////////////////// ciclo de vida /////////////////////////

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFiltrosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FiltrosViewModel.class);

        observarCategorias();
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarNavbar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    ///////////////////////// navbar /////////////////////////

    private void configurarNavbar() {
        ((NavbarHost) requireActivity())
                .mostrarNavbarConVolver(
                        getString(R.string.filtrar_titulo)
                );
    }

    ///////////////////////// categorías /////////////////////////

    private void observarCategorias() {
        viewModel.categorias.observe(getViewLifecycleOwner(), categorias -> {
            binding.chipGroupCategorias.removeAllViews();

            for (Categoria cat : categorias) {
                Button chip = (Button) LayoutInflater.from(requireContext())
                        .inflate(
                                R.layout.bt_secundario,
                                binding.chipGroupCategorias,
                                false
                        );

                chip.setText(cat.getCategoria());
                chip.setTag(cat.getCatId());

                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) chip.getLayoutParams();

                lp.setMargins(
                        0,
                        0,
                        (int) getResources().getDimension(R.dimen.chip_margin),
                        (int) getResources().getDimension(R.dimen.chip_margin)
                );

                chip.setLayoutParams(lp);

                chip.setOnClickListener(v ->
                        navegarAResultados(cat)
                );

                binding.chipGroupCategorias.addView(chip);
            }
        });
    }

    ///////////////////////// navegación /////////////////////////

    private void navegarAResultados(Categoria cat) {
        Bundle args = new Bundle();
        args.putString("categoria", cat.getCatId());
        args.putString("nombreCategoria", cat.getCategoria());

        NavHostFragment.findNavController(this)
                .navigate(
                        R.id.action_filtrosFragment_to_resultadosFragment,
                        args
                );
    }
}