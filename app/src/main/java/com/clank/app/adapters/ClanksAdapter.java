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
import com.clank.app.data.repository.UsuarioRepository;
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

//RECORDAR: BORRAR EL COMPLETO SI AL FINAL NO LO USAMOS

/// //////////////////////adapter para mostrar clanks en pantallas/////////////////////////
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
  private final UsuarioRepository usuarioRepository;
  private final OnClankClickListener listener;
  @Nullable
  private final OnOpcionesClankClickListener listenerOpciones;
  private final boolean mostrarOpciones;

  @Nullable
  private final OnPreparacionTarjetasListener listenerPreparacion;

  private final TraductorTarjetaClank traductorTarjetaClank;

  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido>
          cacheTraducciones = new HashMap<>();

  private int versionPreparacion = 0;

  @Nullable
  private final TextView textViewContador;

  /// //////////////////////para feed (sin contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, null, false, listener, null, null);
  }

  /// //////////////////////para filtrar (con contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       @Nullable TextView textViewContador,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, textViewContador, false, listener, null, null);
  }

  /// //////////////////////para perfil (sin contador, con opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       boolean mostrarOpciones,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, null, mostrarOpciones, listener, null, null);
  }

  /// //////////////////////para perfil (sin contador, con opciones y callback propio)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       boolean mostrarOpciones,
                       OnClankClickListener listener,
                       @Nullable OnOpcionesClankClickListener listenerOpciones) {
    this(options, context, usuarioRepository, null, mostrarOpciones, listener, listenerOpciones, null);
  }

  /// //////////////////////para perfil con traducción preparada/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       boolean mostrarOpciones,
                       OnClankClickListener listener,
                       @Nullable OnOpcionesClankClickListener listenerOpciones,
                       @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    this(options, context, usuarioRepository, null, mostrarOpciones, listener, listenerOpciones, listenerPreparacion);
  }

  /// //////////////////////completo/////////////////////////
  private ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                        Context context,
                        UsuarioRepository usuarioRepository,
                        @Nullable TextView textViewContador,
                        boolean mostrarOpciones,
                        OnClankClickListener listener,
                        @Nullable OnOpcionesClankClickListener listenerOpciones,
                        @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    super(options);
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.textViewContador = textViewContador;
    this.mostrarOpciones = mostrarOpciones;
    this.listener = listener;
    this.listenerOpciones = listenerOpciones;
    this.listenerPreparacion = listenerPreparacion;
    this.traductorTarjetaClank =
            new TraductorTarjetaClank(context);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    //contador de resultado
    if (textViewContador != null) {
      textViewContador.setText(String.valueOf(getSnapshots().size()));
    }

    //botón opciones
    holder.ivOpciones.setVisibility(mostrarOpciones ? View.VISIBLE : View.GONE);

    holder.ivOpciones.setOnClickListener(v -> {
      if (listenerOpciones != null) {
        String tituloClank =
                obtenerTextoTarjeta(clankId, clank).titulo;

        listenerOpciones.onOpcionesClankClick(
                clankId,
                tituloClank
        );
      }
    });

    //título y descripción
    TraductorTarjetaClank.TextoTarjetaTraducido textos =
            obtenerTextoTarjeta(clankId, clank);

    holder.tvTitulo.setText(textos.titulo);
    holder.tvDescripcion.setText(textos.descripcion);

    //icono cohele, liebre o tortuga
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

    //usuario
    String usuarioId = clank.getUsuarioId();
    if (usuarioId != null && !usuarioId.isEmpty()) {
      usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
        if (!doc.exists()) return;
        String nombre = doc.contains("nombre") ? doc.getString("nombre") : null;
        holder.tvUsuario.setText(nombre != null ? nombre : "");
      });
    }

    //listener
    holder.itemView.setOnClickListener(v -> {
      int pos = holder.getBindingAdapterPosition();
      if (pos == RecyclerView.NO_POSITION) return;
      String id = clank.getClankId();
      if (id == null || id.isEmpty())
        id = getSnapshots().getSnapshot(pos).getId();
      if (id != null && !id.isEmpty() && listener != null)
        listener.onClankClick(id);
    });
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

      String clave =
              construirClaveTraduccion(
                      clankId,
                      clank
              );

      if (cacheTraducciones.containsKey(clave)) {
        continue;
      }

      String tituloOriginal =
              clank.getTitulo() != null ? clank.getTitulo() : "";

      String descripcionOriginal =
              clank.getDescripcion() != null ? clank.getDescripcion() : "";

      TraductorTarjetaClank.TextoTarjetaTraducido textoOriginal =
              new TraductorTarjetaClank.TextoTarjetaTraducido(
                      tituloOriginal,
                      descripcionOriginal
              );

      Task<?> tarea = traductorTarjetaClank
              .traducirSiProcede(
                      tituloOriginal,
                      descripcionOriginal
              )
              .addOnSuccessListener(resultado -> {
                cacheTraducciones.put(
                        clave,
                        resultado != null ? resultado : textoOriginal
                );
              })
              .addOnFailureListener(error -> {
                cacheTraducciones.put(
                        clave,
                        textoOriginal
                );
              });

      tareas.add(tarea);
    }

    if (tareas.isEmpty()) {
      finalizarPreparacion(versionActual);
      return;
    }

    Tasks.whenAllComplete(tareas)
            .addOnCompleteListener(tarea ->
                    finalizarPreparacion(versionActual)
            );
  }

  private void finalizarPreparacion(int versionActual) {
    if (versionActual != versionPreparacion) {
      return;
    }

    notifyDataSetChanged();

    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private TraductorTarjetaClank.TextoTarjetaTraducido obtenerTextoTarjeta(
          String clankId,
          Clank clank
  ) {
    String clave =
            construirClaveTraduccion(
                    clankId,
                    clank
            );

    TraductorTarjetaClank.TextoTarjetaTraducido resultado =
            cacheTraducciones.get(clave);

    if (resultado != null) {
      return resultado;
    }

    return new TraductorTarjetaClank.TextoTarjetaTraducido(
            clank.getTitulo() != null ? clank.getTitulo() : "",
            clank.getDescripcion() != null ? clank.getDescripcion() : ""
    );
  }

  private String construirClaveTraduccion(
          String clankId,
          Clank clank
  ) {
    String idiomaDestino =
            GestorIdioma.getInstance(context)
                    .getIdiomaActual();

    return clankId
            + "|"
            + idiomaDestino
            + "|"
            + Objects.hash(
            clank.getTitulo(),
            clank.getDescripcion()
    );
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
    TextView tvUsuario;
    TextView tvDescripcion;
    ImageView ivPortada;
    ImageView ivOpciones;
    ImageView ivTiempo;

    public ViewHolder(@NonNull View view) {
      super(view);
      tvTitulo = view.findViewById(R.id.tvTituloClank);
      tvUsuario = view.findViewById(R.id.tvUsuario);
      tvDescripcion = view.findViewById(R.id.tvDescripcionClank);
      ivPortada = view.findViewById(R.id.ivPortada);
      ivOpciones = view.findViewById(R.id.ivOpciones);
      ivTiempo = view.findViewById(R.id.ivTiempo);
    }
  }
}
