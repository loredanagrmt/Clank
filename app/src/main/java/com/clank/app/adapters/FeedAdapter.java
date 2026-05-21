package com.clank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.clank.app.R;
import com.clank.app.data.model.Clank;
import com.clank.app.data.repository.UsuarioRepository;
import com.clank.app.databinding.ItemFeedBinding;
import com.clank.app.util.AnimUtils;
import com.clank.app.util.FechaUtils;
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

public class FeedAdapter extends FirestoreRecyclerAdapter<Clank, FeedAdapter.ViewHolder> {
  public static final String PAYLOAD_LIKE = "payload_like";

  public interface OnClankClickListener {
    void onClankClick(String clankId);
    void onUsuarioClick(String usuarioId);
  }

  public interface OnLikeClickListener {
    void onLikeClick(String clankId);
  }

  public interface OnItemsListosListener {
    void onItemsListos(List<String> clankIds, List<Integer> numLikesIniciales);
  }
  public interface OnPreparacionTarjetasListener {
    void alIniciarPreparacion();
    void alFinalizarPreparacion();
  }
  private final Context context;
  private final UsuarioRepository usuarioRepository;
  private final String uidUsuario;
  private final OnClankClickListener listener;
  private final OnLikeClickListener likeListener;
  @Nullable private final OnItemsListosListener itemsListosListener;
  @Nullable private final OnPreparacionTarjetasListener listenerPreparacion;
  private final TraductorTarjetaClank traductorTarjetaClank;
  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido> cacheTraducciones = new HashMap<>();
  private final Map<String, Boolean>  estadoLikesLocal   = new HashMap<>();
  private final Map<String, Integer>  contadorLikesLocal = new HashMap<>();

  private int versionPreparacion = 0;
  private boolean tarjetasPreparadas = false;

  /////////////////////////constructor/////////////////////////

  public FeedAdapter(
    @NonNull FirestoreRecyclerOptions<Clank> options,
    Context context,
    UsuarioRepository usuarioRepository,
    String uidUsuario,
    OnClankClickListener listener,
    OnLikeClickListener likeListener,
    @Nullable OnItemsListosListener itemsListosListener,
    @Nullable OnPreparacionTarjetasListener listenerPreparacion) {
    super(options);
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.uidUsuario = uidUsuario;
    this.listener = listener;
    this.likeListener = likeListener;
    this.itemsListosListener = itemsListosListener;
    this.listenerPreparacion = listenerPreparacion;
    this.traductorTarjetaClank = new TraductorTarjetaClank(context);
  }

  @Override
  public int getItemCount() {
    return tarjetasPreparadas ? super.getItemCount() : 0;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemFeedBinding b = ItemFeedBinding.inflate(
      LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(b);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                  @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    String claveTraduccion = construirClaveTraduccion(clankId, clank);

    if (!cacheTraducciones.containsKey(claveTraduccion)) {
      holder.itemView.setVisibility(View.INVISIBLE);
      return;
    }

    holder.itemView.setVisibility(View.VISIBLE);

    //título y descripción
    TraductorTarjetaClank.TextoTarjetaTraducido textos =
      obtenerTextoTarjeta(clankId, clank);
    holder.binding.tarjeta.tvTituloClank.setText(textos.titulo);
    holder.binding.tarjeta.tvDescripcionClank.setText(textos.descripcion);

    //icono de tiempo
    int tiempo = clank.getTiempo();
    int iconoTiempo = tiempo == 0 ? R.drawable.ic_cohete
      : tiempo == 1         ? R.drawable.ic_liebre
      : R.drawable.ic_tortuga;
    holder.binding.tarjeta.ivTiempo.setImageDrawable(
      ContextCompat.getDrawable(context, iconoTiempo));

    //portada
    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context).load(clank.getPortada()).centerCrop()
        .into(holder.binding.tarjeta.ivPortada);
    } else {
      holder.binding.tarjeta.ivPortada.setImageDrawable(null);
    }

    Integer contadorVm = contadorLikesLocal.get(clankId);
    holder.binding.tarjeta.tvNumLikes.setText(
      String.valueOf(contadorVm != null ? contadorVm : clank.getNumLikes()));

    holder.binding.tarjeta.ivOpciones.setVisibility(View.VISIBLE);
    pintarEstadoLike(holder, clankId);

    holder.binding.tarjeta.ivOpciones.setOnClickListener(v -> {
      if (likeListener != null) likeListener.onLikeClick(clankId);
    });
    holder.binding.cabeceraUsuario.tvUsernameItem.setText("");
    holder.binding.cabeceraUsuario.civAvatarUsuario
      .setImageResource(R.drawable.ic_usuario_inactivo);
    Glide.with(context).clear(holder.binding.cabeceraUsuario.civAvatarUsuario);

