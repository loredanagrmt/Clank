package com.clank.app.ui.detalle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.databinding.FragmentDetalleClankBinding;
import com.clank.app.ui.comun.NavbarHost;

import androidx.navigation.Navigation;

import com.clank.app.ui.comun.HojaOpciones;
import com.clank.app.ui.comun.ItemOpcion;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

@AndroidEntryPoint
public class DetalleClankFragment extends Fragment {

  private static final String ARG_CLANK_ID = "clankId";

  private FragmentDetalleClankBinding binding;
  private DetalleClankViewModel viewModel;
  private String clankId;
  private String tituloClankActual = "";
  public int numLikes = 0;

  /////////////////////////instancia/////////////////////////

  public static DetalleClankFragment newInstance(String clankId) {
    DetalleClankFragment f = new DetalleClankFragment();
    Bundle args = new Bundle();
    args.putString(ARG_CLANK_ID, clankId);
    f.setArguments(args);
    return f;
  }

  /////////////////////////ciclo de vida/////////////////////////

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentDetalleClankBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    viewModel = new ViewModelProvider(this).get(DetalleClankViewModel.class);

    if (getArguments() != null)
      clankId = getArguments().getString(ARG_CLANK_ID, "");

    configurarNavbar();
    observarViewModel();

    viewModel.cargarClank(clankId);
  }

  /////////////////////////navbar/////////////////////////

  private void configurarNavbar() {
    NavbarHost host = (NavbarHost) requireActivity();
    host.mostrarNavbar(
      getString(R.string.detalle_titulo),
      null,
      null
    );
  }

  /////////////////////////observadores/////////////////////////

  private void observarViewModel() {
    viewModel.getCargando().observe(getViewLifecycleOwner(), cargando -> {
      boolean estaCargando = Boolean.TRUE.equals(cargando);

      binding.overlayCargando.setVisibility(
              estaCargando ? View.VISIBLE : View.GONE
      );
    });

    viewModel.getDetalle().observe(getViewLifecycleOwner(), datos -> {
      if (datos == null) return;
      rellenarVista(datos);
    });

    viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
      if (msg != null) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
      }
    });

    viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
      if (msg != null) {
        Toast.makeText(
                requireContext(),
                msg,
                Toast.LENGTH_LONG
        ).show();
      }
    });
  }

  /////////////////////////rellenar vista/////////////////////////

  private void rellenarVista(DetalleClankViewModel.DetalleData datos) {
    tituloClankActual = datos.titulo != null ? datos.titulo : "";
    //título en overlay de portada
    binding.tvTitulo.setText(datos.titulo);

    if (datos.numLikes >= 0) {
      binding.tvNumLikesDetalle.setText(String.valueOf(datos.numLikes));
    }

    //portada
    if (!datos.portadaUrl.isEmpty()) {
      Glide.with(this).load(datos.portadaUrl).centerCrop().into(binding.ivPortada);
    }

    //descripción
    binding.tvDescripcion.setText(datos.descripcion);

    mostrarTiempo(datos.tiempo);

    //listas dinámicas
    rellenarMateriales(datos.materiales);
    rellenarHerramientas(datos.herramientas);
    rellenarInstrucciones(datos.instrucciones);
    rellenarCategorias(datos.categorias);

    //cabecera de usuario
    String handle = datos.usuarioClank != null && !datos.usuarioClank.trim().isEmpty()
            ? "@" + datos.usuarioClank.replace("@", "").trim()
            : datos.nombreUsuario;
    binding.cabeceraUsuario.tvUsernameItem.setText(handle);

    if (datos.fechaPublicacion != null) {
      binding.cabeceraUsuario.tvFechaItem.setText(
              formatearFechaRelativa(datos.fechaPublicacion));
      binding.cabeceraUsuario.tvFechaItem.setVisibility(View.VISIBLE);
    } else {
      binding.cabeceraUsuario.tvFechaItem.setVisibility(View.GONE);
    }

    if (datos.fotoPerfil != null && !datos.fotoPerfil.isEmpty()) {
      Glide.with(this)
              .load(datos.fotoPerfil)
              .circleCrop()
              .placeholder(R.drawable.ic_usuario_inactivo)
              .into(binding.cabeceraUsuario.civAvatarUsuario);
    } else {
      binding.cabeceraUsuario.civAvatarUsuario
              .setImageResource(R.drawable.ic_usuario_inactivo);
    }

    binding.cabeceraUsuario.civAvatarUsuario
            .setOnClickListener(v -> navegarAPerfilAutor());
    binding.cabeceraUsuario.tvUsernameItem
            .setOnClickListener(v -> navegarAPerfilAutor());
    configurarBotonOpciones(datos.usuarioId);
    configurarLike(datos.clankId);
  }

  /////////////////////////configura menu navbar/////////////////////////
  private void configurarBotonOpciones(String autorId) {
    NavbarHost host = (NavbarHost) requireActivity();

    com.google.firebase.auth.FirebaseUser usuarioActual =
      com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

    boolean esPropio = usuarioActual != null
      && usuarioActual.getUid().equals(autorId);

    if (esPropio) {
      host.mostrarNavbar(
        getString(R.string.detalle_titulo),
        R.drawable.ic_opciones_activo,
        v -> mostrarOpcionesClank()
      );
    } else {
      host.mostrarNavbar(
        getString(R.string.detalle_titulo),
        null,
        null
      );
    }
  }

  /////////////////////////fecha relativa/////////////////////////

  private String formatearFechaRelativa(Date fecha) {
    long diferencia = System.currentTimeMillis() - fecha.getTime();
    long minutos = TimeUnit.MILLISECONDS.toMinutes(diferencia);
    long horas   = TimeUnit.MILLISECONDS.toHours(diferencia);
    long dias    = TimeUnit.MILLISECONDS.toDays(diferencia);
    long meses   = dias / 30;
    long anyos   = dias / 365;

    if (minutos < 1)  return getString(R.string.feed_ahora);
    if (minutos < 60) return getString(R.string.feed_hace_minutos, minutos);
    if (horas < 24)   return getString(R.string.feed_hace_horas, horas);
    if (dias < 30)    return getString(R.string.feed_hace_dias, dias);
    if (meses < 12)   return getString(R.string.feed_hace_meses, meses);
    return getString(R.string.feed_hace_anyos, anyos);
  }

  /////////////////////////tiempo (solo visual, no clickable)/////////////////////////

  private void mostrarTiempo(int tiempo) {
    android.widget.ImageButton[] botones = {
      binding.btnTiempoCohete,
      binding.btnTiempoLiebre,
      binding.btnTiempoTortuga
    };
    for (int i = 0; i < botones.length; i++) {
      if (i == tiempo) {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
          R.drawable.bg_boton_principal));
        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
          R.color.clank_background_light));
      } else {
        botones[i].setBackground(ContextCompat.getDrawable(requireContext(),
          R.drawable.bg_input));
        botones[i].setImageTintList(ContextCompat.getColorStateList(requireContext(),
          R.color.color_texto_inactivo));
      }
    }
  }

  /////////////////////////materiales/////////////////////////

  private void rellenarMateriales(List<Material> materiales) {
    binding.llContenedorMateriales.removeAllViews();
    if (materiales == null || materiales.isEmpty()) {
      binding.llContenedorMateriales.setVisibility(View.GONE);
      return;
    }
    binding.llContenedorMateriales.setVisibility(View.VISIBLE);
    for (Material m : materiales) {
      View fila = LayoutInflater.from(requireContext())
        .inflate(R.layout.item_detalle_material, binding.llContenedorMateriales, false);
      ((TextView) fila.findViewById(R.id.tvCantidad))
        .setText(String.valueOf(m.getCantidad()));
      ((TextView) fila.findViewById(R.id.tvNombreMaterial))
        .setText(m.getMaterial() != null ? m.getMaterial() : "");
      binding.llContenedorMateriales.addView(fila);
    }
  }

  /////////////////////////herramientas/////////////////////////

  private void rellenarHerramientas(List<Herramienta> herramientas) {
    binding.llContenedorHerramientas.removeAllViews();
    if (herramientas == null || herramientas.isEmpty()) {
      binding.llContenedorHerramientas.setVisibility(View.GONE);
      return;
    }
    binding.llContenedorHerramientas.setVisibility(View.VISIBLE);
    for (Herramienta h : herramientas) {
      View fila = LayoutInflater.from(requireContext())
        .inflate(R.layout.item_detalle_herramienta, binding.llContenedorHerramientas, false);
      ((TextView) fila.findViewById(R.id.tvNombreHerramienta))
        .setText(h.getHerramienta() != null ? h.getHerramienta() : "");
      binding.llContenedorHerramientas.addView(fila);
    }
  }

  /////////////////////////instrucciones/////////////////////////

  private void rellenarInstrucciones(List<Instruccion> instrucciones) {
    binding.llContenedorInstrucciones.removeAllViews();
    if (instrucciones == null || instrucciones.isEmpty()) return;
    for (int i = 0; i < instrucciones.size(); i++) {
      Instruccion ins = instrucciones.get(i);
      View fila = LayoutInflater.from(requireContext())
        .inflate(R.layout.item_detalle_instruccion, binding.llContenedorInstrucciones, false);

      ((TextView) fila.findViewById(R.id.tvNumeroInstruccion))
        .setText((i + 1) + ".");
      ((TextView) fila.findViewById(R.id.tvTextoInstruccion))
        .setText(ins.getInstruccion() != null ? ins.getInstruccion() : "");

      ImageView ivImg = fila.findViewById(R.id.ivImagenInstruccion);
      if (ins.getImagen() != null && !ins.getImagen().isEmpty()) {
        ivImg.setVisibility(View.VISIBLE);
        Glide.with(this).load(ins.getImagen()).centerCrop().into(ivImg);
      } else {
        ivImg.setVisibility(View.GONE);
      }

      binding.llContenedorInstrucciones.addView(fila);
    }
  }

  /////////////////////////categorías (solo visual, no clickables)/////////////////////////
  private void rellenarCategorias(List<String[]> categorias) {
    binding.flexboxCategorias.removeAllViews();
    if (categorias == null || categorias.isEmpty()) return;
    for (String[] cat : categorias) {
      Button chip = (Button) LayoutInflater.from(requireContext())
        .inflate(R.layout.bt_secundario, binding.flexboxCategorias, false);
      chip.setText(cat[1]);
      chip.setTag(cat[0]);
      chip.setSelected(true);
      chip.setBackgroundResource(R.drawable.bg_boton_principal);
      chip.setTextColor(ContextCompat.getColor(requireContext(),
        R.color.clank_background_light));
      chip.setClickable(false);
      chip.setFocusable(false);

      ViewGroup.MarginLayoutParams lp =
        (ViewGroup.MarginLayoutParams) chip.getLayoutParams();
      lp.setMargins(0, 0, 8, 8);
      chip.setLayoutParams(lp);

      binding.flexboxCategorias.addView(chip);
    }
  }
  /////////////////////////hoja de opciones de clank/////////////////////////
  private void mostrarOpcionesClank() {
    HojaOpciones hoja = HojaOpciones.nuevaLista(
            tituloClankActual,
            Arrays.asList(
                    new ItemOpcion("editar", getString(R.string.perfil_opcion_editar_clank)),
                    new ItemOpcion("eliminar", getString(R.string.perfil_opcion_eliminar_clank))
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

    hoja.show(getParentFragmentManager(), "hoja_opciones_clank_detalle");
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

    hoja.show(getParentFragmentManager(), "hoja_confirmar_eliminar_clank_detalle");
  }

  private void eliminarClankDesdeDetalle() {
    viewModel.eliminarClank(clankId)
            .addOnSuccessListener(unused -> {
              if (!isAdded()) return;

              Navigation.findNavController(requireView()).navigateUp();
            })
            .addOnFailureListener(error -> {
              if (!isAdded()) return;

              Toast.makeText(
                      requireContext(),
                      getString(R.string.perfil_error_eliminar_clank),
                      Toast.LENGTH_LONG
              ).show();
            });
  }

  /////////////////////////navegar a perfil del autor/////////////////////////

  private void navegarAPerfilAutor() {
    DetalleClankViewModel.DetalleData datos = viewModel.getDetalle().getValue();
    if (datos == null) return;
    String uid = datos.usuarioId;
    if (uid == null || uid.isEmpty()) return;
    Bundle args = new Bundle();
    args.putString("usuarioId", uid);
    Navigation.findNavController(requireView())
            .navigate(R.id.action_detalle_a_perfil, args);
  }

  /////////////////////////like/////////////////////////
  private void configurarLike(String clankId) {
    viewModel.hasDadoLike(clankId).addOnSuccessListener(haDadoLike -> {
      if (binding == null) return;
      binding.btnLikeDetalle.setImageResource(
        haDadoLike ? R.drawable.ic_like_activo : R.drawable.ic_like_inactivo);
      binding.btnLikeDetalle.setBackgroundResource(
        haDadoLike ? R.drawable.bg_circulo_opciones_activo : R.drawable.bg_circulo_opciones_inactivo);
    });

    binding.btnLikeDetalle.setOnClickListener(v -> {
      viewModel.toggleLike(clankId).addOnSuccessListener(ahoraLikeado -> {
        if (binding == null) return;

        binding.btnLikeDetalle.setImageResource(
          ahoraLikeado ? R.drawable.ic_like_activo : R.drawable.ic_like_inactivo);
        binding.btnLikeDetalle.setBackgroundResource(
          ahoraLikeado ? R.drawable.bg_circulo_opciones_activo : R.drawable.bg_circulo_opciones_inactivo);

        //animación
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.3f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(250);
        set.start();

        //actualizar contador
        DetalleClankViewModel.DetalleData datos = viewModel.getDetalle().getValue();
        if (datos != null) {
          datos.numLikes = ahoraLikeado ? datos.numLikes + 1 : datos.numLikes - 1;
          if (datos.numLikes < 0) datos.numLikes = 0;
          binding.tvNumLikesDetalle.setText(String.valueOf(datos.numLikes));
        }
      }).addOnFailureListener(e -> {
        android.util.Log.e("LikeDetalle", "Error en toggleLike: " + e.getMessage(), e);
        Toast.makeText(requireContext(), "Error al dar like: " + e.getMessage(), Toast.LENGTH_LONG).show();
      });
    });
  }
}
