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
import com.clank.app.data.model.Usuario;
import com.clank.app.databinding.ItemFeedBinding;
import com.clank.app.data.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BusquedaAdapter extends RecyclerView.Adapter<BusquedaAdapter.ViewHolder> {

  public interface OnClankClickListener {
    void onClankClick(String clankId);
  }

  private final Context context;
  private final UsuarioRepository usuarioRepository;
  private final OnClankClickListener listener;
  private final Function<String, Usuario> getCacheUsuario;
  private List<Clank> clanks = new ArrayList<>();

  public BusquedaAdapter(Context context,
                         UsuarioRepository usuarioRepository,
                         Function<String, Usuario> getCacheUsuario,
                         OnClankClickListener listener) {
    this.context = context;
    this.usuarioRepository = usuarioRepository;
    this.getCacheUsuario = getCacheUsuario;
    this.listener = listener;
  }

  public void actualizar(List<Clank> nuevos) {
    this.clanks = nuevos != null ? nuevos : new ArrayList<>();
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ItemFeedBinding b = ItemFeedBinding.inflate(
      LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(b);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Clank clank = clanks.get(position);

    /////////////////////////tarjeta/////////////////////////
    holder.binding.tarjeta.tvTituloClank.setText(
      clank.getTitulo() != null ? clank.getTitulo() : "");
    holder.binding.tarjeta.tvDescripcionClank.setText(
      clank.getDescripcion() != null ? clank.getDescripcion() : "");

    int iconoTiempo = clank.getTiempo() == 0 ? R.drawable.ic_cohete
      : clank.getTiempo() == 1 ? R.drawable.ic_liebre
      : R.drawable.ic_tortuga;
    holder.binding.tarjeta.ivTiempo.setImageDrawable(
      ContextCompat.getDrawable(context, iconoTiempo));

    if (clank.getPortada() != null && !clank.getPortada().isEmpty()) {
      Glide.with(context).load(clank.getPortada())
        .centerCrop().into(holder.binding.tarjeta.ivPortada);
    } else {
      holder.binding.tarjeta.ivPortada.setImageDrawable(null);
    }

    /////////////////////////cabecera usuario/////////////////////////
    holder.binding.tvUsernameItem.setText("");
    holder.binding.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
    Glide.with(context).clear(holder.binding.civAvatarUsuario);

    // Primero intentar caché del ViewModel (ya cargado en la búsqueda)
    String usuarioId = clank.getUsuarioId();
    if (usuarioId != null && !usuarioId.isEmpty()) {
      Usuario cached = getCacheUsuario.apply(usuarioId);
      if (cached != null) {
        bindUsuario(holder, cached);
      } else {
        // fallback: petición individual si no está en caché
        usuarioRepository.getUsuario(usuarioId).addOnSuccessListener(doc -> {
          if (!doc.exists()) return;
          String foto = doc.getString("fotoPerfil");
          String handle = doc.getString("usuarioClank");
          handle = handle != null ? handle.replace("@", "").trim() : "";
          holder.binding.tvUsernameItem.setText(!handle.isEmpty() ? "@" + handle : "");
          if (foto != null && !foto.isEmpty()) {
            Glide.with(context).load(foto).circleCrop()
              .placeholder(R.drawable.ic_usuario_inactivo)
              .into(holder.binding.civAvatarUsuario);
          }
        });
      }
    }

    /////////////////////////fecha/////////////////////////
    if (clank.getFechaPublicacion() != null) {
      holder.binding.tvFechaItem.setText(
        formatearFechaRelativa(clank.getFechaPublicacion()));
    } else {
      holder.binding.tvFechaItem.setText("");
    }

    /////////////////////////click/////////////////////////
    holder.itemView.setOnClickListener(v -> {
      if (listener != null && clank.getClankId() != null) {
        listener.onClankClick(clank.getClankId());
      }
    });
  }

  private void bindUsuario(@NonNull ViewHolder holder, Usuario usuario) {
    String handle = usuario.getUsuarioClank() != null
      ? usuario.getUsuarioClank().replace("@", "").trim() : "";
    holder.binding.tvUsernameItem.setText(!handle.isEmpty() ? "@" + handle : "");
    String foto = usuario.getFotoPerfil();
    if (foto != null && !foto.isEmpty()) {
      Glide.with(context).load(foto).circleCrop()
        .placeholder(R.drawable.ic_usuario_inactivo)
        .into(holder.binding.civAvatarUsuario);
    } else {
      holder.binding.civAvatarUsuario.setImageResource(R.drawable.ic_usuario_inactivo);
    }
  }

  private String formatearFechaRelativa(Date fecha) {
    long diff     = System.currentTimeMillis() - fecha.getTime();
    long minutos  = TimeUnit.MILLISECONDS.toMinutes(diff);
    long horas    = TimeUnit.MILLISECONDS.toHours(diff);
    long dias     = TimeUnit.MILLISECONDS.toDays(diff);
    long meses    = dias / 30;
    long anyos    = dias / 365;

    if (minutos < 1)  return context.getString(R.string.feed_ahora);
    if (minutos < 60) return context.getString(R.string.feed_hace_minutos, minutos);
    if (horas   < 24) return context.getString(R.string.feed_hace_horas,   horas);
    if (dias    < 30) return context.getString(R.string.feed_hace_dias,    dias);
    if (meses   < 12) return context.getString(R.string.feed_hace_meses,   meses);
    return context.getString(R.string.feed_hace_anyos, anyos);
  }

  @Override
  public int getItemCount() { return clanks.size(); }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    final ItemFeedBinding binding;
    public ViewHolder(@NonNull ItemFeedBinding b) {
      super(b.getRoot());
      this.binding = b;
    }
  }
}
