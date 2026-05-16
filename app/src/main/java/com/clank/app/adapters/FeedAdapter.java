package com.clank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
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
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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

public class FeedAdapter extends FirestoreRecyclerAdapter<Clank, FeedAdapter.ViewHolder> {
  public interface OnClankClickListener {
    void onClankClick(String clankId);
    void onUsuarioClick(String usuarioId);
  }

  public interface OnPreparacionTarjetasListener {
    void alIniciarPreparacion();
    void alFinalizarPreparacion();
  }

  private final Context context;
  private final UsuarioRepository usuarioRepository;
  private final OnClankClickListener listener;
  @Nullable
  private final OnPreparacionTarjetasListener listenerPreparacion;
  private final TraductorTarjetaClank traductorTarjetaClank;
  private final Map<String, TraductorTarjetaClank.TextoTarjetaTraducido>
          cacheTraducciones = new HashMap<>();
  private int versionPreparacion = 0;

  public FeedAdapter(
          @NonNull FirestoreRecyclerOptions<Clank> options,
          Context context,
          UsuarioRepository usuarioRepository,
          OnClankClickListener listener
  ) {
    this(
            options,
            context,
            usuarioRepository,
            listener,
            null
    );
  }

  public FeedAdapter(
          @NonNull FirestoreRecyclerOptions<Clank> options,
          Context context,
          UsuarioRepository usuarioRepository,
          OnClankClickListener listener,
          @Nullable OnPreparacionTarjetasListener listenerPreparacion
  ) {
    super(options);
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.listener = listener;
    this.listenerPreparacion = listenerPreparacion;
    this.traductorTarjetaClank =
            new TraductorTarjetaClank(context);
  }

  /////////////////////////inflate/////////////////////////
  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemFeedBinding itemBinding = ItemFeedBinding.inflate(
      LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(itemBinding);
  }

  @Override
  protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Clank clank) {
    String clankId = getSnapshots().getSnapshot(position).getId();

    /////////////////////////tarjeta/////////////////////////
    TraductorTarjetaClank.TextoTarjetaTraducido textos =
            obtenerTextoTarjeta(clankId, clank);

    holder.binding.tarjeta.tvTituloClank.setText(textos.titulo);
    holder.binding.tarjeta.tvDescripcionClank.setText(textos.descripcion);
    //icono tiempo
    int tiempo = clank.getTiempo();
    int iconoTiempo = tiempo == 0 ? R.drawable.ic_cohete : tiempo == 1 ? R.drawable.ic_liebre : R.drawable.ic_tortuga;
    holder.binding.tarjeta.ivTiempo.setImageDrawable(ContextCompat.getDrawable(context, iconoTiempo));

    //portada
    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context).load(clank.getPortada()).centerCrop().into(holder.binding.tarjeta.ivPortada);
    } else {
      holder.binding.tarjeta.ivPortada.setImageDrawable(null);
    }

    /////////////////////////cabecera usuario/////////////////////////
    holder.binding.cabeceraUsuario.tvUsernameItem.setText("");
    holder.binding.cabeceraUsuario.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
    Glide.with(context).clear(holder.binding.cabeceraUsuario.civAvatarUsuario);

    String usuarioId = clank.getUsuarioId();
    if (usuarioId != null && !usuarioId.isEmpty()) {
      usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
        if (!doc.exists()) return;
        String foto = doc.contains("fotoPerfil") ? doc.getString("fotoPerfil") : "";
        String usuarioClank = doc.contains("usuarioClank") ? doc.getString("usuarioClank") : "";
        String handle = usuarioClank != null ? usuarioClank.replace("@", "").trim() : "";
        holder.binding.cabeceraUsuario.tvUsernameItem.setText(!handle.isEmpty() ? "@" + handle : "");

        if (foto != null && !foto.isEmpty()) {
          Glide.with(context)
            .load(foto)
            .circleCrop()
            .placeholder(R.drawable.ic_usuario_inactivo)
            .into(holder.binding.cabeceraUsuario.civAvatarUsuario);
        } else {
          holder.binding.cabeceraUsuario.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
        }
      });
      holder.binding.cabeceraUsuario.civAvatarUsuario.setOnClickListener(v -> {
        if (listener != null) listener.onUsuarioClick(usuarioId);
      });
      holder.binding.cabeceraUsuario.tvUsernameItem.setOnClickListener(v -> {
        if (listener != null) listener.onUsuarioClick(usuarioId);
      });
    }

    //fecha
    if (clank.getFechaPublicacion() != null) {
      holder.binding.cabeceraUsuario.tvFechaItem.setText(formatearFechaRelativa(clank.getFechaPublicacion()));
    }

    /////////////////////////click/////////////////////////
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) listener.onClankClick(clankId);
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

      String clave = construirClaveTraduccion(clankId, clank);

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
            construirClaveTraduccion(clankId, clank);

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

  /////////////////////////fecha relativa/////////////////////////
  private String formatearFechaRelativa(Date fecha) {
    long diferencia = System.currentTimeMillis() - fecha.getTime();
    long minutos = TimeUnit.MILLISECONDS.toMinutes(diferencia);
    long horas = TimeUnit.MILLISECONDS.toHours(diferencia);
    long dias = TimeUnit.MILLISECONDS.toDays(diferencia);
    long meses= dias / 30;
    long anyos = dias / 365;

    if (minutos < 1)  return context.getString(R.string.feed_ahora);
    if (minutos < 60) return context.getString(R.string.feed_hace_minutos, minutos);
    if (horas < 24) return context.getString(R.string.feed_hace_horas, horas);
    if (dias < 30) return context.getString(R.string.feed_hace_dias, dias);
    if (meses < 12) return context.getString(R.string.feed_hace_meses, meses);
    return context.getString(R.string.feed_hace_anyos, anyos);
  }

  /////////////////////////ViewHolder/////////////////////////
  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ItemFeedBinding binding;
    public ViewHolder(@NonNull ItemFeedBinding binding) {
      super(binding.getRoot());
      this.binding = binding;
    }
  }
}
