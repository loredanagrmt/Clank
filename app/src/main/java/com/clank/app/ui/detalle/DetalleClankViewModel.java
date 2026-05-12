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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

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
    public List<Material>    materiales    = new ArrayList<>();
    public List<Herramienta> herramientas  = new ArrayList<>();
    public List<Instruccion> instrucciones = new ArrayList<>();
    public List<String[]>    categorias    = new ArrayList<>(); // [id, nombre]
  }

  private final ClankRepository     clankRepository;
  private final UsuarioRepository   usuarioRepository;
  private final CategoriaRepository categoriaRepository;

  private final MutableLiveData<DetalleData> detalle = new MutableLiveData<>();
  private final MutableLiveData<String>      error   = new MutableLiveData<>();

  private DetalleData datosEnConstruccion;
  private int         pendientes = 0;

  @Inject
  public DetalleClankViewModel(ClankRepository clankRepository,
                               UsuarioRepository usuarioRepository,
                               CategoriaRepository categoriaRepository) {
    this.clankRepository     = clankRepository;
    this.usuarioRepository   = usuarioRepository;
    this.categoriaRepository = categoriaRepository;
  }

  public LiveData<DetalleData> getDetalle() { return detalle; }
  public LiveData<String>      getError()   { return error; }

  public void cargarClank(String clankId) {
    datosEnConstruccion         = new DetalleData();
    datosEnConstruccion.clankId = clankId;
    pendientes = 6;

    clankRepository.getPorId(clankId).addOnSuccessListener(doc -> {
      if (!doc.exists()) {
        error.setValue("Clank no encontrado");
        return;
      }
      Clank clank = doc.toObject(Clank.class);
      if (clank == null) {
        error.setValue("Error al leer el clank");
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
          if (userDoc.exists()) {
            String nombre = userDoc.getString("nombre");
            datosEnConstruccion.nombreUsuario = nombre != null ? nombre : "";
          }
          reducirPendientes();
        }).addOnFailureListener(e -> reducirPendientes());
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
        }).addOnFailureListener(e -> reducirPendientes());
      } else {
        reducirPendientes(); 
      }

    }).addOnFailureListener(e -> {
      error.setValue(e.getMessage());
    });

    clankRepository.getMateriales(clankId).addOnSuccessListener(snap -> {
      List<Material> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Material m = d.toObject(Material.class);
        if (m != null) lista.add(m);
      });
      datosEnConstruccion.materiales = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> reducirPendientes());

    clankRepository.getHerramientas(clankId).addOnSuccessListener(snap -> {
      List<Herramienta> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Herramienta h = d.toObject(Herramienta.class);
        if (h != null) lista.add(h);
      });
      datosEnConstruccion.herramientas = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> reducirPendientes());

    clankRepository.getInstrucciones(clankId).addOnSuccessListener(snap -> {
      List<Instruccion> lista = new ArrayList<>();
      snap.getDocuments().forEach(d -> {
        Instruccion ins = d.toObject(Instruccion.class);
        if (ins != null) lista.add(ins);
      });
      datosEnConstruccion.instrucciones = lista;
      reducirPendientes();
    }).addOnFailureListener(e -> reducirPendientes());
  }

  private synchronized void reducirPendientes() {
    pendientes--;
    if (pendientes <= 0) detalle.postValue(datosEnConstruccion);
  }
}
