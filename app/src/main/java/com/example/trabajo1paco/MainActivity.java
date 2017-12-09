package com.example.trabajo1paco;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.squareup.picasso.Picasso;
import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity{
    private EditText edtUrlFrases;
    private EditText edtUrlImagenes;
    private Button btnDescargar;
    private ImageView imgvImagen;
    private TextView txvFrase;
    private ArrayList<String> urls;
    private ArrayList<String> frases;
    private int delay;
    private static final String URLIMAGENES = "http://alumno.mobi/~alumno/superior/cruz/enlacesImagenes.txt";
    private static final String URLFRASES = "http://alumno.mobi/~alumno/superior/cruz/frases.txt";
    private static final String URLERRORES = "http://alumno.mobi/~alumno/superior/cruz/receptorErrores.php";
    private boolean imagenesDescargadasExito;
    private int imagenActual;
    private int fraseActual;
    private Handler handler;
    private static final int MAX_TIMEOUT = 2000;
    private static final int RETRIES = 1;
    private static final int TIMEOUT_BETWEEN_RETRIES = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urls = new ArrayList<>();
        frases = new ArrayList<>();
        imagenActual = 0;
        fraseActual = 0;
        obtenerDelay();
        imagenesDescargadasExito = false;

        edtUrlFrases = (EditText) findViewById(R.id.edtUrlFrases);
        edtUrlImagenes = (EditText) findViewById(R.id.edtUrlImagenes);
        btnDescargar = (Button) findViewById(R.id.btnDescargar);
        btnDescargar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                descargarImagenes();
                descargarFrases();
                //Esperamos a que se descarguen las imagenes un segundo
                try {
                    Thread.sleep(1000);
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                    empezar();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error al obtener datos", Toast.LENGTH_SHORT).show();
                }

            }
        });
        txvFrase = (TextView) findViewById(R.id.txvFrase);
        imgvImagen = (ImageView) findViewById(R.id.imgvImagen);
    }

    private void obtenerDelay() {
        InputStream is = this.getResources().openRawResource(R.raw.intervalo);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        try {
            delay = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            postError(e.getMessage());
        }
    }

    private void empezar() {
        handler = new Handler();

        handler.postDelayed(new Runnable() {
            public void run() {
                Picasso.with(MainActivity.this).load(urls.get(imagenActual % urls.size())).into(imgvImagen);
                imagenActual++;
                txvFrase.setText(frases.get(fraseActual % frases.size()));
                fraseActual++;

                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    private void descargarImagenes() {
        final ProgressDialog progreso = new ProgressDialog(this);
        final AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.setTimeout(MAX_TIMEOUT);
        cliente.setMaxRetriesAndTimeout(RETRIES, TIMEOUT_BETWEEN_RETRIES);

        if (URLUtil.isValidUrl(edtUrlImagenes.getText().toString())) {
            cliente.get(edtUrlImagenes.getText().toString(), new FileAsyncHttpResponseHandler(this) {

                @Override
                public void onStart() {
                    progreso.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progreso.setMessage("Descargando imágenes...");
                    progreso.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            cliente.cancelAllRequests(true);
                        }
                    });
                    progreso.show();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                    progreso.dismiss();
                    String[] tmp = edtUrlImagenes.getText().toString().split("/");
                    String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + throwable.getMessage());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, File file) {
                    String linea;
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        InputStreamReader isr = new InputStreamReader(fis);
                        BufferedReader br = new BufferedReader(isr);
                        while ((linea = br.readLine()) != null) {
                            urls.add(linea);
                        }
                        br.close();
                    } catch (FileNotFoundException e) {
                        String[] tmp = edtUrlImagenes.getText().toString().split("/");
                        String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + e.getMessage());
                    } catch (IOException e) {
                        String[] tmp = edtUrlImagenes.getText().toString().split("/");
                        String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + e.getMessage());
                    }
                    imagenesDescargadasExito = true;
                    progreso.dismiss();
                }
            });

        } else {
            Toast.makeText(MainActivity.this, "La URL del fichero no es válida", Toast.LENGTH_SHORT).show();
        }
    }

    private void descargarFrases() {
        final ProgressDialog progreso = new ProgressDialog(this);
        final AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.setTimeout(MAX_TIMEOUT);
        cliente.setMaxRetriesAndTimeout(RETRIES, TIMEOUT_BETWEEN_RETRIES);
        String url = edtUrlFrases.getText().toString();

        if (URLUtil.isValidUrl(url)) {
            cliente.get(url, new FileAsyncHttpResponseHandler(this) {

                @Override
                public void onStart() {
                    progreso.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progreso.setMessage("Descargando frases ...");
                    progreso.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            cliente.cancelAllRequests(true);
                        }
                    });
                    progreso.show();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                    progreso.dismiss();
                    String[] tmp = edtUrlFrases.getText().toString().split("/");
                    String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + throwable.getMessage());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, File file) {
                    try {
                        String linea;
                        FileInputStream fis = new FileInputStream(file);
                        InputStreamReader isr = new InputStreamReader(fis, "utf-8");
                        BufferedReader br = new BufferedReader(isr);
                        while ((linea = br.readLine()) != null) {
                            frases.add(linea);
                        }
                        br.close();
                    } catch (FileNotFoundException e) {
                        String[] tmp = edtUrlFrases.getText().toString().split("/");
                        String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + e.getMessage());
                    } catch (IOException e) {
                        String[] tmp = edtUrlFrases.getText().toString().split("/");
                        String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        postError("Fichero a descargar: " + tmp[tmp.length - 1] + " Fecha y hora: " + currentDateandTime + " Código y error: " + statusCode + " " + e.getMessage());
                    }
                    progreso.dismiss();
                }
            });
        }
    }

    private void postError(String error) {
        AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.setTimeout(MAX_TIMEOUT);
        cliente.setMaxRetriesAndTimeout(RETRIES, TIMEOUT_BETWEEN_RETRIES);
        RequestParams params = new RequestParams();
        params.put("error", error);

        cliente.post(URLERRORES, params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(MainActivity.this, "No se ha podido subir el error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Toast.makeText(MainActivity.this, "El error ha sido subido", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
