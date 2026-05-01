package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;

public class Material {

    /////////////////////////atributos documento/////////////////////////
    @DocumentId
    private String matId;
    private int cantidad;
    private String material;


    public Material() {}

    /////////////////////////Getters y setters/////////////////////////
    public String getMatId() { return matId; }
    public void setMatId(String matId) { this.matId = matId; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
}
