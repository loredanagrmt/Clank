package com.clank.app.ui.comun;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

public interface NavbarHost {

  void mostrarNavbar(String titulo);

  // título y botón de acción derecho
  void mostrarNavbar(
          String titulo,
          @DrawableRes int iconoAccion,
          View.OnClickListener onAccion
  );

  void mostrarNavbar(
          String titulo,
          @Nullable Integer iconoAccion,
          @Nullable View.OnClickListener onAccion
  );

  // flecha volver + título
  void mostrarNavbarConVolver(String titulo);

  // flecha volver + título + acción derecha
  void mostrarNavbarConVolver(
          String titulo,
          @DrawableRes int iconoAccion,
          View.OnClickListener onAccion
  );

  // título + botón de acción derecho + botón de filtrar
  void mostrarNavbarConAccionYFiltro(
          String titulo,
          @DrawableRes int iconoAccion,
          View.OnClickListener onAccion,
          View.OnClickListener onFiltrar
  );

  // permite sobrescribir temporalmente la acción de la flecha de volver
  void configurarAccionVolver(@Nullable View.OnClickListener onVolver);

  // habilita o bloquea la flecha de volver, útil durante guardados
  void habilitarVolverNavbar(boolean habilitado);

  // ocultar navbar
  void ocultarNavbar();
}