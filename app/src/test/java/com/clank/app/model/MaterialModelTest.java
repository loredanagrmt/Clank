package com.clank.app.model;

import com.clank.app.data.model.Material;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MaterialModelTest {

  private Material material;

  @Before
  public void setUp() {
    material = new Material();
  }

  /////////////////////////constructor vacío/////////////////////////

  @Test
  public void constructorVacio_todosLosCampos_sonNulosODefecto() {
    assertNull(material.getMatId());
    assertEquals(0, material.getCantidad());
    assertNull(material.getMaterial());
  }

  /////////////////////////constructor con parámetros/////////////////////////

  @Test
  public void constructorConParametros_valoresValidos_seGuardanCorrectamente() {
    Material m = new Material("m001", 3, "Madera de pino");
    assertEquals("m001", m.getMatId());
    assertEquals(3, m.getCantidad());
    assertEquals("Madera de pino", m.getMaterial());
  }

  @Test
  public void constructorConParametros_cantidadCero_getCantidadDevuelveCero() {
    Material m = new Material("m002", 0, "Tornillos");
    assertEquals(0, m.getCantidad());
  }

  @Test
  public void constructorConParametros_idNull_getMatIdDevuelveNull() {
    Material m = new Material(null, 5, "Pintura");
    assertNull(m.getMatId());
    assertEquals(5, m.getCantidad());
  }

  @Test
  public void constructorConParametros_nombreVacio_getMaterialDevuelveVacio() {
    Material m = new Material("m003", 2, "");
    assertEquals("", m.getMaterial());
  }

  /////////////////////////Getters y setters/////////////////////////

  @Test
  public void setMatId_conValorValido_getMatIdDevuelveElMismo() {
    material.setMatId("m004");
    assertEquals("m004", material.getMatId());
  }

  @Test
  public void setMatId_conNull_getMatIdDevuelveNull() {
    material.setMatId(null);
    assertNull(material.getMatId());
  }

  @Test
  public void setCantidad_conValorPositivo_getCantidadDevuelveElMismo() {
    material.setCantidad(10);
    assertEquals(10, material.getCantidad());
  }

  @Test
  public void setCantidad_conCero_getCantidadDevuelveCero() {
    material.setCantidad(0);
    assertEquals(0, material.getCantidad());
  }

  @Test
  public void setMaterial_conValorValido_getMaterialDevuelveElMismo() {
    material.setMaterial("Barniz");
    assertEquals("Barniz", material.getMaterial());
  }

  @Test
  public void setMaterial_conNull_getMaterialDevuelveNull() {
    material.setMaterial(null);
    assertNull(material.getMaterial());
  }

  /////////////////////////sobreescritura///////////////////////

  @Test
  public void setCantidad_sobreescribirValor_devuelveNuevoValor() {
    material.setCantidad(5);
    material.setCantidad(12);
    assertEquals(12, material.getCantidad());
  }

  /////////////////////////independencia entre instancias/////////////////////////

  @Test
  public void dosInstancias_modificarUna_noAfectaALaOtra() {
    Material m2 = new Material("m020", 8, "Cola blanca");
    material.setMaterial("Lija");
    assertNotEquals(material.getMaterial(), m2.getMaterial());
  }
}
