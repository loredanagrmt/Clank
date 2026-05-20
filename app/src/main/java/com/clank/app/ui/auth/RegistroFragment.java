package com.clank.app.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentRegistroBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RegistroFragment extends Fragment {

    private static final long DURACION_POPUP_MILISEGUNDOS = 3000;

    private FragmentRegistroBinding binding;
    private RegistroCompartidoViewModel vistaModeloCompartida;

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

        vistaModeloCompartida = new ViewModelProvider(requireActivity())
                .get(RegistroCompartidoViewModel.class);

        configurarVista();
        configurarListeners();
        precargarDatosRegistro();
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
        binding.inputContrasenya.customEditText.setHint(getString(R.string.hint_contrasenya_oculta));
        binding.inputContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        binding.inputContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);

        binding.inputConfirmarContrasenya.tvInputTitulo.setText(getString(R.string.confirmar_contrasenya));
        binding.inputConfirmarContrasenya.customEditText.setHint(getString(R.string.hint_contrasenya_oculta));
        binding.inputConfirmarContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        binding.inputConfirmarContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputConfirmarContrasenya.inputTrailingIcon.setImageResource(R.drawable.ic_contrasenya_oculta);

        binding.btnRegistrarme.btnSecundario.setText(getString(R.string.registrarme));
        configurarModoRegistroGoogle();
    }


    private void configurarModoRegistroGoogle() {
        boolean registroConGoogle = vistaModeloCompartida.isRegistroConGoogle();

        int visibilidadContrasenya = registroConGoogle
                ? View.GONE
                : View.VISIBLE;

        binding.inputContrasenya.getRoot().setVisibility(visibilidadContrasenya);
        binding.inputConfirmarContrasenya.getRoot().setVisibility(visibilidadContrasenya);

        if (registroConGoogle) {
            binding.inputCorreo.customEditText.setEnabled(false);
        }
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
                procesarRegistro()
        );
    }

    private void precargarDatosRegistro() {
        if (vistaModeloCompartida == null) {
            return;
        }

        vistaModeloCompartida.recargarDatosGuardados();

        binding.inputNombreCompleto.customEditText.setText(vistaModeloCompartida.getNombre());
        binding.inputCorreo.customEditText.setText(vistaModeloCompartida.getCorreo());
        binding.inputTelefono.customEditText.setText(vistaModeloCompartida.getTelefono());
        binding.inputFechaNacimiento.customEditText.setText(vistaModeloCompartida.getFechaNacimiento());

        if (vistaModeloCompartida.isRegistroConGoogle()) {
            binding.inputContrasenya.customEditText.setText("");
            binding.inputConfirmarContrasenya.customEditText.setText("");
        } else {
            binding.inputContrasenya.customEditText.setText(vistaModeloCompartida.getContrasenya());
            binding.inputConfirmarContrasenya.customEditText.setText(vistaModeloCompartida.getContrasenya());
        }
    }

    private void limpiarCamposFormulario() {
        binding.inputNombreCompleto.customEditText.setText("");
        binding.inputCorreo.customEditText.setText("");
        binding.inputTelefono.customEditText.setText("");
        binding.inputFechaNacimiento.customEditText.setText("");
        binding.inputContrasenya.customEditText.setText("");
        binding.inputConfirmarContrasenya.customEditText.setText("");
    }

    private void procesarRegistro() {
        boolean registroConGoogle = vistaModeloCompartida.isRegistroConGoogle();

        String nombre = getTexto(binding.inputNombreCompleto.customEditText.getText());
        String correo = getTexto(binding.inputCorreo.customEditText.getText());
        String telefono = getTexto(binding.inputTelefono.customEditText.getText());
        String fechaNacimiento = getTexto(binding.inputFechaNacimiento.customEditText.getText());

        String contrasenya = registroConGoogle
                ? ""
                : getContrasenya(binding.inputContrasenya.customEditText.getText());

        String confirmarContrasenya = registroConGoogle
                ? ""
                : getContrasenya(binding.inputConfirmarContrasenya.customEditText.getText());

        limpiarErroresFormulario();

        if (!validarFormulario(
                nombre,
                correo,
                telefono,
                fechaNacimiento,
                contrasenya,
                confirmarContrasenya,
                registroConGoogle
        )) {
            return;
        }

        vistaModeloCompartida.guardarDatosRegistro(
                nombre,
                correo,
                telefono,
                fechaNacimiento,
                contrasenya
        );

        mostrarPopupRegistro();
    }
    private boolean validarFormulario(String nombre,
                                      String correo,
                                      String telefono,
                                      String fechaNacimiento,
                                      String contrasenya,
                                      String confirmarContrasenya,
                                      boolean registroConGoogle) {
        if (nombre.isEmpty()) {
            mostrarError(
                    binding.inputNombreCompleto.customEditText,
                    R.string.registro_error_nombre_vacio
            );
            return false;
        }

        if (correo.isEmpty()) {
            mostrarError(
                    binding.inputCorreo.customEditText,
                    R.string.registro_error_correo_vacio
            );
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarError(
                    binding.inputCorreo.customEditText,
                    R.string.registro_error_correo_invalido
            );
            return false;
        }

        if (fechaNacimiento.isEmpty()) {
            mostrarError(
                    binding.inputFechaNacimiento.customEditText,
                    R.string.registro_error_fecha_nacimiento_vacia
            );
            return false;
        }

        if (!registroConGoogle) {
            if (contrasenya.isEmpty()) {
                mostrarError(
                        binding.inputContrasenya.customEditText,
                        R.string.registro_error_contrasenya_vacia
                );
                return false;
            }

            if (contrasenya.length() < 6) {
                mostrarError(
                        binding.inputContrasenya.customEditText,
                        R.string.registro_error_contrasenya_corta
                );
                return false;
            }

            if (confirmarContrasenya.isEmpty()) {
                mostrarError(
                        binding.inputConfirmarContrasenya.customEditText,
                        R.string.registro_error_confirmar_contrasenya_vacia
                );
                return false;
            }

            if (!contrasenya.equals(confirmarContrasenya)) {
                mostrarError(
                        binding.inputConfirmarContrasenya.customEditText,
                        R.string.registro_error_contrasenyas_distintas
                );
                return false;
            }
        }

        return true;
    }

    private void mostrarError(EditText editText, int mensajeError) {
        editText.setError(getString(mensajeError));
        editText.requestFocus();
    }

    private void limpiarErroresFormulario() {
        binding.inputNombreCompleto.customEditText.setError(null);
        binding.inputCorreo.customEditText.setError(null);
        binding.inputTelefono.customEditText.setError(null);
        binding.inputFechaNacimiento.customEditText.setError(null);
        binding.inputContrasenya.customEditText.setError(null);
        binding.inputConfirmarContrasenya.customEditText.setError(null);
    }

    private String getTexto(@Nullable CharSequence texto) {
        if (texto == null) {
            return "";
        }

        return texto.toString().trim();
    }

    private String getContrasenya(@Nullable CharSequence texto) {
        if (texto == null) {
            return "";
        }

        return texto.toString();
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
        binding.inputTelefono.customEditText.setEnabled(habilitado);
        binding.inputFechaNacimiento.customEditText.setEnabled(habilitado);

        boolean registroConGoogle = vistaModeloCompartida.isRegistroConGoogle();

        binding.inputCorreo.customEditText.setEnabled(habilitado && !registroConGoogle);
        binding.inputContrasenya.customEditText.setEnabled(habilitado && !registroConGoogle);
        binding.inputConfirmarContrasenya.customEditText.setEnabled(habilitado && !registroConGoogle);

        binding.inputContrasenya.inputTrailingIcon.setEnabled(habilitado && !registroConGoogle);
        binding.inputConfirmarContrasenya.inputTrailingIcon.setEnabled(habilitado && !registroConGoogle);

        float transparencia = bloqueado ? 0.6f : 1f;
        binding.btnRegistrarme.btnSecundario.setAlpha(transparencia);
    }

    private void volverPantallaAnterior() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.registroFragment) {
            return;
        }

        navegador.navigate(R.id.action_registro_a_bienvenida);
    }

    private void navegarInicioSesion() {
        vistaModeloCompartida.limpiar();

        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.registroFragment) {
            return;
        }

        navegador.navigate(R.id.action_registro_a_inicio_sesion);
    }

    private void navegarPantallaSiguiente() {
        if (binding == null) {
            return;
        }

        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.registroFragment) {
            return;
        }

        navegador.navigate(R.id.action_registro_a_completar_perfil);
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