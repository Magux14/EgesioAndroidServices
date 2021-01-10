package com.egesio.test.egesioservices.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.egesio.test.egesioservices.R;
import com.egesio.test.egesioservices.app.App;
import com.egesio.test.egesioservices.constants.Constans;
import com.egesio.test.egesioservices.service.MedicionesService;
import com.egesio.test.egesioservices.service.RealTimeService;
import com.egesio.test.egesioservices.utils.SendDataFirebase;
import com.egesio.test.egesioservices.utils.Sharedpreferences;
import com.egesio.test.egesioservices.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText textViewMacAddress;
    private EditText textViewIdPulsera;
    private EditText textViewTiempoEjecucion;
    private EditText editTextUsuario;
    private EditText editTextPassword;
    private TextView textViewValores;
    private Button buttonGuardar;
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iniciaComponetes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPermission();
        valoresGuardados();

        if(Sharedpreferences.getInstance(this).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00").equals("00:00:00:00:00:00")){
            if (Utils.isMyServiceRunning(RealTimeService.class, getApplicationContext())) {
                Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                stopService(intentGeoRatioMonitoring);
            }
            Toast.makeText(this, "Servicio No iniciado por falta de Mac", Toast.LENGTH_LONG).show();
        }else{
            Timer mTimer = new Timer(true);
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(App.mBluetoothLeService != null){
                        /*if (!Utils.isMyServiceRunning(RealTimeService.class, getApplicationContext())) {
                            Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                            startService(intentGeoRatioMonitoring);
                        }*/
                        new ObtenToken().execute("{\"usuario\": \"adminDev\",\"contrasenia\": \"admin123456\"}");
                        mTimer.cancel();
                    }
                }
            }, 100, 100);

        }


    }

    @Override
    public void onClick(View v) {
        if(R.id.buttonGuardar ==  v.getId()){
            try{
                String mac  = textViewMacAddress.getText().toString().trim();
                String id   = textViewIdPulsera.getText().toString().trim();
                String time = textViewTiempoEjecucion.getText().toString().trim();
                String user = editTextUsuario.getText().toString().trim();
                String pass = editTextPassword.getText().toString().trim();
                if(mac == null || mac.equals("")){
                    textViewMacAddress.setError("Mac Address Necesaria");
                    return;
                }
                if(id == null || id.equals("")){
                    textViewIdPulsera.setError("Id Pulsera Necesaria");
                    return;
                }
                if(time == null || time.equals("")){
                    textViewTiempoEjecucion.setError("Tiempo Necesaria");
                    return;
                }
                /*if(user == null || user.equals("")){
                    editTextUsuario.setError("Usuario Necesario");
                    return;
                }
                if(pass == null || pass.equals("")){
                    editTextPassword.setError("Password Necesario");
                    return;
                }*/

                Sharedpreferences.getInstance(this).escribeValorString(Constans.MACADDRESS, mac);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.IDPULSERA, id);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.PERIODO, time);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.USER_KEY, user);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.PASSWORD_KEY, pass);


                new ObtenToken().execute("{\"usuario\": \"adminDev\",\"contrasenia\": \"admin123456\"}");

                Toast.makeText(this, "Valores guardados en preferencias", Toast.LENGTH_LONG).show();
                valoresGuardados();
            }catch (Exception e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    class ObtenToken extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... json) {


            try {
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json[0], JSON);
                Request request = new Request.Builder()
                        .header("Content-Type","")
                        .header("responseType","")
                        .header("Access-Control-Allow-Methods","")
                        .header("Access-Control-Allow-Origin","")
                        .header("Access-Control-Allow-Credentials","")
                        .header("idioma","es")
                        .url(Constans.URL_AUTH)
                        .post(body)
                        .build();

                //Response response = client.newCall(request).execute();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"ERROR TOKEN: " +  e.getMessage() +  Utils.getHora() + "\"}");
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"ERROR RESPONNSE TOKEN: " + response.message() +  Utils.getHora() + "\"}");
                            throw new IOException("Unexpected code " + response);
                        } else {
                            try {
                                String r = response.body().string();
                                JSONObject json = new JSONObject(r);
                                String token = json.getString("token");
                                Sharedpreferences.getInstance(getApplicationContext()).escribeValorString(Constans.TOKEN_KEY, token);
                                new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"TOKEN OK  - " + Utils.getHora() + "\"}");
                                if(!Sharedpreferences.getInstance(getApplicationContext()).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00").equals("00:00:00:00:00:00") && Sharedpreferences.getInstance(getApplicationContext()).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00").length() == 17){
                                    if (!Utils.isMyServiceRunning(RealTimeService.class, getApplicationContext())) {
                                        Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                                        startService(intentGeoRatioMonitoring);
                                    }
                                }else{
                                    if (Utils.isMyServiceRunning(RealTimeService.class, getApplicationContext())) {
                                        Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                                        stopService(intentGeoRatioMonitoring);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            return "";

        }

        protected void onPostExecute(String feed) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }



    }

    void iniciaComponetes(){
        textViewMacAddress      = findViewById(R.id.editTextMacAddress);
        textViewIdPulsera       = findViewById(R.id.editTextIdPulsera);
        textViewTiempoEjecucion = findViewById(R.id.editTextTiempo);
        editTextUsuario         = findViewById(R.id.editTextUsuario);
        editTextPassword        = findViewById(R.id.editTextPassword);
        buttonGuardar           = findViewById(R.id.buttonGuardar);
        textViewValores         = findViewById(R.id.textViewValores);
        textViewMacAddress.setText("EE:BF:60:20:A9:D9");
        editTextUsuario.setText("adminDev");
        editTextPassword.setText("admin123456");
        editTextUsuario.setEnabled(false);
        editTextPassword.setEnabled(false);
        textViewMacAddress.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
        textViewIdPulsera.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.IDPULSERA, "117"));
        textViewTiempoEjecucion.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.PERIODO, "300"));
        editTextUsuario.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.USER_KEY, "adminDev"));
        editTextPassword.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.PASSWORD_KEY, "admin123456"));
        buttonGuardar.setOnClickListener(this);
    }

    void valoresGuardados(){
        String mac  = Sharedpreferences.getInstance(this).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00");
        String id   = Sharedpreferences.getInstance(this).obtenValorString(Constans.IDPULSERA, "117");
        String time = Sharedpreferences.getInstance(this).obtenValorString(Constans.PERIODO, "300");
        textViewValores.setText("Valores Guardados\n\nMac: " + mac + " \nId: " + id + " \nPeriodo: " + time + " en Seg.");
    }

    private void initPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
        }

        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }else{
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        }
    }

}