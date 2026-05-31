package com.clank.app.ui.crear;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.AuthRepository;
import com.clank.app.util.Recurso;
import com.clank.app.util.TraductorCategorias;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CrearViewModel extends ViewModel {

  private final FirebaseFirestore db;
  private final FirebaseStorage storage;
  private final AuthRepository authRepository;
  private final TraductorCategorias traductorCategorias;

  private final MutableLiveData<Recurso<Void>> estadoPublicacion = new MutableLiveData<>();
  private final MutableLiveData<List<String[]>> categorias = new MutableLiveData<>();

  private FirebaseAuth.AuthStateListener authStateListener;
  private boolean categoriasCargadas = false;

  @Inject
  public CrearViewModel(FirebaseFirestore db,
                        FirebaseStorage storage,
                        AuthRepository authRepository,
                        TraductorCategorias traductorCategorias) {
    this.db = db;
    this.storage = storage;
    this.authRepository = authRepository;
    this.traductorCategorias = traductorCategorias;
  }

  public LiveData<Recurso<Void>> getEstadoPublicacion() {
    return estadoPublicacion;
  }

  public LiveData<List<String[]>> getCategorias() {
    return categorias;
  }

  public void recargarCategorias() {
    categoriasCargadas = false;
    cargarCategoriasEsperandoAuth();
  }

  /// ////////////////////// categorias de bbdd /////////////////////////

  private final MutableLiveData<Set<String>> categoriasSeleccionadas =
    new MutableLiveData<>(new HashSet<>());

  public LiveData<Set<String>> getCategoriasSeleccionadas() {
    return categoriasSeleccionadas;
  }

  public void toggleCategoriaSeleccionada(String categoriaId) {
    Set<String> actual = new HashSet<>(
      categoriasSeleccionadas.getValue() != null
        ? categoriasSeleccionadas.getValue()
        : new HashSet<>()
    );
    if (actual.contains(categoriaId)) actual.remove(categoriaId);
    else                               actual.add(categoriaId);
    categoriasSeleccionadas.setValue(actual);
  }

  public void limpiarCategoriasSeleccionadasVM() {
    categoriasSeleccionadas.setValue(new HashSet<>());
  }

  public List<String> getListaCategoriasSeleccionadas() {
    Set<String> sel = categoriasSeleccionadas.getValue();
    return sel != null ? new ArrayList<>(sel) : new ArrayList<>();
  }
  private void cargarCategoriasEsperandoAuth() {
    if (categoriasCargadas) return;
    FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

    if (usuario != null) {
      cargarCategorias();
      return;
    }

    authStateListener = firebaseAuth -> {
      FirebaseUser user = firebaseAuth.getCurrentUser();

      if (user != null) {
        cargarCategorias();

        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        authStateListener = null;
      }
    };

    FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
  }

  private void ejecutarCargaCategorias() {
    db.collection("categorias")
            .get()
            .addOnSuccessListener(snapshot -> {
              List<String[]> lista = new ArrayList<>();
              for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String nombre = doc.getString("categoria");
                if (nombre != null && !nombre.trim().isEmpty()) {
                  lista.add(new String[]{doc.getId(), nombre.trim()});
                }
              }

              if (lista.isEmpty()) {
                categorias.setValue(new ArrayList<>());
                return;
              }

              traductorCategorias.traducirSiProcede(lista)
                      .addOnSuccessListener(categoriasTraducidas -> {
                        categoriasCargadas = true;
                        if (categoriasTraducidas != null && !categoriasTraducidas.isEmpty()) {
                          categorias.setValue(categoriasTraducidas);
                        } else {
                          categorias.setValue(lista);
                        }
                      })
                      .addOnFailureListener(error -> {
                        categoriasCargadas = true;
                        categorias.setValue(lista);
                      });
            })
            .addOnFailureListener(error -> {
              categorias.setValue(new ArrayList<>());
            });
  }

  /// ////////////////////// publicar clank /////////////////////////

  public void publicarClank(String titulo,
                            String descripcion,
                            int tiempo,
                            Uri portadaUri,
                            List<String[]> materiales,
                            List<String> herramientas,
                            List<String> instrucciones,
                            List<Uri> imagenesInstrucciones,
                            List<String> categoriasSeleccionadas) {

    guardarNuevoClank(
            true,
            true,
            titulo,
            descripcion,
            tiempo,
            portadaUri,
            materiales,
            herramientas,
            instrucciones,
            imagenesInstrucciones,
            categoriasSeleccionadas
    );
  }

  /// ////////////////////// guardar boceto /////////////////////////

  public void guardarBoceto(String titulo,
                            String descripcion,
                            int tiempo,
                            Uri portadaUri,
                            List<String[]> materiales,
                            List<String> herramientas,
                            List<String> instrucciones,
                            List<Uri> imagenesInstrucciones,
                            List<String> categoriasSeleccionadas) {

    guardarNuevoClank(
            false,
            false,
            titulo,
            descripcion,
            tiempo,
            portadaUri,
            materiales,
            herramientas,
            instrucciones,
            imagenesInstrucciones,
            categoriasSeleccionadas
    );
  }

  /// ////////////////////// guardar nuevo clank /////////////////////////

  private void guardarNuevoClank(boolean acabado,
                                 boolean portadaObligatoria,
                                 String titulo,
                                 String descripcion,
                                 int tiempo,
                                 Uri portadaUri,
                                 List<String[]> materiales,
                                 List<String> herramientas,
                                 List<String> instrucciones,
                                 List<Uri> imagenesInstrucciones,
                                 List<String> categoriasSeleccionadas) {

    estadoPublicacion.setValue(Recurso.cargando());

    String uid = authRepository.getUid();
    String clankId = db.collection("clanks").document().getId();

    if (uid == null || uid.isEmpty()) {
      estadoPublicacion.setValue(Recurso.error("No hay sesion activa"));
      return;
    }

    if (portadaObligatoria && portadaUri == null) {
      estadoPublicacion.setValue(Recurso.error("Debes añadir una portada"));
      return;
    }

    if (portadaUri != null) {
      subirPortadaYContinuar(
              portadaUri,
              uid,
              clankId,
              titulo,
              descripcion,
              tiempo,
              materiales,
              herramientas,
              instrucciones,
              imagenesInstrucciones,
              categoriasSeleccionadas,
              acabado
      );
    } else {
      subirImagenesInstruccionesYCrear(
              "",
              uid,
              clankId,
              titulo,
              descripcion,
              tiempo,
              materiales,
              herramientas,
              instrucciones,
              imagenesInstrucciones,
              categoriasSeleccionadas,
              acabado
      );
    }
  }

  /// ////////////////////// subir portada /////////////////////////

  private void subirPortadaYContinuar(Uri portadaUri,
                                      String uid,
                                      String clankId,
                                      String titulo,
                                      String descripcion,
                                      int tiempo,
                                      List<String[]> materiales,
                                      List<String> herramientas,
                                      List<String> instrucciones,
                                      List<Uri> imagenesInstrucciones,
                                      List<String> categoriasSeleccionadas,
                                      boolean acabado) {

    StorageReference ref = storage.getReference()
            .child(uid + "/" + clankId + "/portada/portada.jpg");

    ref.putFile(portadaUri)
            .continueWithTask(tarea -> {
              if (!tarea.isSuccessful()) {
                throw tarea.getException();
              }

              return ref.getDownloadUrl();
            })
            .addOnSuccessListener(downloadUri ->
                    subirImagenesInstruccionesYCrear(
                            downloadUri.toString(),
                            uid,
                            clankId,
                            titulo,
                            descripcion,
                            tiempo,
                            materiales,
                            herramientas,
                            instrucciones,
                            imagenesInstrucciones,
                            categoriasSeleccionadas,
                            acabado
                    )
            )
            .addOnFailureListener(error ->
                    estadoPublicacion.setValue(Recurso.error(error.getMessage()))
            );
  }

  /// ////////////////////// subir fotos de instrucciones /////////////////////////

  private void subirImagenesInstruccionesYCrear(String portadaUrl,
                                                String uid,
                                                String clankId,
                                                String titulo,
                                                String descripcion,
                                                int tiempo,
                                                List<String[]> materiales,
                                                List<String> herramientas,
                                                List<String> instrucciones,
                                                List<Uri> imagenesInstrucciones,
                                                List<String> categoriasSeleccionadas,
                                                boolean acabado) {

    List<Task<Uri>> tareas = new ArrayList<>();

    for (int i = 0; i < imagenesInstrucciones.size(); i++) {
      Uri uri = imagenesInstrucciones.get(i);

      if (uri != null) {
        StorageReference ref = storage.getReference()
                .child(uid + "/" + clankId + "/instrucciones/" + i + ".jpg");

        Task<Uri> tarea = ref.putFile(uri)
                .continueWithTask(resultado -> {
                  if (!resultado.isSuccessful()) {
                    throw resultado.getException();
                  }

                  return ref.getDownloadUrl();
                });

        tareas.add(tarea);
      } else {
        tareas.add(Tasks.forResult(null));
      }
    }

    Tasks.whenAllComplete(tareas)
            .addOnCompleteListener(resultado -> {
              List<String> urlsImagenes = new ArrayList<>();

              for (Task<?> tarea : tareas) {
                if (tarea.isSuccessful() && tarea.getResult() instanceof Uri) {
                  urlsImagenes.add(((Uri) tarea.getResult()).toString());
                } else {
                  urlsImagenes.add(null);
                }
              }

              crearDocumentoClank(
                      portadaUrl,
                      uid,
                      clankId,
                      titulo,
                      descripcion,
                      tiempo,
                      materiales,
                      herramientas,
                      instrucciones,
                      urlsImagenes,
                      categoriasSeleccionadas,
                      acabado
              );
            });
  }

  /// ////////////////////// crear documento clank /////////////////////////

  private void crearDocumentoClank(String portadaUrl,
                                   String uid,
                                   String clankId,
                                   String titulo,
                                   String descripcion,
                                   int tiempo,
                                   List<String[]> materiales,
                                   List<String> herramientas,
                                   List<String> instrucciones,
                                   List<String> urlsImagenes,
                                   List<String> categoriasSeleccionadas,
                                   boolean acabado) {

    Map<String, Object> clank = new HashMap<>();

    clank.put("clankId", clankId);
    clank.put("titulo", titulo);
    clank.put("descripcion", descripcion);
    clank.put("tiempo", tiempo);
    clank.put("portada", portadaUrl);
    clank.put("usuarioId", uid);
    clank.put("fechaPublicacion", Timestamp.now());
    clank.put("numLikes", 0L);
    clank.put("estadoAcabado", acabado);

    if (categoriasSeleccionadas != null && !categoriasSeleccionadas.isEmpty()) {
      clank.put("categorias", categoriasSeleccionadas);
    }

    db.collection("clanks")
            .document(clankId)
            .set(clank)
            .addOnSuccessListener(resultado ->
                    guardarSubcolecciones(
                            clankId,
                            materiales,
                            herramientas,
                            instrucciones,
                            urlsImagenes
                    )
            )
            .addOnFailureListener(error ->
                    estadoPublicacion.setValue(Recurso.error(error.getMessage()))
            );
  }

  /// ////////////////////// subcolecciones /////////////////////////

  private void guardarSubcolecciones(String clankId,
                                     List<String[]> materiales,
                                     List<String> herramientas,
                                     List<String> instrucciones,
                                     List<String> urlsImagenes) {

    var batch = db.batch();

    for (int i = 0; i < materiales.size(); i++) {
      String[] material = materiales.get(i);

      var ref = db.collection("clanks")
              .document(clankId)
              .collection("materiales")
              .document();

      Map<String, Object> mapaMaterial = new HashMap<>();

      mapaMaterial.put("matId", ref.getId());
      mapaMaterial.put("cantidad", Integer.parseInt(material[0]));
      mapaMaterial.put("material", material[1]);

      batch.set(ref, mapaMaterial);
    }

    for (String herramienta : herramientas) {
      var ref = db.collection("clanks")
              .document(clankId)
              .collection("herramientas")
              .document();

      Map<String, Object> mapaHerramienta = new HashMap<>();

      mapaHerramienta.put("herrId", ref.getId());
      mapaHerramienta.put("herramienta", herramienta);

      batch.set(ref, mapaHerramienta);
    }

    for (int i = 0; i < instrucciones.size(); i++) {
      var ref = db.collection("clanks")
              .document(clankId)
              .collection("instrucciones")
              .document();

      Map<String, Object> mapaInstruccion = new HashMap<>();

      mapaInstruccion.put("orden", i + 1);
      mapaInstruccion.put("instruccion", instrucciones.get(i));

      if (i < urlsImagenes.size() && urlsImagenes.get(i) != null) {
        mapaInstruccion.put("imagen", urlsImagenes.get(i));
      }

      batch.set(ref, mapaInstruccion);
    }

    batch.commit()
            .addOnSuccessListener(resultado ->
                    estadoPublicacion.setValue(Recurso.exito(null))
            )
            .addOnFailureListener(error ->
                    estadoPublicacion.setValue(Recurso.error(error.getMessage()))
            );
  }

  /// ////////////////////// limpieza /////////////////////////

  @Override
  protected void onCleared() {
    super.onCleared();

    if (authStateListener != null) {
      FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
      authStateListener = null;
    }

    traductorCategorias.cerrar();
  }

  public void cargarCategorias() {
    if (categoriasCargadas) return;
    ejecutarCargaCategorias();
  }


}
