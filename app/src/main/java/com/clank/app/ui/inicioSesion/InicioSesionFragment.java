package com.clank.app.ui.inicioSesion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.clank.app.R;
import com.clank.app.databinding.FragmentInicioSesionBinding;
import com.clank.app.ui.auth.RegistroCompartidoViewModel;
import com.clank.app.ui.comun.NavbarHost;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InicioSesionFragment extends Fragment {

    private FragmentInicioSesionBinding binding;
    private InicioSesionViewModel vistaModelo;
    private RegistroCompartidoViewModel vistaModeloRegistro;
    private GoogleSignInClient clienteInicioGoogle;

    private boolean contrasenyaVisible = false;

    private final ActivityResultLauncher<Intent> lanzadorInicioGoogle =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() != Activity.RESULT_OK) {
                            return;
                        }

                        Task<GoogleSignInAccount> tarea =
                                GoogleSignIn.getSignedInAccountFromIntent(resultado.getData());

                        try {
                            GoogleSignInAccount cuenta = tarea.getResult(ApiException.class);
                            vistaModelo.iniciarSesionGoogle(cuenta);
                        } catch (ApiException error) {
                            mostrarMensaje("No se pudo iniciar sesión con Google");
                        }
                    }
            );

    public InicioSesionFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        configurarInicioGoogle();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup contenedor,
                             Bundle estadoGuardado) {
        binding = FragmentInicioSesionBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista,
                              @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        vistaModelo = new ViewModelProvider(this).get(InicioSesionViewModel.class);

        vistaModeloRegistro = new ViewModelProvider(requireActivity())
                .get(RegistroCompartidoViewModel.class);

        configurarVista();
        configurarListeners();
        observarVistaModelo();
    }

    @Override
    public void onResume() {
        super.onResume();
        configurarNavbar();
    }

    private void configurarInicioGoogle() {
        GoogleSignInOptions opcionesInicioGoogle = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.id_cliente_web_firebase))
                .requestEmail()
                .build();

        clienteInicioGoogle = GoogleSignIn.getClient(
                requireContext(),
                opcionesInicioGoogle
        );
    }

    private void configurarVista() {
        binding.inputCorreo.tvInputTitulo.setText(
                getString(R.string.correo_electronico)
        );

        binding.inputCorreo.customEditText.setHint(
                getString(R.string.ejemplo_correo)
        );

        binding.inputCorreo.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        binding.inputCorreo.inputTrailingIcon.setVisibility(View.GONE);

        binding.inputContrasenya.tvInputTitulo.setText(
                getString(R.string.contrasenya)
        );

        binding.inputContrasenya.customEditText.setHint(
                getString(R.string.hint_contrasenya_oculta)
        );

        binding.inputContrasenya.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        binding.inputContrasenya.inputTrailingIcon.setVisibility(View.VISIBLE);
        binding.inputContrasenya.inputTrailingIcon.setImageResource(
                R.drawable.ic_contrasenya_oculta
        );

        binding.btnIniciarSesion.btnSecundario.setText(
                getString(R.string.iniciar_sesion)
        );

        binding.btnRegistrarse.btnSecundario.setText(
                getString(R.string.registrarse)
        );
    }

    private void configurarNavbar() {
        NavbarHost host = (NavbarHost) requireActivity();

        host.mostrarNavbarConVolver(
                getString(R.string.inicio_sesion_titulo)
        );

        host.configurarAccionVolver(v ->
                navegarBienvenida()
        );
    }

    private void configurarListeners() {
        binding.inputContrasenya.inputTrailingIcon.setOnClickListener(
                vista -> alternarVisibilidadContrasenya()
        );

        binding.btnIniciarSesion.btnSecundario.setOnClickListener(
                vista -> iniciarSesion()
        );

        binding.inputContrasenya.customEditText.setOnEditorActionListener((vista, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                iniciarSesion();
                return true;
            }

            return false;
        });

        binding.imgGoogle.setOnClickListener(
                vista -> lanzadorInicioGoogle.launch(clienteInicioGoogle.getSignInIntent())
        );

        binding.btnRegistrarse.btnSecundario.setOnClickListener(
                vista -> navegarRegistro()
        );

        binding.tvAunSinCuenta.setOnClickListener(
                vista -> navegarRegistro()
        );

        binding.tvOlvidoContrasenya.setOnClickListener(
                vista -> navegarOlvideContrasenya()
        );
    }

    private void iniciarSesion() {
        String correo = getTexto(binding.inputCorreo.customEditText.getText());
        String contrasenya = getContrasenya(binding.inputContrasenya.customEditText.getText());

        vistaModelo.iniciarSesion(correo, contrasenya);
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

    private void observarVistaModelo() {
        vistaModelo.obtenerResultadoInicioSesion().observe(getViewLifecycleOwner(), recurso -> {
            if (recurso == null) {
                return;
            }

            switch (recurso.estado) {
                case CARGANDO:
                    mostrarCargando(true);
                    break;

                case EXITO:
                    mostrarCargando(false);
                    gestionarInicioCorrecto(recurso.data);
                    break;

                case ERROR:
                    mostrarCargando(false);
                    mostrarMensaje(recurso.mensaje);
                    break;
            }
        });
    }

    private void gestionarInicioCorrecto(
            InicioSesionViewModel.DestinoNavegacion destinoNavegacion
    ) {
        if (destinoNavegacion == InicioSesionViewModel.DestinoNavegacion.REGISTRO) {
            navegarRegistroGoogle();
            return;
        }

        navegarPerfil();
    }

    private void navegarBienvenida() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inicioSesionFragment) {
            return;
        }

        navegador.navigate(R.id.action_inicio_sesion_a_bienvenida);
    }

    private void navegarRegistro() {
        vistaModeloRegistro.iniciarNuevoRegistro();
        ejecutarNavegacionRegistro();
    }

    private void navegarRegistroGoogle() {
        vistaModeloRegistro.iniciarNuevoRegistroGoogle(
                vistaModelo.getNombreGoogle(),
                vistaModelo.getCorreoGoogle(),
                vistaModelo.getFotoPerfilGoogle()
        );

        ejecutarNavegacionRegistro();
    }

    private void ejecutarNavegacionRegistro() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inicioSesionFragment) {
            return;
        }

        navegador.navigate(R.id.action_inicio_sesion_a_registro);
    }

    private void navegarPerfil() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inicioSesionFragment) {
            return;
        }

        navegador.navigate(R.id.action_inicio_sesion_a_feed);
    }

    private void navegarOlvideContrasenya() {
        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.inicioSesionFragment) {
            return;
        }

        navegador.navigate(R.id.action_inicio_sesion_a_olvide_contrasenya);
    }

    private void mostrarCargando(boolean cargando) {
        if (binding == null) {
            return;
        }

        binding.btnIniciarSesion.btnSecundario.setEnabled(!cargando);
        binding.btnRegistrarse.btnSecundario.setEnabled(!cargando);
        binding.inputCorreo.customEditText.setEnabled(!cargando);
        binding.inputContrasenya.customEditText.setEnabled(!cargando);
        binding.imgGoogle.setEnabled(!cargando);

        float transparencia = cargando ? 0.5f : 1f;

        binding.btnIniciarSesion.btnSecundario.setAlpha(transparencia);
        binding.btnRegistrarse.btnSecundario.setAlpha(transparencia);
        binding.imgGoogle.setAlpha(transparencia);
    }

    private void alternarVisibilidadContrasenya() {
        if (contrasenyaVisible) {
            binding.inputContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            );

            binding.inputContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_oculta
            );

            contrasenyaVisible = false;
        } else {
            binding.inputContrasenya.customEditText.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            );

            binding.inputContrasenya.inputTrailingIcon.setImageResource(
                    R.drawable.ic_contrasenya_visible
            );

            contrasenyaVisible = true;
        }

        binding.inputContrasenya.customEditText.setSelection(
                binding.inputContrasenya.customEditText.getText().length()
        );
    }

    private void mostrarMensaje(String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Ha ocurrido un error",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}