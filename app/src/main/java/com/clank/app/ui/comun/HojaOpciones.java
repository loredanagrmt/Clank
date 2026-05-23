package com.clank.app.ui.comun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.clank.app.databinding.HojaOpcionesBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Collections;
import java.util.List;

public class HojaOpciones extends BottomSheetDialogFragment {

    public interface Callback {
        void alSeleccionar(String id);
    }

    public interface Accion {
        void ejecutar();
    }

    private enum ModoHoja {
        LISTA,
        CONFIRMACION
    }

    private HojaOpcionesBinding binding;

    private ModoHoja modoHoja;

    private String titulo;

    private List<ItemOpcion> opciones;
    private Callback callback;

    private String mensajeConfirmacion;
    private String textoCancelar;
    private String textoConfirmar;
    private Accion accionCancelar;
    private Accion accionConfirmar;

    public static HojaOpciones nuevaLista(
            String titulo,
            List<ItemOpcion> opciones,
            Callback callback
    ) {
        HojaOpciones hoja = new HojaOpciones();
        hoja.modoHoja = ModoHoja.LISTA;
        hoja.titulo = titulo;
        hoja.opciones = opciones;
        hoja.callback = callback;
        return hoja;
    }

    public static HojaOpciones nuevaConfirmacion(
            String titulo,
            String mensajeConfirmacion,
            String textoCancelar,
            String textoConfirmar,
            Accion accionCancelar,
            Accion accionConfirmar
    ) {
        HojaOpciones hoja = new HojaOpciones();
        hoja.modoHoja = ModoHoja.CONFIRMACION;
        hoja.titulo = titulo;
        hoja.mensajeConfirmacion = mensajeConfirmacion;
        hoja.textoCancelar = textoCancelar;
        hoja.textoConfirmar = textoConfirmar;
        hoja.accionCancelar = accionCancelar;
        hoja.accionConfirmar = accionConfirmar;
        return hoja;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = HojaOpcionesBinding.inflate(inflater, container, false);

        binding.tituloPanelOpciones.setText(
                titulo != null ? titulo : ""
        );

        if (modoHoja == ModoHoja.CONFIRMACION) {
            configurarModoConfirmacion();
        } else {
            configurarModoLista();
        }

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!(getDialog() instanceof BottomSheetDialog)) {
            return;
        }

        BottomSheetDialog dialogo = (BottomSheetDialog) getDialog();
        BottomSheetBehavior<FrameLayout> comportamiento = dialogo.getBehavior();

        comportamiento.setFitToContents(true);
        comportamiento.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void configurarModoLista() {
        binding.listaOpciones.setVisibility(View.VISIBLE);
        binding.textoConfirmacion.setVisibility(View.GONE);
        binding.contenedorBotonesConfirmacion.setVisibility(View.GONE);

        binding.listaOpciones.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        List<ItemOpcion> opcionesSeguras =
                opciones != null ? opciones : Collections.emptyList();

        binding.listaOpciones.setAdapter(
                new AdaptadorOpciones(opcionesSeguras, item -> {
                    dismissAllowingStateLoss();

                    if (callback != null) {
                        callback.alSeleccionar(item.id);
                    }
                })
        );
    }

    private void configurarModoConfirmacion() {
        binding.listaOpciones.setVisibility(View.GONE);
        binding.textoConfirmacion.setVisibility(View.VISIBLE);
        binding.contenedorBotonesConfirmacion.setVisibility(View.VISIBLE);

        binding.textoConfirmacion.setText(
                mensajeConfirmacion != null ? mensajeConfirmacion : ""
        );

        binding.includeBotonCancelar.btnSecundario.setText(
                textoCancelar != null ? textoCancelar : ""
        );

        binding.includeBotonConfirmar.btnPrincipal.setText(
                textoConfirmar != null ? textoConfirmar : ""
        );

        binding.includeBotonCancelar.btnSecundario.setOnClickListener(v -> {
            if (accionCancelar != null) {
                accionCancelar.ejecutar();
            }

            dismissAllowingStateLoss();
        });

        binding.includeBotonConfirmar.btnPrincipal.setOnClickListener(v -> {
            if (accionConfirmar != null) {
                accionConfirmar.ejecutar();
            }

            dismissAllowingStateLoss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}