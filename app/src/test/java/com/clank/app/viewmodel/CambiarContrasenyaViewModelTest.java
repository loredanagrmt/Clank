package com.clank.app.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.ui.cambiarContrasenya.CambiarContrasenyaViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

public class CambiarContrasenyaViewModelTest {

  @Rule
  public InstantTaskExecutorRule reglaInstant = new InstantTaskExecutorRule();

  @Mock
  AuthRepository mockAuthRepository;

  private CambiarContrasenyaViewModel viewModel;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    viewModel = new CambiarContrasenyaViewModel(mockAuthRepository);
  }

  /////////////////////////estado inicial/////////////////////////

  @Test
  public void estadoInicial_cargando_esFalse() {
    assertEquals(Boolean.FALSE, viewModel.getCargando().getValue());
  }

  @Test
  public void estadoInicial_estadoCambio_esNull() {
    assertNull(viewModel.getEstadoCambio().getValue());
  }

  /////////////////////////limpiar estado cambio/////////////////////////

  @Test
  public void limpiarEstadoCambio_devuelveNull() {
    viewModel.limpiarEstadoCambio();
    assertNull(viewModel.getEstadoCambio().getValue());
  }

  @Test
  public void limpiarEstadoCambio_llamadoDobleVez_sigueDevolvientoNull() {
    viewModel.limpiarEstadoCambio();
    viewModel.limpiarEstadoCambio();
    assertNull(viewModel.getEstadoCambio().getValue());
  }

  /////////////////////////enum estado cambio contraseña/////////////////////////

  @Test
  public void enum_tieneValorEXITO() {
    assertNotNull(CambiarContrasenyaViewModel.EstadoCambioContrasenya.EXITO);
  }

  @Test
  public void enum_tieneValorCONTRASENYA_ACTUAL_INCORRECTA() {
    assertNotNull(CambiarContrasenyaViewModel.EstadoCambioContrasenya.CONTRASENYA_ACTUAL_INCORRECTA);
  }

  @Test
  public void enum_tieneValorCONTRASENYA_DEBIL() {
    assertNotNull(CambiarContrasenyaViewModel.EstadoCambioContrasenya.CONTRASENYA_DEBIL);
  }

  @Test
  public void enum_tieneValorERROR_GENERAL() {
    assertNotNull(CambiarContrasenyaViewModel.EstadoCambioContrasenya.ERROR_GENERAL);
  }

  @Test
  public void enum_cuatroValoresEnTotal() {
    assertEquals(4, CambiarContrasenyaViewModel.EstadoCambioContrasenya.values().length);
  }
}
