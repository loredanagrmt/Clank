package com.clank.app.util;

public class Recurso<T> {
  public enum Status { SUCCESS, ERROR, LOADING }

  public final Status status;
  public final T data;
  public final String message;

  private Recurso(Status status, T data, String message) {
    this.status = status;
    this.data = data;
    this.message = message;
  }

  public static <T> Recurso<T> success(T data) {
    return new Recurso<>(Status.SUCCESS, data, null);
  }
  public static <T> Recurso<T> error(String msg) {
    return new Recurso<>(Status.ERROR, null, msg);
  }
  public static <T> Recurso<T> loading() {
    return new Recurso<>(Status.LOADING, null, null);
  }
}
