package com.clank.app.ui.bienvenida;

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
import com.clank.app.databinding.FragmentBienvenidaBinding;
import com.clank.app.ui.auth.RegistroCompartidoViewModel;

public class BienvenidaFragment extends Fragment {

    private FragmentBienvenidaBinding binding;
    private RegistroCompartidoViewModel vistaModeloRegistro;

    public BienvenidaFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup contenedor,
                             Bundle estadoGuardado) {
        binding = FragmentBienvenidaBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        vistaModeloRegistro = new ViewModelProvider(requireActivity())
                .get(RegistroCompartidoViewModel.class);

        configurarVista();
        configurarListeners();
    }

    private void configurarVista() {
        configurarNavbar();

        binding.btnSoyNuevo.btnSecundario.setText(getString(R.string.soy_nuevo));
        binding.btnYaHeEstado.btnSecundario.setText(getString(R.string.ya_he_estado));
    }

    private void configurarNavbar() {
        binding.navbar.tvNavbarTitulo.setText("");
        binding.navbar.btnNavbarAccion.setVisibility(View.GONE);
    }

    private void configurarListeners() {
        binding.navbar.btnNavbarVolver.setOnClickListener(vista ->
                navegarInspirar()
        );

        binding.btnSoyNuevo.btnSecundario.setOnClickListener(vista ->
                navegarRegistro()
        );

        binding.btnYaHeEstado.btnSecundario.setOnClickListener(vista ->
                navegarInicioSesion()
        );
    }

    private void navegarInspirar() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.bienvenidaFragment) {
            return;
        }

        navegador.navigate(R.id.action_bienvenida_a_inspirar);
    }

    private void navegarRegistro() {
        vistaModeloRegistro.iniciarNuevoRegistro();

        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.bienvenidaFragment) {
            return;
        }

        navegador.navigate(R.id.action_bienvenida_a_registro);
    }

    private void navegarInicioSesion() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.bienvenidaFragment) {
            return;
        }

        navegador.navigate(R.id.action_bienvenida_a_inicio_sesion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}