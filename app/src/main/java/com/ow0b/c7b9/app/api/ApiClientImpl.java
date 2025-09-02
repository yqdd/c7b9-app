package com.ow0b.c7b9.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.old.util.ApiCallback;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClientImpl implements ApiClient
{
    private final static String TAG = "ApiClient";
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final OkHttpClient client;
    private final SharedPreferences preferences;
    public boolean dispose = false;

    public ApiClientImpl(Context context, int timeout)
    {
        this.preferences = context.getSharedPreferences("PermitPrefs", Context.MODE_PRIVATE);
        // 初始化OkHttpClient 设置CookieJar
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    private final LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
    private String url = null;
    private String method;
    private String type;
    private Object body;
    private Callback callback = null;
    @Override
    public ApiClient url(String url)
    {
        this.url = url;
        return this;
    }
    @Override
    public ApiClient parameter(String key, String value)
    {
        parameters.put(key, value);
        return this;
    }
    @Override
    public ApiClient get()
    {
        return method("GET", new JsonObject());
    }
    @Override
    public ApiClient method(String method)
    {
        this.method = method;
        this.body = "";
        return this;
    }
    @Override
    public ApiClient method(String method, Object body)
    {
        this.method = method;
        this.type = "application/json";
        this.body = gson.toJson(body);
        return this;
    }
    @Override
    public ApiClient method(String method, String body, String type)
    {
        this.method = method;
        this.type = type;
        this.body = body;
        return this;
    }
    @Override
    public ApiClient callback(Callback callback)
    {
        this.callback = callback;
        return this;
    }

    private Request requestInit()
    {
        StringBuilder url = new StringBuilder(this.url);
        if(!parameters.isEmpty())
        {
            url.append("?");
            parameters.forEach((k, v) -> url.append(k).append("=").append(v).append("&"));
            url.deleteCharAt(url.length() - 1);
        }

        Request.Builder reqBuilder = new Request.Builder();
        if(preferences.contains("permit"))
            reqBuilder.addHeader("permit", preferences.getString("permit", ""));
        if(preferences.contains("random"))
            reqBuilder.addHeader("random", preferences.getString("random", ""));

        if(method.equalsIgnoreCase("GET")) reqBuilder.get();
        else if(type == null) reqBuilder.method(method, RequestBody.create(body.toString().getBytes()));
        else reqBuilder.method(method, RequestBody.create(body.toString(), MediaType.get(type)));

        reqBuilder.url(url.toString());
        return reqBuilder.build();
    }
    public void enqueue()
    {
        client.newCall(requestInit()).enqueue(new Callback()
        {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                if(callback instanceof ApiCallback apiCallback)
                {
                    String body = response.body().string();
                    Log.i(TAG, "request: " + call.request());
                    Log.i(TAG, "response: " + body);
                    //这里如果报IllegalStateException，response是空的，是后端json格式没写对（每条请求必须带random和permit）
                    JsonObject respJson = JsonParser.parseString(body).getAsJsonObject();
                    SharedPreferences.Editor editor = preferences.edit();
                    if(respJson.get("random") != null)
                        editor.putString("random", respJson.get("random").getAsString());
                    if(respJson.get("permit") != null)
                        editor.putString("permit", respJson.get("permit").getAsString());
                    editor.apply();
                    apiCallback.onResponse(body);
                    apiCallback.onResponse(response, body);
                }
                else callback.onResponse(call, response);
                dispose = false;
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e)
            {
                if(!dispose) callback.onFailure(call, e);
            }
        });
    }
    public void execute()
    {
        try(Response response = client.newCall(requestInit()).execute())
        {
            callback.onResponse(null, response);
        }
        catch (IOException e)
        {
            callback.onFailure(null, e);
        }
    }
    @Override
    public void dispose()
    {
        this.dispose = true;
    }
}
