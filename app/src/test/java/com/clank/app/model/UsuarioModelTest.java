package com.clank.app.model;

import com.clank.app.data.model.Usuario;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UsuarioModelTest {

  private Usuario usuario;

  @Before
  public void setUp() {
    usuario = new Usuario();
  }

  /////////////////////////constructor/////////////////////////

  @Test
  public void constructorVacio_todosLosCampos_sonNulosODefecto() {
    assertNull(usuario.getUid());
    assertNull(usuario.getCorreo());
    assertNull(usuario.getNombre());
    assertNull(usuario.getTelefono());
    assertNull(usuario.getFotoPerfil());
    assertNull(usuario.getFechaCreacion());
    assertNull(usuario.getFechaNacimiento());
    assertNull(usuario.getUsuarioClank());
  }
  /////////////////////////Getters y setters/////////////////////////

  @Test
  public void setUid_conValorValido_getUidDevuelveElMismo() {
    usuario.setUid("abc123");
    assertEquals("abc123", usuario.getUid());
  }

  @Test
  public void setUid_conNull_getUidDevuelveNull() {
    usuario.setUid(null);
    assertNull(usuario.getUid());
  }

  @Test
  public void setCorreo_conEmailValido_getCorreoDevuelveElMismo() {
    usuario.setCorreo("test@clank.com");
    assertEquals("test@clank.com", usuario.getCorreo());
  }

  @Test
  public void setCorreo_conStringVacio_getCorreoDevuelveVacio() {
    usuario.setCorreo("");
    assertEquals("", usuario.getCorreo());
  }

  @Test
  public void setNombre_conValorValido_getNombreDevuelveElMismo() {
    usuario.setNombre("María García");
    assertEquals("María García", usuario.getNombre());
  }

  @Test
  public void setNombre_conNull_getNombreDevuelveNull() {
    usuario.setNombre(null);
    assertNull(usuario.getNombre());
  }

  @Test
  public void setTelefono_conNumeroValido_getTelefonoDevuelveElMismo() {
    usuario.setTelefono("612345678");
    assertEquals("612345678", usuario.getTelefono());
  }

  @Test
  public void setTelefono_conNull_getTelefonoDevuelveNull() {
    usuario.setTelefono(null);
    assertNull(usuario.getTelefono());
  }

  @Test
  public void setFotoPerfil_conUrl_getFotoPerfilDevuelveElMismo() {
    usuario.setFotoPerfil("https://storage.firebase.com/foto.jpg");
    assertEquals("https://storage.firebase.com/foto.jpg", usuario.getFotoPerfil());
  }

  @Test
  public void setFechaCreacion_conFecha_getFechaCreacionDevuelveElMismo() {
    usuario.setFechaCreacion("2024-01-15");
    assertEquals("2024-01-15", usuario.getFechaCreacion());
  }

  @Test
  public void setFechaNacimiento_conFecha_getFechaNacimientoDevuelveElMismo() {
    usuario.setFechaNacimiento("1999-06-20");
    assertEquals("1999-06-20", usuario.getFechaNacimiento());
  }

  @Test
  public void setUsuarioClank_conNombreUsuario_getUsuarioClankDevuelveElMismo() {
    usuario.setUsuarioClank("mariag_craft");
    assertEquals("mariag_craft", usuario.getUsuarioClank());
  }

  @Test
  public void setUsuarioClank_conStringVacio_getUsuarioClankDevuelveVacio() {
    usuario.setUsuarioClank("");
    assertEquals("", usuario.getUsuarioClank());
  }

  //independencia entre instancias

  @Test
  public void dosInstancias_conDistintosUid_sonIndependientes() {
    Usuario usuario2 = new Usuario();
    usuario.setUid("uid_A");
    usuario2.setUid("uid_B");
    assertNotEquals(usuario.getUid(), usuario2.getUid());
  }

  @Test
  public void setNombre_sobreescribirValor_getNombreDevuelveNuevoValor() {
    usuario.setNombre("Carlos");
    usuario.setNombre("Ana");
    assertEquals("Ana", usuario.getNombre());
  }
}