    String usuarioId = clank.getUsuarioId();
    if (usuarioId != null && !usuarioId.isEmpty()) {
      holder.binding.cabeceraUsuario.civAvatarUsuario.setTag(usuarioId);

      usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
        if (!usuarioId.equals(
          holder.binding.cabeceraUsuario.civAvatarUsuario.getTag())) return;
        if (!doc.exists()) return;

        String foto = doc.contains("fotoPerfil")
          ? doc.getString("fotoPerfil") : "";
        String usuarioClank = doc.contains("usuarioClank")
          ? doc.getString("usuarioClank") : "";
        String handle = usuarioClank != null
          ? usuarioClank.replace("@", "").trim() : "";

        holder.binding.cabeceraUsuario.tvUsernameItem.setText(
          !handle.isEmpty() ? "@" + handle : "");

        if (foto != null && !foto.isEmpty()) {
          Glide.with(context).load(foto).circleCrop()
            .placeholder(R.drawable.ic_usuario_inactivo)
            .into(holder.binding.cabeceraUsuario.civAvatarUsuario);
        } else {
          holder.binding.cabeceraUsuario.civAvatarUsuario
            .setImageResource(R.drawable.ic_usuario_inactivo);
        }
      });

      holder.binding.cabeceraUsuario.civAvatarUsuario
        .setOnClickListener(v -> {
          if (listener != null) listener.onUsuarioClick(usuarioId);
        });
      holder.binding.cabeceraUsuario.tvUsernameItem
        .setOnClickListener(v -> {
          if (listener != null) listener.onUsuarioClick(usuarioId);
        });
    }

    //fecha
    if (clank.getFechaPublicacion() != null) {
      holder.binding.cabeceraUsuario.tvFechaItem.setText(
        FechaUtils.formatearFechaRelativa(context, clank.getFechaPublicacion()));
    }

    holder.itemView.setOnClickListener(v -> {
      if (listener != null) listener.onClankClick(clankId);
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
        holder.binding.tarjeta.tvNumLikes.setText(String.valueOf(contador));
      }
      AnimUtils.animarLike(holder.binding.tarjeta.ivOpciones);
      return;
    }
    super.onBindViewHolder(holder, position, payloads);
  }

  /////////////////////////ondataChanged/////////////////////////

  @Override
  public void onDataChanged() {
    tarjetasPreparadas = false;
    notifyDataSetChanged();

    if (itemsListosListener != null) {
      List<String> ids = new ArrayList<>();
      List<Integer> likes = new ArrayList<>();

      for (int i = 0; i < super.getItemCount(); i++) {
        ids.add(getSnapshots().getSnapshot(i).getId());
        likes.add(getItem(i).getNumLikes());
      }

      itemsListosListener.onItemsListos(ids, likes);
    }

    prepararTraduccionesTarjetas();
  }
  public void actualizarLike(String clankId, boolean isLiked, int numLikes) {
    estadoLikesLocal.put(clankId, isLiked);
    contadorLikesLocal.put(clankId, numLikes);
  }

  private void pintarEstadoLike(@NonNull ViewHolder holder, String clankId) {
    Boolean activo = estadoLikesLocal.get(clankId);
    boolean liked  = Boolean.TRUE.equals(activo);
    holder.binding.tarjeta.ivOpciones.setImageResource(
      liked ? R.drawable.ic_like_activo : R.drawable.ic_like_inactivo);
    holder.binding.tarjeta.ivOpciones.setBackgroundResource(
      liked ? R.drawable.bg_circulo_opciones_activo
        : R.drawable.bg_circulo_opciones_inactivo);
  }

  //////////////////////////traductor/////////////////////////

  @Override
  public void onError(@NonNull FirebaseFirestoreException error) {
    super.onError(error);

    tarjetasPreparadas = true;
    notifyDataSetChanged();

    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private void prepararTraduccionesTarjetas() {
    int versionActual = ++versionPreparacion;
    if (listenerPreparacion != null) listenerPreparacion.alIniciarPreparacion();

    List<Task<?>> tareas = new ArrayList<>();

    for (int i = 0; i < super.getItemCount(); i++) {
      Clank clank = getItem(i);
      String clankId = getSnapshots().getSnapshot(i).getId();
      String clave = construirClaveTraduccion(clankId, clank);
      if (cacheTraducciones.containsKey(clave)) continue;

      String tituloOrig = clank.getTitulo() != null ? clank.getTitulo() : "";
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
    if (versionActual != versionPreparacion) {
      return;
    }

    tarjetasPreparadas = true;
    notifyDataSetChanged();

    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
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
    final ItemFeedBinding binding;
    public ViewHolder(@NonNull ItemFeedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}
