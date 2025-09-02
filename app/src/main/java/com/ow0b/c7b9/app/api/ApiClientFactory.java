package com.ow0b.c7b9.app.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.old.util.Toast;

import java.io.IOException;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ApiClientFactory
{
    private static boolean alive = true;
    public static ApiClient getInstance(Context context)
    {
        return getInstance(context, 20);
    }
    public static ApiClient getInstance(Context context, int timeout)
    {
        ApiClient client = new ApiClientImpl(context, timeout);
        if(!alive) client.dispose();
        return client;
    }
    public static void ping(Context context, String url, Consumer<Boolean> success)
    {
        //检测服务器是否被关闭
        ApiClientFactory.getInstance(context, 2).url(url)
                .get()
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        success.accept(true);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        success.accept(false);
                        alive = false;
                    }
                })
                .enqueue();
    }
    public static String check(Context context, String response)
    {
        JsonElement element = JsonParser.parseString(response);
        if(element.isJsonObject())
        {
            JsonObject json = element.getAsJsonObject();
            JsonElement info = json.get("info"), error = json.get("error");
            if(info != null && !info.isJsonNull())
            {
                Toast.showInfo(context, info.getAsString());
                return "info";
            }
            if(error != null && !error.isJsonNull())
            {
                Toast.showInfo(context, error.getAsString());
                return "error";
            }
            //if(error != null && !error.isJsonNull()) Toast.showError(context, error.getAsString());
        }
        return null;
    }
}
