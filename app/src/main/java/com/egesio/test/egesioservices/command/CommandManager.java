package com.egesio.test.egesioservices.command;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.egesio.test.egesioservices.constants.BleConstans;
import com.egesio.test.egesioservices.constants.Constans;

public class CommandManager {

    private static final String TAG = "CommandManager";
    private static Context mContext;
    private static CommandManager instance;

    private CommandManager() {
    }

    public static synchronized CommandManager getInstance(Context context) {
        if (mContext == null) {
            mContext = context;
        }
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    public void sendVibrar(int control){
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0xB1;
        bytes[5] = (byte) 0x80;
        bytes[6] = (byte)control;
        broadcastData(bytes);
    }

    public void getOneClickMeasurementCommand(int control) {
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x32;
        bytes[5] = (byte) 0x80;
        bytes[6] = (byte) control;
        broadcastData(bytes);
    }

    public void realTimeAndOnceMeasure(int status, int control) {
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x31;
        bytes[5] = (byte) status;
        bytes[6] = (byte) control;
        broadcastData(bytes);
    }

    public void heartRateSensor(int control){
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x31;
        bytes[5] = (byte) 0X0A;
        bytes[6] = (byte) control;
        broadcastData(bytes);
    }

    public void bloodOxygenSensor(int control){
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x31;
        bytes[5] = (byte) 0X12;
        bytes[6] = (byte) control;
        broadcastData(bytes);
    }

    public void bloodPressureSensor(int control){
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x31;
        bytes[5] = (byte) 0X22;
        bytes[6] = (byte) control;
        broadcastData(bytes);
    }

    public void temperatureSensor(int control){
        byte[] bytes = new byte[7];
        bytes[0] = (byte) 0xAB;
        bytes[1] = (byte) 0;
        bytes[2] = (byte) 4;
        bytes[3] = (byte) 0xFF;
        bytes[4] = (byte) 0x86;
        bytes[5] = (byte) 0x80;
        bytes[6] = (byte) control; // Turn ON sensor.
        broadcastData(bytes);
    }

    private void broadcastData(byte[] bytes) {
        final Intent intent = new Intent(BleConstans.ACTION_SEND_DATA_TO_BLE);
        intent.putExtra(Constans.EXTRA_SEND_DATA_TO_BLE, bytes);
        try {
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

}
