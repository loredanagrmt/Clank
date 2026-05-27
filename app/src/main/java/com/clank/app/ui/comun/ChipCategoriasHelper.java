package com.clank.app.ui.comun;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
  private static final int CHIP_PAD_V_DP = 10;
  private static final int MARGEN_V_DP = 8;
  private static final int TEXT_SIZE_SP = 18;

  public interface OnChipClickListener {
    void onChipClick(Button chip, String categoriaId, boolean seleccionado);
  }

  public static void cargarChipsInteractivos(
    Context context, LinearLayout contenedor,
    List<String[]> categorias, Set<String> seleccionadas,
    OnChipClickListener listener) {
    contenedor.removeAllViews();

    if (context == null || contenedor == null) {
      return;
    }

    if (categorias == null || categorias.isEmpty()) return;
    contenedor.getViewTreeObserver().addOnGlobalLayoutListener(
      new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          contenedor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          int anchoReal = contenedor.getWidth();
          if (anchoReal <= 0) return;
          construirFilas(context, contenedor, categorias,
            seleccionadas != null ? seleccionadas : new HashSet<>(),
            true, listener, anchoReal);
        }
      });
  }

  public static void cargarChipsVisuales(
    Context context, LinearLayout contenedor, List<String[]> categorias) {
    contenedor.removeAllViews();
    if (categorias == null || categorias.isEmpty()) return;
    contenedor.getViewTreeObserver().addOnGlobalLayoutListener(
      new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          contenedor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
          int anchoReal = contenedor.getWidth();
          if (anchoReal <= 0) return;
          construirFilas(context, contenedor, categorias,
            new HashSet<>(), false, null, anchoReal);
        }
      });
  }

  public static List<String> recogerSeleccionadas(LinearLayout contenedor) {
    List<String> resultado = new ArrayList<>();
    for (int i = 0; i < contenedor.getChildCount(); i++) {
      View fila = contenedor.getChildAt(i);
      if (!(fila instanceof LinearLayout)) continue;
      LinearLayout ll = (LinearLayout) fila;
      for (int j = 0; j < ll.getChildCount(); j++) {
        View v = ll.getChildAt(j);
        if (v instanceof Button && v.isSelected()) {
          Object tag = v.getTag();
          if (tag instanceof String) resultado.add((String) tag);
        }
      }
    }
    return resultado;
  }

  public static void limpiarSeleccion(Context context, LinearLayout contenedor) {
    for (int i = 0; i < contenedor.getChildCount(); i++) {
      View fila = contenedor.getChildAt(i);
      if (!(fila instanceof LinearLayout)) continue;
      LinearLayout ll = (LinearLayout) fila;
      for (int j = 0; j < ll.getChildCount(); j++) {
        View v = ll.getChildAt(j);
        if (v instanceof Button) {
          v.setSelected(false);
          aplicarEstiloInactivo(context, (Button) v);
        }
      }
    }
  }

  private static void construirFilas(
    Context context, LinearLayout contenedor, List<String[]> categorias,
    Set<String> seleccionadas, boolean interactivo,
    OnChipClickListener listener, int anchoTotal) {

    contenedor.removeAllViews();
    if (categorias == null || categorias.isEmpty()) return;

    int gapPx     = dpToPx(context, GAP_DP);
    int padPx     = dpToPx(context, PAD_H_DP);
    int padVertPx = dpToPx(context, CHIP_PAD_V_DP);
    int margenV   = dpToPx(context, MARGEN_V_DP);

    Paint paint     = construirPaint(context);
    int lineaAltoPx = (int) Math.ceil(paint.getFontSpacing());

    List<Integer> anchoTextos = new ArrayList<>();
    for (String[] cat : categorias) {
      int anchoTexto = (int) Math.ceil(paint.measureText(cat[1]));
      anchoTextos.add(anchoTexto + padPx * 2);
    }

    final int alturaGlobal = dpToPx(context, 40);

    List<List<Integer>> filas = empaquetar(anchoTextos, anchoTotal, gapPx);

    for (List<Integer> indicesFila : filas) {
      int numChips = indicesFila.size();

      LinearLayout fila = new LinearLayout(context);
      fila.setOrientation(LinearLayout.HORIZONTAL);
      LinearLayout.LayoutParams filaLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
      filaLp.bottomMargin = margenV;
      fila.setLayoutParams(filaLp);

      if (numChips == 1) {
        fila.setGravity(Gravity.CENTER_HORIZONTAL);
        int idx0     = indicesFila.get(0);
        String[] cat = categorias.get(idx0);
        Button chip  = crearChip(context, fila, cat, seleccionadas, interactivo, listener);
        chip.setLayoutParams(new LinearLayout.LayoutParams(
          anchoTextos.get(idx0), alturaGlobal));
        fila.addView(chip);

      } else {
        fila.setGravity(Gravity.START);
        int totalGaps       = gapPx * (numChips - 1);
        int anchoRepartible = anchoTotal - totalGaps;
        int sumaMinimos     = 0;
        for (int i : indicesFila) sumaMinimos += anchoTextos.get(i);

        for (int pos = 0; pos < numChips; pos++) {
          int chipIdx  = indicesFila.get(pos);
          String[] cat = categorias.get(chipIdx);

          int anchoChip = (int) Math.round(
            (double) anchoTextos.get(chipIdx) / sumaMinimos * anchoRepartible);

          Button chip = crearChip(context, fila, cat, seleccionadas, interactivo, listener);
          LinearLayout.LayoutParams chipLp =
            new LinearLayout.LayoutParams(anchoChip, alturaGlobal);
          if (pos < numChips - 1) chipLp.rightMargin = gapPx;
          chip.setLayoutParams(chipLp);
          fila.addView(chip);
        }
      }
      contenedor.addView(fila);
    }
  }

  private static List<List<Integer>> empaquetar(
    List<Integer> anchos, int anchoTotal, int gapPx) {

    List<List<Integer>> filas = new ArrayList<>();
    List<Integer> pendientes  = new ArrayList<>();
    for (int i = 0; i < anchos.size(); i++) pendientes.add(i);

    boolean encontrado = true;
    while (encontrado && pendientes.size() >= 3) {
      encontrado = false;
      outer3:
      for (int a = 0; a < pendientes.size() - 2; a++) {
        for (int b = a + 1; b < pendientes.size() - 1; b++) {
          for (int c = b + 1; c < pendientes.size(); c++) {
            int ia = pendientes.get(a);
            int ib = pendientes.get(b);
            int ic = pendientes.get(c);
            if (anchos.get(ia) + anchos.get(ib) + anchos.get(ic) + gapPx * 2 <= anchoTotal) {
              List<Integer> f = new ArrayList<>();
              f.add(ia); f.add(ib); f.add(ic);
              filas.add(f);
              pendientes.remove(Integer.valueOf(ic));
              pendientes.remove(Integer.valueOf(ib));
              pendientes.remove(Integer.valueOf(ia));
              encontrado = true;
              break outer3;
            }
          }
        }
      }
    }
    encontrado = true;
    while (encontrado && pendientes.size() >= 2) {
      encontrado = false;
      outer2:
      for (int a = 0; a < pendientes.size() - 1; a++) {
        for (int b = a + 1; b < pendientes.size(); b++) {
          int ia = pendientes.get(a);
          int ib = pendientes.get(b);
          if (anchos.get(ia) + anchos.get(ib) + gapPx <= anchoTotal) {
            List<Integer> f = new ArrayList<>();
            f.add(ia); f.add(ib);
            filas.add(f);
            pendientes.remove(Integer.valueOf(ib));
            pendientes.remove(Integer.valueOf(ia));
            encontrado = true;
            break outer2;
          }
        }
      }
    }

    for (int idx : pendientes) {
      List<Integer> f = new ArrayList<>();
      f.add(idx);
      filas.add(f);
    }

    return filas;
  }

  private static Button crearChip(
    Context context, LinearLayout parent, String[] cat,
    Set<String> seleccionadas, boolean interactivo, OnChipClickListener listener) {
    Button chip = inflarChip(context, parent);
    chip.setText(cat[1]);
    chip.setTag(cat[0]);

    boolean selec = seleccionadas != null && seleccionadas.contains(cat[0]);
    chip.setSelected(selec);
    if (interactivo && selec)  aplicarEstiloActivo(context, chip);
    else if (interactivo)      aplicarEstiloInactivo(context, chip);
    else                       aplicarEstiloActivo(context, chip);

    if (!interactivo) { chip.setClickable(false); chip.setFocusable(false); }

    if (interactivo) {
      final String catId = cat[0];
      chip.setOnClickListener(v -> {
        boolean ahora = !chip.isSelected();
        chip.setSelected(ahora);
        if (ahora) aplicarEstiloActivo(context, chip);
        else       aplicarEstiloInactivo(context, chip);
        if (listener != null) listener.onChipClick(chip, catId, ahora);
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
    return chip;
  }

  private static void aplicarEstiloActivo(Context context, Button chip) {
    chip.setBackgroundResource(R.drawable.bg_boton_principal);
    chip.setTextColor(ContextCompat.getColor(context, R.color.clank_background_light));
  }

  private static void aplicarEstiloInactivo(Context context, Button chip) {
    chip.setBackgroundResource(R.drawable.bg_boton_secundario);
    chip.setTextColor(ContextCompat.getColor(context, R.color.color_texto_inactivo));
  }

  private static int dpToPx(Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }

  private static float spToPx(Context context, int sp) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
      context.getResources().getDisplayMetrics());
  }

  private static Paint construirPaint(Context context) {
    Paint p = new Paint();
    p.setTextSize(spToPx(context, TEXT_SIZE_SP));
    try {
      Typeface tf = ResourcesCompat.getFont(context, R.font.poppins_semibold);
      if (tf != null) p.setTypeface(tf);
    } catch (Exception ignored) {
      try {
        Typeface tf = ResourcesCompat.getFont(context, R.font.poppins_family);
        if (tf != null) p.setTypeface(tf);
      } catch (Exception ignored2) {}
    }
    return p;
  }
}
