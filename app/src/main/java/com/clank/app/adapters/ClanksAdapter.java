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

//RECORDAR: BORRAR EL COMPLETO SI AL FINAL NO LO USAMOS

/// //////////////////////adapter para mostrar clanks en pantallas/////////////////////////
public class ClanksAdapter extends FirestoreRecyclerAdapter<Clank, ClanksAdapter.ViewHolder> {

  public interface OnClankClickListener {
    void onClankClick(String clankId);
  }

  public interface OnOpcionesClankClickListener {
    void onOpcionesClankClick(String clankId, String tituloClank);
  }

  private final Context context;
  private final UsuarioRepository usuarioRepository;
  private final OnClankClickListener listener;
  @Nullable
  private final OnOpcionesClankClickListener listenerOpciones;
  private final boolean mostrarOpciones;

  @Nullable
  private final TextView textViewContador;

  /// //////////////////////para feed (sin contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, null, false, listener, null);
  }

  /// //////////////////////para filtrar (con contador, sin opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       @Nullable TextView textViewContador,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, textViewContador, false, listener, null);  }

  /// //////////////////////para perfil (sin contador, con opciones)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       boolean mostrarOpciones,
                       OnClankClickListener listener) {
    this(options, context, usuarioRepository, null, mostrarOpciones, listener, null);  }

  /// //////////////////////para perfil (sin contador, con opciones y callback propio)/////////////////////////
  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       UsuarioRepository usuarioRepository,
                       boolean mostrarOpciones,
                       OnClankClickListener listener,
                       @Nullable OnOpcionesClankClickListener listenerOpciones) {
    this(options, context, usuarioRepository, null, mostrarOpciones, listener, listenerOpciones);
  }

  /// //////////////////////completo (por si acaso)/////////////////////////
  private ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                        Context context,
                        UsuarioRepository usuarioRepository,
                        @Nullable TextView textViewContador,
                        boolean mostrarOpciones,
                        OnClankClickListener listener,
                        @Nullable OnOpcionesClankClickListener listenerOpciones) {
    super(options);
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.textViewContador = textViewContador;
    this.mostrarOpciones = mostrarOpciones;
    this.listener = listener;
    this.listenerOpciones = listenerOpciones;
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
        String tituloClank = clank.getTitulo() != null ? clank.getTitulo() : "";
        listenerOpciones.onOpcionesClankClick(clankId, tituloClank);
      }
    });

    //título y descripción
    holder.tvTitulo.setText(clank.getTitulo() != null ? clank.getTitulo() : "");
    holder.tvDescripcion.setText(clank.getDescripcion() != null ? clank.getDescripcion() : "");

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
