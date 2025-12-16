package com.example.apirestpmo13p;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CreateActivity extends AppCompatActivity {

    private File fotoFile;
    private String fotoBase64 = null;
    private String currentPhotoPath = "";
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
                                Log.d("FOTO", "Archivo existe: " + fotoFile.getAbsolutePath());
                                currentPhotoPath = fotoFile.getAbsolutePath();

                                // Cargar y mostrar la imagen
                                mostrarImagenEnImageView(currentPhotoPath);

                                // Convertir a Base64 optimizado
                                fotoBase64 = getFotoParaEnviar();

                                Toast.makeText(CreateActivity.this,
                                        "Foto lista para enviar",
                                        Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(CreateActivity.this,
                                        "Error al procesar la foto: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.e("FOTO", "fotoFile es null o no existe");
                            Toast.makeText(CreateActivity.this,
                                    "No se pudo obtener la foto",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(CreateActivity.this, "Foto cancelada", Toast.LENGTH_SHORT).show();
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
                        limpiarCampos();
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
    private String getFotoParaEnviar() {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            return "";
        }

        // 1. Primero intentar WebP (más eficiente)
        String webpBase64 = convertirAWebpBase64(currentPhotoPath);

        if (webpBase64.isEmpty()) {
            return "";
        }

        // 2. Si es muy grande aún (> 1.5MB texto), reducir más
        if (webpBase64.length() > 1500000) {
            Log.w("OPTIMIZACION", "WebP aún muy grande, usando miniatura");
            return obtenerMiniaturaWebp(currentPhotoPath, 600);
        }

        return webpBase64;
    }
    private void mostrarImagenEnImageView(String path) {
        try {
            // Cargar el bitmap desde el archivo
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Reducir para mostrar

            Bitmap foto = BitmapFactory.decodeFile(path, options);

            if (foto == null) {
                Toast.makeText(this, "Error cargando imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            // Leer orientación EXIF
            ExifInterface exif = new ExifInterface(path);
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
                foto.recycle(); // Liberar memoria del bitmap original
            }

            // Mostrar en ImageView
            imageView.setImageBitmap(rotatedBitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error procesando imagen: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
    private String convertirAWebpBase64(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return "";

            // Decodificar con tamaño reducido
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = calcularInSampleSize(path, 1024, 1024);

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) return "";

            // Rotar según EXIF si es necesario
            bitmap = rotarImagenSegunExif(bitmap, path);

            // Comprimir a WebP (¡MUCHO MÁS PEQUEÑO!)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ soporta calidad ajustable en WebP
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 75, outputStream);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                // Android 4.0+ soporta WebP
                bitmap.compress(Bitmap.CompressFormat.WEBP, 75, outputStream);
            } else {
                // Fallback a JPEG
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            }

            byte[] byteArray = outputStream.toByteArray();
            String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);

            Log.d("WEBP", "Tamaño WebP: " + (byteArray.length / 1024) + " KB");
            Log.d("WEBP", "Base64 WebP: " + (base64.length() / 1024) + " KB texto");

            bitmap.recycle();
            outputStream.close();

            return base64;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private Bitmap rotarImagenSegunExif(Bitmap bitmap, String path) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationInDegrees = exifToDegrees(orientation);

            if (rotationInDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.preRotate(rotationInDegrees);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle(); // Liberar el bitmap original
                return rotatedBitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
    private int calcularInSampleSize(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        Log.d("IN_SAMPLE", "inSampleSize calculado: " + inSampleSize);
        return inSampleSize;
    }
    private String obtenerMiniaturaWebp(String path, int maxSize) {
        try {
            // Obtener dimensiones
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Calcular escala
            int scale = Math.max(options.outWidth / maxSize, options.outHeight / maxSize);
            scale = Math.max(1, scale);

            // Decodificar reducido
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;

            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap == null) return "";

            // Redimensionar exacto manteniendo proporción
            float widthRatio = (float) maxSize / bitmap.getWidth();
            float heightRatio = (float) maxSize / bitmap.getHeight();
            float ratio = Math.min(widthRatio, heightRatio);

            int width = Math.round(bitmap.getWidth() * ratio);
            int height = Math.round(bitmap.getHeight() * ratio);

            Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);

            // Comprimir a WebP
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.WEBP, 75, outputStream);

            String base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);

            Log.d("MINIATURA", "Miniatura WebP: " + width + "x" + height);
            Log.d("MINIATURA", "Base64 size: " + (base64.length() / 1024) + " KB");

            bitmap.recycle();
            resized.recycle();
            outputStream.close();

            return base64;

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private String getRealPathFromURI(Uri contentUri) {
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                cursor.close();
                return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private String guardarImagenDesdeUri(Uri uri) {
        try {
            // Cargar bitmap desde URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

            // Guardar en archivo temporal
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "GALERIA_" + timeStamp + ".jpg";
            File imageFile = new File(storageDir, imageFileName);

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            // Guardar también en fotoFile para consistencia
            fotoFile = imageFile;

            bitmap.recycle();

            return imageFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
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
                    Uri imageUri = result.getData().getData();
                    try {
                        // Obtener ruta real del archivo
                        currentPhotoPath = getRealPathFromURI(imageUri);
                        if (currentPhotoPath == null) {
                            // Si no se puede obtener ruta, guardar temporalmente
                            currentPhotoPath = guardarImagenDesdeUri(imageUri);
                        }

                        // Cargar y mostrar la imagen
                        mostrarImagenEnImageView(currentPhotoPath);

                        // Convertir a Base64 optimizado
                        fotoBase64 = getFotoParaEnviar();

                        Toast.makeText(CreateActivity.this,
                                "Imagen lista para enviar",
                                Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(CreateActivity.this,
                                "Error al cargar la imagen",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
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
    private void limpiarCampos() {
        nombres.setText("");
        apellidos.setText("");
        direccion.setText("");
        telefono.setText("");
        imageView.setImageResource(R.drawable.ic_launcher_foreground);
        fotoBase64 = "";
        currentPhotoPath = "";
        fotoFile = null;
    }
    private int exifToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return 90;
            case ExifInterface.ORIENTATION_ROTATE_180: return 180;
            case ExifInterface.ORIENTATION_ROTATE_270: return 270;
            default: return 0;
        }
    }

}