package com.clank.app.ui.comun;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clank.app.R;

import java.util.List;

public class AdaptadorOpciones extends RecyclerView.Adapter<AdaptadorOpciones.ViewHolder> {

    private List<ItemOpcion> opciones;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ItemOpcion item);
    }

    public AdaptadorOpciones(List<ItemOpcion> opciones, OnItemClickListener listener) {
        this.opciones = opciones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opcion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemOpcion item = opciones.get(position);
        holder.etiquetaOpcion.setText(item.texto);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return opciones.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView etiquetaOpcion;

        ViewHolder(View itemView) {
            super(itemView);
            etiquetaOpcion = itemView.findViewById(R.id.etiquetaOpcion);
        }
    }
}
