package com.clank.app.ui.comun;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.clank.app.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChipCategoriasHelper {

  private static final int GAP_DP = 8;
  private static final int PAD_H_DP = 20;
  private static final int MARGEN_V_DP = 8;
  private static final int TEXT_SIZE_SP = 18;

  private static final int COLOR_TEXTO_ACTIVO_FALLBACK = Color.WHITE;
  private static final int COLOR_TEXTO_INACTIVO_FALLBACK = Color.rgb(90, 90, 90);

  public interface OnChipClickListener {
    void onChipClick(Button chip, String categoriaId, boolean seleccionado);
  }

  public static void cargarChipsInteractivos(Context context,
                                             LinearLayout contenedor,
                                             List<String[]> categorias,
                                             Set<String> seleccionadas,
                                             OnChipClickListener listener) {
    if (context == null || contenedor == null) {
      return;
    }

    contenedor.removeAllViews();

    List<String[]> categoriasValidas = filtrarCategoriasValidas(categorias);

    if (categoriasValidas.isEmpty()) {
      return;
    }

    contenedor.post(() -> {
      Context contextoSeguro = contenedor.getContext();
      if (contextoSeguro == null) return;

      int anchoReal = contenedor.getWidth();

      if (anchoReal <= 0) {
        // Fallback más robusto: reintenta en el siguiente frame
        contenedor.post(() -> {
          int anchoReintento = contenedor.getWidth();
          if (anchoReintento <= 0) {
            int margenFallback = dpToPx(contextoSeguro, 32);
            anchoReintento = contextoSeguro.getResources()
                    .getDisplayMetrics().widthPixels - margenFallback;
          }
          if (anchoReintento <= 0) return;
          construirFilas(contextoSeguro, contenedor, categoriasValidas,
                  seleccionadas != null ? seleccionadas : new HashSet<>(),
                  true, listener, anchoReintento);
        });
        return;
      }

      construirFilas(contextoSeguro, contenedor, categoriasValidas,
              seleccionadas != null ? seleccionadas : new HashSet<>(),
              true, listener, anchoReal);
    });
  }

  public static void cargarChipsVisuales(Context context,
                                         LinearLayout contenedor,
                                         List<String[]> categorias) {
    if (context == null || contenedor == null) {
      return;
    }

    contenedor.removeAllViews();

    List<String[]> categoriasValidas = filtrarCategoriasValidas(categorias);

    if (categoriasValidas.isEmpty()) {
      return;
    }

    contenedor.post(() -> {
      Context contextoSeguro = contenedor.getContext();

      if (contextoSeguro == null) {
        return;
      }

      int anchoReal = contenedor.getWidth();

      if (anchoReal <= 0) {
        int margenFallback = dpToPx(contextoSeguro, 32);
        anchoReal = contextoSeguro.getResources().getDisplayMetrics().widthPixels - margenFallback;
      }

      if (anchoReal <= 0) {
        return;
      }

      construirFilas(
              contextoSeguro,
              contenedor,
              categoriasValidas,
              new HashSet<>(),
              false,
              null,
              anchoReal
      );
    });
  }

  public static List<String> recogerSeleccionadas(LinearLayout contenedor) {
    List<String> resultado = new ArrayList<>();

    if (contenedor == null) {
      return resultado;
    }

    for (int i = 0; i < contenedor.getChildCount(); i++) {
      View fila = contenedor.getChildAt(i);

      if (!(fila instanceof LinearLayout)) {
        continue;
      }

      LinearLayout filaChips = (LinearLayout) fila;

      for (int j = 0; j < filaChips.getChildCount(); j++) {
        View vista = filaChips.getChildAt(j);

        if (vista instanceof Button && vista.isSelected()) {
          Object tag = vista.getTag();

          if (tag instanceof String) {
            resultado.add((String) tag);
          }
        }
      }
    }

    return resultado;
  }

  public static void limpiarSeleccion(Context context, LinearLayout contenedor) {
    if (context == null || contenedor == null) {
      return;
    }

    for (int i = 0; i < contenedor.getChildCount(); i++) {
      View fila = contenedor.getChildAt(i);

      if (!(fila instanceof LinearLayout)) {
        continue;
      }

      LinearLayout filaChips = (LinearLayout) fila;

      for (int j = 0; j < filaChips.getChildCount(); j++) {
        View vista = filaChips.getChildAt(j);

        if (vista instanceof Button) {
          Button chip = (Button) vista;
          chip.setSelected(false);
          aplicarEstiloInactivo(context, chip);
        }
      }
    }
  }

  private static List<String[]> filtrarCategoriasValidas(List<String[]> categorias) {
    List<String[]> resultado = new ArrayList<>();

    if (categorias == null) {
      return resultado;
    }

    for (String[] categoria : categorias) {
      if (categoria == null || categoria.length < 2) {
        continue;
      }

      String id = categoria[0];
      String nombre = categoria[1];

      if (id == null || id.trim().isEmpty()) {
        continue;
      }

      if (nombre == null || nombre.trim().isEmpty()) {
        continue;
      }

      resultado.add(new String[]{id.trim(), nombre.trim()});
    }

    return resultado;
  }

  private static void construirFilas(Context context,
                                     LinearLayout contenedor,
                                     List<String[]> categorias,
                                     Set<String> seleccionadas,
                                     boolean interactivo,
                                     OnChipClickListener listener,
                                     int anchoTotal) {
    if (context == null || contenedor == null) {
      return;
    }

    contenedor.removeAllViews();

    if (categorias == null || categorias.isEmpty()) {
      return;
    }

    int gapPx = dpToPx(context, GAP_DP);
    int padPx = dpToPx(context, PAD_H_DP);
    int margenV = dpToPx(context, MARGEN_V_DP);
    int alturaGlobal = dpToPx(context, 40);

    Paint paint = construirPaint(context);

    List<Integer> anchoTextos = new ArrayList<>();

    for (String[] categoria : categorias) {
      String nombre = categoria[1] != null ? categoria[1] : "";
      int anchoTexto = (int) Math.ceil(paint.measureText(nombre));
      anchoTextos.add(anchoTexto + padPx * 2);
    }

    List<List<Integer>> filas = empaquetar(anchoTextos, anchoTotal, gapPx);

    for (List<Integer> indicesFila : filas) {
      int numChips = indicesFila.size();

      LinearLayout fila = new LinearLayout(context);
      fila.setOrientation(LinearLayout.HORIZONTAL);

      LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT
      );

      filaLp.bottomMargin = margenV;
      fila.setLayoutParams(filaLp);

      if (numChips == 1) {
        fila.setGravity(Gravity.CENTER_HORIZONTAL);

        int indiceCategoria = indicesFila.get(0);
        String[] categoria = categorias.get(indiceCategoria);

        Button chip = crearChip(
                context,
                fila,
                categoria,
                seleccionadas,
                interactivo,
                listener
        );

        chip.setLayoutParams(new LinearLayout.LayoutParams(
                anchoTextos.get(indiceCategoria),
                alturaGlobal
        ));

        fila.addView(chip);
      } else {
        fila.setGravity(Gravity.START);

        int totalGaps = gapPx * (numChips - 1);
        int anchoRepartible = anchoTotal - totalGaps;
        int sumaMinimos = 0;

        for (int indice : indicesFila) {
          sumaMinimos += anchoTextos.get(indice);
        }

        if (sumaMinimos <= 0) {
          continue;
        }

        for (int posicion = 0; posicion < numChips; posicion++) {
          int chipIndice = indicesFila.get(posicion);
          String[] categoria = categorias.get(chipIndice);

          int anchoChip = (int) Math.round(
                  (double) anchoTextos.get(chipIndice) / sumaMinimos * anchoRepartible
          );

          Button chip = crearChip(
                  context,
                  fila,
                  categoria,
                  seleccionadas,
                  interactivo,
                  listener
          );

          LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                  anchoChip,
                  alturaGlobal
          );

          if (posicion < numChips - 1) {
            chipLp.rightMargin = gapPx;
          }

          chip.setLayoutParams(chipLp);
          fila.addView(chip);
        }
      }

      contenedor.addView(fila);
    }
  }

  private static List<List<Integer>> empaquetar(List<Integer> anchos,
                                                int anchoTotal,
                                                int gapPx) {
    List<List<Integer>> filas = new ArrayList<>();
    List<Integer> pendientes = new ArrayList<>();

    for (int i = 0; i < anchos.size(); i++) {
      pendientes.add(i);
    }

    boolean encontrado = true;

    while (encontrado && pendientes.size() >= 3) {
      encontrado = false;

      buscarTres:
      for (int a = 0; a < pendientes.size() - 2; a++) {
        for (int b = a + 1; b < pendientes.size() - 1; b++) {
          for (int c = b + 1; c < pendientes.size(); c++) {
            int indiceA = pendientes.get(a);
            int indiceB = pendientes.get(b);
            int indiceC = pendientes.get(c);

            int anchoFila = anchos.get(indiceA)
                    + anchos.get(indiceB)
                    + anchos.get(indiceC)
                    + gapPx * 2;

            if (anchoFila <= anchoTotal) {
              List<Integer> fila = new ArrayList<>();
              fila.add(indiceA);
              fila.add(indiceB);
              fila.add(indiceC);

              filas.add(fila);

              pendientes.remove(Integer.valueOf(indiceC));
              pendientes.remove(Integer.valueOf(indiceB));
              pendientes.remove(Integer.valueOf(indiceA));

              encontrado = true;
              break buscarTres;
            }
          }
        }
      }
    }

    encontrado = true;

    while (encontrado && pendientes.size() >= 2) {
      encontrado = false;

      buscarDos:
      for (int a = 0; a < pendientes.size() - 1; a++) {
        for (int b = a + 1; b < pendientes.size(); b++) {
          int indiceA = pendientes.get(a);
          int indiceB = pendientes.get(b);

          int anchoFila = anchos.get(indiceA)
                  + anchos.get(indiceB)
                  + gapPx;

          if (anchoFila <= anchoTotal) {
            List<Integer> fila = new ArrayList<>();
            fila.add(indiceA);
            fila.add(indiceB);

            filas.add(fila);

            pendientes.remove(Integer.valueOf(indiceB));
            pendientes.remove(Integer.valueOf(indiceA));

            encontrado = true;
            break buscarDos;
          }
        }
      }
    }

    for (int indice : pendientes) {
      List<Integer> fila = new ArrayList<>();
      fila.add(indice);
      filas.add(fila);
    }

    return filas;
  }

  private static Button crearChip(Context context,
                                  LinearLayout parent,
                                  String[] categoria,
                                  Set<String> seleccionadas,
                                  boolean interactivo,
                                  OnChipClickListener listener) {
    Button chip = inflarChip(context, parent);

    String categoriaId = categoria[0];
    String nombreCategoria = categoria[1];

    chip.setText(nombreCategoria);
    chip.setTag(categoriaId);

    boolean seleccionado = seleccionadas != null && seleccionadas.contains(categoriaId);

    chip.setSelected(seleccionado);

    if (interactivo) {
      if (seleccionado) {
        aplicarEstiloActivo(context, chip);
      } else {
        aplicarEstiloInactivo(context, chip);
      }
    } else {
      aplicarEstiloActivo(context, chip);
      chip.setClickable(false);
      chip.setFocusable(false);
      chip.setEnabled(false);
    }

    if (interactivo) {
      chip.setOnClickListener(v -> {
        boolean ahoraSeleccionado = !chip.isSelected();

        chip.setSelected(ahoraSeleccionado);

        if (ahoraSeleccionado) {
          aplicarEstiloActivo(context, chip);
        } else {
          aplicarEstiloInactivo(context, chip);
        }

        if (listener != null) {
          listener.onChipClick(chip, categoriaId, ahoraSeleccionado);
        }
      });
    }

    return chip;
  }

  private static Button inflarChip(Context context, ViewGroup parent) {
    Button chip = (Button) LayoutInflater.from(context)
            .inflate(R.layout.bt_secundario, parent, false);

    chip.setMinWidth(0);
    chip.setMinHeight(0);
    chip.setPadding(0, 0, 0, 0);
    chip.setAllCaps(false);

    return chip;
  }

  private static void aplicarEstiloActivo(Context context, Button chip) {
    if (context == null || chip == null) {
      return;
    }

    chip.setBackgroundResource(R.drawable.bg_boton_principal);

    int colorTexto = obtenerColorSeguro(
            context,
            R.color.clank_background_light,
            COLOR_TEXTO_ACTIVO_FALLBACK
    );

    chip.setTextColor(colorTexto);
  }

  private static void aplicarEstiloInactivo(Context context, Button chip) {
    if (context == null || chip == null) {
      return;
    }

    chip.setBackgroundResource(R.drawable.bg_boton_secundario);

    int colorTexto = obtenerColorSeguro(
            context,
            R.color.color_texto_inactivo,
            COLOR_TEXTO_INACTIVO_FALLBACK
    );

    chip.setTextColor(colorTexto);
  }

  private static int obtenerColorSeguro(Context context, int colorResId, int colorFallback) {
    if (context == null) {
      return colorFallback;
    }

    try {
      return ContextCompat.getColor(context, colorResId);
    } catch (Exception e) {
      return colorFallback;
    }
  }

  private static int dpToPx(Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }

  private static float spToPx(Context context, int sp) {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.getResources().getDisplayMetrics()
    );
  }

  private static Paint construirPaint(Context context) {
    Paint paint = new Paint();
    paint.setTextSize(spToPx(context, TEXT_SIZE_SP));

    try {
      Typeface typeface = ResourcesCompat.getFont(context, R.font.poppins_semibold);

      if (typeface != null) {
        paint.setTypeface(typeface);
      }
    } catch (Exception ignored) {
      try {
        Typeface typeface = ResourcesCompat.getFont(context, R.font.poppins_family);

        if (typeface != null) {
          paint.setTypeface(typeface);
        }
      } catch (Exception ignored2) {
      }
    }

    return paint;
  }
}