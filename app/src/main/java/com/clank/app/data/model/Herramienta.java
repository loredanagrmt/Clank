package com.clank.app.data.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Herramienta {

    /////////////////////////atributos documento/////////////////////////
    private String herrId;
    private String herramienta;


    public Herramienta() {}
  public Herramienta(String herrId, String herramienta) {
    this.herrId      = herrId;
    this.herramienta = herramienta;
  }


    /////////////////////////Getters y setters/////////////////////////
    public String getHerrId() { return herrId; }
    public void setHerrId(String herrId) { this.herrId = herrId; }

    public String getHerramienta() { return herramienta; }
    public void setHerramienta(String herramienta) { this.herramienta = herramienta; }
}
