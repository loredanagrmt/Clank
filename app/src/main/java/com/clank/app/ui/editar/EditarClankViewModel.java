package com.clank.app.ui.editar;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.util.Recurso;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class EditarClankViewModel extends ViewModel {

  public static class DatosClank {
    public String         clankId       = "";
    public String         titulo        = "";
    public String         descripcion   = "";
    public int            tiempo        = -1;
    public String         portadaUrl    = "";
    public List<String>   categorias    = new ArrayList<>();
    public List<String[]> materiales    = new ArrayList<>();
    public List<String>   herramientas  = new ArrayList<>();
    public List<String[]> instrucciones = new ArrayList<>(); // [texto, urlImagen|null]
  }

  private final FirebaseFirestore db;
  private final FirebaseStorage   storage;
  private final AuthRepository    authRepository;
  private final MutableLiveData<DatosClank>     datosClank    = new MutableLiveData<>();
  private final MutableLiveData<Recurso<Void>>  estadoGuardar = new MutableLiveData<>();
  private final MutableLiveData<List<String[]>> categorias    = new MutableLiveData<>();
  private final MutableLiveData<Boolean>        hayCambios    = new MutableLiveData<>(false);
  private String clankId;

  @Inject
  public EditarClankViewModel(FirebaseFirestore db,
                              FirebaseStorage   storage,
                              AuthRepository    authRepository) {
    this.db             = db;
    this.storage        = storage;
    this.authRepository = authRepository;
    cargarCategorias();
  }

  // ─── Getters LiveData ─────────────────────────────────────────────────────

  public LiveData<DatosClank>     getDatosClank()    { return datosClank; }
  public LiveData<Recurso<Void>>  getEstadoGuardar() { return estadoGuardar; }
  public LiveData<List<String[]>> getCategorias()    { return categorias; }
  public LiveData<Boolean>        getHayCambios()    { return hayCambios; }

  public void marcarCambios()  { hayCambios.setValue(true); }
  public boolean hayCambios()  { return Boolean.TRUE.equals(hayCambios.getValue()); }

  // ─── Carga datos del clank ────────────────────────────────────────────────

  public void cargarClank(String id) {
    this.clankId = id;
    db.collection("clanks").document(id).get()
      .addOnSuccessListener(doc -> {
        if (!doc.exists()) return;
        DatosClank datos = new DatosClank();
        datos.clankId     = id;
        datos.titulo      = strOrEmpty(doc.getString("titulo"));
        datos.descripcion = strOrEmpty(doc.getString("descripcion"));
        datos.portadaUrl  = strOrEmpty(doc.getString("portada"));
        Long tiempo = doc.getLong("tiempo");
        datos.tiempo = tiempo != null ? tiempo.intValue() : -1;
        List<String> cats = (List<String>) doc.get("categorias");
        if (cats != null) datos.categorias = cats;
        cargarSubcolecciones(datos);
      });
  }

  private void cargarSubcolecciones(DatosClank datos) {
    Task<?> tareaMateriales = db.collection("clanks").document(datos.clankId)
      .collection("materiales").get()
      .addOnSuccessListener(snap -> {
        for (QueryDocumentSnapshot doc : snap)
          datos.materiales.add(new String[]{
            strOrEmpty(doc.getString("cantidad")),
            strOrEmpty(doc.getString("nombre"))
          });
      });

    Task<?> tareaHerramientas = db.collection("clanks").document(datos.clankId)
      .collection("herramientas").get()
      .addOnSuccessListener(snap -> {
        for (QueryDocumentSnapshot doc : snap)
          datos.herramientas.add(strOrEmpty(doc.getString("nombre")));
      });

    Task<?> tareaInstrucciones = db.collection("clanks").document(datos.clankId)
      .collection("instrucciones").orderBy("orden").get()
      .addOnSuccessListener(snap -> {
        for (QueryDocumentSnapshot doc : snap) {
          String texto  = strOrEmpty(doc.getString("instruccion"));
          String imagen = doc.contains("imagen")
            ? strOrEmpty(doc.getString("imagen")) : null;
          datos.instrucciones.add(new String[]{texto, imagen});
        }
      });

    Tasks.whenAllComplete(tareaMateriales, tareaHerramientas, tareaInstrucciones)
      .addOnCompleteListener(tarea -> datosClank.postValue(datos));
  }

  // ─── Publicar ─────────────────────────────────────────────────────────────

  public void publicarClank(String titulo, String descripcion, int tiempo,
                            Uri portadaUri, String portadaUrlActual,
                            List<String[]> materiales, List<String> herramientas,
                            List<String> instrucciones, List<Object> imagenesInstrucciones,
                            List<String> categoriasSeleccionadas) {
    estadoGuardar.setValue(Recurso.cargando());
    procesarYGuardar(true, titulo, descripcion, tiempo,
      portadaUri, portadaUrlActual, authRepository.getUid(),
      materiales, herramientas, instrucciones, imagenesInstrucciones,
      categoriasSeleccionadas);
  }

  // ─── Guardar borrador ─────────────────────────────────────────────────────

  public void guardarBorrador(String titulo, String descripcion, int tiempo,
                              Uri portadaUri, String portadaUrlActual,
                              List<String[]> materiales, List<String> herramientas,
                              List<String> instrucciones, List<Object> imagenesInstrucciones,
                              List<String> categoriasSeleccionadas) {
    estadoGuardar.setValue(Recurso.cargando());
    procesarYGuardar(false, titulo, descripcion, tiempo,
      portadaUri, portadaUrlActual, authRepository.getUid(),
      materiales, herramientas, instrucciones, imagenesInstrucciones,
      categoriasSeleccionadas);
  }

  // ─── Flujo común ──────────────────────────────────────────────────────────

  private void procesarYGuardar(boolean acabado,
                                String titulo, String descripcion, int tiempo,
                                Uri portadaUri, String portadaUrlActual, String uid,
                                List<String[]> materiales, List<String> herramientas,
                                List<String> textos, List<Object> imagenesInstrucciones,
                                List<String> categoriasSeleccionadas) {
    if (portadaUri != null) {
      StorageReference ref = storage.getReference()
        .child("portadas/" + uid + "/" + UUID.randomUUID() + ".jpg");
      ref.putFile(portadaUri)
        .continueWithTask(tarea -> ref.getDownloadUrl())
        .addOnSuccessListener(uri ->
          subirImagenesInstruccionesYGuardar(acabado, titulo, descripcion, tiempo,
            uri.toString(), uid, materiales, herramientas,
            textos, imagenesInstrucciones, categoriasSeleccionadas))
        .addOnFailureListener(e ->
          estadoGuardar.postValue(Recurso.error(e.getMessage())));
    } else {
      subirImagenesInstruccionesYGuardar(acabado, titulo, descripcion, tiempo,
        portadaUrlActual, uid, materiales, herramientas,
        textos, imagenesInstrucciones, categoriasSeleccionadas);
    }
  }

  private void subirImagenesInstruccionesYGuardar(boolean acabado,
                                                  String titulo, String descripcion,
                                                  int tiempo, String portadaUrl, String uid,
                                                  List<String[]> materiales,
                                                  List<String> herramientas,
                                                  List<String> textos,
                                                  List<Object> imagenesInstrucciones,
                                                  List<String> categoriasSeleccionadas) {
    List<Task<String>> tareas = new ArrayList<>();

    for (Object imagen : imagenesInstrucciones) {
      if (imagen instanceof Uri) {
        StorageReference ref = storage.getReference()
          .child("instrucciones/" + uid + "/" + UUID.randomUUID() + ".jpg");
        Task<String> tarea = ref.putFile((Uri) imagen)
          .continueWithTask(t -> ref.getDownloadUrl())
          .continueWith(t -> t.getResult().toString());
        tareas.add(tarea);
      } else {
        tareas.add(Tasks.forResult(imagen instanceof String ? (String) imagen : null));
      }
    }

    Tasks.whenAllComplete(new ArrayList<>(tareas))
      .addOnCompleteListener(resultado -> {
        List<String> urlsInstrucciones = new ArrayList<>();
        for (Task<String> tarea : tareas) {
          urlsInstrucciones.add(tarea.isSuccessful() ? tarea.getResult() : null);
        }
        guardarEnFirestore(acabado, titulo, descripcion, tiempo, portadaUrl,
          uid, materiales, herramientas, textos, urlsInstrucciones,
          categoriasSeleccionadas);
      });
  }

  private void guardarEnFirestore(boolean acabado,
                                  String titulo, String descripcion,
                                  int tiempo, String portadaUrl, String uid,
                                  List<String[]> materiales, List<String> herramientas,
                                  List<String> textos, List<String> urlsInstrucciones,
                                  List<String> categoriasSeleccionadas) {
    Map<String, Object> datos = new HashMap<>();
    datos.put("titulo",        titulo);
    datos.put("descripcion",   descripcion);
    datos.put("tiempo",        tiempo);
    datos.put("portada",       portadaUrl);
    datos.put("categorias",    categoriasSeleccionadas);
    datos.put("autorId",       uid);
    datos.put("estadoAcabado", acabado);
    datos.put("fechaEdicion",  Timestamp.now());

    db.collection("clanks").document(clankId).update(datos)
      .addOnSuccessListener(unused -> {
        guardarSubcolecciones(materiales, herramientas, textos, urlsInstrucciones);
      })
      .addOnFailureListener(e ->
        estadoGuardar.postValue(Recurso.error(e.getMessage())));
  }

  private void guardarSubcolecciones(List<String[]> materiales, List<String> herramientas,
                                     List<String> textos, List<String> urlsInstrucciones) {
    String rutaBase = "clanks/" + clankId;

    // Borrar y reescribir materiales
    db.collection(rutaBase + "/materiales").get().addOnSuccessListener(snap -> {
      for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
      for (String[] material : materiales) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("cantidad", material[0]);
        mapa.put("nombre",   material[1]);
        db.collection(rutaBase + "/materiales").add(mapa);
      }
    });

    // Borrar y reescribir herramientas
    db.collection(rutaBase + "/herramientas").get().addOnSuccessListener(snap -> {
      for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
      for (String herramienta : herramientas) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("nombre", herramienta);
        db.collection(rutaBase + "/herramientas").add(mapa);
      }
    });

    // Borrar y reescribir instrucciones
    db.collection(rutaBase + "/instrucciones").get().addOnSuccessListener(snap -> {
      for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
      for (int i = 0; i < textos.size(); i++) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("instruccion", textos.get(i));
        mapa.put("orden",       i);
        String urlImagen = (i < urlsInstrucciones.size()) ? urlsInstrucciones.get(i) : null;
        if (urlImagen != null && !urlImagen.isEmpty()) mapa.put("imagen", urlImagen);
        db.collection(rutaBase + "/instrucciones").add(mapa);
      }
      estadoGuardar.postValue(Recurso.exito(null));
    });
  }

  // ─── Eliminar clank ───────────────────────────────────────────────────────

  public void eliminarClank() {
    db.collection("clanks").document(clankId).delete();
  }

  // ─── Categorías ───────────────────────────────────────────────────────────

  private void cargarCategorias() {
    db.collection("categorias").get()
      .addOnSuccessListener(snap -> {
        List<String[]> lista = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snap)
          lista.add(new String[]{doc.getId(), strOrEmpty(doc.getString("nombre"))});
        categorias.postValue(lista);
      });
  }

  // ─── Utilidades ───────────────────────────────────────────────────────────

  private String strOrEmpty(String valor) {
    return valor != null ? valor : "";
  }
}
