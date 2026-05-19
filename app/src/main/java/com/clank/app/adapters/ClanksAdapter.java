package com.clank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.clank.app.R;
import com.clank.app.data.model.Clank;
import com.bumptech.glide.Glide;

import com.clank.app.util.GestorIdioma;
import com.clank.app.util.TraductorTarjetaClank;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.clank.app.data.repository.ClankRepository;

public class ClanksAdapter extends FirestoreRecyclerAdapter<Clank, ClanksAdapter.ViewHolder> {

  public interface OnClankClickListener {
    void onClankClick(String clankId);
  }

  public interface OnOpcionesClankClickListener {
    void onOpcionesClankClick(String clankId, String tituloClank);
  }

  public interface OnPreparacionTarjetasListener {
    void alIniciarPreparacion();
    void alFinalizarPreparacion();
  }

  private final Context context;
  private final ClankRepository clankRepository;
  private final String uidUsuario;
  private final OnClankClickListener listener;
  @Nullable
  private final OnOpcionesClankClickListener listenerOpciones;
  private final boolean mostrarOpciones;
  @Nullable
  private final OnPreparacionTarjetasListener listenerPreparacion;
  private final TraductorTarjetaClank traductorTarjetaClank;
  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido> cacheTraducciones = new HashMap<>();
  private final Map<String, Boolean> cacheLikes = new HashMap<>();
  private int versionPreparacion = 0;
  @Nullable
  private final TextView textViewContador;

