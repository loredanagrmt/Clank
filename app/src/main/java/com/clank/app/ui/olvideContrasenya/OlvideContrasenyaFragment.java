package com.clank.app.ui.olvideContrasenya;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentOlvideContrasenyaBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class OlvideContrasenyaFragment extends Fragment {

    private FragmentOlvideContrasenyaBinding binding;
    private OlvideContrasenyaViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentOlvideContrasenyaBinding.inflate(
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
                .get(OlvideContrasenyaViewModel.class);

        configurarNavbar();
        configurarInputCorreo();
        configurarBotonContinuar();
        observarViewModel();
    }

    private void configurarNavbar() {
        binding.navbarOlvideContrasenya.btnNavbarAccion.setVisibility(View.GONE);

        binding.navbarOlvideContrasenya.btnNavbarVolver.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp()
        );
    }

    private void configurarInputCorreo() {
        binding.inputCorreoOlvideContrasenya.tvInputTitulo.setText(
                R.string.olvide_contrasenya_label_correo
        );

        binding.inputCorreoOlvideContrasenya.customEditText.setHint(
                R.string.olvide_contrasenya_hint_correo
        );

        binding.inputCorreoOlvideContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
    }

    private void configurarBotonContinuar() {
        binding.btContinuarOlvideContrasenya.btnPrincipal.setText(
                R.string.olvide_contrasenya_boton_continuar
        );

        binding.btContinuarOlvideContrasenya.btnPrincipal.setOnClickListener(v ->
                solicitarCodigoSiCorreoValido()
        );
    }

    private void solicitarCodigoSiCorreoValido() {
        String correo = obtenerCorreo();

        limpiarErrorCorreo();

        if (!validarCorreo(correo)) {
            return;
        }

        viewModel.solicitarCodigoRecuperacion(correo);
    }

    private String obtenerCorreo() {
        return binding.inputCorreoOlvideContrasenya.customEditText
                .getText()
                .toString()
                .trim();
    }

    private boolean validarCorreo(String correo) {
        if (correo.isEmpty()) {
            mostrarErrorCorreo(
                    R.string.olvide_contrasenya_error_correo_vacio
            );
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarErrorCorreo(
                    R.string.olvide_contrasenya_error_correo_invalido
            );
            return false;
        }

        return true;
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            boolean estaCargando = Boolean.TRUE.equals(cargando);

            binding.btContinuarOlvideContrasenya.btnPrincipal.setEnabled(
                    !estaCargando
            );

            binding.inputCorreoOlvideContrasenya.customEditText.setEnabled(
                    !estaCargando
            );
        });

        viewModel.getEstadoSolicitudCodigo().observe(
                getViewLifecycleOwner(),
                estado -> {
                    if (estado == null) {
                        return;
                    }

                    switch (estado) {
                        case EXITO:
                            viewModel.limpiarEstadoSolicitudCodigo();
                            navegarAPantallaCodigo();
                            break;

                        case CORREO_INVALIDO:
                            mostrarErrorCorreo(
                                    R.string.olvide_contrasenya_error_correo_invalido
                            );
                            viewModel.limpiarEstadoSolicitudCodigo();
                            break;

                        case CORREO_NO_REGISTRADO:
                            mostrarErrorCorreo(
                                    R.string.olvide_contrasenya_error_correo_no_registrado
                            );
                            viewModel.limpiarEstadoSolicitudCodigo();
                            break;

                        case ERROR_GENERAL:
                            mostrarErrorCorreo(
                                    R.string.olvide_contrasenya_error_envio_codigo
                            );
                            viewModel.limpiarEstadoSolicitudCodigo();
                            break;
                    }
                }
        );
    }

    private void limpiarErrorCorreo() {
        binding.inputCorreoOlvideContrasenya.customEditText.setError(null);
    }

    private void mostrarErrorCorreo(int mensajeError) {
        binding.inputCorreoOlvideContrasenya.customEditText.setError(
                getString(mensajeError)
        );

        binding.inputCorreoOlvideContrasenya.customEditText.requestFocus();
    }

    private void navegarAPantallaCodigo() {
        Bundle argumentos = new Bundle();
        argumentos.putString("correoRecuperacion", obtenerCorreo());

        NavHostFragment.findNavController(this).navigate(
                R.id.action_olvideContrasenyaFragment_to_codigoRecuperacionContrasenyaFragment,
                argumentos
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}