package com.clank.app.ui.inspirar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentInspirarBinding;

public class InspirarFragment extends Fragment {

    private FragmentInspirarBinding binding;
    private InspirarViewModel viewModel;

    public InspirarFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInspirarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(InspirarViewModel.class);

        configurarVista();
        configurarListeners();
    }

    private void configurarVista() {
        configurarNavbar();

        binding.btnContinuar.getRoot().setText(getString(R.string.continuar));
    }

    private void configurarNavbar() {
        binding.navbar.tvNavbarTitulo.setText("");
        binding.navbar.btnNavbarAccion.setVisibility(View.GONE);
    }

    private void configurarListeners() {
        binding.navbar.btnNavbarVolver.setOnClickListener(vista ->
                navegarElegirIdioma()
        );

        binding.btnContinuar.getRoot().setOnClickListener(vista ->
                navegarBienvenida()
        );
    }

    private void navegarElegirIdioma() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inspirarFragment) {
            return;
        }

        navegador.navigate(R.id.action_inspirar_a_idioma);
    }

    private void navegarBienvenida() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inspirarFragment) {
            return;
        }

        navegador.navigate(R.id.action_inspirar_a_bienvenida);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}