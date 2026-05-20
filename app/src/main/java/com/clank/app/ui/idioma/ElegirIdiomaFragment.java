package com.clank.app.ui.idioma;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;
import com.clank.app.ui.comun.NavbarHost;
import com.clank.app.util.GestorIdioma;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ElegirIdiomaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_elegir_idioma, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mostrarSelectorIdiomas(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((NavbarHost) requireActivity()).ocultarNavbar();
    }

    private void mostrarSelectorIdiomas(View view) {
        HojaOpciones hoja = HojaOpciones.nuevaLista(
                obtenerTextoSiempreEnEspanol(R.string.elige_tu_idioma),
                obtenerIdiomasDisponibles(),
                codigoIdioma -> {
                    GestorIdioma.getInstance(requireContext())
                            .aplicarIdioma(codigoIdioma);

                    NavController navController =
                            Navigation.findNavController(view);

                    navController.navigate(R.id.action_idioma_a_inicio);
                }
        );

        hoja.show(getChildFragmentManager(), "selector_idioma");
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

    private String obtenerTextoSiempreEnEspanol(int idTexto) {
        Configuration configuracion = new Configuration(
                requireContext().getResources().getConfiguration()
        );

        configuracion.setLocale(Locale.forLanguageTag("es"));

        Context contextoEspanol =
                requireContext().createConfigurationContext(configuracion);

        return contextoEspanol.getString(idTexto);
    }
}