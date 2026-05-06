package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;

public class Usuario {

  @DocumentId
  private String uid;
  private String correo;
  private String nombre;
  private String telefono;
  private String fotoPerfil;
  private String fechaCreacion;
  private String fechaNacimiento;
  private long ultimaConexion;
  private boolean enLinea;
  private String usuarioClank;

  public Usuario() {
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getFotoPerfil() {
    return fotoPerfil;
  }

  public void setFotoPerfil(String fotoPerfil) {
    this.fotoPerfil = fotoPerfil;
  }

  public String getFechaCreacion() {
    return fechaCreacion;
  }

  public void setFechaCreacion(String fechaCreacion) {
    this.fechaCreacion = fechaCreacion;
  }

  public String getFechaNacimiento() {
    return fechaNacimiento;
  }

  public void setFechaNacimiento(String fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public long getUltimaConexion() {
    return ultimaConexion;
  }

  public void setUltimaConexion(long ultimaConexion) {
    this.ultimaConexion = ultimaConexion;
  }

  public boolean isEnLinea() {
    return enLinea;
  }

  public void setEnLinea(boolean enLinea) {
    this.enLinea = enLinea;
  }

  public String getUsuarioClank() {
    return usuarioClank;
  }

  public void setUsuarioClank(String usuarioClank) {
    this.usuarioClank = usuarioClank;
  }
}