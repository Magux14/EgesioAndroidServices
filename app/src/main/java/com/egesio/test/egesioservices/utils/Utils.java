package com.egesio.test.egesioservices.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.location.LocationManager;
import static android.content.Context.ACTIVITY_SERVICE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Utils {

    public static boolean isOpenLocationService(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context _ctx) {
        final ActivityManager activityManager = (ActivityManager)_ctx.getSystemService(ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClass.getName())){
                return true;
            }
        }
        return false;
    }

    public static String getHora(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String fecha = dtf.format(now);
        return fecha;
    }
}