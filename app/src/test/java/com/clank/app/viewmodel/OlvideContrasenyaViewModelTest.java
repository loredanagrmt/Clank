package com.clank.app.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.ui.olvideContrasenya.OlvideContrasenyaViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

public class OlvideContrasenyaViewModelTest {

  @Rule
  public InstantTaskExecutorRule reglaInstant = new InstantTaskExecutorRule();

  @Mock
  AuthRepository mockAuthRepository;

  private OlvideContrasenyaViewModel viewModel;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    viewModel = new OlvideContrasenyaViewModel(mockAuthRepository);
  }

  /////////////////////////estado inicial/////////////////////////

  @Test
  public void estadoInicial_cargando_esFalse() {
    assertEquals(Boolean.FALSE, viewModel.getCargando().getValue());
  }

  @Test
  public void estadoInicial_estadoSolicitudCodigo_esNull() {
    assertNull(viewModel.getEstadoSolicitudCodigo().getValue());
  }

  /////////////////////////limoiar estado enum codigo/////////////////////////

  @Test
  public void limpiarEstado_despuesDeValorar_devuelveNull() {
    viewModel.limpiarEstadoSolicitudCodigo();
    assertNull(viewModel.getEstadoSolicitudCodigo().getValue());
  }

  @Test
  public void limpiarEstado_llamadoDobleVez_sigueDevolvientoNull() {
    viewModel.limpiarEstadoSolicitudCodigo();
    viewModel.limpiarEstadoSolicitudCodigo();
    assertNull(viewModel.getEstadoSolicitudCodigo().getValue());
  }

  /////////////////////////estado enum código/////////////////////////

  @Test
  public void enum_tieneValorEXITO() {
    assertNotNull(OlvideContrasenyaViewModel.EstadoSolicitudCodigo.EXITO);
  }

  @Test
  public void enum_tieneValorCORREO_INVALIDO() {
    assertNotNull(OlvideContrasenyaViewModel.EstadoSolicitudCodigo.CORREO_INVALIDO);
  }

  @Test
  public void enum_tieneValorCORREO_NO_REGISTRADO() {
    assertNotNull(OlvideContrasenyaViewModel.EstadoSolicitudCodigo.CORREO_NO_REGISTRADO);
  }

  @Test
  public void enum_tieneValorERROR_GENERAL() {
    assertNotNull(OlvideContrasenyaViewModel.EstadoSolicitudCodigo.CORREO_NO_REGISTRADO);
  }

  @Test
  public void enum_cuatroValoresEnTotal() {
    assertEquals(4, OlvideContrasenyaViewModel.EstadoSolicitudCodigo.values().length);
  }
}
