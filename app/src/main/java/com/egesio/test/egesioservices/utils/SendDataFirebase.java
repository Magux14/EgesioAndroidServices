package com.egesio.test.egesioservices.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendDataFirebase extends AsyncTask<String, Void, String> {

    private Exception exception;
    private SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "EgesioPref" ;
    public static final String macAddress    = "macAddressKey";
    public static final String idPulsera     = "idPuslseraKey";
    public static final String periodo       = "periodoKey";
    private String mac;
    private String id;
    private String time;
    private Context ctx;

    public SendDataFirebase(Context context){
        ctx = context;
    }

    protected String doInBackground(String... json) {


        try {

            sharedpreferences =  ctx.getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

            mac  = sharedpreferences.getString(macAddress,"00:00:00:00:00:00");
            id   = sharedpreferences.getString(idPulsera,"0");
            time = sharedpreferences.getString(periodo,"0");


            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(json[0], JSON);
            Request request = new Request.Builder()
                    .url("https://flutter-varios-b48d1.firebaseio.com/egesio/" + mac + "/" + id + ".json")
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