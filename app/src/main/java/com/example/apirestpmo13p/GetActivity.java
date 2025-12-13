package com.example.apirestpmo13p;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.apirestpmo13p.config.PersonaAdapter;
import com.example.apirestpmo13p.config.Personas;
import com.example.apirestpmo13p.config.ResetApiMethods;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetActivity extends AppCompatActivity {
    RecyclerView regispersonas;
    EditText buscar;
    Button btnvolver, btneliminar, btnactualizar;
    PersonaAdapter adapter;
    ArrayList<Personas> listaPersonas;
    ArrayList<Personas> listaFiltrada;
    private RequestQueue requestQueue;
    private int selectedPosition = -1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get);

        listaPersonas = new ArrayList<>();
        listaFiltrada = new ArrayList<>();

        buscar = (EditText) findViewById(R.id.buscar);
        btnvolver = (Button) findViewById(R.id.btnvolver);
        btnactualizar = (Button) findViewById(R.id.btnactualizar);
        btneliminar = (Button) findViewById(R.id.btneliminar);
        regispersonas = (RecyclerView) findViewById(R.id.registro);

        requestQueue = Volley.newRequestQueue(this);
        setupRecyclerView();
        cargarPersonas();

        btnvolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnactualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPosition != -1) {
                    Personas persona = listaFiltrada.get(selectedPosition);
                    actualizarPersona(persona);
                } else {
                    Toast.makeText(GetActivity.this,
                            "Seleccione una persona para actualizar",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        btneliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPosition != -1) {
                    confirmarEliminacion();
                } else {
                    Toast.makeText(GetActivity.this,
                            "Seleccione una persona para eliminar",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        buscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPersonas(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    private void setupRecyclerView() {
        adapter = new PersonaAdapter(listaFiltrada, new PersonaAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                selectedPosition = position;
                Toast.makeText(GetActivity.this,
                        "Seleccionado: " + listaFiltrada.get(position).getNombres(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        regispersonas.setLayoutManager(new LinearLayoutManager(this));
        regispersonas.setAdapter(adapter);
    }
    private void cargarPersonas() {
        String url = ResetApiMethods.EndPointGet;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            listaPersonas.clear();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject jsonObject = response.getJSONObject(i);

                                Personas persona = new Personas();

                                if (jsonObject.has("id")) {
                                    Object idObj = jsonObject.get("id");
                                    if (idObj instanceof Integer) {
                                        persona.setId(String.valueOf((Integer) idObj));
                                    } else if (idObj instanceof String) {
                                        persona.setId((String) idObj);
                                    } else {
                                        persona.setId(String.valueOf(jsonObject.getInt("id")));
                                    }
                                }

                                persona.setId(jsonObject.getString("id"));
                                persona.setNombres(jsonObject.getString("nombres"));
                                persona.setApellidos(jsonObject.getString("apellidos"));
                                persona.setDireccion(jsonObject.getString("direccion"));
                                persona.setTelefono(jsonObject.getString("telefono"));

                                if (jsonObject.has("foto")) {
                                    persona.setFoto(jsonObject.getString("foto"));
                                }

                                listaPersonas.add(persona);
                            }

                            listaFiltrada.clear();
                            listaFiltrada.addAll(listaPersonas);
                            adapter.notifyDataSetChanged();

                            Toast.makeText(GetActivity.this,
                                    "Datos cargados: " + listaPersonas.size() + " registros",
                                    Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(GetActivity.this,
                                    "Error parseando datos: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        String errorMsg = "Error cargando datos";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg = new String(error.networkResponse.data);
                        }
                        Toast.makeText(GetActivity.this,
                                errorMsg,
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
        jsonArrayRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonArrayRequest);
    }
    private void eliminarPersona(String idPersona) {
        String url = ResetApiMethods.EndPointDelete;

        System.out.println("Eliminando persona con ID: " + idPersona);
        System.out.println("URL DELETE: " + url);

        try {JSONObject jsonBody = new JSONObject();
            jsonBody.put("id", idPersona);

            System.out.println("JSON Body: " + jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,  // Cambiar a POST si tu PHP espera POST
                    url,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String mensaje = response.getString("message");
                                Toast.makeText(GetActivity.this,
                                        "✓ " + mensaje,
                                        Toast.LENGTH_SHORT).show();

                                System.out.println("Respuesta DELETE: " + response.toString());

                                cargarPersonas();
                                selectedPosition = -1;

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(GetActivity.this,
                                        "Error parseando respuesta: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();

                            // Obtener mensaje de error detallado
                            String errorMsg = "Error en el servidor";
                            if (error.networkResponse != null) {
                                int statusCode = error.networkResponse.statusCode;
                                String data = new String(error.networkResponse.data);
                                errorMsg = "Código: " + statusCode + " - " + data;
                                System.out.println("Error DELETE: " + errorMsg);
                            }

                            Toast.makeText(GetActivity.this,
                                    errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    return headers;
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000, // 10 segundos
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(GetActivity.this,
                    "Error creando JSON: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void confirmarEliminacion() {
        if (selectedPosition < 0 || selectedPosition >= listaFiltrada.size()) {
            Toast.makeText(this, "Selección inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        Personas persona = listaFiltrada.get(selectedPosition);

        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro de eliminar a " + persona.getNombres() + " " + persona.getApellidos() + "?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eliminarPersona(persona.getId());
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void actualizarPersona(@NonNull Personas persona) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Persona");

        View view = getLayoutInflater().inflate(R.layout.dialog_actualizar, null);

        EditText etNombres = view.findViewById(R.id.etNombres);
        EditText etApellidos = view.findViewById(R.id.etApellidos);
        EditText etDireccion = view.findViewById(R.id.etDireccion);
        EditText etTelefono = view.findViewById(R.id.etTelefono);

        etNombres.setText(persona.getNombres());
        etApellidos.setText(persona.getApellidos());
        etDireccion.setText(persona.getDireccion());
        etTelefono.setText(persona.getTelefono());

        builder.setView(view);

        builder.setPositiveButton("Actualizar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nuevosNombres = etNombres.getText().toString().trim();
                String nuevosApellidos = etApellidos.getText().toString().trim();
                String nuevaDireccion = etDireccion.getText().toString().trim();
                String nuevoTelefono = etTelefono.getText().toString().trim();

                if (nuevosNombres.isEmpty() || nuevosApellidos.isEmpty()) {
                    Toast.makeText(GetActivity.this,
                            "Nombre y apellido son obligatorios",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int id = Integer.parseInt(persona.getId());
                    ejecutarActualizacion(id, nuevosNombres, nuevosApellidos,
                            nuevaDireccion, nuevoTelefono);
                } catch (NumberFormatException e) {
                    Toast.makeText(GetActivity.this,
                            "Error: ID inválido",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void ejecutarActualizacion(int id, String nombres, String apellidos,
                                       String direccion, String telefono) {
        String url = ResetApiMethods.EndPointUpdate;
        System.out.println("URL UPDATE: " + url);

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", id);
            jsonObject.put("nombres", nombres);
            jsonObject.put("apellidos", apellidos);
            jsonObject.put("direccion", direccion);
            jsonObject.put("telefono", telefono);

            System.out.println("JSON a enviar: " + jsonObject.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String mensaje = response.getString("message");
                                Toast.makeText(GetActivity.this, mensaje, Toast.LENGTH_SHORT).show();

                                // Recargar lista
                                cargarPersonas();
                                selectedPosition = -1;

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            Toast.makeText(GetActivity.this,
                                    "Error actualizando: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            requestQueue.add(request);

        }
        catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(GetActivity.this,
                    "Error creando JSON: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void filtrarPersonas(@NonNull String texto) {
        if (listaPersonas == null || listaFiltrada == null) {
            return;
        }
        listaFiltrada.clear();

        if (texto.isEmpty()) {
            listaFiltrada.addAll(listaPersonas);
        } else {
            String textoLower = texto.toLowerCase();
            for (Personas persona : listaPersonas) {
                if ((persona.getNombres() != null && persona.getNombres().toLowerCase().contains(textoLower)) ||
                        (persona.getApellidos() != null && persona.getApellidos().toLowerCase().contains(textoLower)) ||
                        (persona.getTelefono() != null && persona.getTelefono().toLowerCase().contains(textoLower))) {
                    listaFiltrada.add(persona);
                }
            }
        }

        adapter.notifyDataSetChanged();
        selectedPosition = -1;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestQueue != null) {
            cargarPersonas();
        }
    }
}