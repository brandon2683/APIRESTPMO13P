package com.example.apirestpmo13p.config;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.apirestpmo13p.R;

import java.util.ArrayList;
import java.util.List;

public class PersonaAdapter extends RecyclerView.Adapter<PersonaAdapter.PersonaViewHolder> {

    private ArrayList<Personas> listaPersonas;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public PersonaAdapter(ArrayList<Personas> listaPersonas, OnItemClickListener listener) {
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
        holder.bind(persona, listener, position);
    }

    @Override
    public int getItemCount() {
        return listaPersonas.size();
    }

    public void setData(ArrayList<Personas> newData) {
        this.listaPersonas = newData;
        notifyDataSetChanged();
    }

    static class PersonaViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNombres, tvApellidos, tvDireccion, tvTelefono;

        public PersonaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombres = itemView.findViewById(R.id.tvNombres);
            tvApellidos = itemView.findViewById(R.id.tvApellidos);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvTelefono = itemView.findViewById(R.id.tvTelefono);
        }

        public void bind(final Personas persona, final OnItemClickListener listener, final int position) {
            tvNombres.setText(persona.getNombres() != null ? persona.getNombres() : "");
            tvApellidos.setText(persona.getApellidos() != null ? persona.getApellidos() : "");
            tvDireccion.setText(persona.getDireccion() != null ? persona.getDireccion() : "");
            tvTelefono.setText(persona.getTelefono() != null ? persona.getTelefono() : "");

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}