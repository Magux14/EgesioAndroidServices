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
            Log.d("EGESIO", e.getMessage());
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
            sendMessageToUI(this);
        }catch (Exception e){
            Log.d("EGESIO", e.getMessage());
        }
        return super.onStartCommand(intent, flags, startId); //START_NOT_STICKY;
    }

    public void inicializaValoresPref(Context context){

        Sharedpreferences.getInstance(context).escribeValorString(Constans.MACADDRESS, Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.IDPULSERA, Sharedpreferences.getInstance(context).obtenValorString(Constans.IDPULSERA, "117"));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.PERIODO, Sharedpreferences.getInstance(context).obtenValorString(Constans.PERIODO, "300"));

        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_COUNT, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_COUNT, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_COUNT, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_COUNT, "0");

        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_KEY, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_KEY, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_KEY, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_KEY, "0/0");

        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_LAST, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_LAST, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_LAST, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_LAST, "0");

        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_TIME, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_TIME, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_TIME, "0");
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_TIME, "0");
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
        try{
            for(int i = 0; i < 100000; i++);
            stopService(new Intent(this, RealTimeService.class));
            getApplicationContext().startService(new Intent(getApplicationContext(), RealTimeService.class));
        }catch (Exception e){
        }
    }

    private void sendMessageToUI(Context context) {
        try{
            Intent intent = new Intent(MedicionesService.ACTION_MEDICION_BROADCAST);
            LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
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
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                inicializaValoresPref(context);
                new SendDataFirebase(context).execute("{\"action\": \"DISCONECT - " + Utils.getHora() + "\"}");
                LocalBroadcastManager.getInstance(context).unregisterReceiver(mGattUpdateReceiver);
                /*Timer mTimer = new Timer(true);
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {*/
                       if(!App.mConnected){
                           LocalBroadcastManager.getInstance(context).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                           App.mBluetoothLeService.connect(Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
                       }
                    /*}
                }, 2000);*/
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                List<Integer> data = DataHandlerUtils.bytesToArrayList(txValue);
                //new SendDataFirebase(context).execute("{\"action\": \"DATA_AVAILABLE - " + data + " - " + Utils.getHora() + "\"}");
                if (data.get(4) == 0X91) {
                    //manager.realTimeAndOnceMeasure(0X0A, 1); // 0X09(Single) 0X0A(Real-time)

                    /*Timer mTimer1 = new Timer(true);
                    mTimer1.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            manager.heartRateSensor(1);
                        }
                    }, 100);

                    Timer mTimer2 = new Timer(true);
                    mTimer2.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            manager.bloodOxygenSensor( 1);
                        }
                    }, 1000);

                    Timer mTimer3 = new Timer(true);
                    mTimer3.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            manager.bloodPressureSensor( 1);
                        }
                    }, 2000);*/

                    //manager.getOneClickMeasurementCommand(1);

                    Timer mTimer4 = new Timer(true);
                    mTimer4.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            manager.temperatureSensor(1);
                        }
                    }, 100);

                }

                if (data != null && data.size() == 13 && data.get(4) == 0x32) {
                    new SendDataFirebase(context).execute("{\"action\": \"REGRESO TODAAAAAS - " + Utils.getHora() + "\"}");

                    String heartRateString = "";
                    String bloodOxygenString = "";
                    String temperatureString = "";
                    String bloodPressureString = "";

                    Integer heartRate = data.get(6);
                    Integer bloodOxygen = data.get(7);
                    Integer bloodPressureHypertension = data.get(8);
                    Integer bloodPressureHypotension = data.get(9);
                    Float temp = 0.0f;

                    if (data.get(11) != null && data.get(12) != null && !isNaN(data.get(11)) && !isNaN(data.get(12)))
                        temp = parseFloat(data.get(11) + "." + data.get(12));

                    if (heartRate != null && !isNaN(heartRate) && heartRate > 40 && heartRate < 226)
                        heartRateString = String.valueOf(heartRate);

                    if (bloodOxygen != null && !isNaN(bloodOxygen) && bloodOxygen > 70 && bloodOxygen <= 100)
                        bloodOxygenString = String.valueOf(bloodOxygen);

                    if (temp != null && !isNaN(temp) && temp > 35 && temp < 43)
                        temperatureString = String.valueOf(temp);

                    if (bloodPressureHypertension != null && !isNaN(bloodPressureHypertension) &&
                            bloodPressureHypertension > 70 && bloodPressureHypertension < 200 &&
                            !isNaN(bloodPressureHypotension) && bloodPressureHypotension > 40 && bloodPressureHypotension < 130)
                        bloodPressureString = bloodPressureHypertension + "/" + bloodPressureHypotension;

                    String r = "";

                    r += "-" + heartRateString;
                    r += "-" + bloodOxygenString;
                    r += "-" + temperatureString;
                    r += "-" + bloodPressureString;

                    new SendDataFirebase(context).execute("{\"action\": \"VALORES TODAAAAAS - " + r + "-" + Utils.getHora() + "\"}");

                    Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_KEY, String.valueOf(heartRateString));
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_KEY, String.valueOf(bloodOxygenString));
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_KEY, String.valueOf(bloodPressureString));
                    Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_KEY, String.valueOf(temperatureString));


                }

                if (data.get(0) == 0xAB && data.get(4) == 0x10) { //31
                    switch (data.get(5)) {
                        case 0X0A:
                            //Heart Rate（Real-time）
                            new SendDataFirebase(context).execute("{\"action\": \"HEART - " + Utils.getHora() + "\"}");
                            /*String theLastHeart = Sharedpreferences.getInstance(context).obtenValorString(Constans.HEART_LAST, "0");
                            if(theLastHeart.equals("0"))
                                Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_LAST, String.valueOf(System.currentTimeMillis()));
                            if(isLecturasCompletas(context, "HEART")) {
                                validaDatos(data, "HEART", context);
                                manager.realTimeAndOnceMeasure(0x0A, 0);
                                Timer mTimer = new Timer(true);
                                mTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        manager.realTimeAndOnceMeasure(0X12, 1); // ：0X11(Single) 0X12(Real-time)
                                }
                                }, 1000);
                            }*/

                            break;
                        case 0x12:
                            //Blood Oxygen（Real-time）
                            new SendDataFirebase(context).execute("{\"action\": \"BLOOD_OXYGEN - " + Utils.getHora() + "\"}");
                            /*String theLastOxygen = Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_OXYGEN_LAST, "0");
                            if(theLastOxygen.equals("0"))
                                Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_LAST, String.valueOf(System.currentTimeMillis()));
                            if(isLecturasCompletas(context, "BLOOD_OXYGEN")) {
                                validaDatos(data, "BLOOD_OXYGEN", context);
                                manager.realTimeAndOnceMeasure(0X12, 0);
                                Timer mTimer2 = new Timer(true);
                                mTimer2.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        manager.realTimeAndOnceMeasure(0X22, 1); // ：0X21(Single) 0X22(Real-time)
                                    }
                                }, 1000);
                            }*/
                            break;
                        case 0x22:
                            //Blood Pressure（Real-time）
                            new SendDataFirebase(context).execute("{\"action\": \"BLOOD_PRESSURE - " + Utils.getHora() + "\"}");
                            /*String theLastPressure = Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_LAST, "0/0");
                            if(theLastPressure.equals("0"))
                                Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_LAST, String.valueOf(System.currentTimeMillis()));
                            if(isLecturasCompletas(context, "BLOOD_PRESSURE")) {
                                validaDatos(data, "BLOOD_PRESSURE", context);
                                manager.realTimeAndOnceMeasure(0X22, 0);
                                Timer mTimer3 = new Timer(true);
                                mTimer3.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        manager.realTimeAndOnceMeasure(0X0A, 1);
                                    }
                                }, 1000);
                            }*/
                            break;
                    }
                }

                if (data.get(0) == 0xAB && data.get(4) == 0x86) {
                    new SendDataFirebase(context).execute("{\"action\": \"TEMP - " + Utils.getHora() + "\"}");
                    String theLastTemperature = Sharedpreferences.getInstance(context).obtenValorString(Constans.TEMPERATURE_LAST, "0");
                    if(theLastTemperature.equals("0"))
                        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_LAST, String.valueOf(System.currentTimeMillis()));
                    if(isLecturasCompletas(context, "TEMPERATURE")) {
                        validaDatos(data, "TEMPERATURE", context);
                        new SendDataFirebase(context).execute("{\"action\": \"APAGO TODAS - " + Utils.getHora() + "\"}");
                        manager.getOneClickMeasurementCommand(0);
                        //App.mBluetoothLeService.disconnect();
                        //manager.temperatureSensor(0);
                        /*Timer mTimer4 = new Timer(true);
                        mTimer4.schedule(new TimerTask() {
                            @Override
                            public void run() {
                               // manager.temperatureSensor(0);
                                manager.realTimeAndOnceMeasure(0x0A, 1);
                            }
                        }, 1000);*/
                    }else{
                        if(Integer.valueOf(Sharedpreferences.getInstance(context).obtenValorString("TEMPERATURE_COUNT", "0")) == 3){
                            new SendDataFirebase(context).execute("{\"action\": \"PRENDO TODAS - " + Utils.getHora() + "\"}");
                            manager.getOneClickMeasurementCommand(1);
                        }
                    }
                }
            }
        }
    };

    public boolean isLecturasCompletas(Context context, String key){
        boolean r = false;
        int theHeartCount = Integer.valueOf(Sharedpreferences.getInstance(context).obtenValorString(key + "_COUNT", "0"));
        if(theHeartCount < Constans.LECTURAS){
            Sharedpreferences.getInstance(context).escribeValorString(key + "_COUNT", String.valueOf(theHeartCount + 1));
        }else{
            r = true;
            Sharedpreferences.getInstance(context).escribeValorString(key + "_COUNT", "0");
        }
        return r;
    }


    public void validaDatos(List<Integer> data, String tipo, Context context){
        //new SendDataFirebase(context).execute("{\"Entre a\": \"" + tipo + "-" + Utils.getHora() + "\"}");
        boolean r1 = false, r2 = false, r3 = false, r4 = false;
        /*if(tipo.equals("HEART")){
            int heartRate = data.get(6);
            if(heartRate != 0)
                Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_KEY, String.valueOf(heartRate));
            r1 = periodoCompleto(context, "HEART");
        }else if(tipo.equals("BLOOD_OXYGEN")){
            int bloodOxygen = data.get(6);
            if(bloodOxygen != 0)
                Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_KEY, String.valueOf(bloodOxygen));
            r2 = periodoCompleto(context, "BLOOD_OXYGEN");
        }else if(tipo.equals("BLOOD_PRESSURE")){
            int bloodPressureHigh = data.get(6);
            int bloodPressureLow = data.get(7);
            if(bloodPressureHigh != 0 && bloodPressureLow != 0)
                Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_KEY, bloodPressureHigh + "/" + bloodPressureLow);
            r3 = periodoCompleto(context, "BLOOD_PRESSURE");
        }else if(tipo.equals("TEMPERATURE")){
            int tempEntero = data.get(6);
            int tempDecimal = data.get(7);
            if(tempEntero != 0 && tempDecimal != 0)
                Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_KEY, tempEntero + "." + tempDecimal);*/
            r4 = periodoCompleto(context, "TEMPERATURE");
        //}

        //if(r1 || r2 || r3 || r4){
        if(r4){
            enviadatos(context);
        }
    }

    public boolean periodoCompleto(Context context, String key){
        boolean r = false;
        Long heartLast = Long.valueOf(Sharedpreferences.getInstance(context).obtenValorString(key  + "_LAST", "0"));
        Long heartNow = System.currentTimeMillis();

        Long millseg = heartNow - heartLast;
        Sharedpreferences.getInstance(context).escribeValorString(key  + "_TIME", String.valueOf(millseg));
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
        new SendDataFirebase(context).execute("{\"action\": \"HEART  - " + _heart + " - " + Utils.getHora() + "\"}");
        new SendDataFirebase(context).execute("{\"action\": \"BLOOD_OXYGEN - " + _oxygen + " - " + Utils.getHora() + "\"}");
        new SendDataFirebase(context).execute("{\"action\": \"BLOOD_PRESSURE - " + _pressure + " - " + Utils.getHora() + "\"}");
        new SendDataFirebase(context).execute("{\"action\": \"TEMPERATURE - " + _temperature + " - " + Utils.getHora() + "\"}");
        enviaBaseDeDatos(context);
        inicializaValoresPref(context);
        Sharedpreferences.getInstance(context).escribeValorString(Constans.HEART_LAST, String.valueOf(System.currentTimeMillis()));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_OXYGEN_LAST, String.valueOf(System.currentTimeMillis()));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.BLOOD_PRESSURE_LAST, String.valueOf(System.currentTimeMillis()));
        Sharedpreferences.getInstance(context).escribeValorString(Constans.TEMPERATURE_LAST, String.valueOf(System.currentTimeMillis()));
        manager.getOneClickMeasurementCommand(0);
        //App.mBluetoothLeService.disconnect();
    }



    public void enviaBaseDeDatos(Context context){

        /*String heartRateString = "255";
        String bloodOxygenString = "255";
        String bloodPressureString = "255/255";
        String temperatureString = "255";

        Double heartRate = Double.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.HEART_KEY, "0"));
        Double bloodOxygen = Double.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_OXYGEN_KEY, "0"));
        Double bloodPressureHypertension = Double.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_KEY, "0/0").split("/")[0]);
        Double bloodPressureHypotension = Double.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_KEY, "0/0").split("/")[1]);
        Double temp = Double.valueOf(Sharedpreferences.getInstance(context).obtenValorString(Constans.TEMPERATURE_KEY, "0"));*/

        /*if (data.get(11) != null && data.get(12) != null && !isNaN(data.get(11)) && !isNaN(data.get(12)))
            temp = parseFloat(data.get(11) + "." + data.get(12));*/

        /*if (heartRate != null && !isNaN(heartRate) && heartRate > 40 && heartRate < 226)
            heartRateString = String.valueOf(heartRate);

        if (bloodOxygen != null && !isNaN(bloodOxygen) && bloodOxygen > 70 && bloodOxygen <= 100)
            bloodOxygenString = String.valueOf(bloodOxygen);

        if (temp != null && !isNaN(temp) && temp > 35 && temp < 43)
            temperatureString = String.valueOf(temp);

        if (bloodPressureHypertension != null && !isNaN(bloodPressureHypertension) &&
                bloodPressureHypertension > 70 && bloodPressureHypertension < 200 &&
                !isNaN(bloodPressureHypotension) && bloodPressureHypotension > 40 && bloodPressureHypotension < 130)
            bloodPressureString = bloodPressureHypertension + "/" + bloodPressureHypotension;*/

        Medida   temperatureParam = new Medida();
        Medida     heartRateParam = new Medida();
        Medida   oxygenationParam = new Medida();
        Medida         bloodParam = new Medida();

        String idPulsera = Sharedpreferences.getInstance(this).obtenValorString(Constans.IDPULSERA, "0");

        temperatureParam.setDispositivo_id(idPulsera);
        temperatureParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.TEMPERATURE_KEY, "0"));
        temperatureParam.setDispositivo_parametro_id("1");
        temperatureParam.setFecha(Utils.getHora());
        temperatureParam.setIdioma("es");

        heartRateParam.setDispositivo_id(idPulsera);
        heartRateParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.HEART_KEY, "0"));
        heartRateParam.setDispositivo_parametro_id("2");
        heartRateParam.setFecha(Utils.getHora());
        heartRateParam.setIdioma("es");

        oxygenationParam.setDispositivo_id(idPulsera);
        oxygenationParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_OXYGEN_KEY, "0"));
        oxygenationParam.setDispositivo_parametro_id("3");
        oxygenationParam.setFecha(Utils.getHora());
        oxygenationParam.setIdioma("es");

        bloodParam.setDispositivo_id(idPulsera);
        bloodParam.setValor(Sharedpreferences.getInstance(context).obtenValorString(Constans.BLOOD_PRESSURE_KEY, "0/0"));
        bloodParam.setDispositivo_parametro_id("4");
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
                        .header("Authorization", Constans.TOKENTMPBARER)
                        .header("idioma","es")
                        .url(Constans.URL_DEV_SERV)
                        .post(body)
                        .build();

                //Response response = client.newCall(request).execute();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        } else {
                            Log.d("Response", response.body().toString());
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
