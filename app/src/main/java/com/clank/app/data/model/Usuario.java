package com.clank.app.data.model;

public class Usuario {
  private String uid;
  private String nombre;
  private String email;

  public Usuario() {}

  public String getUid()      { return uid; }
  public String getNombre()   { return nombre; }
  public String getEmail()    { return email; }

  public void setUid(String uid)         { this.uid = uid; }
  public void setNombre(String nombre)   { this.nombre = nombre; }
  public void setEmail(String email)     { this.email = email; }
}
