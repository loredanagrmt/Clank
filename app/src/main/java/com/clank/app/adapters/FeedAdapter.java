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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

  @Nullable
  private final OnItemsListosListener itemsListosListener;

  @Nullable
  private final OnPreparacionTarjetasListener listenerPreparacion;

  private final TraductorTarjetaClank traductorTarjetaClank;

  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido> cacheTraducciones =
          new HashMap<>();

  private final Map<String, Boolean> estadoLikesLocal = new HashMap<>();
  private final Map<String, Integer> contadorLikesLocal = new HashMap<>();

  private int versionPreparacion = 0;
  private boolean tarjetasPreparadas = false;
  private boolean primeraCargaIniciada = false;
  private boolean primeraCargaCompletada = false;

  public FeedAdapter(@NonNull FirestoreRecyclerOptions<Clank> options,
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
    if (primeraCargaCompletada) {
      return super.getItemCount();
    }

    return tarjetasPreparadas ? super.getItemCount() : 0;
  }

  public int getCantidadRealFirestore() {
    return super.getItemCount();
  }

  public boolean estaPreparado() {
    return tarjetasPreparadas || primeraCargaCompletada;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemFeedBinding binding = ItemFeedBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
    );

    return new ViewHolder(binding);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder,
                                  int position,
                                  @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    String claveTraduccion = construirClaveTraduccion(clankId, clank);

    if (!primeraCargaCompletada && !cacheTraducciones.containsKey(claveTraduccion)) {
      holder.itemView.setVisibility(View.INVISIBLE);
      return;
    }

    holder.itemView.setVisibility(View.VISIBLE);

    TraductorTarjetaClank.TextoTarjetaTraducido textos =
            obtenerTextoTarjeta(clankId, clank);

    holder.binding.tarjeta.tvTituloClank.setText(textos.titulo);
    holder.binding.tarjeta.tvDescripcionClank.setText(textos.descripcion);

    int tiempo = clank.getTiempo();

    int iconoTiempo = tiempo == 0
            ? R.drawable.ic_cohete
            : tiempo == 1
            ? R.drawable.ic_liebre
            : R.drawable.ic_tortuga;

    holder.binding.tarjeta.ivTiempo.setImageDrawable(
            ContextCompat.getDrawable(context, iconoTiempo)
    );

    Glide.with(context).clear(holder.binding.tarjeta.ivPortada);

    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      holder.binding.tarjeta.ivPortada.setVisibility(View.INVISIBLE);

      Glide.with(context)
              .load(clank.getPortada())
              .centerCrop()
              .dontAnimate()
              .into(holder.binding.tarjeta.ivPortada);

      holder.binding.tarjeta.ivPortada.postDelayed(
              () -> holder.binding.tarjeta.ivPortada.setVisibility(View.VISIBLE),
              250
      );
    } else {
      holder.binding.tarjeta.ivPortada.setVisibility(View.VISIBLE);
      holder.binding.tarjeta.ivPortada.setImageResource(R.drawable.img_usuario_defecto);
    }

    Integer contadorVm = contadorLikesLocal.get(clankId);

    holder.binding.tarjeta.tvNumLikes.setText(
            String.valueOf(contadorVm != null ? contadorVm : clank.getNumLikes())
    );

    configurarBotonesLike(holder, clankId);
    pintarEstadoLike(holder, clankId);

    holder.binding.cabeceraUsuario.tvUsernameItem.setText("");
    Glide.with(context).clear(holder.binding.cabeceraUsuario.civAvatarUsuario);
    holder.binding.cabeceraUsuario.civAvatarUsuario.setVisibility(View.INVISIBLE);

    String usuarioId = clank.getUsuarioId();

    if (usuarioId != null && !usuarioId.isEmpty()) {
      holder.binding.cabeceraUsuario.civAvatarUsuario.setTag(usuarioId);

      usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
        if (!usuarioId.equals(holder.binding.cabeceraUsuario.civAvatarUsuario.getTag())) {
          return;
        }

        if (!doc.exists()) {
          return;
        }

        String foto = doc.contains("fotoPerfil")
                ? doc.getString("fotoPerfil")
                : "";

        String usuarioClank = doc.contains("usuarioClank")
                ? doc.getString("usuarioClank")
                : "";

        String handle = usuarioClank != null
                ? usuarioClank.replace("@", "").trim()
                : "";

        holder.binding.cabeceraUsuario.tvUsernameItem.setText(
                !handle.isEmpty() ? "@" + handle : ""
        );

        if (foto != null && !foto.isEmpty()) {
          Glide.with(context)
                  .load(foto)
                  .circleCrop()
                  .dontAnimate()
                  .into(holder.binding.cabeceraUsuario.civAvatarUsuario);

          holder.binding.cabeceraUsuario.civAvatarUsuario.postDelayed(
                  () -> holder.binding.cabeceraUsuario.civAvatarUsuario.setVisibility(View.VISIBLE),
                  250
          );
        } else {
          holder.binding.cabeceraUsuario.civAvatarUsuario.setVisibility(View.VISIBLE);
          holder.binding.cabeceraUsuario.civAvatarUsuario
                  .setImageResource(R.drawable.img_usuario_defecto);
        }
      });

      holder.binding.cabeceraUsuario.civAvatarUsuario.setOnClickListener(v -> {
        if (listener != null) {
          listener.onUsuarioClick(usuarioId);
        }
      });

      holder.binding.cabeceraUsuario.tvUsernameItem.setOnClickListener(v -> {
        if (listener != null) {
          listener.onUsuarioClick(usuarioId);
        }
      });
    }

    if (clank.getFechaPublicacion() != null) {
      holder.binding.cabeceraUsuario.tvFechaItem.setText(
              FechaUtils.formatearFechaRelativa(
                      context,
                      clank.getFechaPublicacion()
              )
      );

      holder.binding.cabeceraUsuario.tvFechaItem.setVisibility(View.VISIBLE);
    } else {
      holder.binding.cabeceraUsuario.tvFechaItem.setText("");
      holder.binding.cabeceraUsuario.tvFechaItem.setVisibility(View.GONE);
    }

    holder.itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onClankClick(clankId);
      }
    });
  }

  private void configurarBotonesLike(@NonNull ViewHolder holder, String clankId) {
    boolean estadoLikeListo = estadoLikesLocal.containsKey(clankId);

    holder.binding.tarjeta.ivOpciones.setVisibility(View.VISIBLE);
    holder.binding.tarjeta.ivOpciones.setEnabled(estadoLikeListo);
    holder.binding.tarjeta.ivOpciones.setClickable(estadoLikeListo);
    holder.binding.tarjeta.ivOpciones.setFocusable(estadoLikeListo);

    holder.binding.tarjeta.ivLike.setVisibility(View.VISIBLE);
    holder.binding.tarjeta.ivLike.setImageResource(R.drawable.ic_like_inactivo);
    holder.binding.tarjeta.ivLike.setEnabled(true);
    holder.binding.tarjeta.ivLike.setClickable(true);
    holder.binding.tarjeta.ivLike.setFocusable(false);
    holder.binding.tarjeta.ivLike.setSoundEffectsEnabled(false);
    holder.binding.tarjeta.ivLike.setHapticFeedbackEnabled(false);

    holder.binding.tarjeta.ivLike.setOnClickListener(v -> {
      // Solo contador visual.
    });

    holder.binding.tarjeta.ivOpciones.setOnClickListener(v -> {
      if (!estadoLikesLocal.containsKey(clankId)) {
        return;
      }

      holder.binding.tarjeta.ivOpciones.setEnabled(false);

      if (likeListener != null) {
        likeListener.onLikeClick(clankId);
      }

      holder.binding.tarjeta.ivOpciones.postDelayed(() -> {
        if (holder.getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
          boolean listoDespues = estadoLikesLocal.containsKey(clankId);

          holder.binding.tarjeta.ivOpciones.setEnabled(listoDespues);
          holder.binding.tarjeta.ivOpciones.setClickable(listoDespues);
        }
      }, 500);
    });
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder,
                               int position,
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

  @Override
  public void onDataChanged() {
    boolean debeIniciarPrimeraCarga = !primeraCargaIniciada && !primeraCargaCompletada;
    boolean debeCerrarPrimeraCarga = !primeraCargaCompletada;

    limpiarCachesDeClanksEliminados();

    if (itemsListosListener != null) {
      List<String> ids = new ArrayList<>();
      List<Integer> likes = new ArrayList<>();

      for (int i = 0; i < super.getItemCount(); i++) {
        ids.add(getSnapshots().getSnapshot(i).getId());
        likes.add(getItem(i).getNumLikes());
      }

      itemsListosListener.onItemsListos(ids, likes);
    }

    if (debeIniciarPrimeraCarga) {
      primeraCargaIniciada = true;
      tarjetasPreparadas = false;
      notifyDataSetChanged();

      if (listenerPreparacion != null) {
        listenerPreparacion.alIniciarPreparacion();
      }
    }

    prepararTraduccionesTarjetas(debeCerrarPrimeraCarga);
  }

  private void limpiarCachesDeClanksEliminados() {
    Set<String> idsActuales = new HashSet<>();

    for (int i = 0; i < super.getItemCount(); i++) {
      idsActuales.add(getSnapshots().getSnapshot(i).getId());
    }

    estadoLikesLocal.keySet().removeIf(id -> !idsActuales.contains(id));
    contadorLikesLocal.keySet().removeIf(id -> !idsActuales.contains(id));

    Iterator<String> iterador = cacheTraducciones.keySet().iterator();

    while (iterador.hasNext()) {
      String clave = iterador.next();
      String clankId = clave.split("\\|")[0];

      if (!idsActuales.contains(clankId)) {
        iterador.remove();
      }
    }
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

  private void pintarEstadoLike(@NonNull ViewHolder holder, String clankId) {
    Boolean activo = estadoLikesLocal.get(clankId);
    boolean liked = Boolean.TRUE.equals(activo);

    boolean estadoLikeListo = estadoLikesLocal.containsKey(clankId);

    holder.binding.tarjeta.ivOpciones.setImageResource(
            liked
                    ? R.drawable.ic_like_activo
                    : R.drawable.ic_like_inactivo
    );

    holder.binding.tarjeta.ivOpciones.setBackgroundResource(
            liked
                    ? R.drawable.bg_circulo_opciones_activo
                    : R.drawable.bg_circulo_opciones_inactivo
    );

    holder.binding.tarjeta.ivOpciones.setEnabled(estadoLikeListo);
    holder.binding.tarjeta.ivOpciones.setClickable(estadoLikeListo);
    holder.binding.tarjeta.ivOpciones.setFocusable(estadoLikeListo);

    holder.binding.tarjeta.ivLike.setImageResource(R.drawable.ic_like_inactivo);
  }

  @Override
  public void onError(@NonNull FirebaseFirestoreException error) {
    super.onError(error);

    tarjetasPreparadas = true;
    primeraCargaIniciada = true;
    primeraCargaCompletada = true;

    notifyDataSetChanged();

    if (listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private void prepararTraduccionesTarjetas(boolean debeCerrarPrimeraCarga) {
    int versionActual = ++versionPreparacion;

    if (super.getItemCount() == 0) {
      finalizarPreparacion(versionActual, debeCerrarPrimeraCarga);
      return;
    }

    List<Task<?>> tareas = new ArrayList<>();

    for (int i = 0; i < super.getItemCount(); i++) {
      Clank clank = getItem(i);
      String clankId = getSnapshots().getSnapshot(i).getId();
      String clave = construirClaveTraduccion(clankId, clank);

      if (cacheTraducciones.containsKey(clave)) {
        continue;
      }

      String tituloOriginal = clank.getTitulo() != null
              ? clank.getTitulo()
              : "";

      String descripcionOriginal = clank.getDescripcion() != null
              ? clank.getDescripcion()
              : "";

      TraductorTarjetaClank.TextoTarjetaTraducido textoOriginal =
              new TraductorTarjetaClank.TextoTarjetaTraducido(
                      tituloOriginal,
                      descripcionOriginal
              );

      Task<?> tarea = traductorTarjetaClank
              .traducirSiProcede(tituloOriginal, descripcionOriginal)
              .addOnSuccessListener(resultado -> {
                if (versionActual != versionPreparacion) {
                  return;
                }

                cacheTraducciones.put(
                        clave,
                        resultado != null ? resultado : textoOriginal
                );
              })
              .addOnFailureListener(error -> {
                if (versionActual != versionPreparacion) {
                  return;
                }

                cacheTraducciones.put(clave, textoOriginal);
              });

      tareas.add(tarea);
    }

    if (tareas.isEmpty()) {
      finalizarPreparacion(versionActual, debeCerrarPrimeraCarga);
      return;
    }

    Tasks.whenAllComplete(tareas)
            .addOnCompleteListener(tarea ->
                    finalizarPreparacion(versionActual, debeCerrarPrimeraCarga)
            );
  }

  private void finalizarPreparacion(int versionActual, boolean debeCerrarPrimeraCarga) {
    if (versionActual != versionPreparacion) {
      return;
    }

    tarjetasPreparadas = true;

    if (debeCerrarPrimeraCarga) {
      primeraCargaCompletada = true;
    }

    notifyDataSetChanged();

    if (debeCerrarPrimeraCarga && listenerPreparacion != null) {
      listenerPreparacion.alFinalizarPreparacion();
    }
  }

  private TraductorTarjetaClank.TextoTarjetaTraducido obtenerTextoTarjeta(
          String clankId,
          Clank clank
  ) {
    String clave = construirClaveTraduccion(clankId, clank);
    TraductorTarjetaClank.TextoTarjetaTraducido resultado = cacheTraducciones.get(clave);

    if (resultado != null) {
      return resultado;
    }

    return new TraductorTarjetaClank.TextoTarjetaTraducido(
            clank.getTitulo() != null ? clank.getTitulo() : "",
            clank.getDescripcion() != null ? clank.getDescripcion() : ""
    );
  }

  private String construirClaveTraduccion(String clankId, Clank clank) {
    String idioma = GestorIdioma.getInstance(context).getIdiomaActual();

    return clankId
            + "|"
            + idioma
            + "|"
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