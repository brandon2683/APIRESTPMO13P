package com.example.apirestpmo13p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.apirestpmo13p.config.Personas;
import com.example.apirestpmo13p.config.ResetApiMethods;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CreateActivity extends AppCompatActivity {

    private File fotoFile;
    private String fotoBase64 = null;
    private static final int PERMISO_CAMARA = 101;
    ActivityResultLauncher<Intent> tomarFotoLauncher;
    ImageView imageView;
    Button btnfoto, btncreate, btnGet;
    EditText nombres, apellidos, telefono, foto, direccion;
    private RequestQueue requestQueue;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create);

        imageView =(ImageView) findViewById(R.id.imageView);
        btnfoto =(Button) findViewById(R.id.btntakefoto);
        btncreate =(Button) findViewById(R.id.btncreate);
        btnGet = (Button) findViewById(R.id.btnGet);

        nombres =(EditText) findViewById(R.id.nombres);
        apellidos =(EditText) findViewById(R.id.apellidos);
        direccion =(EditText) findViewById(R.id.direccion);
        telefono =(EditText) findViewById(R.id.telefono);

        btnfoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Permisos();
            }
        });

        btncreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendData();
            }
        });
        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateActivity.this, GetActivity.class);
                startActivity(intent);
            }
        });

        tomarFotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (fotoFile != null && fotoFile.exists()) {
                            try {
                                // Cargar el bitmap desde el archivo
                                Bitmap foto = BitmapFactory.decodeFile(fotoFile.getAbsolutePath());

                                // Leer orientación EXIF
                                ExifInterface exif = new ExifInterface(fotoFile.getAbsolutePath());
                                int orientation = exif.getAttributeInt(
                                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                int rotationInDegrees = exifToDegrees(orientation);

                                // Rotar bitmap si es necesario
                                Bitmap rotatedBitmap = foto;
                                if (rotationInDegrees != 0) {
                                    Matrix matrix = new Matrix();
                                    matrix.preRotate(rotationInDegrees);
                                    rotatedBitmap = Bitmap.createBitmap(foto, 0, 0,
                                            foto.getWidth(), foto.getHeight(), matrix, true);
                                }

                                // Mostrar en ImageView
                                imageView.setImageBitmap(rotatedBitmap);

                                // Convertir a Base64
                                fotoBase64 = bitmapToBase64(rotatedBitmap);

                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(CreateActivity.this, "Error al procesar la foto", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(CreateActivity.this, "No se pudo obtener la foto", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void SendData()
    {
        requestQueue = Volley.newRequestQueue(this);
        Personas personas = new Personas();


        personas.setNombres(nombres.getText().toString());
        personas.setApellidos(apellidos.getText().toString());
        personas.setDireccion(direccion.getText().toString());
        personas.setTelefono(telefono.getText().toString());
        personas.setFoto(fotoBase64);

        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("nombres",personas.getNombres());
            jsonObject.put("apellidos",personas.getApellidos());
            jsonObject.put("direccion",personas.getDireccion());
            jsonObject.put("telefono",personas.getTelefono());
            jsonObject.put("foto",personas.getFoto());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, ResetApiMethods.EndPointPost,
                    jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response)
                {
                    try
                    {
                        String mensaje = response.getString("message");
                        Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_LONG).show();
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Toast.makeText(getApplicationContext(), error.getMessage().toString(),
                            Toast.LENGTH_LONG).show();

                }
            });


            requestQueue.add(request);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }
    private String bitmapToBase64(@NonNull Bitmap bitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    private int exifToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return 90;
            case ExifInterface.ORIENTATION_ROTATE_180: return 180;
            case ExifInterface.ORIENTATION_ROTATE_270: return 270;
            default: return 0;
        }
    }
    private void Permisos() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.CAMERA}, PERMISO_CAMARA);
        }
        else
        {
            seleccionarImagen();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        if(requestCode == PERMISO_CAMARA)
        {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                OpenCamara();
            }
            else
            {
                Toast.makeText(this, "Permiso de Camara denegado "  , Toast.LENGTH_LONG).show();
            }
        }
    }
    private ActivityResultLauncher<Intent> seleccionarImagenLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData(); // URI de la imagen seleccionada
                    try {
                        // Cargar bitmap desde URI
                        Bitmap foto = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                        // Mostrar en ImageView
                        imageView.setImageBitmap(foto);

                        // Convertir a Base64
                        fotoBase64 = bitmapToBase64(foto);

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_LONG).show();
                    }
                }
            }
    );
    private void seleccionarImagen() {
        String[] opciones = {"Cámara", "Galería"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(opciones, (dialog, which) -> {
            if (which == 0) {
                // Tomar foto con cámara
                OpenCamara();
            } else if (which == 1) {
                // Seleccionar imagen de galería
                abrirGaleria();
            }
        });
        builder.show();
    }
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        seleccionarImagenLauncher.launch(intent);
    }
    private void OpenCamara()
    {
        try {
            // Crear archivo temporal
            fotoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "foto_" + System.currentTimeMillis() + ".jpg");
            Uri fotoUri = FileProvider.getUriForFile(this,
                    "com.example.apirestpmo13p.provider", fotoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri);
            tomarFotoLauncher.launch(intent);

        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "Error al abrir cámara: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}