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

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DetalleClankFragment extends Fragment {

  private static final String ARG_CLANK_ID = "clankId";

  private FragmentDetalleClankBinding binding;
  private DetalleClankViewModel viewModel;
  private String clankId;

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

    /////////////////////////RECORDAR: cambiar cuando Lore acabe hoja/////////////////////////
    host.mostrarNavbar(
      getString(R.string.detalle_titulo),
      R.drawable.ic_opciones_activo,
      v -> Toast.makeText(requireContext(),
        "RECORDAR: cambiar cuando Lore acabe hoja", Toast.LENGTH_SHORT).show()
    );
    /////////////////////////RECORDAR: cambiar cuando Lore acabe hoja/////////////////////////
  }

  /////////////////////////observadores/////////////////////////

  private void observarViewModel() {
    viewModel.getDetalle().observe(getViewLifecycleOwner(), datos -> {
      if (datos == null) return;
      rellenarVista(datos);
    });

    viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
      if (msg != null)
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    });
  }

  /////////////////////////rellenar vista/////////////////////////

  private void rellenarVista(DetalleClankViewModel.DetalleData datos) {
    //título en overlay de portada
    binding.tvTitulo.setText(datos.titulo);

    //portada
    if (!datos.portadaUrl.isEmpty()) {
      Glide.with(this).load(datos.portadaUrl).centerCrop().into(binding.ivPortada);
    }

    //descripción
    binding.tvDescripcion.setText(datos.descripcion);

    //tiempo — marca el seleccionado, deja los otros inactivos
    mostrarTiempo(datos.tiempo);

    //listas dinámicas
    rellenarMateriales(datos.materiales);
    rellenarHerramientas(datos.herramientas);
    rellenarInstrucciones(datos.instrucciones);
    rellenarCategorias(datos.categorias);
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
}
