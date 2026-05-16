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
import com.clank.app.databinding.FragmentCrearBinding;
import com.clank.app.ui.comun.HojaOpciones;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CrearFragment extends Fragment {

  private FragmentCrearBinding binding;
  private CrearViewModel viewModel;
  private int tiempoSeleccionado = -1;
  private View targetActivo = null;
  private Uri uriPortadaSeleccionada = null;
  private Uri uriFotoTemporal = null;



  ///////////////////////// launchers /////////////////////////
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



  ///////////////////////// on create /////////////////////////
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentCrearBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }



  ///////////////////////// on view created /////////////////////////
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel = new ViewModelProvider(this).get(CrearViewModel.class);
    configurarNavbar();
    configurarListeners(view);
    observarViewModel();
    anyadirFilaMaterial(false);
    anyadirFilaInstruccion();
  }


  ///////////////////////// navbar /////////////////////////
  private void configurarNavbar() {
    binding.navbar.tvNavbarTitulo.setText(getString(R.string.crear_titulo));
    binding.navbar.btnNavbarAccion.setImageResource(R.drawable.ic_delete);
    binding.navbar.btnNavbarAccion.setVisibility(View.VISIBLE);
  }


  ///////////////////////// listeners /////////////////////////
  private void configurarListeners(View v) {
    binding.navbar.btnNavbarVolver.setOnClickListener(b ->
            Navigation.findNavController(v).navigateUp());

    binding.navbar.btnNavbarAccion.setOnClickListener(b ->
            mostrarConfirmarEliminar(v));

    binding.framePortada.setOnClickListener(b ->
            mostrarDialogoSeleccionImagen(null));

    binding.btnPublicar.setOnClickListener(b ->
            intentarPublicar());

    binding.btnGuardarBoceto.setOnClickListener(b ->
            guardarBoceto());

    binding.btnTiempoCohete.setOnClickListener(b ->
            seleccionarTiempo(0));

    binding.btnTiempoLiebre.setOnClickListener(b ->
            seleccionarTiempo(1));

    binding.btnTiempoTortuga.setOnClickListener(b ->
            seleccionarTiempo(2));

    binding.btnAnyadirMaterial.setOnClickListener(b ->
            anyadirFilaMaterial(false));

    binding.btnAnyadirHerramienta.setOnClickListener(b ->
            anyadirFilaMaterial(true));

    binding.btnAnyadirInstruccion.setOnClickListener(b ->
            anyadirFilaInstruccion());
  }


  ///////////////////////// imagen /////////////////////////
  private void mostrarDialogoSeleccionImagen(@Nullable View target) {
    targetActivo = target;
    new AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.crear_seleccionar_imagen))
            .setItems(new CharSequence[]{
                    getString(R.string.crear_desde_galeria),
                    getString(R.string.crear_desde_camara)
            }, (dialog, which) -> {
              if (which == 0) galeriaLauncher.launch("image/*");
              else solicitarPermisoCamara();
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
      binding.ivPortadaPreview.setVisibility(View.VISIBLE);
      binding.llAnyadirPortada.setVisibility(View.GONE);
      Glide.with(this).load(uri).centerCrop().into(binding.ivPortadaPreview);
    } else {
      View fila = targetActivo;
      View boton = fila.findViewById(R.id.llBotonImagenInstruccion);
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
                View boton = fila.findViewById(R.id.llBotonImagenInstruccion);

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


  ///////////////////////// tiempo /////////////////////////
  private void seleccionarTiempo(int indice) {
    tiempoSeleccionado = indice;

    android.widget.ImageButton[] botones = {
            binding.btnTiempoCohete,
            binding.btnTiempoLiebre,
            binding.btnTiempoTortuga
    };

    for (int i = 0; i < botones.length; i++) {
      if (i == indice) {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_boton_principal));

        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
                R.color.clank_background_light));
      } else {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
                R.drawable.bg_boton_tiempo));

        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
                R.color.color_texto_inactivo));
      }
    }
  }

  private void restablecerBotonesTiempo() {
    android.widget.ImageButton[] botones = {
            binding.btnTiempoCohete,
            binding.btnTiempoLiebre,
            binding.btnTiempoTortuga
    };

    for (android.widget.ImageButton boton : botones) {
      boton.setBackground(ContextCompat.getDrawable(requireContext(),
              R.drawable.bg_boton_tiempo));

      boton.setImageTintList(ContextCompat.getColorStateList(requireContext(),
              R.color.color_texto_inactivo));
    }
  }


  ///////////////////////// añadir filas /////////////////////////
  private void anyadirFilaMaterial(boolean esHerramienta) {
    LinearLayout contenedor = esHerramienta
            ? binding.llContenedorHerramientas
            : binding.llContenedorMateriales;

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
            .inflate(R.layout.item_instruccion, binding.llContenedorInstrucciones, false);

    fila.findViewById(R.id.btnEliminarInstruccion).setOnClickListener(b -> {
      if (binding.llContenedorInstrucciones.getChildCount() > 1) {
        binding.llContenedorInstrucciones.removeView(fila);
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

    binding.llContenedorInstrucciones.addView(fila);
  }


  ///////////////////////// publicar /////////////////////////
  private void intentarPublicar() {
    String titulo = binding.etTitulo.getText().toString().trim();
    String descripcion = binding.etDescripcion.getText().toString().trim();

    if (titulo.isEmpty()) {
      binding.etTitulo.setError(getString(R.string.crear_error_titulo_vacio));
      binding.etTitulo.requestFocus();
      return;
    }

    if (descripcion.isEmpty()) {
      binding.etDescripcion.setError(getString(R.string.crear_error_descripcion_vacia));
      binding.etDescripcion.requestFocus();
      return;
    }

    if (tiempoSeleccionado == -1) {
      Toast.makeText(requireContext(),
              getString(R.string.crear_error_tiempo),
              Toast.LENGTH_SHORT).show();
      return;
    }

    if (!hayAlMenosUnMaterialValido()) {
      mostrarErrorPrimerMaterial();
      return;
    }

    if (!hayAlMenosUnaInstruccionValida()) {
      mostrarErrorPrimeraInstruccion();
      return;
    }

    mostrarConfirmarPublicar();
  }

  private void mostrarConfirmarPublicar() {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
            getString(R.string.crear_publicar_clank_titulo),
            getString(R.string.crear_publicar_clank_mensaje),
            getString(R.string.cancelar),
            getString(R.string.crear_publicar_clank_confirmar),
            () -> {
            },
            this::publicarClankConfirmado
    );

    hoja.show(getChildFragmentManager(), "hoja_publicar_clank");
  }

  private void publicarClankConfirmado() {
    String titulo = binding.etTitulo.getText().toString().trim();
    String descripcion = binding.etDescripcion.getText().toString().trim();

    List<String> textosInstrucciones = new ArrayList<>();
    List<Uri> imagenesInstrucciones = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorInstrucciones.getChildCount(); i++) {
      View fila = binding.llContenedorInstrucciones.getChildAt(i);

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


  ///////////////////////// hoja descartar clank /////////////////////////
  private void mostrarConfirmarEliminar(View v) {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
            getString(R.string.crear_descartar_clank_titulo),
            getString(R.string.crear_descartar_clank_mensaje),
            getString(R.string.cancelar),
            getString(R.string.crear_descartar_clank_confirmar),
            () -> {
              ////// al presionar cancelar la hoja se cierra y el formulario permanece intacto //////
            },
            () -> {
              limpiarFormularioCrear();
              Navigation.findNavController(v).navigate(R.id.action_crear_a_perfil);
            }
    );

    hoja.show(getChildFragmentManager(), "hoja_descartar_clank");
  }

  private void limpiarFormularioCrear() {
    ////// limpia los inputs y fotos //////
    binding.etTitulo.setText("");
    binding.etDescripcion.setText("");

    binding.etTitulo.setError(null);
    binding.etDescripcion.setError(null);

    uriPortadaSeleccionada = null;
    uriFotoTemporal = null;
    targetActivo = null;

    binding.ivPortadaPreview.setImageDrawable(null);
    binding.ivPortadaPreview.setVisibility(View.GONE);
    binding.llAnyadirPortada.setVisibility(View.VISIBLE);

    tiempoSeleccionado = -1;
    restablecerBotonesTiempo();

    binding.llContenedorMateriales.removeAllViews();
    anyadirFilaMaterial(false);

    binding.llContenedorHerramientas.removeAllViews();

    binding.llContenedorInstrucciones.removeAllViews();
    anyadirFilaInstruccion();

    limpiarCategoriasSeleccionadas();

    binding.btnPublicar.setEnabled(true);
  }

  private void limpiarCategoriasSeleccionadas() {
    for (int i = 0; i < binding.flexboxCategorias.getChildCount(); i++) {
      View chip = binding.flexboxCategorias.getChildAt(i);

      if (chip instanceof Button) {
        chip.setSelected(false);
        chip.setBackgroundResource(R.drawable.bg_boton_secundario);

        ((Button) chip).setTextColor(ContextCompat.getColor(
                requireContext(),
                R.color.color_texto_inactivo
        ));
      }
    }
  }


  ///////////////////////// recoger datos clank /////////////////////////
  private List<String[]> recogerMateriales() {
    List<String[]> lista = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorMateriales.getChildCount(); i++) {
      View fila = binding.llContenedorMateriales.getChildAt(i);

      String cant = ((EditText) fila.findViewById(R.id.etCantidad))
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

    for (int i = 0; i < binding.llContenedorHerramientas.getChildCount(); i++) {
      View fila = binding.llContenedorHerramientas.getChildAt(i);

      String nombre = ((EditText) fila.findViewById(R.id.etNombreElemento))
              .getText().toString().trim();

      if (!nombre.isEmpty()) lista.add(nombre);
    }

    return lista;
  }

  private List<String> recogerCategoriasSeleccionadas() {
    List<String> seleccionadas = new ArrayList<>();

    for (int i = 0; i < binding.flexboxCategorias.getChildCount(); i++) {
      View chip = binding.flexboxCategorias.getChildAt(i);

      if (chip.isSelected() && chip instanceof Button) {
        Object tag = chip.getTag();

        if (tag instanceof String) {
          seleccionadas.add((String) tag);
        }
      }
    }

    return seleccionadas;
  }


  ///////////////////////// observadores /////////////////////////
  private void observarViewModel() {
    viewModel.getEstadoPublicacion().observe(getViewLifecycleOwner(), estado -> {
      if (estado == null) return;

      switch (estado.estado) {
        case CARGANDO:
          binding.btnPublicar.setEnabled(false);
          break;

        case EXITO:
          binding.btnPublicar.setEnabled(true);
          Navigation.findNavController(requireView())
                  .navigate(R.id.action_crear_a_perfil);
          break;

        case ERROR:
          binding.btnPublicar.setEnabled(true);

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

    binding.flexboxCategorias.removeAllViews();

    for (String[] cat : categorias) {
      String catNombre = cat[1];

      Button chip = (Button) LayoutInflater.from(requireContext())
              .inflate(R.layout.bt_secundario, binding.flexboxCategorias, false);

      chip.setText(catNombre);
      chip.setTag(catNombre);

      ViewGroup.MarginLayoutParams lp =
              (ViewGroup.MarginLayoutParams) chip.getLayoutParams();
      lp.setMargins(0, 0, 8, 8);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(b -> {
        boolean activo = chip.isSelected();
        chip.setSelected(!activo);
        chip.setBackgroundResource(!activo ? R.drawable.bg_boton_principal : R.drawable.bg_boton_secundario);
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                !activo
                        ? R.color.clank_background_light
                        : R.color.color_texto_inactivo));
      });

      binding.flexboxCategorias.addView(chip);
    }
  }
  private boolean hayAlMenosUnMaterialValido() {
    for (int i = 0; i < binding.llContenedorMateriales.getChildCount(); i++) {
      View fila = binding.llContenedorMateriales.getChildAt(i);

      EditText etNombreMaterial = fila.findViewById(R.id.etNombreElemento);

      if (etNombreMaterial != null &&
              !etNombreMaterial.getText().toString().trim().isEmpty()) {
        return true;
      }
    }

    return false;
  }

  private void mostrarErrorPrimerMaterial() {
    if (binding.llContenedorMateriales.getChildCount() == 0) {
      return;
    }

    View primeraFila = binding.llContenedorMateriales.getChildAt(0);
    EditText etNombreMaterial = primeraFila.findViewById(R.id.etNombreElemento);

    if (etNombreMaterial != null) {
      etNombreMaterial.setError(getString(R.string.crear_error_min_material));
      etNombreMaterial.requestFocus();
    }
  }

  private boolean hayAlMenosUnaInstruccionValida() {
    for (int i = 0; i < binding.llContenedorInstrucciones.getChildCount(); i++) {
      View fila = binding.llContenedorInstrucciones.getChildAt(i);

      EditText etTextoInstruccion = fila.findViewById(R.id.etTextoInstruccion);

      if (etTextoInstruccion != null &&
              !etTextoInstruccion.getText().toString().trim().isEmpty()) {
        return true;
      }
    }

    return false;
  }

  private void mostrarErrorPrimeraInstruccion() {
    if (binding.llContenedorInstrucciones.getChildCount() == 0) {
      return;
    }

    View primeraFila = binding.llContenedorInstrucciones.getChildAt(0);
    EditText etTextoInstruccion = primeraFila.findViewById(R.id.etTextoInstruccion);

    if (etTextoInstruccion != null) {
      etTextoInstruccion.setError(getString(R.string.crear_error_min_instruccion));
      etTextoInstruccion.requestFocus();
    }
  }
}