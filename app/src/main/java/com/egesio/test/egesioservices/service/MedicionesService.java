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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;


import android.os.Handler;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.egesio.test.egesioservices.R;
import com.egesio.test.egesioservices.app.App;
import com.egesio.test.egesioservices.command.CommandManager;
import com.egesio.test.egesioservices.constants.Constans;
import com.egesio.test.egesioservices.utils.CallHelper;
import com.egesio.test.egesioservices.utils.DataHandlerUtils;
import com.egesio.test.egesioservices.utils.SendDataFirebase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.PowerManager.FULL_WAKE_LOCK;


public class MedicionesService extends Service {

    private CallHelper callHelper;

    private static final String TAG = MedicionesService.class.getSimpleName();
    public static final long NOTIFY_INTERVAL = 1 * 60 * 1000; // 1 minutos
    private final static int MIN_BATTERY_LEVEL = 0;
    private final static int MAX_BATTERY_LEVEL = 100;
    public static final String ACTION_MEDICION_BROADCAST = MedicionesService.class.getName() + "BroadcastMedicion";

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();
    //private Timer mTimer = null;
    private CommandManager manager;
    private BroadcastReceiver mMedicionesUpdateReceiver;

    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    NotificationChannel notificationChannel;
    String NOTIFICATION_CHANNEL_ID = "17";


    public MedicionesService() { }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("WatchMan : ", "\nOnCreate...");

        mNotifyManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this, null);
        mBuilder.setContentTitle("Servicios Activos")
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
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
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
            //startForeground(17, mBuilder.build());
            mNotifyManager.notify(17, mBuilder.build());
        }

        try{
            manager = CommandManager.getInstance(this);
            final int requestCode = 1337;
            AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000 , pendingIntent );

        }catch (Exception e){
            Log.d("EGESIO", e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callHelper = new CallHelper(this);
        int res = super.onStartCommand(intent, flags, startId);
        callHelper.start();
        //LocalBroadcastManager.getInstance(this).registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        return res; //START_NOT_STICKY;
    }

    @Override
    public void onDestroy()
    {
        callHelper.stop();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
        Log.d("WatchMan : ", "\nDestroyed....");
        Log.d("WatchMan : ", "\nWill be created again....");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //super.onTaskRemoved(rootIntent);
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
        try{
            for(int i = 0; i < 100000; i++);
            stopService(new Intent(this, MedicionesService.class));
            getApplicationContext().startService(new Intent(getApplicationContext(), MedicionesService.class));


        }catch (Exception e){
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        return null;//throw new UnsupportedOperationException("Not yet implemented");
    }

}


