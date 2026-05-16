package com.clank.app.ui.comun;

import androidx.annotation.DrawableRes;

import android.view.View;

public interface NavbarHost {
  void mostrarNavbar(String titulo);

  //título y botón de acción derecho
  void mostrarNavbar(String titulo, @DrawableRes int iconoAccion, View.OnClickListener onAccion);

  //flecha volver + título
  void mostrarNavbarConVolver(String titulo);

  //flecha volver + título + acción derecha
  void mostrarNavbarConVolver(String titulo, @DrawableRes int iconoAccion, View.OnClickListener onAccion);

  //ocultar navbar
  void ocultarNavbar();
}
