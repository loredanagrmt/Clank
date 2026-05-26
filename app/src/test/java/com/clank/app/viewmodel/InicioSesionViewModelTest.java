package com.clank.app.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.ui.inicioSesion.InicioSesionViewModel;
import com.clank.app.util.Recurso;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

public class InicioSesionViewModelTest {

  @Rule
  public InstantTaskExecutorRule reglaInstant = new InstantTaskExecutorRule();

  @Mock
  AuthRepository mockRepositorioAuth;

  @Mock
  UsuarioRepository mockRepositorioUsuario;

  private InicioSesionViewModel viewModel;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    viewModel = new InicioSesionViewModel(
      mockRepositorioAuth,
      mockRepositorioUsuario
    );
  }

  /////////////////////////estado inicial/////////////////////////

  @Test
  public void estadoInicial_resultadoInicioSesion_esNull() {
    assertNull(viewModel.obtenerResultadoInicioSesion().getValue());
  }

  /////////////////////////validaciones iniciar sesion/////////////////////////

  @Test
  public void iniciarSesion_correoVacio_emiteEstadoError() {
    viewModel.iniciarSesion("", "contrasenya123");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_contrasenyaVacia_emiteEstadoError() {
    viewModel.iniciarSesion("test@clank.com", "");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_ambosVacios_emiteEstadoError() {
    viewModel.iniciarSesion("", "");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_correoNull_emiteEstadoError() {
    viewModel.iniciarSesion(null, "contrasenya123");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_contrasenyaNull_emiteEstadoError() {
    viewModel.iniciarSesion("test@clank.com", null);
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_soloEspacios_emiteEstadoError() {
    viewModel.iniciarSesion("   ", "   ");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesion_camposValidos_emiteMensajeErrorNoNulo() {
    viewModel.iniciarSesion("", "pass");
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertNotNull(resultado.mensaje);
    assertFalse(resultado.mensaje.isEmpty());
  }

  /////////////////////////validaciones iniciar sesion google/////////////////////////

  @Test
  public void iniciarSesionGoogle_cuentaNull_emiteEstadoError() {
    viewModel.iniciarSesionGoogle(null);
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado);
    assertEquals(Recurso.Estado.ERROR, resultado.estado);
  }

  @Test
  public void iniciarSesionGoogle_cuentaNull_mensajeNoEsNulo() {
    viewModel.iniciarSesionGoogle(null);
    Recurso<?> resultado = viewModel.obtenerResultadoInicioSesion().getValue();
    assertNotNull(resultado.mensaje);
    assertFalse(resultado.mensaje.isEmpty());
  }
}
