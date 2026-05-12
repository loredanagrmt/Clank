package com.clank.app.data.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Material {

    /////////////////////////atributos documento/////////////////////////
    private String matId;
    private int cantidad;
    private String material;


    public Material() {}
  public Material(String matId, int cantidad, String material) {
    this.matId    = matId;
    this.cantidad = cantidad;
    this.material = material;
  }

    /////////////////////////Getters y setters/////////////////////////
    public String getMatId() { return matId; }
    public void setMatId(String matId) { this.matId = matId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
}
