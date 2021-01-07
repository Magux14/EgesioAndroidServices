package com.egesio.test.egesioservices.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.egesio.test.egesioservices.app.App;
import com.egesio.test.egesioservices.command.CommandManager;
import com.egesio.test.egesioservices.constants.Constans;
import com.egesio.test.egesioservices.utils.Sharedpreferences;

public class AlarmReceiver extends BroadcastReceiver {

    private CommandManager manager;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        //if(!App.mConnected){
            //App.mBluetoothLeService.connect(Sharedpreferences.getInstance(context).obtenValorString(Constans.MACADDRESS, "00:00:00:00:00:00"));
        //}else{
        //    manager = CommandManager.getInstance(context);
        //    manager.realTimeAndOnceMeasure(0X0A, 1); // 0X09(Single) 0X0A(Real-time).
        //}

        Intent intent2 = new Intent(MedicionesService.ACTION_MEDICION_BROADCAST);
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent2);

        final int requestCode = 1337;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intentAlarm = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 120000 , pendingIntent );
    }


/*
*/

}

