package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;

public class Categoria {

    /////////////////////////atributos documento/////////////////////////
    @DocumentId
    private String catId;
    private String categoria;

    public Categoria() {}


    /////////////////////////Getters y setters/////////////////////////
    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
}
