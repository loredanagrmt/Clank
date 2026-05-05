package com.clank.app.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentRegistroBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegistroFragment extends Fragment {

    private static final long DURACION_POPUP_MILISEGUNDOS = 2300;

    private FragmentRegistroBinding binding;

    private final Handler temporizador = new Handler(Looper.getMainLooper());
    private Runnable accionDespuesPopup;

    private boolean contrasenyaVisible = false;
    private boolean confirmarContrasenyaVisible = false;

    public RegistroFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup contenedor,
                             Bundle estadoGuardado) {
        binding = FragmentRegistroBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        configurarVista();
        configurarListeners();
    }

    private void configurarVista() {
        configurarNavbar();
        configurarFormulario();
        configurarPopup();
    }

    private void configurarNavbar() {
        binding.navbar.tvNavbarTitulo.setText(getString(R.string.registro_titulo));
        binding.navbar.btnNavbarAccion.setVisibility(View.GONE);
    }

    private void configurarFormulario() {
        binding.inputNombreCompleto.tvInputTitulo.setText(getString(R.string.nombre_completo));
        binding.inputNombreCompleto.customEditText.setHint(getString(R.string.nombre_apellidos));
        binding.inputNombreCompleto.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        );
        binding.inputNombreCompleto.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputCorreo.tvInputTitulo.setText(getString(R.string.correo_electronico));
        binding.inputCorreo.customEditText.setHint(getString(R.string.ejemplo_correo));
        binding.inputCorreo.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        binding.inputCorreo.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputTelefono.tvInputTitulo.setText(getString(R.string.numero_telefono));
        binding.inputTelefono.customEditText.setHint(getString(R.string.ejemplo_telefono));
        binding.inputTelefono.customEditText.setInputType(InputType.TYPE_CLASS_PHONE);
        binding.inputTelefono.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputFechaNacimiento.tvInputTitulo.setText(getString(R.string.fecha_nacimiento));
        binding.inputFechaNacimiento.customEditText.setHint(getString(R.string.ejemplo_fecha_nacimiento));
        binding.inputFechaNacimiento.customEditText.setInputType(
                InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE
        );
        binding.inputFechaNacimiento.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputContrasenya.tvInputTitulo.setText(getString(R.string.contrasenya));
        binding.inputContrasenya.customEditText.setHint("••••••••");
        binding.inputContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        binding.inputContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);

        binding.inputConfirmarContrasenya.tvInputTitulo.setText(getString(R.string.confirmar_contrasenya));
        binding.inputConfirmarContrasenya.customEditText.setHint("••••••••");
        binding.inputConfirmarContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        binding.inputConfirmarContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputConfirmarContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);

        binding.btnRegistrarme.btnSecundario.setText(getString(R.string.registrarme));
    }

    private void configurarPopup() {
        binding.capaPopup.setVisibility(View.GONE);

        binding.popup.tvTituloPopup.setText(getString(R.string.popup_registro_perfil_titulo));
        binding.popup.tvMensajePopup.setText(getString(R.string.popup_registro_perfil_mensaje));
        binding.popup.imgIconoPopup.setImageResource(R.drawable.ic_usuario_inactivo);

        binding.popup.contenedorBotonPopup.setVisibility(View.GONE);
    }

    private void configurarListeners() {
        binding.navbar.btnNavbarVolver.setOnClickListener(vista ->
                volverPantallaAnterior()
        );

        binding.tvIrInicioSesion.setOnClickListener(vista ->
                navegarInicioSesion()
        );

        binding.inputContrasenya.inputTrailingIcon.setOnClickListener(vista ->
                alternarVisibilidadContrasenya()
        );

        binding.inputConfirmarContrasenya.inputTrailingIcon.setOnClickListener(vista ->
                alternarVisibilidadConfirmarContrasenya()
        );

        binding.btnRegistrarme.btnSecundario.setOnClickListener(vista ->
                mostrarPopupRegistro()
        );
    }

    private void mostrarPopupRegistro() {
        binding.capaPopup.setVisibility(View.VISIBLE);
        bloquearFormulario(true);

        if (accionDespuesPopup != null) {
            temporizador.removeCallbacks(accionDespuesPopup);
        }

        accionDespuesPopup = () -> {
            if (binding == null) {
                return;
            }

            binding.capaPopup.setVisibility(View.GONE);
            bloquearFormulario(false);
            navegarPantallaSiguiente();
        };

        temporizador.postDelayed(accionDespuesPopup, DURACION_POPUP_MILISEGUNDOS);
    }

    private void bloquearFormulario(boolean bloqueado) {
        boolean habilitado = !bloqueado;

        binding.navbar.btnNavbarVolver.setEnabled(habilitado);
        binding.btnRegistrarme.btnSecundario.setEnabled(habilitado);
        binding.tvIrInicioSesion.setEnabled(habilitado);

        binding.inputNombreCompleto.customEditText.setEnabled(habilitado);
        binding.inputCorreo.customEditText.setEnabled(habilitado);
        binding.inputTelefono.customEditText.setEnabled(habilitado);
        binding.inputFechaNacimiento.customEditText.setEnabled(habilitado);
        binding.inputContrasenya.customEditText.setEnabled(habilitado);
        binding.inputConfirmarContrasenya.customEditText.setEnabled(habilitado);

        binding.inputContrasenya.inputTrailingIcon.setEnabled(habilitado);
        binding.inputConfirmarContrasenya.inputTrailingIcon.setEnabled(habilitado);

        float transparencia = bloqueado ? 0.6f : 1f;
        binding.btnRegistrarme.btnSecundario.setAlpha(transparencia);
    }

    private void volverPantallaAnterior() {
        NavController navegador = Navigation.findNavController(requireView());
        boolean haVuelto = navegador.popBackStack();

        if (!haVuelto) {
            navegador.navigate(R.id.inicioSesionFragment);
        }
    }

    private void navegarInicioSesion() {
        NavController navegador = Navigation.findNavController(requireView());

        NavOptions opciones = new NavOptions.Builder()
                .setPopUpTo(R.id.registroFragment, true)
                .build();

        navegador.navigate(R.id.inicioSesionFragment, null, opciones);
    }

    private void navegarPantallaSiguiente() {
        NavController navegador = Navigation.findNavController(requireView());

        NavOptions opciones = new NavOptions.Builder()
                .setPopUpTo(R.id.registroFragment, true)
                .build();

        navegador.navigate(R.id.crearFragment, null, opciones);
    }

    private void alternarVisibilidadContrasenya() {
        if (contrasenyaVisible) {
            binding.inputContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            binding.inputContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);
            contrasenyaVisible = false;
        } else {
            binding.inputContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            binding.inputContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_visible);
            contrasenyaVisible = true;
        }

        binding.inputContrasenya.customEditText.setSelection(
                binding.inputContrasenya.customEditText.getText().length()
        );
    }

    private void alternarVisibilidadConfirmarContrasenya() {
        if (confirmarContrasenyaVisible) {
            binding.inputConfirmarContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
            binding.inputConfirmarContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);
            confirmarContrasenyaVisible = false;
        } else {
            binding.inputConfirmarContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );
            binding.inputConfirmarContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_visible);
            confirmarContrasenyaVisible = true;
        }

        binding.inputConfirmarContrasenya.customEditText.setSelection(
                binding.inputConfirmarContrasenya.customEditText.getText().length()
        );
    }

    @Override
    public void onDestroyView() {
        if (accionDespuesPopup != null) {
            temporizador.removeCallbacks(accionDespuesPopup);
        }

        super.onDestroyView();
        binding = null;
    }
}