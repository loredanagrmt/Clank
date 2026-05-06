package com.clank.app.ui.comun;

import androidx.annotation.DrawableRes;

import android.view.View;

public interface NavbarHost {
  void mostrarNavbar(String titulo);

  void mostrarNavbar(String titulo, @DrawableRes int iconoAccion, View.OnClickListener onAccion);

  void ocultarNavbar();
}
