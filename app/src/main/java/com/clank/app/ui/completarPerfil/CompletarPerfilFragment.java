package com.clank.app.ui.completarPerfil;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.databinding.FragmentCompletarPerfilBinding;
import com.clank.app.ui.auth.RegistroCompartidoViewModel;
import com.clank.app.util.Recurso;

import java.io.File;
import java.io.IOException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CompletarPerfilFragment extends Fragment {

    private static final long DURACION_POPUP_MILISEGUNDOS = 3000;

    private static final String CLAVE_URI_FOTO_PERFIL = "uriFotoPerfil";
    private static final String CLAVE_URI_FOTO_CAMARA_TEMPORAL = "uriFotoCamaraTemporal";

    private FragmentCompletarPerfilBinding binding;
    private CompletarPerfilViewModel viewModel;
    private RegistroCompartidoViewModel vistaModeloCompartida;

    private final Handler temporizador = new Handler(Looper.getMainLooper());
    private Runnable accionDespuesPopup;

    private Uri uriFotoPerfil;
    private Uri uriFotoCamaraTemporal;

    private ActivityResultLauncher<String> lanzadorGaleria;
    private ActivityResultLauncher<Uri> lanzadorCamara;
    private ActivityResultLauncher<String> lanzadorPermisoCamara;

    public CompletarPerfilFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);

        if (estadoGuardado != null) {
            String fotoPerfilGuardada = estadoGuardado.getString(CLAVE_URI_FOTO_PERFIL);
            String fotoCamaraGuardada = estadoGuardado.getString(CLAVE_URI_FOTO_CAMARA_TEMPORAL);

            if (fotoPerfilGuardada != null && !fotoPerfilGuardada.isEmpty()) {
                uriFotoPerfil = Uri.parse(fotoPerfilGuardada);
            }

            if (fotoCamaraGuardada != null && !fotoCamaraGuardada.isEmpty()) {
                uriFotoCamaraTemporal = Uri.parse(fotoCamaraGuardada);
            }
        }

        configurarLanzadoresImagen();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup contenedor,
                             Bundle estadoGuardado) {
        binding = FragmentCompletarPerfilBinding.inflate(inflater, contenedor, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View vista, @Nullable Bundle estadoGuardado) {
        super.onViewCreated(vista, estadoGuardado);

        viewModel = new ViewModelProvider(this).get(CompletarPerfilViewModel.class);
        vistaModeloCompartida = new ViewModelProvider(requireActivity())
                .get(RegistroCompartidoViewModel.class);

        configurarVista();
        configurarListeners();
        observarViewModel();
        restaurarPreviewFotoPerfil();
        precargarFotoPerfilGoogleSiProcede();
    }

    private void configurarLanzadoresImagen() {
        lanzadorGaleria = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        return;
                    }

                    uriFotoPerfil = uri;
                    mostrarPreviewFotoPerfil(uriFotoPerfil);
                }
        );

        lanzadorCamara = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                resultado -> {
                    if (Boolean.TRUE.equals(resultado) && uriFotoCamaraTemporal != null) {
                        uriFotoPerfil = uriFotoCamaraTemporal;
                        mostrarPreviewFotoPerfil(uriFotoPerfil);
                    }
                }
        );

        lanzadorPermisoCamara = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                permisoConcedido -> {
                    if (binding == null) {
                        return;
                    }

                    if (Boolean.TRUE.equals(permisoConcedido)) {
                        abrirCamara();
                    } else {
                        mostrarError(
                                binding.inputUsuarioClank.customEditText,
                                R.string.error_permiso_camara
                        );
                    }
                }
        );
    }

    private void configurarVista() {
        configurarNavbar();
        configurarFormulario();
        configurarPopup();
    }

    private void configurarNavbar() {
        binding.navbar.tvNavbarTitulo.setText(getString(R.string.completar_perfil_titulo));
        binding.navbar.btnNavbarAccion.setVisibility(View.GONE);
    }

    private void configurarFormulario() {
        binding.tvTituloCompletarPerfil.setText(getString(R.string.completar_perfil_titulo_pantalla));

        binding.imgFotoPerfil.setContentDescription(getString(R.string.imagen_foto_perfil));
        binding.btnEditarFotoPerfil.setContentDescription(getString(R.string.editar_foto_perfil));

        binding.inputUsuarioClank.tvInputTitulo.setText(getString(R.string.clank_user));
        binding.inputUsuarioClank.customEditText.setHint(getString(R.string.ejemplo_clank_user));
        binding.inputUsuarioClank.customEditText.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL
        );
        binding.inputUsuarioClank.inputTrailingIcon.setVisibility(View.GONE);

        binding.btnContinuar.btnSecundario.setText(getString(R.string.continuar));
    }

    private void configurarPopup() {
        binding.capaPopup.setVisibility(View.GONE);

        binding.popup.tvTituloPopup.setText(getString(R.string.popup_completar_perfil_titulo));
        binding.popup.tvMensajePopup.setText(getString(R.string.popup_completar_perfil_mensaje));
        binding.popup.imgIconoPopup.setImageResource(R.drawable.ic_check_inactivo);
        binding.popup.imgIconoPopup.setContentDescription(
                getString(R.string.popup_completar_perfil_icono_desc)
        );
        binding.popup.contenedorBotonPopup.setVisibility(View.GONE);
    }

    private void configurarListeners() {
        binding.navbar.btnNavbarVolver.setOnClickListener(vista ->
                volverPantallaAnterior()
        );

        binding.btnEditarFotoPerfil.setOnClickListener(vista ->
                seleccionarFotoPerfil()
        );

        binding.imgFotoPerfil.setOnClickListener(vista ->
                seleccionarFotoPerfil()
        );

        binding.btnContinuar.btnSecundario.setOnClickListener(vista ->
                procesarCompletarPerfil()
        );
    }

    private void observarViewModel() {
        viewModel.getEstado().observe(getViewLifecycleOwner(), recurso -> {
            if (recurso == null) {
                return;
            }

            if (recurso.estado == Recurso.Estado.CARGANDO) {
                limpiarErroresFormulario();
                bloquearFormulario(true);
                return;
            }

            if (recurso.estado == Recurso.Estado.EXITO) {
                vistaModeloCompartida.limpiar();
                mostrarPopupExito();
                return;
            }

            if (recurso.estado == Recurso.Estado.ERROR) {
                bloquearFormulario(false);
                gestionarError(recurso.mensaje);
            }
        });
    }

    private void procesarCompletarPerfil() {
        String usuarioClank = getTexto(binding.inputUsuarioClank.customEditText.getText());

        limpiarErroresFormulario();

        if (!validarFormulario(usuarioClank)) {
            return;
        }

        vistaModeloCompartida.recargarDatosGuardados();

        if (!vistaModeloCompartida.tieneDatosRegistro()) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_datos_registro
            );
            return;
        }

        viewModel.completarPerfil(
                vistaModeloCompartida.getNombre(),
                vistaModeloCompartida.getCorreo(),
                vistaModeloCompartida.getTelefono(),
                vistaModeloCompartida.getFechaNacimiento(),
                vistaModeloCompartida.getContrasenya(),
                usuarioClank,
                uriFotoPerfil,
                vistaModeloCompartida.isRegistroConGoogle(),
                vistaModeloCompartida.getFotoPerfilGoogle()
        );
    }

    private boolean validarFormulario(String usuarioClank) {
        if (usuarioClank.isEmpty()) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_usuario_clank_vacio
            );
            return false;
        }

        return true;
    }

    private void gestionarError(String error) {
        if (CompletarPerfilViewModel.ERROR_USUARIO_CLANK_EXISTE.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_usuario_clank_existente
            );
            return;
        }

        if (CompletarPerfilViewModel.ERROR_CORREO_EXISTENTE.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_correo_existente
            );
            return;
        }

        if (CompletarPerfilViewModel.ERROR_DATOS_REGISTRO.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_datos_registro
            );
            return;
        }

        if (CompletarPerfilViewModel.ERROR_REGISTRO.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_registro
            );
            return;
        }

        if (CompletarPerfilViewModel.ERROR_SUBIR_FOTO.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_subir_foto
            );
            return;
        }

        if (CompletarPerfilViewModel.ERROR_GUARDAR_USUARIO.equals(error)) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_guardar_usuario
            );
            return;
        }

        mostrarError(
                binding.inputUsuarioClank.customEditText,
                R.string.completar_perfil_error_generico
        );
    }

    private void seleccionarFotoPerfil() {
        String[] opciones = {
                getString(R.string.completar_perfil_desde_galeria),
                getString(R.string.completar_perfil_desde_camara)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.completar_perfil_seleccionar_foto))
                .setItems(opciones, (dialogo, posicion) -> {
                    if (posicion == 0) {
                        abrirGaleria();
                    } else {
                        abrirCamaraConPermiso();
                    }
                })
                .show();
    }

    private void abrirGaleria() {
        lanzadorGaleria.launch("image/*");
    }

    private void abrirCamaraConPermiso() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
            return;
        }

        lanzadorPermisoCamara.launch(Manifest.permission.CAMERA);
    }

    private void abrirCamara() {
        try {
            File archivoFoto = File.createTempFile(
                    System.currentTimeMillis() + "_foto_perfil",
                    ".jpg",
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );

            uriFotoCamaraTemporal = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    archivoFoto
            );

            lanzadorCamara.launch(uriFotoCamaraTemporal);
        } catch (IOException error) {
            mostrarError(
                    binding.inputUsuarioClank.customEditText,
                    R.string.completar_perfil_error_crear_archivo_foto
            );
        }
    }

    private void mostrarPreviewFotoPerfil(Uri uri) {
        if (binding == null || uri == null) {
            return;
        }

        Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.ic_usuario_inactivo)
                .error(R.drawable.ic_usuario_inactivo)
                .into(binding.imgFotoPerfil);
    }

    private void restaurarPreviewFotoPerfil() {
        if (uriFotoPerfil != null) {
            mostrarPreviewFotoPerfil(uriFotoPerfil);
        }
    }

    private void precargarFotoPerfilGoogleSiProcede() {
        if (uriFotoPerfil != null) {
            return;
        }

        if (!vistaModeloCompartida.isRegistroConGoogle()) {
            return;
        }

        String fotoPerfilGoogle = vistaModeloCompartida.getFotoPerfilGoogle();

        if (fotoPerfilGoogle == null || fotoPerfilGoogle.trim().isEmpty()) {
            return;
        }

        Glide.with(this)
                .load(fotoPerfilGoogle)
                .circleCrop()
                .placeholder(R.drawable.ic_usuario_inactivo)
                .error(R.drawable.ic_usuario_inactivo)
                .into(binding.imgFotoPerfil);
    }

    private void mostrarError(EditText editText, int mensajeError) {
        editText.setError(getString(mensajeError));
        editText.requestFocus();
    }

    private void limpiarErroresFormulario() {
        binding.inputUsuarioClank.customEditText.setError(null);
    }

    private String getTexto(@Nullable CharSequence texto) {
        if (texto == null) {
            return "";
        }

        return texto.toString().trim();
    }

    private void mostrarPopupExito() {
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
        if (binding == null) {
            return;
        }

        boolean habilitado = !bloqueado;

        binding.navbar.btnNavbarVolver.setEnabled(habilitado);
        binding.btnEditarFotoPerfil.setEnabled(habilitado);
        binding.imgFotoPerfil.setEnabled(habilitado);
        binding.btnContinuar.btnSecundario.setEnabled(habilitado);
        binding.inputUsuarioClank.customEditText.setEnabled(habilitado);

        binding.btnContinuar.btnSecundario.setText(
                bloqueado
                        ? getString(R.string.completar_perfil_guardando)
                        : getString(R.string.continuar)
        );

        float transparencia = bloqueado ? 0.6f : 1f;
        binding.btnContinuar.btnSecundario.setAlpha(transparencia);
        binding.btnEditarFotoPerfil.setAlpha(transparencia);
        binding.imgFotoPerfil.setAlpha(transparencia);
    }

    private void volverPantallaAnterior() {
        NavController navegador = Navigation.findNavController(requireView());
        boolean haVuelto = navegador.popBackStack();

        if (!haVuelto) {
            navegador.navigate(R.id.action_completar_perfil_a_registro);
        }
    }

    private void navegarPantallaSiguiente() {
        if (binding == null) {
            return;
        }

        NavController navegador = Navigation.findNavController(requireView());

        if (navegador.getCurrentDestination() == null
                || navegador.getCurrentDestination().getId() != R.id.completarPerfilFragment) {
            return;
        }

        navegador.navigate(R.id.action_completar_perfil_a_feed);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle estadoSalida) {
        super.onSaveInstanceState(estadoSalida);

        if (uriFotoPerfil != null) {
            estadoSalida.putString(CLAVE_URI_FOTO_PERFIL, uriFotoPerfil.toString());
        }

        if (uriFotoCamaraTemporal != null) {
            estadoSalida.putString(CLAVE_URI_FOTO_CAMARA_TEMPORAL, uriFotoCamaraTemporal.toString());
        }
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
