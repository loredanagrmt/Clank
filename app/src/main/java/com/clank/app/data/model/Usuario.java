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

  public Usuario() {
  }

  public String obtenerUid() {
    return uid;
  }

  public void establecerUid(String uid) {
    this.uid = uid;
  }

  public String obtenerCorreo() {
    return correo;
  }

  public void establecerCorreo(String correo) {
    this.correo = correo;
  }

  public String obtenerNombre() {
    return nombre;
  }

  public void establecerNombre(String nombre) {
    this.nombre = nombre;
  }

  public String obtenerTelefono() {
    return telefono;
  }

  public void establecerTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String obtenerFotoPerfil() {
    return fotoPerfil;
  }

  public void establecerFotoPerfil(String fotoPerfil) {
    this.fotoPerfil = fotoPerfil;
  }

  public String obtenerFechaCreacion() {
    return fechaCreacion;
  }

  public void establecerFechaCreacion(String fechaCreacion) {
    this.fechaCreacion = fechaCreacion;
  }

  public String obtenerFechaNacimiento() {
    return fechaNacimiento;
  }

  public void establecerFechaNacimiento(String fechaNacimiento) {
    this.fechaNacimiento = fechaNacimiento;
  }

  public long obtenerUltimaConexion() {
    return ultimaConexion;
  }

  public void establecerUltimaConexion(long ultimaConexion) {
    this.ultimaConexion = ultimaConexion;
  }

  public boolean estaEnLinea() {
    return enLinea;
  }

  public void establecerEnLinea(boolean enLinea) {
    this.enLinea = enLinea;
  }
}