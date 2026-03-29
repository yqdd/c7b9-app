package com.ow0b.c7b9.app.api;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class Api
{
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final Retrofit retrofit = new Retrofit.Builder()
            .client(client)
            .baseUrl("http://192.168.188.11:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build();


    /// 快速创建api接口代理类
    public static <T> T create(Class<T> clazz)
    {
        return retrofit.create(clazz);
    }
    /// 保存服务器返回header中的Permit和Random
    public static void save(Context context, Response response)
    {
        SharedPreferences preferences = context.getSharedPreferences("PermitPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if(response.headers().get("Permit") != null) editor.putString("Permit", response.headers().get("Permit"));
        if(response.headers().get("Random") != null) editor.putString("Random", response.headers().get("Random"));
        editor.apply();
    }
    /// 初始化本地保存的Permit和Random参数
    public static Map<String, Object> header(Context context)
    {
        SharedPreferences preferences = context.getSharedPreferences("PermitPrefs", Context.MODE_PRIVATE);
        Map<String, Object> header = new HashMap<>();
        if(preferences.contains("permit")) header.put("permit", preferences.getString("permit", ""));
        if(preferences.contains("random")) header.put("random", preferences.getString("random", ""));
        return header;
    }
}
