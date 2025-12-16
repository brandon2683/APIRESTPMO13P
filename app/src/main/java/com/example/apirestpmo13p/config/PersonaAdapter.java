package com.example.apirestpmo13p.config;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apirestpmo13p.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonaAdapter extends RecyclerView.Adapter<PersonaAdapter.PersonaViewHolder> {

    private ArrayList<Personas> listaPersonas;
    private ArrayList<Personas> listaFiltrada;
    private OnPersonaClickListener listener;
    private int expandedPosition = -1;

    public PersonaAdapter(ArrayList<Personas> listaPersonas,
                          ArrayList<Personas> listaFiltrada,
                          OnPersonaClickListener listener) {
        this.listaPersonas = listaPersonas != null ? listaPersonas : new ArrayList<>();
        this.listaFiltrada = listaFiltrada != null ? listaFiltrada : new ArrayList<>();
        this.listener = listener;
    }
    public interface OnPersonaClickListener {
        void onEditarClick(Personas persona, int position);
        void onEliminarClick(Personas persona, int position);
        void onItemClick(Personas persona, int position);
    }

    public PersonaAdapter(ArrayList<Personas> listaPersonas, OnPersonaClickListener listener) {
        this.listaPersonas = listaPersonas != null ? listaPersonas : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public PersonaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_persona_card, parent, false);
        return new PersonaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonaViewHolder holder, int position) {
        Personas persona = listaPersonas.get(position);
        holder.bind(persona, position, listener, expandedPosition == position);

        // Manejar clic en el item
        holder.itemView.setOnClickListener(v -> {
            if (expandedPosition == holder.getAdapterPosition()) {
                expandedPosition = -1;
                notifyItemChanged(holder.getAdapterPosition());
            } else {
                int prevExpanded = expandedPosition;
                expandedPosition = holder.getAdapterPosition();
                notifyItemChanged(prevExpanded);
                notifyItemChanged(expandedPosition);
            }
        });

        // Manejar clic en el botón de menú
        holder.btnMenu.setOnClickListener(v -> {
            if (expandedPosition == holder.getAdapterPosition()) {
                expandedPosition = -1;
            } else {
                int prevExpanded = expandedPosition;
                expandedPosition = holder.getAdapterPosition();
                notifyItemChanged(prevExpanded);
                notifyItemChanged(expandedPosition);
            }
        });

        // Botón Editar
        holder.btnEditar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditarClick(persona, holder.getAdapterPosition());
                // Ocultar botones después de hacer clic
                expandedPosition = -1;
                notifyItemChanged(holder.getAdapterPosition());
            }
        });

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminarClick(persona, holder.getAdapterPosition());
                // Ocultar botones después de hacer clic
                expandedPosition = -1;
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPersonas.size();
    }

    public void setData(ArrayList<Personas> newData) {
        this.listaPersonas = newData;
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    public void removeItem(int positionInFilteredList) {
        if (positionInFilteredList >= 0 && positionInFilteredList < listaFiltrada.size()) {
            // Obtener la persona de la lista filtrada
            Personas personaAEliminar = listaFiltrada.get(positionInFilteredList);

            // Eliminar de la lista principal
            int positionInMainList = listaPersonas.indexOf(personaAEliminar);
            if (positionInMainList != -1) {
                listaPersonas.remove(positionInMainList);
            }

            // Eliminar de la lista filtrada
            listaFiltrada.remove(positionInFilteredList);

            // Notificar al adaptador
            notifyItemRemoved(positionInFilteredList);

            // Si hay búsqueda activa, notificar el rango
            if (positionInFilteredList < listaFiltrada.size()) {
                notifyItemRangeChanged(positionInFilteredList, listaFiltrada.size() - positionInFilteredList);
            }
        }
    }

    static class PersonaViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgPersona;
        TextView tvNombreCompleto, tvTelefono, tvDireccion;
        ImageButton btnMenu;
        LinearLayout layoutAcciones;
        Button btnEditar, btnEliminar;

        public PersonaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPersona = itemView.findViewById(R.id.imgPersona);
            tvNombreCompleto = itemView.findViewById(R.id.tvNombreCompleto);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            layoutAcciones = itemView.findViewById(R.id.layoutAcciones);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }

        public void bind(Personas persona, int position,
                         OnPersonaClickListener listener, boolean isExpanded) {

            // Mostrar nombre completo
            tvNombreCompleto.setText(persona.getNombres() + " " + persona.getApellidos());
            tvTelefono.setText(persona.getTelefono());
            tvDireccion.setText(persona.getDireccion());

            // Cargar imagen desde Base64
            if (persona.getFoto() != null && !persona.getFoto().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(persona.getFoto(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (bitmap != null) {
                        imgPersona.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Si hay error, mantener imagen por defecto
                }
            }

            // Mostrar/ocultar botones según estado expandido
            if (isExpanded) {
                layoutAcciones.setVisibility(View.VISIBLE);
                btnMenu.setVisibility(View.GONE);
            } else {
                layoutAcciones.setVisibility(View.GONE);
                btnMenu.setVisibility(View.VISIBLE);
            }
        }
    }
}