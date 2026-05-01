package com.clank.app.ui.crear;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.util.Recurso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CrearFragment extends Fragment {

  private CrearViewModel viewModel;

  /////////////////////////navbar pdte crear elemento comun/////////////////////////
  private ImageButton btnCrearVolver;
  private ImageButton btnCrearEliminar;
  private Button btnPublicar;
  private Button btnGuardarBoceto;
  private FrameLayout framePortada;
  private ImageView ivPortadaPreview;
  private View llAnyadirPortada;
  private Uri uriPortadaSeleccionada;
  private EditText etTitulo;
  private EditText etDescripcion;
  private ImageButton btnTiempoCohete;
  private ImageButton btnTiempoLiebre;
  private ImageButton btnTiempoTortuga;
  private int tiempoSeleccionado = -1;
  private LinearLayout llContenedorMateriales;
  private LinearLayout llContenedorHerramientas;
  private LinearLayout llContenedorInstrucciones;
  private Button btnAnyadirMaterial;
  private Button btnAnyadirHerramienta;
  private Button btnAnyadirInstruccion;
  private com.google.android.flexbox.FlexboxLayout flexboxCategorias;
  private View targetActivo = null;
  private Uri uriFotoTemporal = null;

  /////////////////////////launchers/////////////////////////


  private final ActivityResultLauncher<String> galeriaLauncher =
          registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) procesarImagenSeleccionada(uri);
          });

  private final ActivityResultLauncher<Uri> camaraLauncher =
          registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (Boolean.TRUE.equals(success) && uriFotoTemporal != null)
              procesarImagenSeleccionada(uriFotoTemporal);
          });

  private final ActivityResultLauncher<String> permisoCamaraLauncher =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (Boolean.TRUE.equals(granted)) abrirCamara();
            else Toast.makeText(requireContext(),
                    getString(R.string.error_permiso_camara), Toast.LENGTH_SHORT).show();
          });

  /////////////////////////On create/////////////////////////


  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_crear, container, false);
  }

  /////////////////////////on View Created/////////////////////////


  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(CrearViewModel.class);
    enlazarVistas(view);
    establecerTextosBotones();
    configurarListeners(view);
    observarViewModel();
    anyadirFilaMaterial(false);
    anyadirFilaInstruccion();
  }

  /////////////////////////binding. Migrar a view binding?/////////////////////////
  private void enlazarVistas(View v) {
    btnCrearVolver            = v.findViewById(R.id.btnCrearVolver);
    btnCrearEliminar          = v.findViewById(R.id.btnCrearEliminar);
    btnPublicar               = v.findViewById(R.id.btnPublicar);
    btnGuardarBoceto          = v.findViewById(R.id.btnGuardarBoceto);
    framePortada              = v.findViewById(R.id.framePortada);
    ivPortadaPreview          = v.findViewById(R.id.ivPortadaPreview);
    llAnyadirPortada           = v.findViewById(R.id.llAnyadirPortada);
    etTitulo                  = v.findViewById(R.id.etTitulo);
    etDescripcion             = v.findViewById(R.id.etDescripcion);
    btnTiempoCohete           = v.findViewById(R.id.btnTiempoCohete);
    btnTiempoLiebre           = v.findViewById(R.id.btnTiempoLiebre);
    btnTiempoTortuga          = v.findViewById(R.id.btnTiempoTortuga);
    llContenedorMateriales    = v.findViewById(R.id.llContenedorMateriales);
    llContenedorHerramientas  = v.findViewById(R.id.llContenedorHerramientas);
    llContenedorInstrucciones = v.findViewById(R.id.llContenedorInstrucciones);
    btnAnyadirMaterial         = v.findViewById(R.id.btnAnyadirMaterial);
    btnAnyadirHerramienta      = v.findViewById(R.id.btnAnyadirHerramienta);
    btnAnyadirInstruccion      = v.findViewById(R.id.btnAnyadirInstruccion);
    flexboxCategorias         = v.findViewById(R.id.flexboxCategorias);
  }

  private void establecerTextosBotones() {
    btnPublicar.setText(getString(R.string.crear_publicar));
    btnGuardarBoceto.setText(getString(R.string.crear_guardar_boceto));
    btnAnyadirMaterial.setText(getString(R.string.crear_anyadir_material));
    btnAnyadirHerramienta.setText(getString(R.string.crear_anyadir_herramienta));
    btnAnyadirInstruccion.setText(getString(R.string.crear_anyadir_instruccion));
  }

  /////////////////////////listeners/////////////////////////

  private void configurarListeners(View v) {
    btnCrearVolver.setOnClickListener(b ->
            Navigation.findNavController(v).navigateUp());

    btnCrearEliminar.setOnClickListener(b -> mostrarConfirmarEliminar(v));

    framePortada.setOnClickListener(b -> mostrarDialogoSeleccionImagen(null));

    btnPublicar.setOnClickListener(b -> intentarPublicar(v));
    btnGuardarBoceto.setOnClickListener(b -> guardarBoceto());

    btnTiempoCohete.setOnClickListener(b  -> seleccionarTiempo(0));
    btnTiempoLiebre.setOnClickListener(b  -> seleccionarTiempo(1));
    btnTiempoTortuga.setOnClickListener(b -> seleccionarTiempo(2));

    btnAnyadirMaterial.setOnClickListener(b    -> anyadirFilaMaterial(false));
    btnAnyadirHerramienta.setOnClickListener(b -> anyadirFilaMaterial(true));
    btnAnyadirInstruccion.setOnClickListener(b -> anyadirFilaInstruccion());
  }

  /////////////////////////imagen/////////////////////////

  private void mostrarDialogoSeleccionImagen(@Nullable View target) {
    targetActivo = target;
    new AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.crear_seleccionar_imagen))
            .setItems(new CharSequence[]{
                    getString(R.string.crear_desde_galeria),
                    getString(R.string.crear_desde_camara)
            }, (dialog, which) -> {
              if (which == 0) galeriaLauncher.launch("image/*");
              else            solicitarPermisoCamara();
            })
            .show();
  }

  private void solicitarPermisoCamara() {
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
      abrirCamara();
    } else {
      permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
    }
  }

  private void abrirCamara() {
    try {
      File dir = requireContext().getExternalFilesDir(
              android.os.Environment.DIRECTORY_PICTURES);
      if (dir != null) dir.mkdirs();
      File archivo = new File(dir, "foto_" + UUID.randomUUID() + ".jpg");
      uriFotoTemporal = FileProvider.getUriForFile(
              requireContext(),
              requireContext().getPackageName() + ".fileprovider",
              archivo);
      camaraLauncher.launch(uriFotoTemporal);
    } catch (Exception e) {
      Toast.makeText(requireContext(),
              getString(R.string.error_camara), Toast.LENGTH_SHORT).show();
    }
  }

  private void procesarImagenSeleccionada(Uri uri) {
    if (targetActivo == null) {
      uriPortadaSeleccionada = uri;
      ivPortadaPreview.setVisibility(View.VISIBLE);
      llAnyadirPortada.setVisibility(View.GONE);
      Glide.with(this).load(uri).centerCrop().into(ivPortadaPreview);
    } else {
      View fila         = targetActivo;
      View boton        = fila.findViewById(R.id.llBotonImagenInstruccion);
      ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);
      fila.setTag(R.id.ivPreviewInstruccion, uri);
      boton.setVisibility(View.GONE);
      preview.setVisibility(View.VISIBLE);
      Glide.with(this).load(uri).centerCrop().into(preview);
    }
  }

  private void mostrarDialogoAccionesImagen(Uri uri, View fila) {
    new AlertDialog.Builder(requireContext())
            .setItems(new CharSequence[]{
                    getString(R.string.crear_ver_imagen),
                    getString(R.string.crear_eliminar_imagen)
            }, (dialog, which) -> {
              if (which == 0) {
                mostrarImagenCompleta(uri);
              } else {
                ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);
                View boton        = fila.findViewById(R.id.llBotonImagenInstruccion);
                fila.setTag(R.id.ivPreviewInstruccion, null);
                preview.setImageDrawable(null);
                preview.setVisibility(View.GONE);
                boton.setVisibility(View.VISIBLE);
              }
            })
            .show();
  }

  private void mostrarImagenCompleta(Uri uri) {
    Dialog dialog = new Dialog(requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    ImageView iv = new ImageView(requireContext());
    iv.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
    iv.setBackgroundColor(Color.BLACK);
    Glide.with(this).load(uri).into(iv);
    iv.setOnClickListener(v -> dialog.dismiss());
    dialog.setContentView(iv);
    dialog.show();
  }

  ///////////////////////// Tiempo enum/////////////////////////


  private void seleccionarTiempo(int indice) {
    tiempoSeleccionado = indice;
    ImageButton[] botones = {btnTiempoCohete, btnTiempoLiebre, btnTiempoTortuga};
    for (int i = 0; i < botones.length; i++) {
      if (i == indice) {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_boton_principal));
        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
                R.color.clank_background_light));
      } else {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_tiempo_redondo));
        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
                R.color.color_texto_inactivo));
      }
    }
  }

  /////////////////////////Añadir filas/////////////////////////


  private void anyadirFilaMaterial(boolean esHerramienta) {
    LinearLayout contenedor = esHerramienta
            ? llContenedorHerramientas : llContenedorMateriales;
    View fila = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_material, contenedor, false);

    if (esHerramienta) {
      fila.findViewById(R.id.etCantidad).setVisibility(View.GONE);
      ((EditText) fila.findViewById(R.id.etNombreElemento))
              .setHint(getString(R.string.crear_hint_herramienta));
    }

    fila.findViewById(R.id.btnEliminarElemento).setOnClickListener(b -> {
      if (contenedor.getChildCount() > 1 || esHerramienta) {
        contenedor.removeView(fila);
      } else {
        Toast.makeText(requireContext(),
                getString(R.string.crear_error_min_material), Toast.LENGTH_SHORT).show();
      }
    });

    contenedor.addView(fila);
  }

  private void anyadirFilaInstruccion() {
    View fila = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_instruccion, llContenedorInstrucciones, false);

    fila.findViewById(R.id.btnEliminarInstruccion).setOnClickListener(b -> {
      if (llContenedorInstrucciones.getChildCount() > 1) {
        llContenedorInstrucciones.removeView(fila);
      } else {
        Toast.makeText(requireContext(),
                getString(R.string.crear_error_min_instruccion), Toast.LENGTH_SHORT).show();
      }
    });

    View botonImagen = fila.findViewById(R.id.llBotonImagenInstruccion);
    botonImagen.setOnClickListener(b -> mostrarDialogoSeleccionImagen(fila));

    ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);
    preview.setOnClickListener(b -> {
      Uri uri = (Uri) fila.getTag(R.id.ivPreviewInstruccion);
      if (uri != null) mostrarDialogoAccionesImagen(uri, fila);
    });

    llContenedorInstrucciones.addView(fila);
  }

  /////////////////////////publicar/////////////////////////
  private void intentarPublicar(View v) {
    String titulo      = etTitulo.getText().toString().trim();
    String descripcion = etDescripcion.getText().toString().trim();

    if (titulo.isEmpty()) {
      etTitulo.setError(getString(R.string.crear_error_titulo_vacio));
      etTitulo.requestFocus();
      return;
    }
    if (descripcion.isEmpty()) {
      etDescripcion.setError(getString(R.string.crear_error_descripcion_vacia));
      etDescripcion.requestFocus();
      return;
    }
    if (tiempoSeleccionado == -1) {
      Toast.makeText(requireContext(),
              getString(R.string.crear_error_tiempo), Toast.LENGTH_SHORT).show();
      return;
    }

    List<String> textosInstrucciones   = new ArrayList<>();
    List<Uri>    imagenesInstrucciones = new ArrayList<>();

    for (int i = 0; i < llContenedorInstrucciones.getChildCount(); i++) {
      View fila    = llContenedorInstrucciones.getChildAt(i);
      String texto = ((EditText) fila.findViewById(R.id.etTextoInstruccion))
              .getText().toString().trim();
      if (!texto.isEmpty()) {
        textosInstrucciones.add(texto);
        imagenesInstrucciones.add((Uri) fila.getTag(R.id.ivPreviewInstruccion));
      }
    }

    viewModel.publicarClank(
            titulo,
            descripcion,
            tiempoSeleccionado,
            uriPortadaSeleccionada,
            recogerMateriales(),
            recogerHerramientas(),
            textosInstrucciones,
            imagenesInstrucciones,
            recogerCategoriasSeleccionadas()
    );
  }

  private void guardarBoceto() {
    Toast.makeText(requireContext(),
            getString(R.string.crear_boceto_proximamente), Toast.LENGTH_SHORT).show();
  }

  private void mostrarConfirmarEliminar(View v) {
    new AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.crear_confirmar_descartar_titulo))
            .setMessage(getString(R.string.crear_confirmar_descartar_mensaje))
            .setPositiveButton(getString(R.string.crear_descartar), (d, w) ->
                    Navigation.findNavController(v).navigateUp())
            .setNegativeButton(getString(R.string.cancelar), null)
            .show();
  }

  /////////////////////////Recoger datos clank/////////////////////////


  private List<String[]> recogerMateriales() {
    List<String[]> lista = new ArrayList<>();
    for (int i = 0; i < llContenedorMateriales.getChildCount(); i++) {
      View fila     = llContenedorMateriales.getChildAt(i);
      String cant   = ((EditText) fila.findViewById(R.id.etCantidad))
              .getText().toString().trim();
      String nombre = ((EditText) fila.findViewById(R.id.etNombreElemento))
              .getText().toString().trim();
      if (cant.isEmpty()) cant = "1";
      if (!nombre.isEmpty()) lista.add(new String[]{cant, nombre});
    }
    return lista;
  }

  private List<String> recogerHerramientas() {
    List<String> lista = new ArrayList<>();
    for (int i = 0; i < llContenedorHerramientas.getChildCount(); i++) {
      View fila     = llContenedorHerramientas.getChildAt(i);
      String nombre = ((EditText) fila.findViewById(R.id.etNombreElemento))
              .getText().toString().trim();
      if (!nombre.isEmpty()) lista.add(nombre);
    }
    return lista;
  }

  private List<String> recogerCategoriasSeleccionadas() {
    List<String> seleccionadas = new ArrayList<>();
    for (int i = 0; i < flexboxCategorias.getChildCount(); i++) {
      View chip = flexboxCategorias.getChildAt(i);
      if (chip.isSelected() && chip instanceof Button) {
        Object tag = chip.getTag();
        if (tag instanceof String) seleccionadas.add((String) tag);
      }
    }
    return seleccionadas;
  }

  /////////////////////////Observadores/////////////////////////


  private void observarViewModel() {
    viewModel.getEstadoPublicacion().observe(getViewLifecycleOwner(), estado -> {
      if (estado == null) return;
      switch (estado.estado) {
        case CARGANDO:
          btnPublicar.setEnabled(false);
          break;
        case EXITO:
          /// PENDIENTE: debe ir al feed
          break;
        case ERROR:
          btnPublicar.setEnabled(true);
          String msg = estado.mensaje != null
                  ? estado.mensaje
                  : getString(R.string.crear_error_publicar);
          Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
          break;
      }
    });

    viewModel.getCategorias().observe(getViewLifecycleOwner(),
            this::cargarChipsCategorias);
  }


  private void cargarChipsCategorias(List<String[]> categorias) {
    if (categorias == null || categorias.isEmpty()) return;
    flexboxCategorias.removeAllViews();
    for (String[] cat : categorias) {
      String catId     = cat[0];
      String catNombre = cat[1];

      Button chip = (Button) LayoutInflater.from(requireContext())
              .inflate(R.layout.bt_secundario, flexboxCategorias, false);
      chip.setText(catNombre);
      chip.setTag(catId);

      ViewGroup.MarginLayoutParams lp =
              (ViewGroup.MarginLayoutParams) chip.getLayoutParams();
      lp.setMargins(0, 0, 8, 8);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(b -> {
        boolean activo = chip.isSelected();
        chip.setSelected(!activo);
        chip.setBackgroundResource(
                !activo ? R.drawable.bg_boton_principal
                        : R.drawable.bg_boton_secundario);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                !activo ? R.color.clank_background_light
                        : R.color.color_texto_inactivo));
      });

      flexboxCategorias.addView(chip);
    }
  }
}
