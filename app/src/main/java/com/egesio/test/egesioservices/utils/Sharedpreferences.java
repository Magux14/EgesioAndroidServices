package com.egesio.test.egesioservices.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.egesio.test.egesioservices.constants.Constans;

public final class Sharedpreferences {

    private static final Sharedpreferences INSTANCE = new Sharedpreferences();
    private static Context context;
    private SharedPreferences sharedpreferences;

    private Sharedpreferences() {

    }

    public static Sharedpreferences getInstance(Context ctx) {
        context = ctx;
        return INSTANCE;
    }

    public void escribeValorString(String key, String value){
        sharedpreferences  = context.getSharedPreferences(Constans.MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public String obtenValorString(String key, String s) {
        sharedpreferences  = context.getSharedPreferences(Constans.MyPREFERENCES, Context.MODE_PRIVATE);
        return sharedpreferences.getString(key,s);
    }

}
