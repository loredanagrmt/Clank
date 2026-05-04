package com.clank.app.ui.crear;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CrearViewModel extends ViewModel {


  private final FirebaseFirestore db;
  private final FirebaseStorage storage;
  private final AuthRepository authRepository;

  private final MutableLiveData<Recurso<Void>> estadoPublicacion = new MutableLiveData<>();
  private final MutableLiveData<List<String[]>> categorias = new MutableLiveData<>();

  @Inject
  public CrearViewModel(FirebaseFirestore db,
                        FirebaseStorage storage,
                        AuthRepository authRepository) {
    this.db             = db;
    this.storage        = storage;
    this.authRepository = authRepository;
    cargarCategorias();
  }

  public LiveData<Recurso<Void>> getEstadoPublicacion() { return estadoPublicacion; }
  public LiveData<List<String[]>> getCategorias()        { return categorias; }

  /////////////////////////categorias de bbdd/////////////////////////
  private void cargarCategorias() {
    db.collection("categorias").get()
            .addOnSuccessListener(snapshot -> {
              List<String[]> lista = new ArrayList<>();
              for (var doc : snapshot.getDocuments()) {
                String nombre = doc.getString("categoria");
                if (nombre != null && !nombre.isEmpty())
                  lista.add(new String[]{doc.getId(), nombre});
              }
              categorias.setValue(lista);
            })
            .addOnFailureListener(e -> categorias.setValue(new ArrayList<>()));
  }

  public void publicarClank(String titulo,
                            String descripcion,
                            int tiempo,
                            Uri portadaUri,
                            List<String[]> materiales,
                            List<String> herramientas,
                            List<String> instrucciones,
                            List<Uri> imagenesInstrucciones,
                            List<String> categoriasSeleccionadas) {

    estadoPublicacion.setValue(Recurso.cargando());
    String uid     = authRepository.getUid();
    String clankId = db.collection("clanks").document().getId();

    if (portadaUri != null) {
      subirPortadaYContinuar(portadaUri, uid, clankId, titulo, descripcion, tiempo,
              materiales, herramientas, instrucciones,
              imagenesInstrucciones, categoriasSeleccionadas);
    } else {
      subirImagenesInstruccionesYCrear("", uid, clankId, titulo, descripcion, tiempo,
              materiales, herramientas, instrucciones,
              imagenesInstrucciones, categoriasSeleccionadas);
    }
  }

  /////////////////////////subir portada/////////////////////////

  private void subirPortadaYContinuar(Uri portadaUri, String uid, String clankId,
                                      String titulo, String descripcion, int tiempo,
                                      List<String[]> materiales, List<String> herramientas,
                                      List<String> instrucciones, List<Uri> imagenesInstrucciones,
                                      List<String> cats) {
    StorageReference ref = storage.getReference()
            .child(uid + "/" + clankId + "/portada/portada.jpg");

    ref.putFile(portadaUri)
            .continueWithTask(task -> {
              if (!task.isSuccessful()) throw task.getException();
              return ref.getDownloadUrl();
            })
            .addOnSuccessListener(downloadUri ->
                    subirImagenesInstruccionesYCrear(
                            downloadUri.toString(), uid, clankId, titulo, descripcion, tiempo,
                            materiales, herramientas, instrucciones, imagenesInstrucciones, cats))
            .addOnFailureListener(e ->
                    estadoPublicacion.setValue(Recurso.error(e.getMessage())));
  }

  /////////////////////////subir fotos pasos/////////////////////////
  private void subirImagenesInstruccionesYCrear(String portadaUrl, String uid, String clankId,
                                                String titulo, String descripcion, int tiempo,
                                                List<String[]> materiales,
                                                List<String> herramientas,
                                                List<String> instrucciones,
                                                List<Uri> imagenesInstrucciones,
                                                List<String> cats) {
    List<Task<Uri>> tareas = new ArrayList<>();

    for (int i = 0; i < imagenesInstrucciones.size(); i++) {
      Uri uri = imagenesInstrucciones.get(i);
      if (uri != null) {
        StorageReference ref = storage.getReference()
                .child(uid + "/" + clankId + "/instrucciones/" + i + ".jpg");
        Task<Uri> t = ref.putFile(uri)
                .continueWithTask(task -> {
                  if (!task.isSuccessful()) throw task.getException();
                  return ref.getDownloadUrl();
                });
        tareas.add(t);
      } else {
        tareas.add(Tasks.forResult(null));
      }
    }

    Tasks.whenAllComplete(tareas).addOnCompleteListener(allDone -> {
      List<String> urlsImagenes = new ArrayList<>();
      for (Task<?> t : tareas) {
        if (t.isSuccessful() && t.getResult() instanceof Uri)
          urlsImagenes.add(((Uri) t.getResult()).toString());
        else
          urlsImagenes.add(null);
      }
      crearDocumentoClank(portadaUrl, uid, clankId, titulo, descripcion, tiempo,
              materiales, herramientas, instrucciones, urlsImagenes, cats);
    });
  }

  /////////////////////////crea clank/////////////////////////
  private void crearDocumentoClank(String portadaUrl, String uid, String clankId,
                                   String titulo, String descripcion, int tiempo,
                                   List<String[]> materiales, List<String> herramientas,
                                   List<String> instrucciones, List<String> urlsImagenes,
                                   List<String> cats) {
    Map<String, Object> clank = new HashMap<>();
    clank.put("clankId",          clankId);
    clank.put("titulo",           titulo);
    clank.put("descripcion",      descripcion);
    clank.put("tiempo",           tiempo);
    clank.put("portada",          portadaUrl);
    clank.put("usuarioId",        uid);
    clank.put("fechaPublicacion", Timestamp.now());
    clank.put("numLikes",         0L);
    clank.put("estadoAcabado",    true);
    if (cats != null && !cats.isEmpty()) clank.put("categorias", cats);

    db.collection("clanks").document(clankId)
            .set(clank)
            .addOnSuccessListener(v ->
                    guardarSubcolecciones(clankId, materiales, herramientas,
                            instrucciones, urlsImagenes))
            .addOnFailureListener(e ->
                    estadoPublicacion.setValue(Recurso.error(e.getMessage())));
  }

  /////////////////////////launchers/////////////////////////
  private void guardarSubcolecciones(String clankId,
                                     List<String[]> materiales,
                                     List<String> herramientas,
                                     List<String> instrucciones,
                                     List<String> urlsImagenes) {
    var batch = db.batch();

    for (String[] m : materiales) {
      var ref = db.collection("clanks").document(clankId)
              .collection("materiales").document();
      Map<String, Object> mat = new HashMap<>();
      mat.put("cantidad", m[0]);
      mat.put("nombre",   m[1]);
      batch.set(ref, mat);
    }

    for (String h : herramientas) {
      var ref = db.collection("clanks").document(clankId)
              .collection("herramientas").document();
      Map<String, Object> herr = new HashMap<>();
      herr.put("nombre", h);
      batch.set(ref, herr);
    }

    for (int i = 0; i < instrucciones.size(); i++) {
      var ref = db.collection("clanks").document(clankId)
              .collection("instrucciones").document();
      Map<String, Object> paso = new HashMap<>();
      paso.put("orden",       i + 1);
      paso.put("instruccion", instrucciones.get(i));
      if (i < urlsImagenes.size() && urlsImagenes.get(i) != null)
        paso.put("imagen", urlsImagenes.get(i));
      batch.set(ref, paso);
    }

    batch.commit()
            .addOnSuccessListener(v ->
                    estadoPublicacion.setValue(Recurso.exito(null)))
            .addOnFailureListener(e ->
                    estadoPublicacion.setValue(Recurso.error(e.getMessage())));
  }
}
