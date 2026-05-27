package com.clank.app.ui.detalle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.databinding.FragmentDetalleClankBinding;
import com.clank.app.ui.comun.ChipCategoriasHelper;
import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;
import com.clank.app.ui.comun.NavbarHost;
import com.clank.app.util.AnimUtils;
import com.clank.app.util.FechaUtils;

import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DetalleClankFragment extends Fragment {

  private static final String ARG_CLANK_ID = "clankId";

  private FragmentDetalleClankBinding binding;
  private DetalleClankViewModel viewModel;
  private String clankId;
  private String tituloClankActual = "";
  public int numLikes = 0;
  private com.google.firebase.firestore.ListenerRegistration listenerLikes;

  ///////////////////////// instancia /////////////////////////

  public static DetalleClankFragment newInstance(String clankId) {
    DetalleClankFragment f = new DetalleClankFragment();
    Bundle args = new Bundle();
    args.putString(ARG_CLANK_ID, clankId);
    f.setArguments(args);
    return f;
  }

  ///////////////////////// ciclo de vida /////////////////////////

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentDetalleClankBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view,
                            @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(DetalleClankViewModel.class);

    if (getArguments() != null) {
      clankId = getArguments().getString(ARG_CLANK_ID, "");
    }

    configurarNavbarInicial();
    observarViewModel();

    viewModel.cargarClank(clankId);
  }

  @Override
  public void onResume() {
    super.onResume();

    if (viewModel != null &&
            viewModel.getDetalle().getValue() != null) {

      configurarBotonOpciones(
              viewModel.getDetalle().getValue().usuarioId
      );
    } else {
      configurarNavbarInicial();
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    if (listenerLikes != null) {
      listenerLikes.remove();
      listenerLikes = null;
    }

    binding = null;
  }

  ///////////////////////// navbar /////////////////////////

  private void configurarNavbarInicial() {
    NavbarHost host = (NavbarHost) requireActivity();

    // Se deja vacío hasta saber si el Clank es propio o ajeno.
    host.mostrarNavbarConVolver("");
  }

  ///////////////////////// observadores /////////////////////////

  private void observarViewModel() {
    viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
      boolean estaCargando = Boolean.TRUE.equals(cargando);

      binding.overlayCargando.setVisibility(
              estaCargando ? View.VISIBLE : View.GONE
      );
    });

    viewModel.getDetalle().observe(getViewLifecycleOwner(), datos -> {
      if (datos == null) {
        return;
      }

      rellenarVista(datos);
    });

    viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
      if (msg != null) {
        binding.overlayCargando.setVisibility(View.GONE);
      }
    });
  }

  ///////////////////////// rellenar vista /////////////////////////

  private void rellenarVista(DetalleClankViewModel.DetalleData datos) {
    tituloClankActual = datos.titulo != null ? datos.titulo : "";

    // título en overlay de portada
    binding.tvTitulo.setText(datos.titulo);

    if (datos.numLikes >= 0) {
      binding.tvNumLikesDetalle.setText(String.valueOf(datos.numLikes));
    }

    // portada
    if (!datos.portadaUrl.isEmpty()) {
      Glide.with(this)
              .load(datos.portadaUrl)
              .centerCrop()
              .into(binding.ivPortada);
    }

    // descripción
    binding.tvDescripcion.setText(datos.descripcion);

    mostrarTiempo(datos.tiempo);

    // listas dinámicas
    rellenarMateriales(datos.materiales);
    rellenarHerramientas(datos.herramientas);
    rellenarInstrucciones(datos.instrucciones);
    rellenarCategorias(datos.categorias);

    // cabecera de usuario
    String handle = datos.usuarioClank != null &&
            !datos.usuarioClank.trim().isEmpty()
            ? "@" + datos.usuarioClank.replace("@", "").trim()
            : datos.nombreUsuario;

    binding.cabeceraUsuario.tvUsernameItem.setText(handle);

    if (datos.fechaPublicacion != null) {
      binding.cabeceraUsuario.tvFechaItem.setText(
              FechaUtils.formatearFechaRelativa(
                      requireContext(),
                      datos.fechaPublicacion
              )
      );

      binding.cabeceraUsuario.tvFechaItem.setVisibility(View.VISIBLE);
    } else {
      binding.cabeceraUsuario.tvFechaItem.setVisibility(View.GONE);
    }

    if (datos.fotoPerfil != null && !datos.fotoPerfil.isEmpty()) {
      Glide.with(this)
              .load(datos.fotoPerfil)
              .circleCrop()
              .placeholder(R.drawable.img_usuario_defecto)
              .into(binding.cabeceraUsuario.civAvatarUsuario);
    } else {
      binding.cabeceraUsuario.civAvatarUsuario
              .setImageResource(R.drawable.img_usuario_defecto);
    }

    binding.cabeceraUsuario.civAvatarUsuario
            .setOnClickListener(v -> navegarAPerfilAutor());

    binding.cabeceraUsuario.tvUsernameItem
            .setOnClickListener(v -> navegarAPerfilAutor());

    configurarBotonOpciones(datos.usuarioId);
    configurarLike(datos.clankId);
  }

  ///////////////////////// configura menú navbar /////////////////////////

  private void configurarBotonOpciones(String autorId) {
    NavbarHost host = (NavbarHost) requireActivity();

    com.google.firebase.auth.FirebaseUser usuarioActual =
            com.google.firebase.auth.FirebaseAuth
                    .getInstance()
                    .getCurrentUser();

    boolean esPropio = usuarioActual != null &&
            usuarioActual.getUid().equals(autorId);

    if (esPropio) {
      host.mostrarNavbarConVolver(
              getString(R.string.detalle_titulo),
              R.drawable.ic_opciones_activo,
              v -> mostrarOpcionesClank()
      );
    } else {
      String titulo = tituloClankActual != null ? tituloClankActual : "";
      String tituloNavbar = titulo.length() > 25
        ? titulo.substring(0, 25) + "…"
        : titulo;
      host.mostrarNavbarConVolver(tituloNavbar);
    }
  }

  ///////////////////////// tiempo (solo visual, no clickable) /////////////////////////

  private void mostrarTiempo(int tiempo) {
    android.widget.ImageButton[] botones = {
            binding.btnTiempoCohete,
            binding.btnTiempoLiebre,
            binding.btnTiempoTortuga
    };

    for (int i = 0; i < botones.length; i++) {
      if (i == tiempo) {
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
  }

  ///////////////////////// materiales /////////////////////////

  private void rellenarMateriales(List<Material> materiales) {
    binding.llContenedorMateriales.removeAllViews();

    if (materiales == null || materiales.isEmpty()) {
      binding.llContenedorMateriales.setVisibility(View.GONE);
      return;
    }

    binding.llContenedorMateriales.setVisibility(View.VISIBLE);

    for (Material m : materiales) {
      View fila = LayoutInflater.from(requireContext())
              .inflate(
                      R.layout.item_detalle_material,
                      binding.llContenedorMateriales,
                      false
              );

      ((TextView) fila.findViewById(R.id.tvCantidad))
              .setText(String.valueOf(m.getCantidad()));

      ((TextView) fila.findViewById(R.id.tvNombreMaterial))
              .setText(m.getMaterial() != null ? m.getMaterial() : "");

      binding.llContenedorMateriales.addView(fila);
    }
  }

  ///////////////////////// herramientas /////////////////////////

  private void rellenarHerramientas(List<Herramienta> herramientas) {
    binding.llContenedorHerramientas.removeAllViews();

    boolean hayHerramientas = herramientas != null && !herramientas.isEmpty();

    binding.tvTituloHerramientas.setVisibility(
      hayHerramientas ? View.VISIBLE : View.GONE);
    binding.llContenedorHerramientas.setVisibility(
      hayHerramientas ? View.VISIBLE : View.GONE);

    if (!hayHerramientas) return;

    for (Herramienta h : herramientas) {
      View fila = LayoutInflater.from(requireContext())
              .inflate(
                      R.layout.item_detalle_herramienta,
                      binding.llContenedorHerramientas,
                      false
              );

      ((TextView) fila.findViewById(R.id.tvNombreHerramienta))
              .setText(h.getHerramienta() != null ? h.getHerramienta() : "");

      binding.llContenedorHerramientas.addView(fila);
    }
  }

  ///////////////////////// instrucciones /////////////////////////

  private void rellenarInstrucciones(List<Instruccion> instrucciones) {
    binding.llContenedorInstrucciones.removeAllViews();

    if (instrucciones == null || instrucciones.isEmpty()) {
      return;
    }

    for (int i = 0; i < instrucciones.size(); i++) {
      Instruccion ins = instrucciones.get(i);

      View fila = LayoutInflater.from(requireContext())
              .inflate(
                      R.layout.item_detalle_instruccion,
                      binding.llContenedorInstrucciones,
                      false
              );

      ((TextView) fila.findViewById(R.id.tvNumeroInstruccion))
              .setText((i + 1) + ".");

      ((TextView) fila.findViewById(R.id.tvTextoInstruccion))
              .setText(
                      ins.getInstruccion() != null
                              ? ins.getInstruccion()
                              : ""
              );

      ImageView ivImg = fila.findViewById(R.id.ivImagenInstruccion);

      if (ins.getImagen() != null && !ins.getImagen().isEmpty()) {
        ivImg.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(ins.getImagen())
                .centerCrop()
                .into(ivImg);
      } else {
        ivImg.setVisibility(View.GONE);
      }

      binding.llContenedorInstrucciones.addView(fila);
    }
  }

  ///////////////////////// categorías (solo visual, no clickables) /////////////////////////

  private void rellenarCategorias(List<String[]> categorias) {
    ChipCategoriasHelper.cargarChipsVisuales(
      requireContext(),
      binding.contenedorCategorias,
      categorias);
  }

  ///////////////////////// hoja de opciones de clank /////////////////////////

  private void mostrarOpcionesClank() {
    HojaOpciones hoja = HojaOpciones.nuevaLista(
            tituloClankActual,
            Arrays.asList(
                    new ItemOpcion(
                            "editar",
                            getString(R.string.perfil_opcion_editar_clank)
                    ),
                    new ItemOpcion(
                            "eliminar",
                            getString(R.string.perfil_opcion_eliminar_clank)
                    )
            ),
            opcionSeleccionada -> {
              if ("editar".equals(opcionSeleccionada)) {
                navegarAEditarClank();
              } else if ("eliminar".equals(opcionSeleccionada)) {
                requireView().post(() ->
                        mostrarConfirmarEliminarClank()
                );
              }
            }
    );

    hoja.show(
            getParentFragmentManager(),
            "hoja_opciones_clank_detalle"
    );
  }

  private void navegarAEditarClank() {
    Bundle args = new Bundle();
    args.putString("clankId", clankId);

    Navigation.findNavController(requireView())
            .navigate(R.id.action_detalle_a_editar_clank, args);
  }

  private void mostrarConfirmarEliminarClank() {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
            getString(R.string.perfil_eliminar_clank_titulo),
            getString(R.string.perfil_eliminar_clank_mensaje, tituloClankActual),
            getString(R.string.cancelar),
            getString(R.string.perfil_eliminar_clank_confirmar),
            null,
            this::eliminarClankDesdeDetalle
    );

    hoja.show(
            getParentFragmentManager(),
            "hoja_confirmar_eliminar_clank_detalle"
    );
  }

  private void eliminarClankDesdeDetalle() {
    viewModel.eliminarClank(clankId)
      .addOnSuccessListener(unused -> {
        if (!isAdded()) return;
        Navigation.findNavController(requireView()).navigateUp();
      })
      .addOnFailureListener(error -> {
        if (!isAdded()) return;
        mostrarErrorEliminar();
      });
  }

  private void mostrarErrorEliminar() {
    HojaOpciones hoja = HojaOpciones.nuevaConfirmacion(
      getString(R.string.perfil_eliminar_clank_titulo),
      getString(R.string.detalle_error_eliminar_clank),
      getString(R.string.cancelar),
      getString(R.string.reintentar),
      null,
      () -> eliminarClankDesdeDetalle()
    );

    hoja.show(
      getParentFragmentManager(),
      "hoja_error_eliminar_clank_detalle"
    );
  }

  ///////////////////////// navegar a perfil del autor /////////////////////////

  private void navegarAPerfilAutor() {
    DetalleClankViewModel.DetalleData datos =
            viewModel.getDetalle().getValue();

    if (datos == null) {
      return;
    }

    String uid = datos.usuarioId;

    if (uid == null || uid.isEmpty()) {
      return;
    }

    Bundle args = new Bundle();
    args.putString("usuarioId", uid);

    Navigation.findNavController(requireView())
            .navigate(R.id.action_detalle_a_perfil, args);
  }

  ///////////////////////// like /////////////////////////

  private void configurarLike(String clankId) {
    viewModel.hasDadoLike(clankId).addOnSuccessListener(haDadoLike -> {
      if (binding == null) {
        return;
      }

      pintarBotonLike(haDadoLike);
    });

    if (listenerLikes != null) {
      listenerLikes.remove();
      listenerLikes = null;
    }

    listenerLikes = viewModel.escucharNumLikes(clankId, cantidad -> {
      if (binding == null) {
        return;
      }

      binding.tvNumLikesDetalle.setText(String.valueOf(cantidad));
    });

    binding.btnLikeDetalle.setOnClickListener(v -> {
      viewModel.toggleLike(clankId)
              .addOnSuccessListener(ahoraLikeado -> {
                if (binding == null) {
                  return;
                }

                pintarBotonLike(ahoraLikeado);
                AnimUtils.animarLike(binding.btnLikeDetalle);
              })
              .addOnFailureListener(e -> {
                if (!isAdded()) {
                  return;
                }
              });
    });
  }

  private void pintarBotonLike(boolean activo) {
    binding.btnLikeDetalle.setImageResource(
            activo
                    ? R.drawable.ic_like_activo
                    : R.drawable.ic_like_inactivo
    );

    binding.btnLikeDetalle.setBackgroundResource(
            activo
                    ? R.drawable.bg_circulo_opciones_activo
                    : R.drawable.bg_circulo_opciones_inactivo
    );
  }
}
