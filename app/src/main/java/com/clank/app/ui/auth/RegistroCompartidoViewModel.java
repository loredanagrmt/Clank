package com.clank.app.ui.auth;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class RegistroCompartidoViewModel extends AndroidViewModel {

    private static final String PREFERENCIAS_REGISTRO = "registro_temporal";

    private static final String CLAVE_NOMBRE = "nombre";
    private static final String CLAVE_CORREO = "correo";
    private static final String CLAVE_TELEFONO = "telefono";
    private static final String CLAVE_FECHA_NACIMIENTO = "fechaNacimiento";
    private static final String CLAVE_CONTRASENYA = "contrasenya";
    private static final String CLAVE_CONFIRMAR_CONTRASENYA = "confirmarContrasenya";
    private static final String CLAVE_REGISTRO_CON_GOOGLE = "registroConGoogle";
    private static final String CLAVE_FOTO_PERFIL_GOOGLE = "fotoPerfilGoogle";

    private final SharedPreferences preferencias;

    private String nombre = "";
    private String correo = "";
    private String telefono = "";
    private String fechaNacimiento = "";
    private String contrasenya = "";
    private String confirmarContrasenya = "";
    private boolean registroConGoogle = false;
    private String fotoPerfilGoogle = "";

    public RegistroCompartidoViewModel(@NonNull Application application) {
        super(application);

        preferencias = application.getSharedPreferences(PREFERENCIAS_REGISTRO, Context.MODE_PRIVATE);

        recargarDatosGuardados();
    }

    public void iniciarNuevoRegistro() {
        limpiar();
    }

    public void iniciarNuevoRegistroGoogle(String nombre, String correo, String fotoPerfilGoogle) {
        limpiar();

        this.registroConGoogle = true;
        this.nombre = limpiarTexto(nombre);
        this.correo = limpiarTexto(correo);
        this.fotoPerfilGoogle = limpiarTexto(fotoPerfilGoogle);

        preferencias.edit()
                .putBoolean(CLAVE_REGISTRO_CON_GOOGLE, true)
                .putString(CLAVE_NOMBRE, this.nombre)
                .putString(CLAVE_CORREO, this.correo)
                .putString(CLAVE_FOTO_PERFIL_GOOGLE, this.fotoPerfilGoogle)
                .commit();
    }

    public void guardarDatosRegistro(String nombre,
                                     String correo,
                                     String telefono,
                                     String fechaNacimiento,
                                     String contrasenya,
                                     String confirmarContrasenya) {
        this.nombre = limpiarTexto(nombre);
        this.correo = limpiarTexto(correo);
        this.telefono = limpiarTexto(telefono);
        this.fechaNacimiento = limpiarTexto(fechaNacimiento);
        this.contrasenya = limpiarContrasenya(contrasenya);
        this.confirmarContrasenya = limpiarContrasenya(confirmarContrasenya);

        preferencias.edit()
                .putString(CLAVE_NOMBRE, this.nombre)
                .putString(CLAVE_CORREO, this.correo)
                .putString(CLAVE_TELEFONO, this.telefono)
                .putString(CLAVE_FECHA_NACIMIENTO, this.fechaNacimiento)
                .putString(CLAVE_CONTRASENYA, this.contrasenya)
                .putString(CLAVE_CONFIRMAR_CONTRASENYA, this.confirmarContrasenya)
                .putBoolean(CLAVE_REGISTRO_CON_GOOGLE, this.registroConGoogle)
                .putString(CLAVE_FOTO_PERFIL_GOOGLE, this.fotoPerfilGoogle)
                .commit();
    }

    public void recargarDatosGuardados() {
        nombre = preferencias.getString(CLAVE_NOMBRE, "");
        correo = preferencias.getString(CLAVE_CORREO, "");
        telefono = preferencias.getString(CLAVE_TELEFONO, "");
        fechaNacimiento = preferencias.getString(CLAVE_FECHA_NACIMIENTO, "");
        contrasenya = preferencias.getString(CLAVE_CONTRASENYA, "");
        confirmarContrasenya = preferencias.getString(CLAVE_CONFIRMAR_CONTRASENYA, "");
        registroConGoogle = preferencias.getBoolean(CLAVE_REGISTRO_CON_GOOGLE, false);
        fotoPerfilGoogle = preferencias.getString(CLAVE_FOTO_PERFIL_GOOGLE, "");
    }

    public boolean tieneDatosRegistro() {
        recargarDatosGuardados();

        if (registroConGoogle) {
            return !estaVacio(nombre)
                    && !estaVacio(correo)
                    && !estaVacio(fechaNacimiento);
        }

        return !estaVacio(nombre)
                && !estaVacio(correo)
                && !estaVacio(fechaNacimiento)
                && !estaVacio(contrasenya);
    }

    public String getNombre() {
        recargarDatosGuardados();
        return nombre;
    }

    public String getCorreo() {
        recargarDatosGuardados();
        return correo;
    }

    public String getTelefono() {
        recargarDatosGuardados();
        return telefono;
    }

    public String getFechaNacimiento() {
        recargarDatosGuardados();
        return fechaNacimiento;
    }

    public String getContrasenya() {
        recargarDatosGuardados();
        return contrasenya;
    }

    public String getConfirmarContrasenya() {
        recargarDatosGuardados();
        return confirmarContrasenya;
    }

    public boolean isRegistroConGoogle() {
        recargarDatosGuardados();
        return registroConGoogle;
    }

    public String getFotoPerfilGoogle() {
        recargarDatosGuardados();
        return fotoPerfilGoogle;
    }

    public void limpiar() {
        nombre = "";
        correo = "";
        telefono = "";
        fechaNacimiento = "";
        contrasenya = "";
        confirmarContrasenya = "";
        registroConGoogle = false;
        fotoPerfilGoogle = "";

        preferencias.edit()
                .remove(CLAVE_NOMBRE)
                .remove(CLAVE_CORREO)
                .remove(CLAVE_TELEFONO)
                .remove(CLAVE_FECHA_NACIMIENTO)
                .remove(CLAVE_CONTRASENYA)
                .remove(CLAVE_CONFIRMAR_CONTRASENYA)
                .remove(CLAVE_REGISTRO_CON_GOOGLE)
                .remove(CLAVE_FOTO_PERFIL_GOOGLE)
                .commit();
    }

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        return texto.trim();
    }

    private String limpiarContrasenya(String contrasenya) {
        if (contrasenya == null) {
            return "";
        }

        return contrasenya;
    }

    private boolean estaVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}