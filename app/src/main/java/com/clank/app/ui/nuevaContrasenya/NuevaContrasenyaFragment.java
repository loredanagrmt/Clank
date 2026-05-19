package com.clank.app.ui.nuevaContrasenya;

import android.os.Bundle;
import android.text.InputType;
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
import com.clank.app.databinding.FragmentNuevaContrasenyaBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NuevaContrasenyaFragment extends Fragment {

    private FragmentNuevaContrasenyaBinding binding;
    private NuevaContrasenyaViewModel viewModel;

    private String correoRecuperacion;
    private String tokenRecuperacion;

    private boolean nuevaContrasenyaVisible = false;
    private boolean repetirNuevaContrasenyaVisible = false;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentNuevaContrasenyaBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this)
                .get(NuevaContrasenyaViewModel.class);

        obtenerArgumentos();
        configurarNavbar();
        configurarInputs();
        configurarBotonContinuar();
        observarViewModel();
    }

    private void obtenerArgumentos() {
        Bundle argumentos = getArguments();

        if (argumentos != null) {
            correoRecuperacion = argumentos.getString("correoRecuperacion", "");
            tokenRecuperacion = argumentos.getString("tokenRecuperacion", "");
        } else {
            correoRecuperacion = "";
            tokenRecuperacion = "";
        }
    }

    private void configurarNavbar() {
        binding.navbarNuevaContrasenya.tvNavbarTitulo.setText(
                R.string.nueva_contrasenya_titulo_navbar
        );

        binding.navbarNuevaContrasenya.btnNavbarAccion.setVisibility(View.GONE);

        binding.navbarNuevaContrasenya.btnNavbarVolver.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp()
        );
    }

    private void configurarInputs() {
        binding.inputNuevaContrasenya.tvInputTitulo.setText(
                R.string.nueva_contrasenya_label_nueva
        );

        binding.inputNuevaContrasenya.customEditText.setHint(
                R.string.hint_contrasenya_oculta
        );

        binding.inputNuevaContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        binding.inputNuevaContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputNuevaContrasenya.inputTrailingIcon.setImageResource(
                R.drawable.ic_contrasenya_oculta
        );

        binding.inputNuevaContrasenya.inputTrailingIcon.setOnClickListener(
                vista -> alternarVisibilidadNuevaContrasenya()
        );

        binding.inputRepetirNuevaContrasenya.tvInputTitulo.setText(
                R.string.nueva_contrasenya_label_repetir
        );

        binding.inputRepetirNuevaContrasenya.customEditText.setHint(
                R.string.hint_contrasenya_oculta
        );

        binding.inputRepetirNuevaContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        binding.inputRepetirNuevaContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputRepetirNuevaContrasenya.inputTrailingIcon.setImageResource(
                R.drawable.ic_contrasenya_oculta
        );

        binding.inputRepetirNuevaContrasenya.inputTrailingIcon.setOnClickListener(
                vista -> alternarVisibilidadRepetirNuevaContrasenya()
        );
    }

    private void configurarBotonContinuar() {
        binding.btContinuarNuevaContrasenya.btnPrincipal.setText(
                R.string.nueva_contrasenya_boton_continuar
        );

        binding.btContinuarNuevaContrasenya.btnPrincipal.setOnClickListener(v ->
                actualizarContrasenyaSiEsValida()
        );
    }

    private void actualizarContrasenyaSiEsValida() {
        limpiarErrores();

        String nuevaContrasenya = obtenerTextoContrasenya(
                binding.inputNuevaContrasenya.customEditText
        );

        String repetirContrasenya = obtenerTextoContrasenya(
                binding.inputRepetirNuevaContrasenya.customEditText
        );

        if (nuevaContrasenya.isEmpty()) {
            mostrarErrorNuevaContrasenya(
                    R.string.nueva_contrasenya_error_vacia
            );
            return;
        }

        if (nuevaContrasenya.length() < 6) {
            mostrarErrorNuevaContrasenya(
                    R.string.nueva_contrasenya_error_minimo_caracteres
            );
            return;
        }

        if (repetirContrasenya.isEmpty()) {
            mostrarErrorRepetirContrasenya(
                    R.string.nueva_contrasenya_error_repetir_vacia
            );
            return;
        }

        if (!nuevaContrasenya.equals(repetirContrasenya)) {
            mostrarErrorRepetirContrasenya(
                    R.string.nueva_contrasenya_error_no_coinciden
            );
            return;
        }

        if (correoRecuperacion.trim().isEmpty()
                || tokenRecuperacion.trim().isEmpty()) {
            mostrarErrorNuevaContrasenya(
                    R.string.nueva_contrasenya_error_datos_recuperacion
            );
            return;
        }

        viewModel.actualizarContrasenya(
                correoRecuperacion,
                tokenRecuperacion,
                nuevaContrasenya
        );
    }

    private String obtenerTextoContrasenya(EditText editText) {
        return editText.getText().toString();
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            boolean estaCargando = Boolean.TRUE.equals(cargando);

            binding.btContinuarNuevaContrasenya.btnPrincipal.setEnabled(!estaCargando);
            binding.inputNuevaContrasenya.customEditText.setEnabled(!estaCargando);
            binding.inputRepetirNuevaContrasenya.customEditText.setEnabled(!estaCargando);
        });

        viewModel.getEstadoActualizacion().observe(
                getViewLifecycleOwner(),
                estado -> {
                    if (estado == null) {
                        return;
                    }

                    switch (estado) {
                        case EXITO:
                            mostrarPopupContrasenyaActualizada();
                            viewModel.limpiarEstadoActualizacion();
                            break;

                        case CONTRASENYA_DEBIL:
                            mostrarErrorNuevaContrasenya(
                                    R.string.nueva_contrasenya_error_minimo_caracteres
                            );
                            viewModel.limpiarEstadoActualizacion();
                            break;

                        case TOKEN_INVALIDO:
                            mostrarErrorNuevaContrasenya(
                                    R.string.nueva_contrasenya_error_token_invalido
                            );
                            viewModel.limpiarEstadoActualizacion();
                            break;

                        case TOKEN_CADUCADO:
                            mostrarErrorNuevaContrasenya(
                                    R.string.nueva_contrasenya_error_token_caducado
                            );
                            viewModel.limpiarEstadoActualizacion();
                            break;

                        case DATOS_INVALIDOS:
                            mostrarErrorNuevaContrasenya(
                                    R.string.nueva_contrasenya_error_datos_recuperacion
                            );
                            viewModel.limpiarEstadoActualizacion();
                            break;

                        case ERROR_GENERAL:
                            mostrarErrorNuevaContrasenya(
                                    R.string.nueva_contrasenya_error_general
                            );
                            viewModel.limpiarEstadoActualizacion();
                            break;
                    }
                }
        );
    }

    private void limpiarErrores() {
        binding.inputNuevaContrasenya.customEditText.setError(null);
        binding.inputRepetirNuevaContrasenya.customEditText.setError(null);
    }

    private void mostrarErrorNuevaContrasenya(int mensaje) {
        binding.inputNuevaContrasenya.customEditText.setError(
                getString(mensaje)
        );

        binding.inputNuevaContrasenya.customEditText.requestFocus();
    }

    private void mostrarErrorRepetirContrasenya(int mensaje) {
        binding.inputRepetirNuevaContrasenya.customEditText.setError(
                getString(mensaje)
        );

        binding.inputRepetirNuevaContrasenya.customEditText.requestFocus();
    }

    private void mostrarPopupContrasenyaActualizada() {
        binding.capaPopup.setVisibility(View.VISIBLE);

        binding.popup.tvTituloPopup.setText(
                R.string.popup_nueva_contrasenya_titulo
        );

        binding.popup.tvMensajePopup.setText(
                R.string.popup_nueva_contrasenya_mensaje
        );

        binding.popup.imgIconoPopup.setContentDescription(
                getString(R.string.popup_nueva_contrasenya_icono_desc)
        );

        binding.popup.contenedorBotonPopup.setVisibility(View.VISIBLE);

        binding.popup.btnPopupAccion.btnSecundario.setText(
                R.string.popup_nueva_contrasenya_boton
        );

        binding.popup.btnPopupAccion.btnSecundario.setOnClickListener(v ->
                navegarAInicioSesion()
        );
    }

    private void navegarAInicioSesion() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.nuevaContrasenyaFragment) {
            return;
        }

        navegador.navigate(
                R.id.action_nuevaContrasenyaFragment_to_inicioSesionFragment
        );
    }

    private void alternarVisibilidadNuevaContrasenya() {
        if (nuevaContrasenyaVisible) {
            binding.inputNuevaContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            binding.inputNuevaContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_oculta
            );

            nuevaContrasenyaVisible = false;
        } else {
            binding.inputNuevaContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            binding.inputNuevaContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_visible
            );

            nuevaContrasenyaVisible = true;
        }

        binding.inputNuevaContrasenya.customEditText.setSelection(
                binding.inputNuevaContrasenya.customEditText.getText().length()
        );
    }

    private void alternarVisibilidadRepetirNuevaContrasenya() {
        if (repetirNuevaContrasenyaVisible) {
            binding.inputRepetirNuevaContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            binding.inputRepetirNuevaContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_oculta
            );

            repetirNuevaContrasenyaVisible = false;
        } else {
            binding.inputRepetirNuevaContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            binding.inputRepetirNuevaContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_visible
            );

            repetirNuevaContrasenyaVisible = true;
        }

        binding.inputRepetirNuevaContrasenya.customEditText.setSelection(
                binding.inputRepetirNuevaContrasenya.customEditText.getText().length()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}