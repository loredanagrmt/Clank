package com.clank.app.ui.comun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.clank.app.databinding.HojaOpcionesBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class HojaOpciones extends BottomSheetDialogFragment {

    public interface Callback {
        void alSeleccionar(String id);
    }

    private HojaOpcionesBinding binding;
    private String titulo;
    private List<ItemOpcion> opciones;
    private Callback callback;

    public static HojaOpciones nuevaLista(String titulo, List<ItemOpcion> opciones, Callback callback) {
        HojaOpciones hoja = new HojaOpciones();
        hoja.titulo = titulo;
        hoja.opciones = opciones;
        hoja.callback = callback;
        return hoja;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = HojaOpcionesBinding.inflate(inflater, container, false);

        binding.tituloPanelOpciones.setText(titulo);

        binding.listaOpciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listaOpciones.setAdapter(new AdaptadorOpciones(opciones, item -> {
            if (callback != null) callback.alSeleccionar(item.id);
            dismiss();
        }));

        binding.listaOpciones.setVisibility(View.VISIBLE);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
