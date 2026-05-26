package com.clank.app.ui.detalle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.model.Clank;
import com.clank.app.data.model.Herramienta;
import com.clank.app.data.model.Instruccion;
import com.clank.app.data.model.Material;
import com.clank.app.data.repository.AuthRepository;
import com.clank.app.data.repository.CategoriaRepository;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.UsuarioRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.clank.app.util.TraductorCategorias;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import com.clank.app.data.repository.LikeRepository;

@HiltViewModel
public class DetalleClankViewModel extends ViewModel {

  public static class DetalleData {
    public String clankId        = "";
    public String titulo         = "";
    public String descripcion    = "";
    public String portadaUrl     = "";
    public int    tiempo         = -1;
    public boolean esAcabado     = true;
    public String  nombreUsuario = "";
    public String  usuarioId     = "";
    public String  fotoPerfil    = "";
    public String  usuarioClank  = "";
    public Date fechaPublicacion = null;
    public int numLikes = 0;

    public List<Material>    materiales    = new ArrayList<>();
    public List<Herramienta> herramientas  = new ArrayList<>();
    public List<Instruccion> instrucciones = new ArrayList<>();
    public List<String[]>    categorias    = new ArrayList<>(); // [id, nombre]
  }

  private final ClankRepository     clankRepository;
  private final UsuarioRepository   usuarioRepository;
  private final CategoriaRepository categoriaRepository;
  private final TraductorDetalleClank traductorDetalleClank;
  private final TraductorCategorias traductorCategorias;
  private final MutableLiveData<DetalleData> detalle = new MutableLiveData<>();
  private final MutableLiveData<String>      error   = new MutableLiveData<>();

  private final MutableLiveData<Boolean> cargando = new MutableLiveData<>(false);
  private final AuthRepository authRepository;
  private final LikeRepository likeRepository;
  private DetalleData datosEnConstruccion;
  private int         pendientes = 0;
  private boolean     procesoFinalLanzado = false;
  private boolean     cargaCancelada = false;

  @Inject
  public DetalleClankViewModel(ClankRepository clankRepository,
                               UsuarioRepository usuarioRepository,
                               CategoriaRepository categoriaRepository,
                               TraductorDetalleClank traductorDetalleClank,
                               TraductorCategorias traductorCategorias,
                               AuthRepository authRepository,
                               LikeRepository likeRepository) {
    this.clankRepository       = clankRepository;
    this.usuarioRepository     = usuarioRepository;
    this.categoriaRepository   = categoriaRepository;
    this.traductorDetalleClank = traductorDetalleClank;
    this.traductorCategorias   = traductorCategorias;
    this.authRepository        = authRepository;
    this.likeRepository        = likeRepository;
  }

  public LiveData<DetalleData> getDetalle() { return detalle; }
  public LiveData<String>      getError()   { return error; }
  public LiveData<Boolean>     getCargando() { return cargando; }
  public com.google.firebase.firestore.ListenerRegistration escucharNumLikes(
    String clankId,
    LikeRepository.OnNumLikesListener listener) {
    return likeRepository.escucharNumLikes(clankId, listener);
  }

  public void cargarClank(String clankId) {
    cargando.setValue(true);

    datosEnConstruccion         = new DetalleData();
    datosEnConstruccion.clankId = clankId;
    pendientes = 5;

    procesoFinalLanzado = false;
    cargaCancelada = false;

    clankRepository.getPorId(clankId).addOnSuccessListener(doc -> {
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
      datosEnConstruccion.fechaPublicacion = clank.getFechaPublicacion();
      datosEnConstruccion.numLikes = clank.getNumLikes();

      String uid = clank.getUsuarioId();
      datosEnConstruccion.usuarioId = uid != null ? uid : "";
      if (uid != null && !uid.isEmpty()) {
        usuarioRepository.getUsuario(uid).addOnSuccessListener(userDoc -> {
          if (userDoc.exists()) {
            String nombre = userDoc.getString("nombre");
            datosEnConstruccion.nombreUsuario = nombre != null ? nombre : "";
            String foto = userDoc.getString("fotoPerfil");
            datosEnConstruccion.fotoPerfil = foto != null ? foto : "";
            String clankId2 = userDoc.getString("usuarioClank");
            datosEnConstruccion.usuarioClank = clankId2 != null ? clankId2 : "";
          }
          reducirPendientes();
        }).addOnFailureListener(e -> {
          reducirPendientes();
        });
      } else {
        reducirPendientes();
      }

      List<String> catIds = clank.getCategorias();
      if (catIds != null && !catIds.isEmpty()) {
        categoriaRepository.getTodas().addOnSuccessListener(catSnap -> {
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
      List<Material> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Material m = d.toObject(Material.class);
        if (m != null) lista.add(m);
      });
      datosEnConstruccion.materiales = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      reducirPendientes();
    });

    clankRepository.getHerramientas(clankId).addOnSuccessListener(snap -> {
      List<Herramienta> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Herramienta h = d.toObject(Herramienta.class);
        if (h != null) lista.add(h);
      });
      datosEnConstruccion.herramientas = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      reducirPendientes();
    });
    clankRepository.getInstrucciones(clankId).addOnSuccessListener(snap -> {
      List<Instruccion> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Instruccion ins = d.toObject(Instruccion.class);
        if (ins != null) lista.add(ins);
      });
      datosEnConstruccion.instrucciones = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> {
      reducirPendientes();
    });
  }

  private synchronized void reducirPendientes() {
    if (cargaCancelada) {
      return;
    }

    pendientes--;

    if (pendientes <= 0 && !procesoFinalLanzado) {
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
    Task<Boolean> tareaContenido =
            traductorDetalleClank.traducirSiProcede(datosEnConstruccion);

    Task<List<String[]>> tareaCategorias =
            traductorCategorias.traducirSiProcede(
                    datosEnConstruccion.categorias
            );

    Tasks.whenAllComplete(
            tareaContenido,
            tareaCategorias
    ).addOnCompleteListener(tareaFinal -> {
      if (cargaCancelada) {
        return;
      }

      if (tareaCategorias.isSuccessful()
              && tareaCategorias.getResult() != null) {
        datosEnConstruccion.categorias =
                tareaCategorias.getResult();
      }

      detalle.postValue(datosEnConstruccion);
      cargando.postValue(false);
    });
  }

  /////////////////////////eliminar clank/////////////////////////
  public Task<Void> eliminarClank(String clankId) {
    return clankRepository.eliminarCompletoPorId(clankId);
  }
  public String getCurrentUserId() {
    return authRepository.getUid();
  }
  public Task<Boolean> toggleLike(String clankId) {
    String uid = authRepository.getUid();
    if (uid == null || uid.isEmpty())
      return Tasks.forException(new Exception("Usuario no autenticado"));
    return likeRepository.toggleLike(clankId, uid);
  }
  public Task<Boolean> hasDadoLike(String clankId) {
    String uid = authRepository.getUid();
    if (uid == null || uid.isEmpty()) return Tasks.forResult(false);
    return likeRepository.hasDadoLike(clankId, uid);
  }

  /////////////////////////cierra traductor/////////////////////////
  @Override
  protected void onCleared() {
    super.onCleared();
    traductorDetalleClank.cerrar();
    traductorCategorias.cerrar();
  }
}
