package com.clank.app.model;

import com.clank.app.data.model.Herramienta;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HerramientaModelTest {

  private Herramienta herramienta;

  @Before
  public void setUp() {
    herramienta = new Herramienta();
  }

  /////////////////////////constructor vacío/////////////////////////

  @Test
  public void constructorVacio_todosLosCampos_sonNulos() {
    assertNull(herramienta.getHerrId());
    assertNull(herramienta.getHerramienta());
  }

  /////////////////////////constructor con parámetros/////////////////////////

  @Test
  public void constructorConParametros_valoresValidos_seGuardanCorrectamente() {
    Herramienta h = new Herramienta("h001", "Martillo");
    assertEquals("h001", h.getHerrId());
    assertEquals("Martillo", h.getHerramienta());
  }

  @Test
  public void constructorConParametros_idNull_getHerrIdDevuelveNull() {
    Herramienta h = new Herramienta(null, "Sierra");
    assertNull(h.getHerrId());
    assertEquals("Sierra", h.getHerramienta());
  }

  @Test
  public void constructorConParametros_nombreVacio_getHerramientaDevuelveVacio() {
    Herramienta h = new Herramienta("h002", "");
    assertEquals("h002", h.getHerrId());
    assertEquals("", h.getHerramienta());
  }

  /////////////////////////Getters y setters/////////////////////////

  @Test
  public void setHerrId_conValorValido_getHerrIdDevuelveElMismo() {
    herramienta.setHerrId("h003");
    assertEquals("h003", herramienta.getHerrId());
  }

  @Test
  public void setHerrId_conNull_getHerrIdDevuelveNull() {
    herramienta.setHerrId(null);
    assertNull(herramienta.getHerrId());
  }

  @Test
  public void setHerramienta_conValorValido_getHerramientaDevuelveElMismo() {
    herramienta.setHerramienta("Destornillador");
    assertEquals("Destornillador", herramienta.getHerramienta());
  }

  @Test
  public void setHerramienta_conNull_getHerramientaDevuelveNull() {
    herramienta.setHerramienta(null);
    assertNull(herramienta.getHerramienta());
  }

  /////////////////////////sobreescritura/////////////////////////

  @Test
  public void setHerramienta_sobreescribirValor_devuelveNuevoValor() {
    herramienta.setHerramienta("Lima");
    herramienta.setHerramienta("Cepillo");
    assertEquals("Cepillo", herramienta.getHerramienta());
  }

  /////////////////////////independencia entre instancias/////////////////////////

  @Test
  public void dosInstancias_modificarUna_noAfectaALaOtra() {
    Herramienta h2 = new Herramienta("h010", "Taladro");
    herramienta.setHerramienta("Cincel");
    assertNotEquals(herramienta.getHerramienta(), h2.getHerramienta());
  }
}
