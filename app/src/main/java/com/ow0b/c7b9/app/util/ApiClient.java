package com.ow0b.c7b9.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient
{
    private final static String TAG = ApiClient.class.getName();
    private final static Gson gson = new GsonBuilder().create();
    public static boolean serverAlive = true;
    private final OkHttpClient client;
    private final SharedPreferences sharedPreferences;
    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences("PermitPrefs", Context.MODE_PRIVATE);
    }
    private ApiClient(Context context, int timeout)
    {
        sharedPreferences = getSharedPreferences(context);

        // 初始化OkHttpClient 设置CookieJar
        client = new OkHttpClient.Builder()
                .cookieJar(new CookieJar()
                {
                    @Override
                    public void saveFromResponse(HttpUrl url, List<Cookie> cookies)
                    {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        for (Cookie cookie : cookies) editor.putString(url.host(), cookie.toString());
                        editor.apply();
                    }
                    @Override
                    public List<Cookie> loadForRequest(HttpUrl url)
                    {
                        String cookieString = sharedPreferences.getString(url.host(), null);
                        if (cookieString != null) return Collections.singletonList(Cookie.parse(url, cookieString));
                        else return Collections.emptyList();
                    }
                })
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }
    public static ApiClient getInstance(Context context)
    {
        return getInstance(context, 20);
    }
    public static ApiClient getInstance(Context context, int timeout)
    {
        return new ApiClient(context, timeout);
    }
    public static void pingServer(Activity activity)
    {
        //检测服务器是否被关闭
        ApiClient.getInstance(activity, 2).url(activity.getResources().getString(R.string.server) + "/alive")
                .get()
                .callback(new Callback()
                {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        activity.runOnUiThread(() ->
                        {
                            View view = View.inflate(activity, R.layout.layout_server_dead, null);
                            AlertDialog dialog = new AlertDialog.Builder(activity)
                                    .setView(view)
                                    .create();
                            TextView ok = view.findViewById(R.id.server_dead_ok);
                            ok.setOnClickListener(v -> dialog.cancel());

                            dialog.show();
                        });
                        //设置后其他的onFailure就不会触发（特别是Toast）
                        serverAlive = false;
                    }
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException { }
                })
                .enqueue();
    }

    private String url = null;
    private LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
    private String method;
    private String type;
    private Object body;
    private Callback callback = null;
    public ApiClient url(String url)
    {
        this.url = url;
        return this;
    }
    public ApiClient parameter(String key, String value)
    {
        parameters.put(key, value);
        return this;
    }
    public ApiClient get()
    {
        return method("GET", new JsonObject());
    }
    public ApiClient method(String method)
    {
        this.method = method;
        this.body = "";
        return this;
    }
    public ApiClient method(String method, JsonElement body)
    {
        this.method = method;
        this.type = "application/json";
        this.body = body;
        return this;
    }
    public ApiClient method(String method, Object body)
    {
        this.method = method;
        this.type = "application/json";
        this.body = gson.toJson(body);
        return this;
    }
    public ApiClient method(String method, String body, String type)
    {
        this.method = method;
        this.type = type;
        this.body = body;
        return this;
    }
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
        if(sharedPreferences.contains("permit"))
            reqBuilder.addHeader("permit", sharedPreferences.getString("permit", ""));
        if(sharedPreferences.contains("random"))
            reqBuilder.addHeader("random", sharedPreferences.getString("random", ""));

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
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if(respJson.get("random") != null)
                        editor.putString("random", respJson.get("random").getAsString());
                    if(respJson.get("permit") != null)
                        editor.putString("permit", respJson.get("permit").getAsString());
                    editor.apply();
                    apiCallback.onResponse(body);
                    apiCallback.onResponse(response, body);
                }
                else callback.onResponse(call, response);
                serverAlive = true;
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e)
            {
                if(serverAlive) callback.onFailure(call, e);
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
    public static void downloadResource(Context context, ProgressBar progressBar, int rid, File saveFile, Runnable callback)
    {
        downloadResourceClient(context, progressBar, saveFile, callback)
                .parameter("id", String.valueOf(rid))
                .enqueue();
    }
    public static void downloadResource(Context context, ProgressBar progressBar, String secret, File saveFile, Runnable callback)
    {
        downloadResourceClient(context, progressBar, saveFile, callback)
                .parameter("secret", secret)
                .enqueue();
    }
    private static ApiClient downloadResourceClient(Context context, ProgressBar progressBar, File saveFile, Runnable callback)
    {
        return ApiClient.getInstance(context).url(context.getResources().getString(R.string.server) + "/resource")
                .get()
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        InputStream body = response.body().byteStream();
                        long length = response.body().contentLength();
                        long current = 0;
                        progressBar.setMax(10000);
                        try(OutputStream output = new FileOutputStream(saveFile))
                        {
                            int b;
                            while((b = body.read()) != -1)
                            {
                                output.write(b);
                                current ++;
                            }
                            progressBar.setProgress((int) ((current / length) * 10000));
                        }
                        callback.run();
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        Toast.showError(context, "下载音频文件出现未知错误");
                    }
                });
    }
}
