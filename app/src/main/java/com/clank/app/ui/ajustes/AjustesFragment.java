package com.clank.app.ui.ajustes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.clank.app.util.GestorTema;
import com.clank.app.R;
import com.clank.app.databinding.FragmentAjustesBinding;

import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;
import com.clank.app.util.GestorIdioma;
import android.content.Intent;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup contenedor, @Nullable Bundle estadoGuardado) {
        binding = FragmentAjustesBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);
        vistaModelo = new ViewModelProvider(this).get(AjustesViewModel.class);

        binding.btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
        binding.btnLenguaje.setOnClickListener(v -> mostrarSelectorIdiomas());
        binding.switchTemaOscuro.setChecked(GestorTema.obtenerModoOscuroGuardado(requireContext()));

        binding.switchTemaOscuro.setOnCheckedChangeListener((boton, activado) -> GestorTema.cambiarModoOscuro(requireContext(), activado));
    }

    private void cerrarSesion() {
        vistaModelo.cerrarSesion();

        Navigation.findNavController(requireView()).navigate(R.id.action_ajustes_a_inicio_sesion);
    }

    private void mostrarSelectorIdiomas() {
        HojaOpciones hoja = HojaOpciones.nuevaLista(
                getString(R.string.elige_tu_idioma),
                obtenerIdiomasDisponibles(),
                codigoIdioma -> {
                    GestorIdioma.getInstance(requireContext()).guardarIdioma(codigoIdioma);
                    reiniciarAppTrasCambioIdioma();
                }
        );

        hoja.show(getChildFragmentManager(), "selector_idioma_ajustes");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}