  /// //////////////////////para feed (sin contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       ClankRepository clankRepository,
                       String uidUsuario,
                       OnClankClickListener listener) {
    this(options, context, clankRepository, uidUsuario, null, false, listener, null, null);
  }

  /// //////////////////////para filtrar (con contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       ClankRepository clankRepository,
                       String uidUsuario,
                       @Nullable TextView textViewContador,
                       OnClankClickListener listener) {
    this(options, context, clankRepository, uidUsuario, textViewContador, false, listener, null, null);
  }

  /// //////////////////////para perfil (sin contador, con opciones y callback)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       ClankRepository clankRepository,
                       String uidUsuario,
                       boolean mostrarOpciones,
                       OnClankClickListener listener,
                       @Nullable OnOpcionesClankClickListener listenerOpciones,
                       @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    this(options, context, clankRepository, uidUsuario, null, mostrarOpciones, listener, listenerOpciones, listenerPreparacion);
  }

  /// //////////////////////completo/////////////////////////
  private ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                        Context context,
                        ClankRepository clankRepository,
                        String uidUsuario,
                        @Nullable TextView textViewContador,
                        boolean mostrarOpciones,
                        OnClankClickListener listener,
                        @Nullable OnOpcionesClankClickListener listenerOpciones,
                        @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    super(options);
    this.context = context;
    this.clankRepository = clankRepository;
    this.uidUsuario = uidUsuario;
    this.textViewContador = textViewContador;
    this.mostrarOpciones = mostrarOpciones;
    this.listener = listener;
    this.listenerOpciones = listenerOpciones;
    this.listenerPreparacion = listenerPreparacion;
    this.traductorTarjetaClank = new TraductorTarjetaClank(context);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    //contador de resultado
    if (textViewContador != null) {
      textViewContador.setText(String.valueOf(getSnapshots().size()));
    }

    //título y descripción
    TraductorTarjetaClank.TextoTarjetaTraducido textos = obtenerTextoTarjeta(clankId, clank);
    holder.tvTitulo.setText(textos.titulo);
    holder.tvDescripcion.setText(textos.descripcion);

    // icono tiempo
    int tiempo = clank.getTiempo();
    int iconoTiempo;
    if (tiempo == 0) iconoTiempo = R.drawable.ic_cohete;
    else if (tiempo == 1) iconoTiempo = R.drawable.ic_liebre;
    else iconoTiempo = R.drawable.ic_tortuga;
    holder.ivTiempo.setImageDrawable(ContextCompat.getDrawable(context, iconoTiempo));

    //portada
    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context).load(clank.getPortada()).centerCrop().into(holder.ivPortada);
    } else {
      holder.ivPortada.setImageDrawable(null);
    }

    //contador likes
    holder.tvNumLikes.setText(String.valueOf(clank.getNumLikes()));

    //esquina superior derecha: opciones o like
    if (mostrarOpciones) {
      holder.ivOpciones.setVisibility(View.VISIBLE);
      holder.ivOpciones.setImageResource(R.drawable.ic_opciones_activo);
      holder.ivOpciones.setBackgroundResource(R.drawable.bg_circulo_opciones_inactivo);
      holder.ivOpciones.setOnClickListener(v -> {
        if (listenerOpciones != null) {
          listenerOpciones.onOpcionesClankClick(
            clankId,
            obtenerTextoTarjeta(clankId, clank).titulo
          );
        }
      });
    } else {
      holder.ivOpciones.setVisibility(View.VISIBLE);
      actualizarBotonLike(holder, clankId);
      holder.ivOpciones.setOnClickListener(v -> {
        if (uidUsuario == null || uidUsuario.isEmpty()) return;
        clankRepository.toggleLike(clankId, uidUsuario)
          .addOnSuccessListener(ahorraTieneLike -> {
            cacheLikes.put(clankId, ahorraTieneLike);
            actualizarBotonLike(holder, clankId);
          });
      });
    }

    holder.itemView.setOnClickListener(v -> {
      int pos = holder.getBindingAdapterPosition();
      if (pos == RecyclerView.NO_POSITION) return;
      String id = clank.getClankId();
      if (id == null || id.isEmpty())
        id = getSnapshots().getSnapshot(pos).getId();
      if (id != null && !id.isEmpty() && listener != null)
        listener.onClankClick(id);
    });

    //carga estado like si no está en caché
    if (!mostrarOpciones && uidUsuario != null && !uidUsuario.isEmpty()
      && !cacheLikes.containsKey(clankId)) {
      clankRepository.hasDadoLike(clankId, uidUsuario)
        .addOnSuccessListener(dioLike -> {
          cacheLikes.put(clankId, dioLike);
          actualizarBotonLike(holder, clankId);
        });
    }
  }
  private void actualizarBotonLike(@NonNull ViewHolder holder, String clankId) {
    Boolean dioLike = cacheLikes.get(clankId);
    boolean activo = Boolean.TRUE.equals(dioLike);
    holder.ivOpciones.setImageResource(
      activo ? R.drawable.ic_like_activo : R.drawable.ic_like_inactivo
    );
    holder.ivOpciones.setBackgroundResource(
      activo ? R.drawable.bg_circulo_opciones_activo : R.drawable.bg_circulo_opciones_inactivo
    );
  }

  @Override
  public void onDataChanged() {
    prepararTraduccionesTarjetas();
  }

  @Override
  public void onError(@NonNull FirebaseFirestoreException error) {
    super.onError(error);
    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private void prepararTraduccionesTarjetas() {
    int versionActual = ++versionPreparacion;
    if (listenerPreparacion != null) {
      listenerPreparacion.alIniciarPreparacion();
    }

    List<Task<?>> tareas = new ArrayList<>();

    for (int i = 0; i < getItemCount(); i++) {
      Clank clank = getItem(i);
      String clankId = getSnapshots().getSnapshot(i).getId();
      String clave = construirClaveTraduccion(clankId, clank);

      if (cacheTraducciones.containsKey(clave)) continue;

      String tituloOriginal = clank.getTitulo() != null ? clank.getTitulo() : "";
      String descripcionOriginal = clank.getDescripcion() != null ? clank.getDescripcion() : "";
      TraductorTarjetaClank.TextoTarjetaTraducido textoOriginal =
        new TraductorTarjetaClank.TextoTarjetaTraducido(tituloOriginal, descripcionOriginal);

      Task<?> tarea = traductorTarjetaClank
        .traducirSiProcede(tituloOriginal, descripcionOriginal)
        .addOnSuccessListener(resultado ->
          cacheTraducciones.put(clave, resultado != null ? resultado : textoOriginal))
        .addOnFailureListener(error ->
          cacheTraducciones.put(clave, textoOriginal));

      tareas.add(tarea);
    }

    if (tareas.isEmpty()) {
      finalizarPreparacion(versionActual);
      return;
    }

    Tasks.whenAllComplete(tareas)
      .addOnCompleteListener(tarea -> finalizarPreparacion(versionActual));
  }

  private void finalizarPreparacion(int versionActual) {
    if (versionActual != versionPreparacion) return;
    notifyDataSetChanged();
    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private TraductorTarjetaClank.TextoTarjetaTraducido obtenerTextoTarjeta(String clankId, Clank clank) {
    String clave = construirClaveTraduccion(clankId, clank);
    TraductorTarjetaClank.TextoTarjetaTraducido resultado = cacheTraducciones.get(clave);
    if (resultado != null) return resultado;
    return new TraductorTarjetaClank.TextoTarjetaTraducido(
            clank.getTitulo() != null ? clank.getTitulo() : "",
            clank.getDescripcion() != null ? clank.getDescripcion() : ""
    );
  }

  private String construirClaveTraduccion(String clankId, Clank clank) {
    String idiomaDestino = GestorIdioma.getInstance(context).getIdiomaActual();
    return clankId + "|" + idiomaDestino + "|" + Objects.hash(clank.getTitulo(), clank.getDescripcion());
  }
  public void cerrar() {
    traductorTarjetaClank.cerrar();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.tarjeta_clank, parent, false);
    return new ViewHolder(view);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    TextView tvTitulo;
    TextView tvDescripcion;
    TextView tvNumLikes;
    ImageView ivPortada;
    ImageView ivOpciones;
    ImageView ivTiempo;
    ImageView ivLike;

    public ViewHolder(@NonNull View view) {
      super(view);
      tvTitulo = view.findViewById(R.id.tvTituloClank);
      tvDescripcion = view.findViewById(R.id.tvDescripcionClank);
      ivPortada = view.findViewById(R.id.ivPortada);
      ivOpciones = view.findViewById(R.id.ivOpciones);
      ivTiempo = view.findViewById(R.id.ivTiempo);
    }
  }
}
