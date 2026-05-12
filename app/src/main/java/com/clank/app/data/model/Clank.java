package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class Clank {

  /////////////////////////atributos documento/////////////////////////
  @DocumentId
  private String clankId;
  private String usuarioId;
  private String titulo;
  private String descripcion;
  private String portada;
  private int tiempo;
  private int numLikes;
  private boolean estadoAcabado;
  private List<String> categorias;
  @ServerTimestamp
  private Date fechaPublicacion;


  public Clank() {}

  /////////////////////////Getters y setters/////////////////////////
  @Exclude
  public String getClankId() { return clankId; }
  public void setClankId(String clankId) { this.clankId = clankId; }
  public String getUsuarioId() { return usuarioId; }
  public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }
  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public String getPortada() { return portada; }
  public void setPortada(String portada) { this.portada = portada; }
  public int getTiempo() { return tiempo; }
  public void setTiempo(int tiempo) { this.tiempo = tiempo; }
  public int getNumLikes() { return numLikes; }
  public void setNumLikes(int numLikes) { this.numLikes = numLikes; }
  public boolean isEstadoAcabado() { return estadoAcabado; }
  public void setEstadoAcabado(boolean estadoAcabado) { this.estadoAcabado = estadoAcabado; }
  public List<String> getCategorias() { return categorias; }
  public void setCategorias(List<String> categorias) { this.categorias = categorias; }
  public Date getFechaPublicacion() { return fechaPublicacion; }
  public void setFechaPublicacion(Date fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
}
