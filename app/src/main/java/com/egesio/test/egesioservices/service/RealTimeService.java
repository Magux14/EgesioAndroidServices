package com.egesio.test.egesioservices.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.SyncStateContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.egesio.test.egesioservices.R;
import com.egesio.test.egesioservices.app.App;
import com.egesio.test.egesioservices.command.CommandManager;
import com.egesio.test.egesioservices.constants.Constans;
import com.egesio.test.egesioservices.model.Medida;
import com.egesio.test.egesioservices.utils.CallHelper;
import com.egesio.test.egesioservices.utils.DataHandlerUtils;
import com.egesio.test.egesioservices.utils.SendDataFirebase;
import com.egesio.test.egesioservices.utils.Sharedpreferences;
import com.egesio.test.egesioservices.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.lang.Double.isNaN;
import static java.lang.Float.parseFloat;

public class RealTimeService extends Service {


    private CallHelper callHelper;

    private static final String TAG = MedicionesService.class.getSimpleName();

    private CommandManager manager;
    private SharedPreferences sharedpreferences;

    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;
    private NotificationChannel notificationChannel;
    private String NOTIFICATION_CHANNEL_ID = "17";

    @Override
    public void onCreate()
    {
        super.onCreate();
        try{
            creaNotificacion();
            manager = CommandManager.getInstance(this);
        }catch (Exception e){
            try{
                if(App.mConnected)
                    App.mBluetoothLeService.disconnect();
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
                Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                getApplicationContext().startService(intentGeoRatioMonitoring);
            }catch (Exception e2){
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
            callHelper = new CallHelper(this);
            callHelper.start();
            creaAlarmaRecurrenteBluetooth();
            inicializaValoresPref(this);
            LocalBroadcastManager.getInstance(this).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            Sharedpreferences.getInstance(this).escribeValorString(Constans.LAST_TIME_GENERAL, String.valueOf(System.currentTimeMillis()));
            sendMessageToUI(this);
        }catch (Exception e){
            //Log.d("EGESIO", e.getMessage());
            try{
                if(App.mConnected)
                    App.mBluetoothLeService.disconnect();
                LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
                Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
                getApplicationContext().startService(intentGeoRatioMonitoring);
            }catch (Exception e2){
            }
        }
        return super.onStartCommand(intent, flags, startId); //START_NOT_STICKY;
    }

    public void inicializaValoresPref(Context context){

        Sharedpreferences.getInstance(context).escribeValorString(Constans.MACADDRESS, Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.IDPULSERA, Sharedpreferences.getInstance(context).obtenValorString(Constans.IDPULSERA, "117"));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.PERIODO, Sharedpreferences.getInstance(context).obtenValorString(Constans.PERIODO, "300"));


        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_KEY, "255");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_KEY, "255");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_KEY, "255");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_KEY, "255/255");

        Sharedpreferences.getInstance(context).escribeValorString(Constans.COUNT_GENERAL, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.LAST_TIME_GENERAL, "0");

    }

    @Override
    public void onDestroy()
    {
        callHelper.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        try{
            if(App.mConnected)
                App.mBluetoothLeService.disconnect();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
            Intent intentGeoRatioMonitoring = new Intent(getApplicationContext(), RealTimeService.class);
            getApplicationContext().startService(intentGeoRatioMonitoring);
        }catch (Exception e){
        }
    }

    private void sendMessageToUI(Context context) {
        try{
            Timer mTimer = new Timer(true);
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(App.mBluetoothLeService != null){
                        Intent intent = new Intent(MedicionesService.ACTION_MEDICION_BROADCAST);
                        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
                        mTimer.cancel();
                    }
                }
            }, 100, 100);
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }


    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MedicionesService.ACTION_MEDICION_BROADCAST.equals(action)) {
                //if(!App.mConnected)
                    App.mBluetoothLeService.connect(Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
                //else
                //    App.mBluetoothLeService.disconnect();
                new SendDataFirebase(context).execute("{\"action\": \"ACTION_MEDICION_BROADCAST - " + Utils.getHora() + "\"}");
            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                new SendDataFirebase(context).execute("{\"action\": \"ACTION_GATT_CONNECTED - " +  Utils.getHora() + "\"}");
                manager.heartRateSensor(1);
                manager.temperatureSensor(1);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                inicializaValoresPref(context);
                new SendDataFirebase(context).execute("{\"action\": \"DISCONECT - " + Utils.getHora() + "\"}");
                Sharedpreferences.getInstance(context).escribeValorString(Constans.LAST_TIME_GENERAL, String.valueOf(System.currentTimeMillis()));
                LocalBroadcastManager.getInstance(context).unregisterReceiver(mGattUpdateReceiver);
                       if(!App.mConnected){
                           LocalBroadcastManager.getInstance(context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                           App.mBluetoothLeService.connect(Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
                       }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                List<Integer> data = DataHandlerUtils.bytesToArrayList(txValue);
                //new SendDataFirebase(context).execute("{\"action\": \"DATA_AVAILABLE - " + data + " - " + Utils.getHora() + "\"}");
                //manager.temperatureSensor(1);
                if (data.get(4) == 0X91) {
                    manager.findBracelet();
                    manager.heartRateSensor(1);
                }

                if (data.get(0) == 0xAB && data.get(4) == 0x31) {
                    switch (data.get(5)) {
                        case 0X0A:
                            //Heart Rate（Real-time）
                            int heartRate = data.get(6);
                            //new SendDataFirebase(context).execute("{\"action\": \"HEART - " + data + " - " + Utils.getHora() + "\"}");
                            //manager.getOneClickMeasurementCommand(1);
                            manager.temperatureSensor(1);
                            manager.bloodOxygenSensor(1);
                            guardaDato(context, Constans.HEART_KEY, String.valueOf(heartRate));
                            validaLecturas(context);
                            break;
                        case 0x12:
                            //Blood Oxygen（Real-time）
                            int bloodOxygen = data.get(6);
                            //new SendDataFirebase(context).execute("{\"action\": \"OXYGEN - " + data + " - " + Utils.getHora() + "\"}");
                            manager.temperatureSensor(1);
                            manager.bloodPressureSensor(1);
                            guardaDato(context, Constans.BLOOD_OXYGEN_KEY, String.valueOf(bloodOxygen));
                            validaLecturas(context);
                            break;
                        case 0x22:
                            //Blood Pressure（Real-time）
                            int bloodPressureHypertension = data.get(6);
                            int bloodPressureHypotension = data.get(7);
                            //new SendDataFirebase(context).execute("{\"action\": \"PRESSURE - " + data + " - " + Utils.getHora() + "\"}");
                            manager.temperatureSensor(1);
                            manager.heartRateSensor(1);
                            guardaDato(context, Constans.BLOOD_PRESSURE_KEY, bloodPressureHypertension + "/" + bloodPressureHypotension);
                            validaLecturas(context);
                            break;
                    }
                }

                if (data.get(0) == 0xAB && data.get(4) == 0x86) {
                    //new SendDataFirebase(context).execute("{\"action\": \"TEMP - " + Utils.getHora() + "\"}");
                    int entero = data.get(6);
                    int decimal = data.get(7);
                    guardaDato(context, Constans.TEMPERATURE_KEY, entero + "." + decimal);
                    validaLecturas(context);

                }

                if (data != null && data.size() == 13 && data.get(4) == 0x32) {
                    //new SendDataFirebase(context).execute("{\"action\": \"REGRESO TODAAAAAS - " + Utils.getHora() + "\"}");

                    Integer heartRate = data.get(6);
                    Integer bloodOxygen = data.get(7);
                    Integer bloodPressureHypertension = data.get(8);
                    Integer bloodPressureHypotension = data.get(9);


                    guardaDato(context, Constans.HEART_KEY, String.valueOf(heartRate));
                    guardaDato(context, Constans.BLOOD_OXYGEN_KEY, String.valueOf(bloodOxygen));
                    guardaDato(context, Constans.BLOOD_PRESSURE_KEY, bloodPressureHypertension + "/" + bloodPressureHypotension);
                    guardaDato(context, Constans.TEMPERATURE_KEY, data.get(11) + "." + data.get(12));

                }


            }
        }
    };

    public void guardaDato(Context context, String key, String valor){
        try{
            //new SendDataFirebase(context).execute("{\"action\": \"" + key + " - " + valor + " - " + Utils.getHora() + "\"}");
            if(key.equals(Constans.TEMPERATURE_KEY) && valor != null){
                Double temperature = Double.valueOf(valor);
                if (temperature != null && !isNaN(temperature) && temperature > 35 && temperature < 43)
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_KEY, String.valueOf(temperature));
            }else if(key.equals(Constans.HEART_KEY) && valor != null){
                Integer heartRate = Integer.valueOf(valor);
                if (heartRate != null && !isNaN(heartRate) && heartRate > 40 && heartRate < 226)
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_KEY, String.valueOf(heartRate));
            }else if(key.equals(Constans.BLOOD_OXYGEN_KEY) && valor != null){
                Integer bloodOxygen = Integer.valueOf(valor);
                if (bloodOxygen != null && !isNaN(bloodOxygen) && bloodOxygen > 70 && bloodOxygen <= 100)
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_KEY, String.valueOf(bloodOxygen));
            }else if(key.equals(Constans.BLOOD_PRESSURE_KEY) && valor != null){
                Integer bloodPressureHypertension = Integer.valueOf(valor.split("/")[0]);
                Integer bloodPressureHypotension = Integer.valueOf(valor.split("/")[1]);
                if (bloodPressureHypertension != null && !isNaN(bloodPressureHypertension) &&
                        bloodPressureHypertension > 70 && bloodPressureHypertension < 200 &&
                        !isNaN(bloodPressureHypotension) && bloodPressureHypotension > 40 && bloodPressureHypotension < 130)
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_KEY, bloodPressureHypertension + "/" + bloodPressureHypotension);
            }
        }catch (Exception e){

        }
    }

    public void validaLecturas(Context context){
        if(isLecturasCompletas(context)) {
            //validaDatos(data, "TEMPERATURE", context);
            new SendDataFirebase(context).execute("{\"action\": \"APAGO TODAS - " + Utils.getHora() + "\"}");
            manager.getOneClickMeasurementCommand(0);

        }else{
            if(Integer.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.COUNT_GENERAL, "0")) == 3){
                new SendDataFirebase(context).execute("{\"action\": \"PRENDO TODAS - " + Utils.getHora() + "\"}");
                manager.getOneClickMeasurementCommand(1);
            }
        }

        //Valida si el periodo esta completo para enviar la peticion
        if(isPeriodoCompleto(context)){
            enviadatos(context);
        }
    }

    public boolean isLecturasCompletas(Context context){
        boolean r = false;
        try{
            int theCountGeneral = Integer.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.COUNT_GENERAL, "0"));
            if(theCountGeneral < Constans.LECTURAS){
                Sharedpreferences.getInstance(context).escribeValorString(Constans.COUNT_GENERAL, String.valueOf(theCountGeneral + 1));
            }else{
                r = true;
                Sharedpreferences.getInstance(context).escribeValorString(Constans.COUNT_GENERAL, "0");
            }
        }catch (Exception e){
            new SendDataFirebase(context).execute("{\"action\": \"ERROR  - " + e.getMessage() + " - " + Utils.getHora() + "\"}");
        }
        return r;
    }


    public void validaDatos(List<Integer> data, String tipo, Context context){
        boolean r4 = false;
        //r4 = periodoCompleto(context, "TEMPERATURE");
        if(r4){
            enviadatos(context);
        }
    }

    public boolean isPeriodoCompleto(Context context){
        boolean r = false;
        Long theLastGeneral = Long.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.LAST_TIME_GENERAL, "0"));
        Long generalNow = System.currentTimeMillis();

        Long millseg = generalNow - theLastGeneral;
        //new SendDataFirebase(context).execute("{\"action\": \"TIME: -" + millseg  + "-" +  Utils.getHora() + "\"}");
        Long periodo = Long.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.PERIODO, "0")) * 1000;
        if(millseg > periodo)
            r = true;
        return r;
    }

    public void enviadatos(Context context){
        String _heart = Sharedpreferences.getInstance(context).obtenValorString(Constans.HEART_KEY, "0");
        String _oxygen = Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_OXYGEN_KEY, "0");
        String _pressure = Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_KEY, "0/0");
        String _temperature = Sharedpreferences.getInstance(context).obtenValorString(Constans.TEMPERATURE_KEY, "0");
        //new SendDataFirebase(context).execute("{\"action\": \"HEART  - " + _heart + " - " + Utils.getHora() + "\"}");
        //new SendDataFirebase(context).execute("{\"action\": \"BLOOD_OXYGEN - " + _oxygen + " - " + Utils.getHora() + "\"}");
        //new SendDataFirebase(context).execute("{\"action\": \"BLOOD_PRESSURE - " + _pressure + " - " + Utils.getHora() + "\"}");
        //new SendDataFirebase(context).execute("{\"action\": \"TEMPERATURE - " + _temperature + " - " + Utils.getHora() + "\"}");
        new SendDataFirebase(context).execute("{\"action\": \"INVOCANDO DATABASE: "  +  Utils.getHora() + "\"}");
        enviaBaseDeDatos(context);
        inicializaValoresPref(context);
        Sharedpreferences.getInstance(context).escribeValorString(Constans.LAST_TIME_GENERAL, String.valueOf(System.currentTimeMillis()));
        //manager.getOneClickMeasurementCommand(0);
        //App.mBluetoothLeService.disconnect();
    }



    public void enviaBaseDeDatos(Context context){

        Medida   temperatureParam = new Medida();
        Medida     heartRateParam = new Medida();
        Medida   oxygenationParam = new Medida();
        Medida         bloodParam = new Medida();

        int idPulsera = Integer.valueOf(Sharedpreferences.getInstance(this).obtenValorString(Constans.IDPULSERA, "0"));

        temperatureParam.setDispositivo_id(idPulsera);
        temperatureParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.TEMPERATURE_KEY, "0"));
        temperatureParam.setDispositivo_parametro_id(1);
        temperatureParam.setFecha(Utils.getHora());
        temperatureParam.setIdioma("es");

        heartRateParam.setDispositivo_id(idPulsera);
        heartRateParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.HEART_KEY, "0"));
        heartRateParam.setDispositivo_parametro_id(2);
        heartRateParam.setFecha(Utils.getHora());
        heartRateParam.setIdioma("es");

        oxygenationParam.setDispositivo_id(idPulsera);
        oxygenationParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_OXYGEN_KEY, "0"));
        oxygenationParam.setDispositivo_parametro_id(3);
        oxygenationParam.setFecha(Utils.getHora());
        oxygenationParam.setIdioma("es");

        bloodParam.setDispositivo_id(idPulsera);
        bloodParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_KEY, "0/0"));
        bloodParam.setDispositivo_parametro_id(4);
        bloodParam.setFecha(Utils.getHora());
        bloodParam.setIdioma("es");

        String json = "[" + temperatureParam.toJSON() + "," + heartRateParam.toJSON() + "," + oxygenationParam.toJSON() + "," + heartRateParam.toJSON() + "," + bloodParam.toJSON() +  "]";
        //App.mBluetoothLeService.disconnect();

        new RetrieveFeedTask().execute(json);


    }



    void creaAlarmaRecurrenteBluetooth(){
        final int requestCode = 1337;
        AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000 , pendingIntent );

    }

    private IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(MedicionesService.ACTION_MEDICION_BROADCAST);
        return intentFilter;
    }

    @SuppressLint("WrongConstant")
    public void creaNotificacion(){
        mNotifyManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this, null);
        mBuilder.setContentTitle("Egesio Servicios Activos")
                .setContentText("Servicios en completa ejecución")
                .setTicker("Servicios en completa ejecución")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Egesio Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Egesio Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotifyManager.createNotificationChannel(notificationChannel);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            startForeground(17, mBuilder.build());
        }
        else
        {
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotifyManager.notify(17, mBuilder.build());
        }
    }


    class RetrieveFeedTask extends AsyncTask<String, Void, String> {

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
                        .header("Authorization", Constans.TOKEN_KEY)
                        .header("idioma","es")
                        .url(Constans.URL_DEV_SERV)
                        .post(body)
                        .build();

                //Response response = client.newCall(request).execute();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"ERROR: " +  e.getMessage() +  Utils.getHora() + "\"}");
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"ERROR: " + response.message() +  Utils.getHora() + "\"}");
                            throw new IOException("Unexpected code " + response);
                        } else {
                            Log.d("Response", response.body().toString());
                            new SendDataFirebase(getApplicationContext()).execute("{\"action\": \"ESCRIBIO EN BD  - " + Utils.getHora() + "\"}");
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



}
