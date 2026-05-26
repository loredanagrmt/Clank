package com.clank.app.ui.editar;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.databinding.FragmentCrearBinding;
import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.NavbarHost;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditarClankFragment extends Fragment {

  private static final String ARG_CLANK_ID = "clankId";

  private FragmentCrearBinding binding;
  private EditarClankViewModel viewModel;
  private String clankId;
  private int tiempoSeleccionado = -1;
  private View targetActivo = null;
  private Uri uriPortadaNueva = null;
  private String urlPortadaActual = "";
  private Uri uriFotoTemporal = null;

  // si la última acción fue publicar (true) o guardar boceto (false)
  private boolean esPublicacion = false;

  ///////////////////////// instancia /////////////////////////

  public static EditarClankFragment newInstance(String clankId) {
    EditarClankFragment f = new EditarClankFragment();
    Bundle args = new Bundle();
    args.putString(ARG_CLANK_ID, clankId);
    f.setArguments(args);
    return f;
  }

  ///////////////////////// launchers /////////////////////////

  private final ActivityResultLauncher<String> galeriaLauncher =
          registerForActivityResult(
                  new ActivityResultContracts.GetContent(),
                  uri -> {
                    if (uri != null) {
                      procesarImagenSeleccionada(uri);
                    }
                  }
          );

  private final ActivityResultLauncher<Uri> camaraLauncher =
          registerForActivityResult(
                  new ActivityResultContracts.TakePicture(),
                  success -> {
                    if (Boolean.TRUE.equals(success) && uriFotoTemporal != null) {
                      procesarImagenSeleccionada(uriFotoTemporal);
                    }
                  }
          );

  private final ActivityResultLauncher<String> permisoCamaraLauncher =
          registerForActivityResult(
                  new ActivityResultContracts.RequestPermission(),
                  granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                      abrirCamara();
                    } else {
                      binding.etTitulo.setError(getString(R.string.error_permiso_camara));
                      binding.etTitulo.requestFocus();
                    }
                  }
          );

  ///////////////////////// ciclo de vida /////////////////////////

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentCrearBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view,
                            @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(EditarClankViewModel.class);

    if (getArguments() != null) {
      clankId = getArguments().getString(ARG_CLANK_ID, "");
    }

    configurarListeners(view);
    configurarBotonAtras(view);
    observarViewModel();

    viewModel.cargarClank(clankId);
  }

  @Override
  public void onResume() {
    super.onResume();
    configurarNavbar();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  ///////////////////////// navbar /////////////////////////

  private void configurarNavbar() {
    NavbarHost host = (NavbarHost) requireActivity();

    host.mostrarNavbarConVolver(
            getString(R.string.editar_titulo),
            R.drawable.ic_delete_inactivo,
            v -> mostrarConfirmacionEliminarClank()
    );

    host.configurarAccionVolver(v ->
            mostrarConfirmacionSalirDeEdicion()
    );
  }

  ///////////////////////// botón atrás /////////////////////////

  private void configurarBotonAtras(View v) {
    requireActivity().getOnBackPressedDispatcher().addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                mostrarConfirmacionSalirDeEdicion();
              }
            }
    );
  }

  ///////////////////////// listeners /////////////////////////

  private void configurarListeners(View v) {
    binding.framePortada.setOnClickListener(b -> {
      mostrarDialogoSeleccionImagen(null);
      viewModel.marcarCambios();
    });

    binding.btnPublicar.setEnabled(false);
    binding.btnPublicar.setAlpha(0.5f);

    binding.btnPublicar.setOnClickListener(b ->
            intentarPublicar(v));

    binding.btnGuardarBoceto.setOnClickListener(b ->
            guardarBoceto(v));

    binding.btnTiempoCohete.setOnClickListener(b -> {
      seleccionarTiempo(0);
      viewModel.marcarCambios();
    });

    binding.btnTiempoLiebre.setOnClickListener(b -> {
      seleccionarTiempo(1);
      viewModel.marcarCambios();
    });

    binding.btnTiempoTortuga.setOnClickListener(b -> {
      seleccionarTiempo(2);
      viewModel.marcarCambios();
    });

    binding.btnAnyadirMaterial.setOnClickListener(b -> {
      anyadirFilaMaterial(false);
      viewModel.marcarCambios();
    });

    binding.btnAnyadirHerramienta.setOnClickListener(b -> {
      anyadirFilaMaterial(true);
      viewModel.marcarCambios();
    });

    binding.btnAnyadirInstruccion.setOnClickListener(b -> {
      anyadirFilaInstruccion();
      viewModel.marcarCambios();
    });

    vigilarCambiosTexto(binding.etTitulo);
    vigilarCambiosTexto(binding.etDescripcion);
  }

  private void vigilarCambiosTexto(EditText et) {
    et.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s,
                                    int st,
                                    int c,
                                    int a) {
      }

      @Override
      public void onTextChanged(CharSequence s,
                                int st,
                                int b,
                                int c) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        viewModel.marcarCambios();
        actualizarEstadoBotonPublicar();
      }
    });
  }

  private void actualizarEstadoBotonPublicar() {
    boolean valido =
            !binding.etTitulo.getText().toString().trim().isEmpty()
                    && !binding.etDescripcion.getText().toString().trim().isEmpty()
                    && tiempoSeleccionado != -1
                    && viewModel.hayCambios();

    binding.btnPublicar.setEnabled(valido);
    binding.btnPublicar.setAlpha(valido ? 1f : 0.5f);
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
              if (which == 0) {
                galeriaLauncher.launch("image/*");
              } else {
                solicitarPermisoCamara();
              }
            })
            .show();
  }

  private void solicitarPermisoCamara() {
    if (ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED) {
      abrirCamara();
    } else {
      permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
    }
  }

  private void abrirCamara() {
    try {
      File dir = requireContext().getExternalFilesDir(
              android.os.Environment.DIRECTORY_PICTURES
      );

      if (dir != null) {
        dir.mkdirs();
      }

      File archivo = new File(
              dir,
              "foto_" + UUID.randomUUID() + ".jpg"
      );

      uriFotoTemporal = FileProvider.getUriForFile(
              requireContext(),
              requireContext().getPackageName() + ".fileprovider",
              archivo
      );

      camaraLauncher.launch(uriFotoTemporal);

    } catch (Exception e) {
      binding.etTitulo.setError(getString(R.string.error_camara));
      binding.etTitulo.requestFocus();
    }
  }

  private void procesarImagenSeleccionada(Uri uri) {
    if (targetActivo == null) {
      uriPortadaNueva = uri;

      binding.ivPortadaPreview.setVisibility(View.VISIBLE);
      binding.llAnyadirPortada.setVisibility(View.GONE);

      Glide.with(this)
              .load(uri)
              .centerCrop()
              .into(binding.ivPortadaPreview);
    } else {
      View fila = targetActivo;
      View boton = fila.findViewById(R.id.llBotonImagenInstruccion);
      ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);

      fila.setTag(R.id.ivPreviewInstruccion, uri);
      boton.setVisibility(View.GONE);
      preview.setVisibility(View.VISIBLE);

      Glide.with(this)
              .load(uri)
              .centerCrop()
              .into(preview);
    }

    viewModel.marcarCambios();
  }

  private void mostrarDialogoAccionesImagen(Object img, View fila) {
    new AlertDialog.Builder(requireContext())
            .setItems(new CharSequence[]{
                    getString(R.string.crear_ver_imagen),
                    getString(R.string.crear_eliminar_imagen)
            }, (dialog, which) -> {
              if (which == 0) {
                Uri uri = img instanceof Uri
                        ? (Uri) img
                        : Uri.parse((String) img);

                mostrarImagenCompleta(uri);
              } else {
                ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);
                View boton = fila.findViewById(R.id.llBotonImagenInstruccion);

                fila.setTag(R.id.ivPreviewInstruccion, null);
                preview.setImageDrawable(null);
                preview.setVisibility(View.GONE);
                boton.setVisibility(View.VISIBLE);

                viewModel.marcarCambios();
              }
            })
            .show();
  }

  private void mostrarImagenCompleta(Uri uri) {
    Dialog dialog = new Dialog(
            requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
    );

    ImageView iv = new ImageView(requireContext());

    iv.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
    ));

    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
    iv.setBackgroundColor(Color.BLACK);

    Glide.with(this).load(uri).into(iv);

    iv.setOnClickListener(v -> dialog.dismiss());

    dialog.setContentView(iv);
    dialog.show();
  }

  ////////////////////////// tiempo /////////////////////////

  private void seleccionarTiempo(int indice) {
    tiempoSeleccionado = indice;

    binding.tvErrorTiempo.setVisibility(View.GONE);
    binding.tvErrorTiempo.setText("");

    android.widget.ImageButton[] botones = {
            binding.btnTiempoCohete,
            binding.btnTiempoLiebre,
            binding.btnTiempoTortuga
    };

    for (int i = 0; i < botones.length; i++) {
      if (i == indice) {
        botones[i].setBackground(ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_boton_principal
        ));

        botones[i].setImageTintList(ContextCompat.getColorStateList(
                requireContext(),
                R.color.clank_background_light
        ));
      } else {
        botones[i].setBackground(ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_input
        ));

        botones[i].setImageTintList(ContextCompat.getColorStateList(
                requireContext(),
                R.color.color_texto_inactivo
        ));
      }
    }

    actualizarEstadoBotonPublicar();
  }

  ///////////////////////// filas dinámicas /////////////////////////

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
        viewModel.marcarCambios();
      } else {
        EditText etNombreMaterial = fila.findViewById(R.id.etNombreElemento);

        if (etNombreMaterial != null) {
          etNombreMaterial.setError(getString(R.string.crear_error_min_material));
          etNombreMaterial.requestFocus();
        }
      }
    });

    contenedor.addView(fila);
  }

  private void anyadirFilaInstruccion() {
    View fila = LayoutInflater.from(requireContext())
            .inflate(
                    R.layout.item_instruccion,
                    binding.llContenedorInstrucciones,
                    false
            );

    fila.findViewById(R.id.btnEliminarInstruccion).setOnClickListener(b -> {
      if (binding.llContenedorInstrucciones.getChildCount() > 1) {
        binding.llContenedorInstrucciones.removeView(fila);
        viewModel.marcarCambios();
      } else {
        EditText etTextoInstruccion = fila.findViewById(R.id.etTextoInstruccion);

        if (etTextoInstruccion != null) {
          etTextoInstruccion.setError(getString(R.string.crear_error_min_instruccion));
          etTextoInstruccion.requestFocus();
        }
      }
    });

    fila.findViewById(R.id.llBotonImagenInstruccion)
            .setOnClickListener(b ->
                    mostrarDialogoSeleccionImagen(fila)
            );

    fila.findViewById(R.id.ivPreviewInstruccion)
            .setOnClickListener(b -> {
              Object tag = fila.getTag(R.id.ivPreviewInstruccion);

              if (tag != null) {
                mostrarDialogoAccionesImagen(tag, fila);
              }
            });

    binding.llContenedorInstrucciones.addView(fila);
  }

  ///////////////////////// publicar / boceto /////////////////////////

  private void intentarPublicar(View v) {
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
      binding.tvErrorTiempo.setText(getString(R.string.crear_error_tiempo));
      binding.tvErrorTiempo.setVisibility(View.VISIBLE);
      return;
    }

    esPublicacion = true;

    viewModel.publicarClank(
            titulo,
            descripcion,
            tiempoSeleccionado,
            uriPortadaNueva,
            urlPortadaActual,
            recogerMateriales(),
            recogerHerramientas(),
            recogerTextosInstrucciones(),
            recogerImagenesInstrucciones(),
            recogerCategoriasSeleccionadas()
    );
  }

  private void guardarBoceto(View v) {
    esPublicacion = false;

    viewModel.guardarBoceto(
            binding.etTitulo.getText().toString().trim(),
            binding.etDescripcion.getText().toString().trim(),
            tiempoSeleccionado,
            uriPortadaNueva,
            urlPortadaActual,
            recogerMateriales(),
            recogerHerramientas(),
            recogerTextosInstrucciones(),
            recogerImagenesInstrucciones(),
            recogerCategoriasSeleccionadas()
    );
  }

  ///////////////////////// hojas de confirmación /////////////////////////

  private void mostrarConfirmacionSalirDeEdicion() {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
            getString(R.string.editar_descartar_cambios_titulo),
            getString(R.string.editar_descartar_cambios_mensaje),
            getString(R.string.cancelar),
            getString(R.string.editar_descartar_confirmar),
            null,
            () -> Navigation.findNavController(requireView()).navigateUp()
    );

    hoja.show(
            getChildFragmentManager(),
            "hoja_salir_edicion_clank"
    );
  }

  private void mostrarConfirmacionEliminarClank() {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
            getString(R.string.perfil_eliminar_clank_titulo),
            getString(R.string.perfil_eliminar_clank_mensaje, binding.etTitulo.getText().toString().trim()),
            getString(R.string.cancelar),
            getString(R.string.perfil_eliminar_clank_confirmar),
            null,
            this::eliminarClankYVolverAlFeed
    );

    hoja.show(
            getChildFragmentManager(),
            "hoja_eliminar_clank_edicion"
    );
  }

  private void eliminarClankYVolverAlFeed() {
    viewModel.eliminarClank()
            .addOnSuccessListener(unused -> {
              if (!isAdded()) {
                return;
              }

              NavOptions opciones = new NavOptions.Builder()
                      .setPopUpTo(R.id.nav_graph, true)
                      .build();

              Navigation.findNavController(requireView())
                      .navigate(R.id.feedFragment, null, opciones);
            })
            .addOnFailureListener(error -> {
              if (!isAdded()) {
                return;
              }

              binding.etTitulo.setError(getString(R.string.perfil_error_eliminar_clank));
              binding.etTitulo.requestFocus();
            });
  }

  private List<String[]> recogerMateriales() {
    List<String[]> lista = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorMateriales.getChildCount(); i++) {
      View fila = binding.llContenedorMateriales.getChildAt(i);

      String cant = ((EditText) fila.findViewById(R.id.etCantidad))
              .getText()
              .toString()
              .trim();

      String nombre = ((EditText) fila.findViewById(R.id.etNombreElemento))
              .getText()
              .toString()
              .trim();

      if (cant.isEmpty()) {
        cant = "1";
      }

      if (!nombre.isEmpty()) {
        lista.add(new String[]{cant, nombre});
      }
    }

    return lista;
  }

  private List<String> recogerHerramientas() {
    List<String> lista = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorHerramientas.getChildCount(); i++) {
      View fila = binding.llContenedorHerramientas.getChildAt(i);

      String nombre = ((EditText) fila.findViewById(R.id.etNombreElemento))
              .getText()
              .toString()
              .trim();

      if (!nombre.isEmpty()) {
        lista.add(nombre);
      }
    }

    return lista;
  }

  private List<String> recogerTextosInstrucciones() {
    List<String> lista = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorInstrucciones.getChildCount(); i++) {
      View fila = binding.llContenedorInstrucciones.getChildAt(i);

      String texto = ((EditText) fila.findViewById(R.id.etTextoInstruccion))
              .getText()
              .toString()
              .trim();

      lista.add(texto);
    }

    return lista;
  }

  private List<Object> recogerImagenesInstrucciones() {
    List<Object> lista = new ArrayList<>();

    for (int i = 0; i < binding.llContenedorInstrucciones.getChildCount(); i++) {
      View fila = binding.llContenedorInstrucciones.getChildAt(i);
      lista.add(fila.getTag(R.id.ivPreviewInstruccion));
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
    viewModel.getDatosClank().observe(getViewLifecycleOwner(), datos -> {
      if (datos == null) {
        return;
      }

      urlPortadaActual = datos.portadaUrl;
      rellenarFormulario(datos);

      List<String[]> categoriasDisponibles =
              viewModel.getCategorias().getValue();

      if (categoriasDisponibles != null && !categoriasDisponibles.isEmpty()) {
        cargarChipsCategorias(categoriasDisponibles);
      }
    });

    viewModel.getEstadoGuardar().observe(getViewLifecycleOwner(), recurso -> {
      if (recurso == null) {
        return;
      }

      switch (recurso.estado) {
        case CARGANDO:
          binding.btnPublicar.setEnabled(false);
          binding.btnGuardarBoceto.setEnabled(false);
          break;

        case EXITO:
          // navega al perfil abriendo el tab
          Bundle args = new Bundle();
          args.putString(
                  "tabInicial",
                  esPublicacion ? "clanks" : "bocetos"
          );

          Navigation.findNavController(requireView())
                  .navigate(R.id.action_editar_a_perfil, args);
          break;

        case ERROR:
          binding.btnPublicar.setEnabled(viewModel.hayCambios());
          binding.btnGuardarBoceto.setEnabled(true);

          binding.etTitulo.setError(
                  recurso.mensaje != null
                          ? recurso.mensaje
                          : getString(R.string.crear_error_publicar)
          );

          binding.etTitulo.requestFocus();
          break;
      }
    });

    viewModel.getCategorias().observe(
            getViewLifecycleOwner(),
            this::cargarChipsCategorias
    );
  }

  ///////////////////////// rellenar formulario /////////////////////////

  private void rellenarFormulario(EditarClankViewModel.DatosClank datos) {
    binding.etTitulo.setText(datos.titulo);
    binding.etDescripcion.setText(datos.descripcion);

    if (datos.tiempo >= 0) {
      seleccionarTiempo(datos.tiempo);
    }

    if (!datos.portadaUrl.isEmpty()) {
      binding.ivPortadaPreview.setVisibility(View.VISIBLE);
      binding.llAnyadirPortada.setVisibility(View.GONE);

      Glide.with(this)
              .load(datos.portadaUrl)
              .centerCrop()
              .into(binding.ivPortadaPreview);
    }

    binding.llContenedorMateriales.removeAllViews();

    if (datos.materiales.isEmpty()) {
      anyadirFilaMaterial(false);
    } else {
      for (String[] material : datos.materiales) {
        anyadirFilaMaterialConDatos(material[0], material[1]);
      }
    }

    binding.llContenedorHerramientas.removeAllViews();

    for (String herramienta : datos.herramientas) {
      anyadirFilaHerramientaConDatos(herramienta);
    }

    binding.llContenedorInstrucciones.removeAllViews();

    if (datos.instrucciones.isEmpty()) {
      anyadirFilaInstruccion();
    } else {
      for (String[] instruccion : datos.instrucciones) {
        anyadirFilaInstruccionConDatos(
                instruccion[0],
                instruccion[1]
        );
      }
    }
  }

  private void anyadirFilaMaterialConDatos(String cantidad, String nombre) {
    View fila = LayoutInflater.from(requireContext())
            .inflate(
                    R.layout.item_material,
                    binding.llContenedorMateriales,
                    false
            );

    ((EditText) fila.findViewById(R.id.etCantidad)).setText(cantidad);
    ((EditText) fila.findViewById(R.id.etNombreElemento)).setText(nombre);

    fila.findViewById(R.id.btnEliminarElemento).setOnClickListener(b -> {
      if (binding.llContenedorMateriales.getChildCount() > 1) {
        binding.llContenedorMateriales.removeView(fila);
        viewModel.marcarCambios();
      } else {
        EditText etNombreMaterial = fila.findViewById(R.id.etNombreElemento);

        if (etNombreMaterial != null) {
          etNombreMaterial.setError(getString(R.string.crear_error_min_material));
          etNombreMaterial.requestFocus();
        }
      }
    });

    binding.llContenedorMateriales.addView(fila);
  }

  private void anyadirFilaHerramientaConDatos(String nombre) {
    View fila = LayoutInflater.from(requireContext())
            .inflate(
                    R.layout.item_material,
                    binding.llContenedorHerramientas,
                    false
            );

    fila.findViewById(R.id.etCantidad).setVisibility(View.GONE);

    ((EditText) fila.findViewById(R.id.etNombreElemento))
            .setText(nombre);

    ((EditText) fila.findViewById(R.id.etNombreElemento))
            .setHint(getString(R.string.crear_hint_herramienta));

    fila.findViewById(R.id.btnEliminarElemento).setOnClickListener(b -> {
      binding.llContenedorHerramientas.removeView(fila);
      viewModel.marcarCambios();
    });

    binding.llContenedorHerramientas.addView(fila);
  }

  private void anyadirFilaInstruccionConDatos(String texto,
                                              @Nullable String urlImagen) {
    View fila = LayoutInflater.from(requireContext())
            .inflate(
                    R.layout.item_instruccion,
                    binding.llContenedorInstrucciones,
                    false
            );

    ((EditText) fila.findViewById(R.id.etTextoInstruccion)).setText(texto);

    if (urlImagen != null && !urlImagen.isEmpty()) {
      ImageView preview = fila.findViewById(R.id.ivPreviewInstruccion);
      View boton = fila.findViewById(R.id.llBotonImagenInstruccion);

      fila.setTag(R.id.ivPreviewInstruccion, urlImagen);
      boton.setVisibility(View.GONE);
      preview.setVisibility(View.VISIBLE);

      Glide.with(this)
              .load(urlImagen)
              .centerCrop()
              .into(preview);
    }

    fila.findViewById(R.id.btnEliminarInstruccion).setOnClickListener(b -> {
      if (binding.llContenedorInstrucciones.getChildCount() > 1) {
        binding.llContenedorInstrucciones.removeView(fila);
        viewModel.marcarCambios();
      } else {
        EditText etTextoInstruccion = fila.findViewById(R.id.etTextoInstruccion);

        if (etTextoInstruccion != null) {
          etTextoInstruccion.setError(getString(R.string.crear_error_min_instruccion));
          etTextoInstruccion.requestFocus();
        }
      }
    });

    fila.findViewById(R.id.llBotonImagenInstruccion)
            .setOnClickListener(b ->
                    mostrarDialogoSeleccionImagen(fila)
            );

    fila.findViewById(R.id.ivPreviewInstruccion)
            .setOnClickListener(b -> {
              Object tag = fila.getTag(R.id.ivPreviewInstruccion);

              if (tag != null) {
                mostrarDialogoAccionesImagen(tag, fila);
              }
            });

    binding.llContenedorInstrucciones.addView(fila);
  }

  ////////////////////////// categorías /////////////////////////

  private void cargarChipsCategorias(List<String[]> categorias) {
    if (categorias == null || categorias.isEmpty()) {
      return;
    }

    EditarClankViewModel.DatosClank datos =
            viewModel.getDatosClank().getValue();

    List<String> seleccionadas =
            datos != null ? datos.categorias : new ArrayList<>();

    binding.flexboxCategorias.removeAllViews();

    for (String[] cat : categorias) {
      String catId = cat[0];
      String catNombre = cat[1];
      boolean activo = seleccionadas.contains(catId);

      Button chip = (Button) LayoutInflater.from(requireContext())
              .inflate(
                      R.layout.bt_secundario,
                      binding.flexboxCategorias,
                      false
              );

      chip.setText(catNombre);
      chip.setTag(catId);
      chip.setSelected(activo);

      chip.setBackgroundResource(
              activo
                      ? R.drawable.bg_boton_principal
                      : R.drawable.bg_boton_secundario
      );

      chip.setTextColor(ContextCompat.getColor(
              requireContext(),
              activo
                      ? R.color.clank_background_light
                      : R.color.color_texto_inactivo
      ));

      ViewGroup.MarginLayoutParams lp =
              (ViewGroup.MarginLayoutParams) chip.getLayoutParams();

      lp.setMargins(0, 0, 8, 8);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(b -> {
        boolean estaActivo = chip.isSelected();

        chip.setSelected(!estaActivo);

        chip.setBackgroundResource(
                !estaActivo
                        ? R.drawable.bg_boton_principal
                        : R.drawable.bg_boton_secundario
        );

        chip.setTextColor(ContextCompat.getColor(
                requireContext(),
                !estaActivo
                        ? R.color.clank_background_light
                        : R.color.color_texto_inactivo
        ));

        viewModel.marcarCambios();
        actualizarEstadoBotonPublicar();
      });

      binding.flexboxCategorias.addView(chip);
    }
  }
}