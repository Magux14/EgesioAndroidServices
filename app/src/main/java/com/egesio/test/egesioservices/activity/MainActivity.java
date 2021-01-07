package com.egesio.test.egesioservices.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.egesio.test.egesioservices.R;
import com.egesio.test.egesioservices.app.App;
import com.egesio.test.egesioservices.constants.Constans;
import com.egesio.test.egesioservices.service.MedicionesService;
import com.egesio.test.egesioservices.utils.Sharedpreferences;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText textViewMacAddress;
    private EditText textViewIdPulsera;
    private EditText textViewTiempoEjecucion;
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
    }

    @Override
    public void onClick(View v) {
        if(R.id.buttonGuardar ==  v.getId()){
            try{
                String mac  = textViewMacAddress.getText().toString().trim();
                String id   = textViewIdPulsera.getText().toString().trim();
                String time = textViewTiempoEjecucion.getText().toString().trim();
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

                Sharedpreferences.getInstance(this).escribeValorString(Constans.MACADDRESS, mac);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.IDPULSERA, id);
                Sharedpreferences.getInstance(this).escribeValorString(Constans.PERIODO, time);

                /*if(App.mConnected){
                    App.mBluetoothLeService.disconnect();
                }

                App.mBluetoothLeService.connect(mac);*/

                //while(App.mBluetoothLeService != null);

                Intent intent2 = new Intent(MedicionesService.ACTION_MEDICION_BROADCAST);
                LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent2);


                Toast.makeText(this, "Valores guardados en preferencias", Toast.LENGTH_LONG).show();
                valoresGuardados();
            }catch (Exception e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    void iniciaComponetes(){
        textViewMacAddress      = findViewById(R.id.editTextMacAddress);
        textViewIdPulsera       = findViewById(R.id.editTextIdPulsera);
        textViewTiempoEjecucion = findViewById(R.id.editTextTiempo);
        buttonGuardar           = findViewById(R.id.buttonGuardar);
        textViewValores         = findViewById(R.id.textViewValores);
        //textViewMacAddress.setText("EE:BF:60:20:A9:D9");
        textViewMacAddress.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
        textViewIdPulsera.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.IDPULSERA, "117"));
        textViewTiempoEjecucion.setText(Sharedpreferences.getInstance(this).obtenValorString(Constans.PERIODO, "300"));
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