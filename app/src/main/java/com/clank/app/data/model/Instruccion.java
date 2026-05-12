package com.clank.app.data.model;

import com.google.firebase.firestore.DocumentId;

public class Instruccion {

    /////////////////////////atributos documento/////////////////////////
    @DocumentId
    private String instruccionId;
    private int orden;
    private String instruccion;
    private String imagen;


    public Instruccion() {}

git
    /////////////////////////Getters y setters/////////////////////////
    public String getInstruccionId() { return instruccionId; }
    public void setInstruccionId(String instruccionId) { this.instruccionId = instruccionId; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public String getInstruccion() { return instruccion; }
    public void setInstruccion(String instruccion) { this.instruccion = instruccion; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
