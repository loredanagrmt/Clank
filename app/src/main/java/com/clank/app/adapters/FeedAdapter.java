package com.clank.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
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

public class FeedAdapter extends FirestoreRecyclerAdapter<Clank, FeedAdapter.ViewHolder> {
  public interface OnClankClickListener {
    void onClankClick(String clankId);
  }

  private final Context context;
  private final UsuarioRepository usuarioRepository;
  private final OnClankClickListener listener;

  public FeedAdapter(@NonNull FirestoreRecyclerOptions<Clank> options, Context context, UsuarioRepository usuarioRepository, OnClankClickListener listener) {
    super(options);
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.listener = listener;
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
    holder.binding.tarjeta.tvTituloClank.setText(clank.getTitulo() != null ? clank.getTitulo() : "");
    holder.binding.tarjeta.tvDescripcionClank.setText(clank.getDescripcion() != null ? clank.getDescripcion() : "");

    //icono tiempo
    int tiempo = clank.getTiempo();
    int iconoTiempo = tiempo == 0 ? R.drawable.ic_cohete
      : tiempo == 1 ? R.drawable.ic_liebre
      : R.drawable.ic_tortuga;
    holder.binding.tarjeta.ivTiempo.setImageDrawable(
      ContextCompat.getDrawable(context, iconoTiempo));

    //portada
    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context)
        .load(clank.getPortada())
        .centerCrop()
        .into(holder.binding.tarjeta.ivPortada);
    } else {
      holder.binding.tarjeta.ivPortada.setImageDrawable(null);
    }

    /////////////////////////cabecera usuario/////////////////////////
    holder.binding.tvUsernameItem.setText("");
    holder.binding.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
    Glide.with(context).clear(holder.binding.civAvatarUsuario);

    String usuarioId = clank.getUsuarioId();
    if (usuarioId != null && !usuarioId.isEmpty()) {
      usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
        if (!doc.exists()) return;
        String foto = doc.contains("fotoPerfil") ? doc.getString("fotoPerfil") : "";
        String usuarioClank = doc.contains("usuarioClank") ? doc.getString("usuarioClank") : "";
        String handle = usuarioClank != null ? usuarioClank.replace("@", "").trim() : "";
        holder.binding.tvUsernameItem.setText(!handle.isEmpty() ? "@" + handle : "");

        if (foto != null && !foto.isEmpty()) {
          Glide.with(context)
            .load(foto)
            .circleCrop()
            .placeholder(R.drawable.ic_usuario_inactivo)
            .into(holder.binding.civAvatarUsuario);
        } else {
          holder.binding.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
        }
      });
    }

    //fecha
    if (clank.getFechaPublicacion() != null) {
      holder.binding.tvFechaItem.setText(formatearFechaRelativa(clank.getFechaPublicacion()));
    }

    /////////////////////////click/////////////////////////
    holder.itemView.setOnClickListener(v -> {
      if (listener != null) listener.onClankClick(clankId);
    });
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
