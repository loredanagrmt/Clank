package com.clank.app.ui.codigoRecuperacionContrasenya;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.clank.app.R;
import com.clank.app.databinding.FragmentCodigoRecuperacionContrasenyaBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CodigoRecuperacionContrasenyaFragment extends Fragment {

    private FragmentCodigoRecuperacionContrasenyaBinding binding;
    private CodigoRecuperacionContrasenyaViewModel viewModel;

    private String correoRecuperacion;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCodigoRecuperacionContrasenyaBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CodigoRecuperacionContrasenyaViewModel.class);

        configurarNavbar();
        obtenerArgumentos();
        configurarVista();
        configurarCasillasCodigo();
        configurarBotonContinuar();
        observarViewModel();
    }

    private void obtenerArgumentos() {
        Bundle argumentos = getArguments();

        if (argumentos != null) {
            correoRecuperacion = argumentos.getString("correoRecuperacion", "");
        } else {
            correoRecuperacion = "";
        }
    }

    private void configurarVista() {
        binding.btContinuarCodigoRecuperacionContrasenya.btnPrincipal.setText(R.string.codigo_recuperacion_contrasenya_boton_continuar);
    }

    private void configurarBotonContinuar() {
        binding.btContinuarCodigoRecuperacionContrasenya.btnPrincipal.setOnClickListener(vista -> verificarCodigoSiEsValido());
    }

    private void configurarCasillasCodigo() {
        EditText[] casillas = obtenerCasillasCodigo();

        for (int i = 0; i < casillas.length; i++) {
            int indiceActual = i;

            casillas[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence texto, int inicio, int cantidad, int despues) {
                }

                @Override
                public void onTextChanged(CharSequence texto, int inicio, int antes, int cantidad) {
                    limpiarErroresCodigo();

                    if (texto.length() == 1 && indiceActual < casillas.length - 1) {
                        casillas[indiceActual + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            casillas[i].setOnKeyListener((vista, tecla, evento) -> {
                if (evento.getAction() == KeyEvent.ACTION_DOWN && tecla == KeyEvent.KEYCODE_DEL && casillas[indiceActual].getText().toString().isEmpty() && indiceActual > 0) {

                    casillas[indiceActual - 1].requestFocus();
                    casillas[indiceActual - 1].setSelection(casillas[indiceActual - 1].getText().length());

                    return true;
                }

                return false;
            });
        }
    }

    private void verificarCodigoSiEsValido() {
        limpiarErroresCodigo();

        if (correoRecuperacion == null || correoRecuperacion.trim().isEmpty()) {
            mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_general);
            return;
        }

        String codigo = obtenerCodigoCompleto();

        if (codigo.length() < 6) {
            mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_codigo_incompleto);
            return;
        }

        viewModel.verificarCodigo(correoRecuperacion, codigo);
    }

    private String obtenerCodigoCompleto() {
        StringBuilder codigo = new StringBuilder();

        for (EditText casilla : obtenerCasillasCodigo()) {
            codigo.append(casilla.getText().toString().trim());
        }

        return codigo.toString();
    }

    private EditText[] obtenerCasillasCodigo() {
        return new EditText[]{binding.etCodigoUno, binding.etCodigoDos, binding.etCodigoTres, binding.etCodigoCuatro, binding.etCodigoCinco, binding.etCodigoSeis};
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
            boolean estaCargando = Boolean.TRUE.equals(cargando);

            binding.btContinuarCodigoRecuperacionContrasenya.btnPrincipal.setEnabled(!estaCargando);

            for (EditText casilla : obtenerCasillasCodigo()) {
                casilla.setEnabled(!estaCargando);
            }
        });

        viewModel.getEstadoVerificacion().observe(getViewLifecycleOwner(), estado -> {
            if (estado == null) {
                return;
            }

            switch (estado) {
                case EXITO:
                    String token = viewModel.getTokenRecuperacion().getValue();

                    if (token == null || token.trim().isEmpty()) {
                        mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_general);
                    } else {
                        navegarANuevaContrasenya(token);
                    }

                    viewModel.limpiarEstadoVerificacion();
                    break;

                case CODIGO_INVALIDO:
                    mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_codigo_invalido);
                    viewModel.limpiarEstadoVerificacion();
                    break;

                case CODIGO_CADUCADO:
                    mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_codigo_caducado);
                    viewModel.limpiarEstadoVerificacion();
                    break;

                case DEMASIADOS_INTENTOS:
                    mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_demasiados_intentos);
                    viewModel.limpiarEstadoVerificacion();
                    break;

                case CORREO_INVALIDO:
                case ERROR_GENERAL:
                    mostrarErrorCodigo(R.string.codigo_recuperacion_contrasenya_error_general);
                    viewModel.limpiarEstadoVerificacion();
                    break;
            }
        });
    }

    private void limpiarErroresCodigo() {
        for (EditText casilla : obtenerCasillasCodigo()) {
            casilla.setError(null);
        }
    }

    private void mostrarErrorCodigo(int mensajeError) {
        binding.etCodigoUno.setError(getString(mensajeError));
        binding.etCodigoUno.requestFocus();
    }

    private void navegarANuevaContrasenya(String tokenRecuperacion) {
        Bundle argumentos = new Bundle();
        argumentos.putString("correoRecuperacion", correoRecuperacion);
        argumentos.putString("tokenRecuperacion", tokenRecuperacion);

        NavHostFragment.findNavController(this).navigate(R.id.action_codigoRecuperacionContrasenyaFragment_to_nuevaContrasenyaFragment, argumentos);
    }

    private void configurarNavbar() {
        binding.navbarCodigoRecuperacionContrasenya.btnNavbarAccion.setVisibility(View.GONE);

        binding.navbarCodigoRecuperacionContrasenya.btnNavbarVolver.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}