package com.clank.app.util;

import com.clank.app.util.TraductorTarjetaClank.TextoTarjetaTraducido;

import org.junit.Test;

import static org.junit.Assert.*;

public class TextoTarjetaTraducidoTest {

  /////////////////////////constructor valores válidos/////////////////////////

  @Test
  public void constructor_valoresValidos_seGuardanCorrectamente() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido("Silla", "Una silla de madera");
    assertEquals("Silla", texto.titulo);
    assertEquals("Una silla de madera", texto.descripcion);
  }

  /////////////////////////nulos/////////////////////////

  @Test
  public void constructor_tituloNull_tituloDevuelveCadenaVacia() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido(null, "Descripción");
    assertEquals("", texto.titulo);
  }

  @Test
  public void constructor_descripcionNull_descripcionDevuelveCadenaVacia() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido("Título", null);
    assertEquals("", texto.descripcion);
  }

  @Test
  public void constructor_ambosNull_ambosDevuelvenCadenaVacia() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido(null, null);
    assertEquals("", texto.titulo);
    assertEquals("", texto.descripcion);
  }

  /////////////////////////cadenas vacias/////////////////////////

  @Test
  public void constructor_tituloVacio_tituloDevuelveCadenaVacia() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido("", "Descripción");
    assertEquals("", texto.titulo);
  }

  @Test
  public void constructor_ambosVacios_ambosDevuelvenCadenaVacia() {
    TextoTarjetaTraducido texto = new TextoTarjetaTraducido("", "");
    assertEquals("", texto.titulo);
    assertEquals("", texto.descripcion);
  }
}
