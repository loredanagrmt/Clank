package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;

public class Herramienta {

    /////////////////////////atributos documento/////////////////////////
    @DocumentId
    private String herrId;
    private String herramienta;


    public Herramienta() {}


    /////////////////////////Getters y setters/////////////////////////
    public String getHerrId() { return herrId; }
    public void setHerrId(String herrId) { this.herrId = herrId; }

    public String getHerramienta() { return herramienta; }
    public void setHerramienta(String herramienta) { this.herramienta = herramienta; }
}
