package com.clank.app.model;

import com.clank.app.data.model.Clank;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class ClankModelTest {

  private Clank clank;

  @Before
  public void setUp() {
    clank = new Clank();
  }

  /////////////////////////constructor/////////////////////////

  @Test
  public void constructorVacio_todosLosCampos_sonNulosODefecto() {
    assertNull(clank.getClankId());
    assertNull(clank.getUsuarioId());
    assertNull(clank.getTitulo());
    assertNull(clank.getDescripcion());
    assertNull(clank.getPortada());
    assertEquals(0, clank.getTiempo());
    assertEquals(0, clank.getNumLikes());
    assertFalse(clank.isEstadoAcabado());
    assertNull(clank.getCategorias());
    assertNull(clank.getFechaPublicacion());
  }

  /////////////////////////Getters y setters/////////////////////////

  @Test
  public void setTitulo_conValorValido_getTituloDevuelveElMismo() {
    clank.setTitulo("Silla de madera");
    assertEquals("Silla de madera", clank.getTitulo());
  }

  @Test
  public void setTitulo_conStringVacio_getTituloDevuelveVacio() {
    clank.setTitulo("");
    assertEquals("", clank.getTitulo());
  }

  @Test
  public void setTitulo_conNull_getTituloDevuelveNull() {
    clank.setTitulo(null);
    assertNull(clank.getTitulo());
  }

  @Test
  public void setUsuarioId_conValorValido_getUsuarioIdDevuelveElMismo() {
    clank.setUsuarioId("uid_12345");
    assertEquals("uid_12345", clank.getUsuarioId());
  }

  @Test
  public void setDescripcion_conValorValido_getDescripcionDevuelveElMismo() {
    clank.setDescripcion("Una silla hecha a mano con madera de pino.");
    assertEquals("Una silla hecha a mano con madera de pino.", clank.getDescripcion());
  }

  @Test
  public void setPortada_conUrl_getPortadaDevuelveElMismo() {
    clank.setPortada("https://storage.firebase.com/img.jpg");
    assertEquals("https://storage.firebase.com/img.jpg", clank.getPortada());
  }
  @Test
  public void setTiempo_conValorPositivo_getTiempoDevuelveElMismo() {
    clank.setTiempo(90);
    assertEquals(90, clank.getTiempo());
  }
  @Test
  public void setTiempo_conCero_getTiempoDevuelveCero() {
    clank.setTiempo(0);
    assertEquals(0, clank.getTiempo());
  }

  @Test
  public void setNumLikes_conValorPositivo_getNumLikesDevuelveElMismo() {
    clank.setNumLikes(42);
    assertEquals(42, clank.getNumLikes());
  }
  @Test
  public void setEstadoAcabado_conTrue_isEstadoAcabadoDevuelveTrue() {
    clank.setEstadoAcabado(true);
    assertTrue(clank.isEstadoAcabado());
  }

  @Test
  public void setEstadoAcabado_conFalse_isEstadoAcabadoDevuelveFalse() {
    clank.setEstadoAcabado(false);
    assertFalse(clank.isEstadoAcabado());
  }

  @Test
  public void setCategorias_conLista_getCategoriasDevuelveLaMisma() {
    List<String> cats = Arrays.asList("Madera", "Decoración");
    clank.setCategorias(cats);
    assertEquals(cats, clank.getCategorias());
    assertEquals(2, clank.getCategorias().size());
  }

  @Test
  public void setCategorias_conListaVacia_getCategoriasDevuelveListaVacia() {
    clank.setCategorias(Arrays.asList());
    assertNotNull(clank.getCategorias());
    assertEquals(0, clank.getCategorias().size());
  }
  @Test
  public void setFechaPublicacion_conFecha_getFechaDevuelveLaMisma() {
    Date ahora = new Date();
    clank.setFechaPublicacion(ahora);
    assertEquals(ahora, clank.getFechaPublicacion());
  }

  @Test
  public void dosInstancias_conMismosTitulos_sonIndependientes() {
    Clank clank2 = new Clank();
    clank.setTitulo("Título A");
    clank2.setTitulo("Título B");
    assertNotEquals(clank.getTitulo(), clank2.getTitulo());
  }
}
