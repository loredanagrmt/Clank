package com.clank.app.ui.ajustes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentAjustesBinding;
import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;
import com.clank.app.ui.comun.NavbarHost;
import com.clank.app.util.GestorIdioma;
import com.clank.app.util.GestorTema;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AjustesFragment extends Fragment {

    private FragmentAjustesBinding binding;
    private AjustesViewModel vistaModelo;

    public AjustesFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup contenedor,
                             @Nullable Bundle estadoGuardado) {
        binding = FragmentAjustesBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        vistaModelo = new ViewModelProvider(this).get(AjustesViewModel.class);

        configurarListeners();
        configurarEstadoInicial();
        observarVistaModelo();
    }

    @Override
    public void onResume() {
        super.onResume();

        ((NavbarHost) requireActivity())
                .mostrarNavbarConVolver(getString(R.string.ajustes_titulo));
    }

    private void configurarListeners() {
        binding.btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
        binding.btnLenguaje.setOnClickListener(v -> mostrarSelectorIdiomas());
        binding.btnCambiarContrasenya.setOnClickListener(v -> navegarACambiarContrasenya());
        binding.btnBorrarCuenta.setOnClickListener(v -> mostrarConfirmacionBorrarCuenta());

        binding.switchTemaOscuro.setOnCheckedChangeListener(
                (boton, activado) ->
                        GestorTema.cambiarModoOscuro(requireContext(), activado)
        );
    }

    private void configurarEstadoInicial() {
        binding.switchTemaOscuro.setChecked(
                GestorTema.obtenerModoOscuroGuardado(requireContext())
        );
    }

    private void observarVistaModelo() {
        vistaModelo.getEliminandoCuenta().observe(getViewLifecycleOwner(), eliminando -> {
            boolean estaEliminando = Boolean.TRUE.equals(eliminando);

            binding.btnBorrarCuenta.setEnabled(!estaEliminando);
            binding.btnCerrarSesion.setEnabled(!estaEliminando);
            binding.btnCambiarContrasenya.setEnabled(!estaEliminando);
            binding.btnLenguaje.setEnabled(!estaEliminando);
            binding.switchTemaOscuro.setEnabled(!estaEliminando);
        });

        vistaModelo.getEstadoEliminacionCuenta().observe(getViewLifecycleOwner(), estado -> {
            if (estado == null) {
                return;
            }

            switch (estado) {
                case EXITO:
                    vistaModelo.limpiarEstadoEliminacionCuenta();
                    vistaModelo.cerrarSesion();
                    navegarABienvenida();
                    break;

                case ERROR_GENERAL:
                    vistaModelo.limpiarEstadoEliminacionCuenta();
                    break;
            }
        });
    }

    private void cerrarSesion() {
        vistaModelo.cerrarSesion();

        Navigation.findNavController(requireView())
                .navigate(R.id.action_ajustes_a_inicio_sesion);
    }

    private void mostrarSelectorIdiomas() {
        HojaOpciones hoja = HojaOpciones.nuevaLista(
                getString(R.string.elige_tu_idioma),
                obtenerIdiomasDisponibles(),
                codigoIdioma -> {
                    GestorIdioma.getInstance(requireContext()).aplicarIdioma(codigoIdioma);
                    reiniciarAppTrasCambioIdioma();
                }
        );

        hoja.show(getChildFragmentManager(), "selector_idioma_ajustes");
    }

    private void mostrarConfirmacionBorrarCuenta() {
        HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
                getString(R.string.ajustes_eliminar_cuenta_titulo),
                getString(R.string.ajustes_eliminar_cuenta_mensaje),
                getString(R.string.ajustes_eliminar_cuenta_cancelar),
                getString(R.string.ajustes_eliminar_cuenta_confirmar),
                () -> {
                },
                () -> vistaModelo.eliminarCuentaCompleta()
        );

        hoja.show(getChildFragmentManager(), "confirmacion_borrar_cuenta");
    }

    private void reiniciarAppTrasCambioIdioma() {
        Intent intent = requireActivity().getIntent();

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }

    private List<ItemOpcion> obtenerIdiomasDisponibles() {
        List<ItemOpcion> idiomas = new ArrayList<>();

        idiomas.add(new ItemOpcion("es", "Español"));
        idiomas.add(new ItemOpcion("en", "English"));
        idiomas.add(new ItemOpcion("fr", "Français"));
        idiomas.add(new ItemOpcion("de", "Deutsch"));
        idiomas.add(new ItemOpcion("pt", "Português"));
        idiomas.add(new ItemOpcion("it", "Italiano"));

        return idiomas;
    }

    private void navegarACambiarContrasenya() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_ajustes_a_cambiar_contrasenya);
    }

    private void navegarABienvenida() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null ||
                navegador.getCurrentDestination().getId() != R.id.ajustesFragment) {
            return;
        }

        NavOptions opcionesNavegacion = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        navegador.navigate(
                R.id.bienvenidaFragment,
                null,
                opcionesNavegacion
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}