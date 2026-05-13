package com.clank.app.ui.editar;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clank.app.data.repository.ClankRepository;
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
        public String clankId = "";
        public String titulo = "";
        public String descripcion = "";
        public int tiempo = -1;
        public String portadaUrl = "";
        public List<String> categorias = new ArrayList<>();
        public List<String[]> materiales = new ArrayList<>();
        public List<String> herramientas = new ArrayList<>();
        public List<String[]> instrucciones = new ArrayList<>(); // [texto, urlImagen|null]
    }

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final AuthRepository authRepository;
    private final MutableLiveData<DatosClank> datosClank = new MutableLiveData<>();
    private final MutableLiveData<Recurso<Void>> estadoGuardar = new MutableLiveData<>();
    private final MutableLiveData<List<String[]>> categorias = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hayCambios = new MutableLiveData<>(false);
    private String clankId;
    private final ClankRepository clankRepository;

    @Inject
    public EditarClankViewModel(FirebaseFirestore db,
                                FirebaseStorage storage,
                                AuthRepository authRepository,
                                ClankRepository clankRepository) {
        this.db = db;
        this.storage = storage;
        this.authRepository = authRepository;
        this.clankRepository = clankRepository;
        cargarCategorias();
    }

    /// //////////////////////getters/////////////////////////
    public LiveData<DatosClank> getDatosClank() {
        return datosClank;
    }

    public LiveData<Recurso<Void>> getEstadoGuardar() {
        return estadoGuardar;
    }

    public LiveData<List<String[]>> getCategorias() {
        return categorias;
    }

    public LiveData<Boolean> getHayCambios() {
        return hayCambios;
    }

    public void marcarCambios() {
        hayCambios.setValue(true);
    }

    public boolean hayCambios() {
        return Boolean.TRUE.equals(hayCambios.getValue());
    }


    public void cargarClank(String id) {
        this.clankId = id;
        db.collection("clanks").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    DatosClank datos = new DatosClank();
                    datos.clankId = id;
                    datos.titulo = strOrEmpty(doc.getString("titulo"));
                    datos.descripcion = strOrEmpty(doc.getString("descripcion"));
                    datos.portadaUrl = strOrEmpty(doc.getString("portada"));
                    Long tiempo = doc.getLong("tiempo");
                    datos.tiempo = tiempo != null ? tiempo.intValue() : -1;
                    @SuppressWarnings("unchecked")
                    List<String> cats = (List<String>) doc.get("categorias");
                    if (cats != null) datos.categorias = cats;
                    cargarSubcolecciones(datos);
                });
    }

    private void cargarSubcolecciones(DatosClank datos) {
        Task<com.google.firebase.firestore.QuerySnapshot> tareaMateriales =
                db.collection("clanks").document(datos.clankId)
                        .collection("materiales").get();

        Task<com.google.firebase.firestore.QuerySnapshot> tareaHerramientas =
                db.collection("clanks").document(datos.clankId)
                        .collection("herramientas").get();

        Task<com.google.firebase.firestore.QuerySnapshot> tareaInstrucciones =
                db.collection("clanks").document(datos.clankId)
                        .collection("instrucciones")
                        .orderBy("orden")
                        .get();

        Tasks.whenAllSuccess(tareaMateriales, tareaHerramientas, tareaInstrucciones)
                .addOnSuccessListener(resultados -> {
                    com.google.firebase.firestore.QuerySnapshot snapMateriales =
                            (com.google.firebase.firestore.QuerySnapshot) resultados.get(0);

                    com.google.firebase.firestore.QuerySnapshot snapHerramientas =
                            (com.google.firebase.firestore.QuerySnapshot) resultados.get(1);

                    com.google.firebase.firestore.QuerySnapshot snapInstrucciones =
                            (com.google.firebase.firestore.QuerySnapshot) resultados.get(2);

                    android.util.Log.d("EDITAR_CLANK",
                            "Materiales encontrados: " + snapMateriales.size());
                    android.util.Log.d("EDITAR_CLANK",
                            "Herramientas encontradas: " + snapHerramientas.size());
                    android.util.Log.d("EDITAR_CLANK",
                            "Instrucciones encontradas: " + snapInstrucciones.size());

                    datos.materiales.clear();
                    datos.herramientas.clear();
                    datos.instrucciones.clear();

                    for (QueryDocumentSnapshot doc : snapMateriales) {
                        Long cantidad = doc.getLong("cantidad");

                        android.util.Log.d("EDITAR_CLANK",
                                "Material -> cantidad: " + cantidad
                                        + ", nombre: " + doc.getString("nombre"));

                        datos.materiales.add(new String[]{
                                cantidad != null ? String.valueOf(cantidad.intValue()) : "1",
                                strOrEmpty(doc.getString("material"))
                        });
                    }

                    for (QueryDocumentSnapshot doc : snapHerramientas) {
                        android.util.Log.d("EDITAR_CLANK",
                                "Herramienta -> nombre: " + doc.getString("nombre"));

                        datos.herramientas.add(
                                strOrEmpty(doc.getString("herramienta"))
                        );
                    }

                    for (QueryDocumentSnapshot doc : snapInstrucciones) {
                        String texto = strOrEmpty(doc.getString("instruccion"));
                        String imagen = doc.contains("imagen")
                                ? strOrEmpty(doc.getString("imagen"))
                                : null;

                        android.util.Log.d("EDITAR_CLANK",
                                "Instruccion -> texto: " + texto
                                        + ", imagen: " + imagen);

                        datos.instrucciones.add(new String[]{
                                texto,
                                imagen
                        });
                    }

                    datosClank.setValue(datos);
                })
                .addOnFailureListener(e ->
                        android.util.Log.e("EDITAR_CLANK",
                                "Error cargando subcolecciones", e)
                );
    }

    /// //////////////////////publicar/////////////////////////

    public void publicarClank(String titulo, String descripcion, int tiempo, Uri portadaUri, String portadaUrlActual,
                              List<String[]> materiales, List<String> herramientas, List<String> instrucciones, List<Object> imagenesInstrucciones,
                              List<String> categoriasSeleccionadas) {
        estadoGuardar.setValue(Recurso.cargando());
        procesarYGuardar(true, titulo, descripcion, tiempo,
                portadaUri, portadaUrlActual, authRepository.getUid(),
                materiales, herramientas, instrucciones, imagenesInstrucciones,
                categoriasSeleccionadas);
    }

    /// //////////////////////boceto/////////////////////////
    public void guardarBoceto(String titulo, String descripcion, int tiempo, Uri portadaUri, String portadaUrlActual,
                              List<String[]> materiales, List<String> herramientas, List<String> instrucciones, List<Object> imagenesInstrucciones,
                              List<String> categoriasSeleccionadas) {
        estadoGuardar.setValue(Recurso.cargando());
        procesarYGuardar(false, titulo, descripcion, tiempo,
                portadaUri, portadaUrlActual, authRepository.getUid(),
                materiales, herramientas, instrucciones, imagenesInstrucciones,
                categoriasSeleccionadas);
    }

    /// //////////////////////guardar/////////////////////////
    private void procesarYGuardar(boolean acabado, String titulo, String descripcion, int tiempo, Uri portadaUri, String portadaUrlActual, String uid,
                                  List<String[]> materiales, List<String> herramientas, List<String> textos, List<Object> imagenesInstrucciones, List<String> categoriasSeleccionadas) {
        if (portadaUri != null) {
            //ruta: {uid}/{clankId}/portada/{fileName} — coincide con las reglas de Storage
            StorageReference ref = storage.getReference()
                    .child(uid + "/" + clankId + "/portada/" + UUID.randomUUID() + ".jpg");
            ref.putFile(portadaUri)
                    .continueWithTask(tarea -> {
                        if (!tarea.isSuccessful()) throw tarea.getException();
                        return ref.getDownloadUrl();
                    })
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

    private void subirImagenesInstruccionesYGuardar(boolean acabado, String titulo, String descripcion, int tiempo, String portadaUrl, String uid,
                                                    List<String[]> materiales, List<String> herramientas, List<String> textos,
                                                    List<Object> imagenesInstrucciones, List<String> categoriasSeleccionadas) {
        List<Task<String>> tareas = new ArrayList<>();

        for (int i = 0; i < imagenesInstrucciones.size(); i++) {
            Object imagen = imagenesInstrucciones.get(i);
            if (imagen instanceof Uri) {
                //ruta: {uid}/{clankId}/instrucciones/{fileName} — coincide con las reglas de Storage
                StorageReference ref = storage.getReference()
                        .child(uid + "/" + clankId + "/instrucciones/" + UUID.randomUUID() + ".jpg");
                Task<String> tarea = ref.putFile((Uri) imagen)
                        .continueWithTask(t -> {
                            if (!t.isSuccessful()) throw t.getException();
                            return ref.getDownloadUrl();
                        })
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

    private void guardarEnFirestore(boolean acabado, String titulo, String descripcion, int tiempo, String portadaUrl, String uid,
                                    List<String[]> materiales, List<String> herramientas, List<String> textos, List<String> urlsInstrucciones, List<String> categoriasSeleccionadas) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("titulo", titulo);
        datos.put("descripcion", descripcion);
        datos.put("tiempo", tiempo);
        datos.put("portada", portadaUrl);
        datos.put("categorias", categoriasSeleccionadas);
        datos.put("usuarioId", uid);
        datos.put("estadoAcabado", acabado);
        datos.put("fechaEdicion", Timestamp.now());

        db.collection("clanks").document(clankId).update(datos).addOnSuccessListener(unused -> {
                    guardarSubcolecciones(materiales, herramientas, textos, urlsInstrucciones);
                })
                .addOnFailureListener(e ->
                        estadoGuardar.postValue(Recurso.error(e.getMessage())));
    }

    private void guardarSubcolecciones(List<String[]> materiales, List<String> herramientas, List<String> textos, List<String> urlsInstrucciones) {
        String rutaBase = "clanks/" + clankId;

        //borra y reescribe materiales
        db.collection(rutaBase + "/materiales").get().addOnSuccessListener(snap -> {
            for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
            for (String[] material : materiales) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("cantidad", convertirCantidadAEntero(material[0]));
                mapa.put("material", material[1]);
                db.collection(rutaBase + "/materiales").add(mapa);
            }
        });

        //borra y reescribe herramientas
        db.collection(rutaBase + "/herramientas").get().addOnSuccessListener(snap -> {
            for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
            for (String herramienta : herramientas) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("herramienta", herramienta);
                db.collection(rutaBase + "/herramientas").add(mapa);
            }
        });

        //borra y reescribe instrucciones
        db.collection(rutaBase + "/instrucciones").get().addOnSuccessListener(snap -> {
            for (QueryDocumentSnapshot doc : snap) doc.getReference().delete();
            for (int i = 0; i < textos.size(); i++) {
                Map<String, Object> mapa = new HashMap<>();
                mapa.put("instruccion", textos.get(i));
                mapa.put("orden", i);
                String urlImagen = (i < urlsInstrucciones.size()) ? urlsInstrucciones.get(i) : null;
                if (urlImagen != null && !urlImagen.isEmpty()) mapa.put("imagen", urlImagen);
                db.collection(rutaBase + "/instrucciones").add(mapa);
            }
            estadoGuardar.postValue(Recurso.exito(null));
        });
    }

    /// //////////////////////eliminar clank/////////////////////////
    public Task<Void> eliminarClank() {
        return clankRepository.eliminarCompletoPorId(clankId);
    }

    /// //////////////////////categorías de bbdd/////////////////////////
    private void cargarCategorias() {
        db.collection("categorias").get()
                .addOnSuccessListener(snap -> {
                    List<String[]> lista = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap)
                        lista.add(new String[]{doc.getId(), strOrEmpty(doc.getString("categoria"))});
                    categorias.postValue(lista);
                });
    }

    private String strOrEmpty(String valor) {
        return valor != null ? valor : "";
    }

    private int convertirCantidadAEntero(String cantidad) {
        if (cantidad == null || cantidad.trim().isEmpty()) {
            return 1;
        }

        try {
            return Integer.parseInt(cantidad.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
