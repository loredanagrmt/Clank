package com.clank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.data.model.Clank;
import com.clank.app.data.repository.ClankRepository;
import com.clank.app.data.repository.LikeRepository;
import com.clank.app.databinding.TarjetaClankBinding;
import com.clank.app.util.AnimUtils;
import com.clank.app.util.GestorIdioma;
import com.clank.app.util.TraductorTarjetaClank;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClanksAdapter extends FirestoreRecyclerAdapter<Clank, ClanksAdapter.ViewHolder> {

  public static final String PAYLOAD_LIKE = "payload_like";

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

  public interface OnItemsListosListener {
    void onItemsListos(List<String> clankIds, List<Integer> numLikesIniciales);
  }

  public interface OnLikeClickListener {
    void onLikeClick(String clankId);
  }

  private final Context context;
  private final ClankRepository clankRepository;
  private final String uidUsuario;
  private final OnClankClickListener listener;
  @Nullable private final OnOpcionesClankClickListener listenerOpciones;
  private final boolean mostrarOpciones;
  @Nullable private final OnPreparacionTarjetasListener listenerPreparacion;
  @Nullable private OnItemsListosListener itemsListosListener;
  @Nullable private OnLikeClickListener likeListener;
  private final TraductorTarjetaClank traductorTarjetaClank;
  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido> cacheTraducciones = new HashMap<>();
  private final Map<String, Boolean>  estadoLikesLocal   = new HashMap<>();
  private final Map<String, Integer>  contadorLikesLocal = new HashMap<>();
  private int versionPreparacion = 0;
  @Nullable private final TextView textViewContador;

  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       ClankRepository clankRepository,
                       LikeRepository likeRepository,
                       String uidUsuario,
                       boolean mostrarOpciones,
                       OnClankClickListener listener,
                       @Nullable OnOpcionesClankClickListener listenerOpciones,
                       @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    this(options, context, clankRepository, uidUsuario,
      null, mostrarOpciones, listener, listenerOpciones, listenerPreparacion);
  }

  public ClanksAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
                       Context context,
                       ClankRepository clankRepository,
                       LikeRepository likeRepository,
                       String uidUsuario,
                       @Nullable TextView textViewContador,
                       OnClankClickListener listener) {
    this(options, context, clankRepository, uidUsuario,
      textViewContador, false, listener, null, null);
  }

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
    this.context             = context;
    this.clankRepository     = clankRepository;
    this.uidUsuario          = uidUsuario;
    this.textViewContador    = textViewContador;
    this.mostrarOpciones     = mostrarOpciones;
    this.listener            = listener;
    this.listenerOpciones    = listenerOpciones;
    this.listenerPreparacion = listenerPreparacion;
    this.traductorTarjetaClank = new TraductorTarjetaClank(context);
  }

  public void setLikeListener(OnLikeClickListener likeListener) {
    this.likeListener = likeListener;
  }

  public void setItemsListosListener(OnItemsListosListener itemsListosListener) {
    this.itemsListosListener = itemsListosListener;
  }

  public void actualizarEstadoLike(String clankId, boolean isLiked) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    estadoLikesLocal.put(clankId, isLiked);
  }

  public void actualizarContadorLike(String clankId, int numLikes) {
    if (clankId == null || clankId.trim().isEmpty()) {
      return;
    }

    contadorLikesLocal.put(clankId, Math.max(0, numLikes));
  }

  public void actualizarLike(String clankId, boolean isLiked, int numLikes) {
    actualizarEstadoLike(clankId, isLiked);
    actualizarContadorLike(clankId, numLikes);
  }


  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    TarjetaClankBinding b = TarjetaClankBinding.inflate(
      LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(b);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                  @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    if (textViewContador != null) {
      textViewContador.setText(String.valueOf(getSnapshots().size()));
    }

    //título y descripción
    TraductorTarjetaClank.TextoTarjetaTraducido textos =
      obtenerTextoTarjeta(clankId, clank);
    holder.binding.tvTituloClank.setText(textos.titulo);
    holder.binding.tvDescripcionClank.setText(textos.descripcion);

    //icono tiempo
    int tiempo = clank.getTiempo();
    int iconoTiempo = tiempo == 0 ? R.drawable.ic_cohete
      : tiempo == 1         ? R.drawable.ic_liebre
      : R.drawable.ic_tortuga;
    holder.binding.ivTiempo.setImageDrawable(
      ContextCompat.getDrawable(context, iconoTiempo));

    //portada
    holder.binding.ivPortada.setTag(clankId);
    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context).load(clank.getPortada()).centerCrop()
        .into(holder.binding.ivPortada);
    } else {
      holder.binding.ivPortada.setImageDrawable(null);
    }

    //contador likes
    Integer contadorVm = contadorLikesLocal.get(clankId);
    holder.binding.tvNumLikes.setText(
      String.valueOf(contadorVm != null ? contadorVm : clank.getNumLikes()));


    holder.binding.ivOpciones.setVisibility(View.VISIBLE);

    if (mostrarOpciones) {
      //perfil propio: menú de opciones
      holder.binding.ivOpciones.setImageResource(R.drawable.ic_opciones_activo);
      holder.binding.ivOpciones.setBackgroundResource(
        R.drawable.bg_circulo_opciones_inactivo);
      holder.binding.ivOpciones.setOnClickListener(v -> {
        if (listenerOpciones != null) {
          listenerOpciones.onOpcionesClankClick(
            clankId,
            obtenerTextoTarjeta(clankId, clank).titulo);
        }
      });
    } else {
      pintarEstadoLike(holder, clankId);

      holder.binding.ivOpciones.setOnClickListener(v -> {
        if (!estadoLikesLocal.containsKey(clankId)
                || !contadorLikesLocal.containsKey(clankId)) {
          return;
        }

        holder.binding.ivOpciones.setEnabled(false);

        if (likeListener != null) {
          likeListener.onLikeClick(clankId);
        }

        holder.binding.ivOpciones.postDelayed(() -> {
          if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
            boolean listoDespues = estadoLikesLocal.containsKey(clankId)
                    && contadorLikesLocal.containsKey(clankId);

            holder.binding.ivOpciones.setEnabled(listoDespues);
            holder.binding.ivOpciones.setClickable(listoDespues);
          }
        }, 500);
      });
    }

    //click tarjeta
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
  public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                               @NonNull List<Object> payloads) {
    if (!payloads.isEmpty() && payloads.contains(PAYLOAD_LIKE)) {
      String clankId = getSnapshots().getSnapshot(position).getId();
      pintarEstadoLike(holder, clankId);
      Integer contador = contadorLikesLocal.get(clankId);
      if (contador != null) {
        holder.binding.tvNumLikes.setText(String.valueOf(contador));
      }
      AnimUtils.animarLike(holder.binding.ivOpciones);
      return;
    }
    super.onBindViewHolder(holder, position, payloads);
  }

  @Override
  public void onDataChanged() {
    if (itemsListosListener != null) {
      List<String>  ids   = new ArrayList<>();
      List<Integer> likes = new ArrayList<>();
      for (int i = 0; i < getItemCount(); i++) {
        ids.add(getSnapshots().getSnapshot(i).getId());
        likes.add(getItem(i).getNumLikes());
      }
      itemsListosListener.onItemsListos(ids, likes);
    }
    prepararTraduccionesTarjetas();
  }


  private void pintarEstadoLike(@NonNull ViewHolder holder, String clankId) {
    Boolean activo = estadoLikesLocal.get(clankId);
    boolean liked = Boolean.TRUE.equals(activo);

    boolean likeListo = estadoLikesLocal.containsKey(clankId)
            && contadorLikesLocal.containsKey(clankId);

    holder.binding.ivOpciones.setImageResource(
            liked
                    ? R.drawable.ic_like_activo
                    : R.drawable.ic_like_inactivo
    );

    holder.binding.ivOpciones.setBackgroundResource(
            liked
                    ? R.drawable.bg_circulo_opciones_activo
                    : R.drawable.bg_circulo_opciones_inactivo
    );

    holder.binding.ivOpciones.setEnabled(likeListo);
    holder.binding.ivOpciones.setClickable(likeListo);
    holder.binding.ivOpciones.setFocusable(likeListo);
  }


  @Override
  public void onError(@NonNull FirebaseFirestoreException error) {
    super.onError(error);
    if (listenerPreparacion != null) listenerPreparacion.alFinalizarPreparacion();
  }

  private void prepararTraduccionesTarjetas() {
    int versionActual = ++versionPreparacion;
    if (listenerPreparacion != null) listenerPreparacion.alIniciarPreparacion();

    List<Task<?>> tareas = new ArrayList<>();

    for (int i = 0; i < getItemCount(); i++) {
      Clank clank   = getItem(i);
      String clankId = getSnapshots().getSnapshot(i).getId();
      String clave   = construirClaveTraduccion(clankId, clank);
      if (cacheTraducciones.containsKey(clave)) continue;

      String tituloOrig = clank.getTitulo()      != null ? clank.getTitulo()      : "";
      String descOrig   = clank.getDescripcion() != null ? clank.getDescripcion() : "";
      TraductorTarjetaClank.TextoTarjetaTraducido textoOrig =
        new TraductorTarjetaClank.TextoTarjetaTraducido(tituloOrig, descOrig);

      Task<?> tarea = traductorTarjetaClank
        .traducirSiProcede(tituloOrig, descOrig)
        .addOnSuccessListener(r ->
          cacheTraducciones.put(clave, r != null ? r : textoOrig))
        .addOnFailureListener(e ->
          cacheTraducciones.put(clave, textoOrig));
      tareas.add(tarea);
    }

    if (tareas.isEmpty()) {
      finalizarPreparacion(versionActual);
      return;
    }

    Tasks.whenAllComplete(tareas)
      .addOnCompleteListener(t -> finalizarPreparacion(versionActual));
  }

  private void finalizarPreparacion(int versionActual) {
    if (versionActual != versionPreparacion) return;
    notifyDataSetChanged();
    if (listenerPreparacion != null) listenerPreparacion.alFinalizarPreparacion();
  }

  private TraductorTarjetaClank.TextoTarjetaTraducido obtenerTextoTarjeta(
    String clankId, Clank clank) {
    String clave = construirClaveTraduccion(clankId, clank);
    TraductorTarjetaClank.TextoTarjetaTraducido r = cacheTraducciones.get(clave);
    if (r != null) return r;
    return new TraductorTarjetaClank.TextoTarjetaTraducido(
            clank.getTitulo() != null ? clank.getTitulo() : "",
            clank.getDescripcion() != null ? clank.getDescripcion() : ""
    );
  }

  private String construirClaveTraduccion(String clankId, Clank clank) {
    String idioma = GestorIdioma.getInstance(context).getIdiomaActual();
    return clankId + "|" + idioma + "|"
      + Objects.hash(clank.getTitulo(), clank.getDescripcion());
  }
  public void cerrar() {
    traductorTarjetaClank.cerrar();
  }


  public static class ViewHolder extends RecyclerView.ViewHolder {
    final TarjetaClankBinding binding;
    public ViewHolder(@NonNull TarjetaClankBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}
