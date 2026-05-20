package com.clank.app.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.ui.busqueda.BusquedaViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.*;

public class BusquedaViewModelTest {

  @Rule
  public InstantTaskExecutorRule reglaInstant = new InstantTaskExecutorRule();

  @Mock
  ClankRepository mockClankRepository;

  @Mock
  UsuarioRepository mockUsuarioRepository;

  private BusquedaViewModel viewModel;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    viewModel = new BusquedaViewModel(mockClankRepository, mockUsuarioRepository);
  }

  /////////////////////////estado inicial/////////////////////////

  @Test
  public void estadoInicial_resultados_esNull() {
    assertNull(viewModel.getResultados().getValue());
  }

  @Test
  public void estadoInicial_cargando_esFalse() {
    assertEquals(Boolean.FALSE, viewModel.getCargando().getValue());
  }

  @Test
  public void estadoInicial_error_esNull() {
    assertNull(viewModel.getError().getValue());
  }

  /////////////////////////validaciones locales/////////////////////////

  @Test
  public void buscar_queryVacia_resultadosEsListaVacia() {
    viewModel.buscar("");
    List<?> resultado = viewModel.getResultados().getValue();
    assertNotNull(resultado);
    assertTrue(resultado.isEmpty());
  }

  @Test
  public void buscar_queryNull_resultadosEsListaVacia() {
    viewModel.buscar(null);
    List<?> resultado = viewModel.getResultados().getValue();
    assertNotNull(resultado);
    assertTrue(resultado.isEmpty());
  }

  @Test
  public void buscar_querySoloEspacios_resultadosEsListaVacia() {
    viewModel.buscar("   ");
    List<?> resultado = viewModel.getResultados().getValue();
    assertNotNull(resultado);
    assertTrue(resultado.isEmpty());
  }

  /////////////////////////limpiar/////////////////////////

  @Test
  public void limpiar_resultados_emiteListaVacia() {
    viewModel.limpiar();
    List<?> resultado = viewModel.getResultados().getValue();
    assertNotNull(resultado);
    assertTrue(resultado.isEmpty());
  }

  @Test
  public void limpiar_error_emiteNull() {
    viewModel.limpiar();
    assertNull(viewModel.getError().getValue());
  }

  /////////////////////////cache/////////////////////////

  @Test
  public void getUsuarioCacheado_cacheVacio_devuelveNull() {
    assertNull(viewModel.getUsuarioCacheado("uid_inexistente"));
  }

  @Test
  public void getUsuarioCacheado_uidNull_devuelveNull() {
    assertNull(viewModel.getUsuarioCacheado(null));
  }
}
