package com.clank.app.ui.editarPerfil;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.databinding.FragmentEditarPerfilBinding;
import com.clank.app.ui.comun.NavbarHost;

import java.io.File;
import java.io.IOException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditarPerfilFragment extends Fragment {

  private static final String CLAVE_URI_FOTO_PERFIL = "uriFotoPerfil";
  private static final String CLAVE_URI_FOTO_CAMARA_TEMPORAL = "uriFotoCamaraTemporal";

  private FragmentEditarPerfilBinding binding;
  private EditarPerfilViewModel viewModel;
  private Uri uriFotoPerfil;
  private Uri uriFotoCamaraTemporal;

  private ActivityResultLauncher<String> launcherGaleria;
  private ActivityResultLauncher<Uri> launcherCamara;
  private ActivityResultLauncher<String> launcherPermisoCamara;

  public EditarPerfilFragment() {
  }

  ///////////////////////// ciclo de vida /////////////////////////

  @Override
  public void onCreate(@Nullable Bundle estadoGuardado) {
    super.onCreate(estadoGuardado);

    if (estadoGuardado != null) {
      String fotoPerfilGuardada =
              estadoGuardado.getString(CLAVE_URI_FOTO_PERFIL);

      String fotoCamaraGuardada =
              estadoGuardado.getString(CLAVE_URI_FOTO_CAMARA_TEMPORAL);

      if (fotoPerfilGuardada != null && !fotoPerfilGuardada.isEmpty()) {
        uriFotoPerfil = Uri.parse(fotoPerfilGuardada);
      }

      if (fotoCamaraGuardada != null && !fotoCamaraGuardada.isEmpty()) {
        uriFotoCamaraTemporal = Uri.parse(fotoCamaraGuardada);
      }
    }

    configurarLaunchersImagen();
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup contenedor,
                           Bundle estadoGuardado) {
    binding = FragmentEditarPerfilBinding.inflate(inflater, contenedor, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View vista,
                            @Nullable Bundle estadoGuardado) {
    super.onViewCreated(vista, estadoGuardado);

    viewModel = new ViewModelProvider(this).get(EditarPerfilViewModel.class);

    configurarFormulario();
    configurarPopup();
    configurarListeners();
    observarViewModel();
    restaurarPreviewFotoPerfil();

    if (viewModel.getPerfil().getValue() == null) {
      viewModel.cargarUsuario();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    configurarNavbar();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle estadoSalida) {
    super.onSaveInstanceState(estadoSalida);

    if (uriFotoPerfil != null) {
      estadoSalida.putString(
              CLAVE_URI_FOTO_PERFIL,
              uriFotoPerfil.toString()
      );
    }

    if (uriFotoCamaraTemporal != null) {
      estadoSalida.putString(
              CLAVE_URI_FOTO_CAMARA_TEMPORAL,
              uriFotoCamaraTemporal.toString()
      );
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  ///////////////////////// navbar /////////////////////////

  private void configurarNavbar() {
    ((NavbarHost) requireActivity())
            .mostrarNavbarConVolver(
                    getString(R.string.editar_perfil_titulo)
            );
  }

  ///////////////////////// formulario /////////////////////////

  private void configurarFormulario() {
    // Nombre
    binding.inputNombre.tvInputTitulo.setText(
            getString(R.string.nombre_completo)
    );

    binding.inputNombre.customEditText.setHint(
            getString(R.string.perfil_hint_nombre)
    );

    binding.inputNombre.customEditText.setInputType(
            InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME
    );

    binding.inputNombre.inputTrailingIcon.setVisibility(View.GONE);

    // clank User
    binding.inputUsuarioClank.tvInputTitulo.setText(
            getString(R.string.clank_user)
    );

    binding.inputUsuarioClank.customEditText.setHint(
            getString(R.string.ejemplo_clank_user)
    );

    binding.inputUsuarioClank.customEditText.setInputType(
            InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_NORMAL
    );

    binding.inputUsuarioClank.inputTrailingIcon.setVisibility(View.GONE);

    // teléfono
    binding.inputTelefono.tvInputTitulo.setText(
            getString(R.string.numero_telefono)
    );

    binding.inputTelefono.customEditText.setHint(
            getString(R.string.ejemplo_telefono)
    );

    binding.inputTelefono.customEditText.setInputType(
            InputType.TYPE_CLASS_PHONE
    );

    binding.inputTelefono.inputTrailingIcon.setVisibility(View.GONE);

    // correo (no editable)
    binding.inputCorreo.tvInputTitulo.setText(
            getString(R.string.correo_electronico)
    );

    binding.inputCorreo.customEditText.setHint(
            getString(R.string.ejemplo_correo)
    );

    binding.inputCorreo.customEditText.setEnabled(false);
    binding.inputCorreo.customEditText.setAlpha(0.5f);
    binding.inputCorreo.inputTrailingIcon.setVisibility(View.GONE);

    // fecha de nacimiento (no editable)
    binding.inputFechaNacimiento.tvInputTitulo.setText(
            getString(R.string.fecha_nacimiento)
    );

    binding.inputFechaNacimiento.customEditText.setHint(
            getString(R.string.ejemplo_fecha_nacimiento)
    );

    binding.inputFechaNacimiento.customEditText.setEnabled(false);
    binding.inputFechaNacimiento.customEditText.setAlpha(0.5f);
    binding.inputFechaNacimiento.inputTrailingIcon.setVisibility(View.GONE);

    // guardar
    binding.btnGuardar.btnPrincipal.setText(
            getString(R.string.editar_perfil_guardar)
    );

    binding.tvCambiarContrasenya.setPaintFlags(
            binding.tvCambiarContrasenya.getPaintFlags() |
                    android.graphics.Paint.UNDERLINE_TEXT_FLAG
    );
  }

  private void configurarPopup() {
    binding.capaPopup.setVisibility(View.GONE);

    binding.popup.tvTituloPopup.setText(
            getString(R.string.popup_editar_perfil_titulo)
    );

    binding.popup.tvMensajePopup.setText(
            getString(R.string.popup_editar_perfil_mensaje)
    );

    binding.popup.imgIconoPopup.setImageResource(
            R.drawable.ic_check_inactivo
    );

    binding.popup.imgIconoPopup.setContentDescription(
            getString(R.string.popup_editar_perfil_icono_desc)
    );

    binding.popup.contenedorBotonPopup.setVisibility(View.VISIBLE);

    binding.popup.btnPopupAccion.btnSecundario.setText(
            getString(R.string.continuar)
    );
  }

  ///////////////////////// listeners /////////////////////////

  private void configurarListeners() {
    binding.btnEditarFotoPerfil.setOnClickListener(vista ->
            seleccionarFotoPerfil());

    binding.imgFotoPerfil.setOnClickListener(vista ->
            seleccionarFotoPerfil());

    binding.tvCambiarContrasenya.setOnClickListener(vista ->
            Navigation.findNavController(vista)
                    .navigate(R.id.action_editar_perfil_a_cambiar_contrasenya));

    binding.btnGuardar.btnPrincipal.setOnClickListener(vista ->
            procesarGuardarCambios());
  }

  ///////////////////////// imagen /////////////////////////

  private void configurarLaunchersImagen() {
    launcherGaleria = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
              if (uri == null) {
                return;
              }

              uriFotoPerfil = uri;
              mostrarPreviewFotoPerfil(uriFotoPerfil);
            }
    );

    launcherCamara = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            resultado -> {
              if (Boolean.TRUE.equals(resultado) &&
                      uriFotoCamaraTemporal != null) {
                uriFotoPerfil = uriFotoCamaraTemporal;
                mostrarPreviewFotoPerfil(uriFotoPerfil);
              }
            }
    );

    launcherPermisoCamara = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            permisoConcedido -> {
              if (binding == null) {
                return;
              }

              if (Boolean.TRUE.equals(permisoConcedido)) {
                abrirCamara();
              } else {
                mostrarError(
                        binding.inputNombre.customEditText,
                        R.string.error_permiso_camara
                );
              }
            }
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
                launcherGaleria.launch("image/*");
              } else {
                abrirCamaraConPermiso();
              }
            })
            .show();
  }

  private void abrirCamaraConPermiso() {
    if (ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED) {
      abrirCamara();
      return;
    }

    launcherPermisoCamara.launch(Manifest.permission.CAMERA);
  }

  private void abrirCamara() {
    try {
      File archivoFoto = File.createTempFile(
              System.currentTimeMillis() + "_foto_perfil",
              ".jpg",
              requireContext().getExternalFilesDir(
                      Environment.DIRECTORY_PICTURES
              )
      );

      uriFotoCamaraTemporal = FileProvider.getUriForFile(
              requireContext(),
              requireContext().getPackageName() + ".fileprovider",
              archivoFoto
      );

      launcherCamara.launch(uriFotoCamaraTemporal);

    } catch (IOException error) {
      mostrarError(
              binding.inputNombre.customEditText,
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
            .placeholder(R.drawable.img_usuario_defecto)
            .error(R.drawable.ic_usuario_inactivo)
            .into(binding.imgFotoPerfil);
  }

  private void restaurarPreviewFotoPerfil() {
    if (uriFotoPerfil != null) {
      mostrarPreviewFotoPerfil(uriFotoPerfil);
    }
  }

  ///////////////////////// guardar /////////////////////////

  private void procesarGuardarCambios() {
    String nombre = getTexto(binding.inputNombre.customEditText.getText());
    String usuarioClank = getTexto(binding.inputUsuarioClank.customEditText.getText());
    String telefono = getTexto(binding.inputTelefono.customEditText.getText());

    limpiarErrores();

    if (nombre.isEmpty()) {
      mostrarError(
              binding.inputNombre.customEditText,
              R.string.registro_error_nombre_vacio
      );
      return;
    }

    if (usuarioClank.isEmpty()) {
      mostrarError(
              binding.inputUsuarioClank.customEditText,
              R.string.completar_perfil_error_usuario_clank_vacio
      );
      return;
    }

    viewModel.guardarCambios(
            nombre,
            usuarioClank,
            telefono,
            uriFotoPerfil
    );
  }

  ///////////////////////// observadores /////////////////////////

  private void observarViewModel() {
    viewModel.getPerfil().observe(getViewLifecycleOwner(), usuario -> {
      if (usuario == null) {
        return;
      }

      binding.inputNombre.customEditText.setText(
              usuario.getNombre()
      );

      binding.inputUsuarioClank.customEditText.setText(
              usuario.getUsuarioClank()
      );

      binding.inputTelefono.customEditText.setText(
              usuario.getTelefono()
      );

      binding.inputCorreo.customEditText.setText(
              usuario.getCorreo()
      );

      binding.inputFechaNacimiento.customEditText.setText(
              usuario.getFechaNacimiento()
      );

      if (uriFotoPerfil == null &&
              usuario.getFotoPerfil() != null &&
              !usuario.getFotoPerfil().isEmpty()) {

        Glide.with(this)
                .load(usuario.getFotoPerfil())
                .circleCrop()
                .placeholder(R.drawable.img_usuario_defecto)
                .error(R.drawable.img_usuario_defecto)
                .into(binding.imgFotoPerfil);
      }
    });

    viewModel.getEstado().observe(getViewLifecycleOwner(), recurso -> {
      if (recurso == null) {
        return;
      }

      switch (recurso.estado) {
        case CARGANDO:
          limpiarErrores();
          bloquearFormulario(true);
          break;

        case EXITO:
          bloquearFormulario(false);
          mostrarPopupExito();
          break;

        case ERROR:
          bloquearFormulario(false);
          gestionarError(recurso.mensaje);
          break;
      }
    });
  }

  private void gestionarError(String error) {
    if (EditarPerfilViewModel.ERROR_USUARIO_CLANK_EXISTE.equals(error)) {
      mostrarError(
              binding.inputUsuarioClank.customEditText,
              R.string.completar_perfil_error_usuario_clank_existente
      );
      return;
    }

    if (EditarPerfilViewModel.ERROR_SUBIR_FOTO.equals(error)) {
      mostrarError(
              binding.inputNombre.customEditText,
              R.string.completar_perfil_error_subir_foto
      );
      return;
    }

    if (EditarPerfilViewModel.ERROR_GUARDAR_USUARIO.equals(error)) {
      mostrarError(
              binding.inputNombre.customEditText,
              R.string.completar_perfil_error_guardar_usuario
      );
      return;
    }

    if (EditarPerfilViewModel.ERROR_CARGAR_USUARIO.equals(error)) {
      mostrarError(
              binding.inputNombre.customEditText,
              R.string.error_generico
      );
      return;
    }

    mostrarError(
            binding.inputNombre.customEditText,
            R.string.error_generico
    );
  }

  ///////////////////////// popup /////////////////////////

  private void mostrarPopupExito() {
    binding.capaPopup.setVisibility(View.VISIBLE);
    bloquearFormulario(true);

    binding.popup.btnPopupAccion.btnSecundario.setOnClickListener(vista -> {
      if (binding == null) {
        return;
      }

      binding.capaPopup.setVisibility(View.GONE);
      bloquearFormulario(false);

      Navigation.findNavController(requireView()).navigateUp();
    });
  }

  ///////////////////////// bloqueo de formulario /////////////////////////

  private void bloquearFormulario(boolean bloqueado) {
    if (binding == null) {
      return;
    }

    boolean habilitado = !bloqueado;

    ((NavbarHost) requireActivity())
            .habilitarVolverNavbar(habilitado);

    binding.btnEditarFotoPerfil.setEnabled(habilitado);
    binding.imgFotoPerfil.setEnabled(habilitado);
    binding.btnGuardar.btnPrincipal.setEnabled(habilitado);
    binding.inputNombre.customEditText.setEnabled(habilitado);
    binding.inputUsuarioClank.customEditText.setEnabled(habilitado);
    binding.inputTelefono.customEditText.setEnabled(habilitado);
    binding.tvCambiarContrasenya.setEnabled(habilitado);

    binding.btnGuardar.btnPrincipal.setText(
            bloqueado
                    ? getString(R.string.completar_perfil_guardando)
                    : getString(R.string.editar_perfil_guardar)
    );

    float transparencia = bloqueado ? 0.6f : 1f;

    binding.btnGuardar.btnPrincipal.setAlpha(transparencia);
    binding.btnEditarFotoPerfil.setAlpha(transparencia);
    binding.imgFotoPerfil.setAlpha(transparencia);
  }

  ///////////////////////// utilidades /////////////////////////

  private void mostrarError(EditText editText, int mensajeError) {
    editText.setError(getString(mensajeError));
    editText.requestFocus();
  }

  private void limpiarErrores() {
    binding.inputNombre.customEditText.setError(null);
    binding.inputUsuarioClank.customEditText.setError(null);
    binding.inputTelefono.customEditText.setError(null);
  }

  private String getTexto(@Nullable CharSequence texto) {
    if (texto == null) {
      return "";
    }

    return texto.toString().trim();
  }
}