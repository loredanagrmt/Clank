package com.clank.app.ui.cambiarContrasenya;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.widget.EditText;
import android.widget.ImageView;

import com.clank.app.R;
import com.clank.app.databinding.FragmentCambiarContrasenyaBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CambiarContrasenyaFragment extends Fragment {

    private FragmentCambiarContrasenyaBinding binding;
    private CambiarContrasenyaViewModel viewModel;

    public CambiarContrasenyaFragment() {
        super(R.layout.fragment_cambiar_contrasenya);
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(vista, savedInstanceState);

        binding = FragmentCambiarContrasenyaBinding.bind(vista);
        viewModel = new ViewModelProvider(this).get(CambiarContrasenyaViewModel.class);

        configurarNavbar();
        configurarInputs();
        configurarBoton();
        observarViewModel();
    }

    private void configurarNavbar() {
        binding.navbar.tvNavbarTitulo.setText(
                R.string.cambiar_contrasenya_titulo_navbar
        );

        binding.navbar.btnNavbarVolver.setOnClickListener(vista ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    private void configurarInputContrasenya(EditText editText, ImageView icono) {
        editText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        icono.setVisibility(View.VISIBLE);
        icono.setImageResource(R.drawable.ic_contrasenya_oculta);
        icono.setContentDescription(
                getString(R.string.descripcion_mostrar_contrasenya)
        );

        icono.setOnClickListener(v -> {
            boolean estaOculta =
                    (editText.getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD)
                            == InputType.TYPE_TEXT_VARIATION_PASSWORD;

            if (estaOculta) {
                editText.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );

                icono.setImageResource(R.drawable.ic_contrasenya_visible);
                icono.setContentDescription(
                        getString(R.string.descripcion_ocultar_contrasenya)
                );
            } else {
                editText.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );

                icono.setImageResource(R.drawable.ic_contrasenya_oculta);
                icono.setContentDescription(
                        getString(R.string.descripcion_mostrar_contrasenya)
                );
            }

            editText.setSelection(editText.getText().length());
        });
    }

    private void configurarInputs() {
        binding.inputContrasenyaActual.tvInputTitulo.setText(
                R.string.cambiar_contrasenya_label_actual
        );

        binding.inputNuevaContrasenya.tvInputTitulo.setText(
                R.string.cambiar_contrasenya_label_nueva
        );

        binding.inputRepetirContrasenya.tvInputTitulo.setText(
                R.string.cambiar_contrasenya_label_repetir
        );

        configurarInputContrasenya(
                binding.inputContrasenyaActual.customEditText,
                binding.inputContrasenyaActual.inputTrailingIcon
        );

        configurarInputContrasenya(
                binding.inputNuevaContrasenya.customEditText,
                binding.inputNuevaContrasenya.inputTrailingIcon
        );

        configurarInputContrasenya(
                binding.inputRepetirContrasenya.customEditText,
                binding.inputRepetirContrasenya.inputTrailingIcon
        );
    }

    private void configurarBoton() {
        binding.btContinuarCambiarContrasenya.btnPrincipal.setText(
                R.string.cambiar_contrasenya_boton_continuar
        );

        binding.btContinuarCambiarContrasenya.btnPrincipal.setOnClickListener(vista ->
                validarFormulario()
        );
    }

    private void validarFormulario() {
        limpiarErrores();

        String contrasenyaActual = obtenerContrasenyaActual();
        String nuevaContrasenya = obtenerNuevaContrasenya();
        String repetirContrasenya = obtenerRepetirContrasenya();

        boolean formularioValido = true;

        if (contrasenyaActual.isEmpty()) {
            binding.inputContrasenyaActual.customEditText.setError(
                    getString(R.string.cambiar_contrasenya_error_actual_vacia)
            );
            formularioValido = false;
        }

        if (nuevaContrasenya.isEmpty()) {
            binding.inputNuevaContrasenya.customEditText.setError(
                    getString(R.string.cambiar_contrasenya_error_nueva_vacia)
            );
            formularioValido = false;
        } else if (nuevaContrasenya.length() < 6) {
            binding.inputNuevaContrasenya.customEditText.setError(
                    getString(R.string.cambiar_contrasenya_error_minimo_caracteres)
            );
            formularioValido = false;
        }

        if (repetirContrasenya.isEmpty()) {
            binding.inputRepetirContrasenya.customEditText.setError(
                    getString(R.string.cambiar_contrasenya_error_repetir_vacia)
            );
            formularioValido = false;
        } else if (!nuevaContrasenya.equals(repetirContrasenya)) {
            binding.inputRepetirContrasenya.customEditText.setError(
                    getString(R.string.cambiar_contrasenya_error_no_coinciden)
            );
            formularioValido = false;
        }

        if (!formularioValido) {
            return;
        }

        viewModel.cambiarContrasenya(
                contrasenyaActual,
                nuevaContrasenya
        );
    }

    private String obtenerContrasenyaActual() {
        if (binding.inputContrasenyaActual.customEditText.getText() == null) {
            return "";
        }

        return binding.inputContrasenyaActual.customEditText
                .getText()
                .toString()
                .trim();
    }

    private String obtenerNuevaContrasenya() {
        if (binding.inputNuevaContrasenya.customEditText.getText() == null) {
            return "";
        }

        return binding.inputNuevaContrasenya.customEditText
                .getText()
                .toString()
                .trim();
    }

    private String obtenerRepetirContrasenya() {
        if (binding.inputRepetirContrasenya.customEditText.getText() == null) {
            return "";
        }

        return binding.inputRepetirContrasenya.customEditText
                .getText()
                .toString()
                .trim();
    }

    private void limpiarErrores() {
        binding.inputContrasenyaActual.customEditText.setError(null);
        binding.inputNuevaContrasenya.customEditText.setError(null);
        binding.inputRepetirContrasenya.customEditText.setError(null);
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            boolean estaCargando = Boolean.TRUE.equals(cargando);

            binding.btContinuarCambiarContrasenya.btnPrincipal.setEnabled(
                    !estaCargando
            );
        });

        viewModel.getEstadoCambio().observe(getViewLifecycleOwner(), estado -> {
            if (estado == null) {
                return;
            }

            switch (estado) {
                case EXITO:
                    viewModel.limpiarEstadoCambio();
                    mostrarPopupContrasenyaActualizada();
                    break;

                case CONTRASENYA_ACTUAL_INCORRECTA:
                    binding.inputContrasenyaActual.customEditText.setError(
                            getString(R.string.cambiar_contrasenya_error_actual_incorrecta)
                    );

                    binding.inputContrasenyaActual.customEditText.requestFocus();
                    viewModel.limpiarEstadoCambio();
                    break;

                case CONTRASENYA_DEBIL:
                    binding.inputNuevaContrasenya.customEditText.setError(
                            getString(R.string.cambiar_contrasenya_error_minimo_caracteres)
                    );

                    binding.inputNuevaContrasenya.customEditText.requestFocus();
                    viewModel.limpiarEstadoCambio();
                    break;

                case ERROR_GENERAL:
                    Toast.makeText(
                            requireContext(),
                            R.string.cambiar_contrasenya_error_general,
                            Toast.LENGTH_SHORT
                    ).show();

                    viewModel.limpiarEstadoCambio();
                    break;
            }
        });
    }

    private void mostrarPopupContrasenyaActualizada() {
        binding.capaPopup.setVisibility(View.VISIBLE);

        binding.popup.tvTituloPopup.setText(
                R.string.popup_cambiar_contrasenya_titulo
        );

        binding.popup.tvMensajePopup.setText(
                R.string.popup_cambiar_contrasenya_mensaje
        );

        binding.popup.imgIconoPopup.setImageResource(
                R.drawable.ic_check_inactivo
        );

        binding.popup.imgIconoPopup.setContentDescription(
                getString(R.string.popup_cambiar_contrasenya_icono_desc)
        );

        binding.popup.contenedorBotonPopup.setVisibility(View.VISIBLE);

        binding.popup.btnPopupAccion.btnSecundario.setText(
                R.string.popup_cambiar_contrasenya_boton
        );

        binding.popup.btnPopupAccion.btnSecundario.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
