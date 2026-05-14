package com.clank.app.ui.detalle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.data.repository.CategoriaRepository;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.google.android.gms.tasks.Task;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DetalleClankViewModel extends ViewModel {

  private static final String TAG = "DetalleClankVM";

  public static class DetalleData {
    public String clankId        = "";
    public String titulo         = "";
    public String descripcion    = "";
    public String portadaUrl     = "";
    public int    tiempo         = -1;
    public boolean esAcabado     = true;
    public String  nombreUsuario = "";
    public List<Material>    materiales    = new ArrayList<>();
    public List<Herramienta> herramientas  = new ArrayList<>();
    public List<Instruccion> instrucciones = new ArrayList<>();
    public List<String[]>    categorias    = new ArrayList<>(); // [id, nombre]
  }

  private final ClankRepository     clankRepository;
  private final UsuarioRepository   usuarioRepository;
  private final CategoriaRepository categoriaRepository;
  private final TraductorDetalleClank traductorDetalleClank;

  private final MutableLiveData<DetalleData> detalle = new MutableLiveData<>();
  private final MutableLiveData<String>      error   = new MutableLiveData<>();

  private final MutableLiveData<Boolean> cargando =
          new MutableLiveData<>(false);
  private DetalleData datosEnConstruccion;
  private int         pendientes = 0;
  private boolean     procesoFinalLanzado = false;
  private boolean     cargaCancelada = false;

  @Inject
  public DetalleClankViewModel(ClankRepository clankRepository,
                               UsuarioRepository usuarioRepository,
                               CategoriaRepository categoriaRepository,
                               TraductorDetalleClank traductorDetalleClank) {
    this.clankRepository       = clankRepository;
    this.usuarioRepository     = usuarioRepository;
    this.categoriaRepository   = categoriaRepository;
    this.traductorDetalleClank = traductorDetalleClank;
  }

  public LiveData<DetalleData> getDetalle() { return detalle; }
  public LiveData<String>      getError()   { return error; }
  public LiveData<Boolean>     getCargando() { return cargando; }

  public void cargarClank(String clankId) {
    Log.d(TAG, "Inicio carga del clank: " + clankId);
    cargando.setValue(true);

    datosEnConstruccion         = new DetalleData();
    datosEnConstruccion.clankId = clankId;
    pendientes = 6;

    procesoFinalLanzado = false;
    cargaCancelada = false;

    clankRepository.getPorId(clankId).addOnSuccessListener(doc -> {
      Log.d(TAG, "Documento principal cargado");
      if (!doc.exists()) {
        cancelarCarga("Clank no encontrado");
        return;
      }
      Clank clank = doc.toObject(Clank.class);
      if (clank == null) {
        cancelarCarga("Error al leer el clank");
        return;
      }

      datosEnConstruccion.titulo      = clank.getTitulo()      != null ? clank.getTitulo()      : "";
      datosEnConstruccion.descripcion = clank.getDescripcion() != null ? clank.getDescripcion() : "";
      datosEnConstruccion.portadaUrl  = clank.getPortada()     != null ? clank.getPortada()     : "";
      datosEnConstruccion.tiempo      = clank.getTiempo();
      datosEnConstruccion.esAcabado   = clank.isEstadoAcabado();

      reducirPendientes(); 

      String uid = clank.getUsuarioId();
      if (uid != null && !uid.isEmpty()) {
        usuarioRepository.getUsuario(uid).addOnSuccessListener(userDoc -> {
          Log.d(TAG, "Usuario cargado");
          if (userDoc.exists()) {
            String nombre = userDoc.getString("nombre");
            datosEnConstruccion.nombreUsuario = nombre != null ? nombre : "";
          }
          reducirPendientes();
        }).addOnFailureListener(e -> {
          Log.e(TAG, "Error cargando usuario", e);
          reducirPendientes();
        });
      } else {
        reducirPendientes();
      }

      List<String> catIds = clank.getCategorias();
      if (catIds != null && !catIds.isEmpty()) {
        categoriaRepository.getTodas().addOnSuccessListener(catSnap -> {
          Log.d(TAG, "Categorías cargadas");
          List<String[]> catNombres = new ArrayList<>();
          catSnap.getDocuments().forEach(catDoc -> {
            if (catIds.contains(catDoc.getId())) {
              String nombre = catDoc.getString("categoria");
              catNombres.add(new String[]{
                catDoc.getId(),
                nombre != null ? nombre : ""
              });
            }
          });
          datosEnConstruccion.categorias = catNombres;
          reducirPendientes(); 
        }).addOnFailureListener(e -> {
          Log.e(TAG, "Error cargando categorías", e);
          reducirPendientes();
        });
      } else {
        reducirPendientes(); 
      }

    }).addOnFailureListener(e -> {
      cancelarCarga(
              e.getMessage() != null
                      ? e.getMessage()
                      : "Error al cargar el clank"
      );
    });

    clankRepository.getMateriales(clankId).addOnSuccessListener(snap -> {
      Log.d(TAG, "Materiales cargados");
      List<Material> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Material m = d.toObject(Material.class);
        if (m != null) lista.add(m);
      });
      datosEnConstruccion.materiales = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error cargando materiales", e);
      reducirPendientes();
    });

    clankRepository.getHerramientas(clankId).addOnSuccessListener(snap -> {
      Log.d(TAG, "Herramientas cargadas");
      List<Herramienta> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Herramienta h = d.toObject(Herramienta.class);
        if (h != null) lista.add(h);
      });
      datosEnConstruccion.herramientas = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error cargando herramientas", e);
    });
    clankRepository.getInstrucciones(clankId).addOnSuccessListener(snap -> {
      Log.d(TAG, "Instrucciones cargadas");
      List<Instruccion> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Instruccion ins = d.toObject(Instruccion.class);
        if (ins != null) lista.add(ins);
      });
      datosEnConstruccion.instrucciones = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Error cargando instrucciones", e);
    });
  }

  private synchronized void reducirPendientes() {
    if (cargaCancelada) {
      Log.d(TAG, "Carga cancelada. No se reducen pendientes.");
      return;
    }

    pendientes--;
    Log.d(TAG, "Pendientes restantes: " + pendientes);

    if (pendientes <= 0 && !procesoFinalLanzado) {
      Log.d(TAG, "Todas las cargas completadas. Se inicia traducción.");
      procesoFinalLanzado = true;
      traducirYPublicarDetalle();
    }
  }

  private synchronized void cancelarCarga(String mensaje) {
    cargaCancelada = true;
    error.postValue(mensaje);
    cargando.postValue(false);
  }

  private void traducirYPublicarDetalle() {
    Log.d(TAG, "Llamada a TraductorDetalleClank.traducirSiProcede()");

    traductorDetalleClank.traducirSiProcede(datosEnConstruccion)
            .addOnSuccessListener(resultado -> {
              Log.d(TAG, "Traducción terminada. Se tradujo: " + resultado);
            })
            .addOnFailureListener(errorTraduccion -> {
              Log.e(TAG, "Fallo en traducción", errorTraduccion);
            })
            .addOnCompleteListener(tarea -> {
              Log.d(TAG, "Proceso de traducción completado. Se publica detalle.");

              if (cargaCancelada) {
                return;
              }

              detalle.postValue(datosEnConstruccion);
              cargando.postValue(false);
            });
  }

  /////////////////////////eliminar clank/////////////////////////
  public Task<Void> eliminarClank(String clankId) {
    return clankRepository.eliminarCompletoPorId(clankId);
  }
}
