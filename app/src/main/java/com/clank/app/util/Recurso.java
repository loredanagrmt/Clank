package com.clank.app.util;

public class Recurso<T> {
  public enum Estado { EXITO, ERROR, CARGANDO }

  public final Estado estado;
  public final T data;
  public final String mensaje;

  private Recurso(Estado estado, T data, String mensaje) {
    this.estado  = estado;
    this.data    = data;
    this.mensaje = mensaje;
  }

  public static <T> Recurso<T> exito(T data) {
    return new Recurso<>(Estado.EXITO, data, null);
  }

  public static <T> Recurso<T> error(String msg) {
    return new Recurso<>(Estado.ERROR, null, msg);
  }

  public static <T> Recurso<T> cargando() {
    return new Recurso<>(Estado.CARGANDO, null, null);
  }
}
