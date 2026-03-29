package com.ow0b.c7b9.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.file.Path;
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
    private Context context;
    private final OkHttpClient client;
    private final SharedPreferences sharedPreferences;
    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences("PermitPrefs", Context.MODE_PRIVATE);
    }
    public static boolean isLogin(Context context)
    {
        return getSharedPreferences(context).contains("permit");
    }
    private ApiClient(Context context, int timeout)
    {
        this.context = context;
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
        ApiClient.getInstance(activity, 5).url(activity.getResources().getString(R.string.server) + "/alive")
                .get()
                .callback(new Callback()
                {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        Log.e(TAG, "onFailure: ", e);
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
    private boolean cache;
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
    public ApiClient cache()
    {
        this.cache = true;
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
        if(cache && !(callback instanceof ApiCallback)) throw new RuntimeException("需要缓存需要使用ApiCallback多次获取response");
        return this;
    }

    private String requestUrl()
    {
        StringBuilder url = new StringBuilder(this.url);
        if(!parameters.isEmpty())
        {
            url.append("?");
            parameters.forEach((k, v) -> url.append(k).append("=").append(v).append("&"));
            url.deleteCharAt(url.length() - 1);
        }
        return url.toString();
    }
    private Request requestInit()
    {
        Request.Builder reqBuilder = new Request.Builder();
        if(sharedPreferences.contains("permit"))
            reqBuilder.addHeader("permit", sharedPreferences.getString("permit", ""));
        if(sharedPreferences.contains("random"))
            reqBuilder.addHeader("random", sharedPreferences.getString("random", ""));

        if(method.equalsIgnoreCase("GET")) reqBuilder.get();
        else if(type == null) reqBuilder.method(method, RequestBody.create(body.toString().getBytes()));
        else reqBuilder.method(method, RequestBody.create(body.toString(), MediaType.get(type)));

        reqBuilder.url(requestUrl());
        return reqBuilder.build();
    }
    public void enqueue()
    {
        File cacheFile = cacheFile(context, requestUrl());
        if(cache && cacheFile.exists() && callback instanceof ApiCallback apiCallback)
        {
            new Thread(() ->
            {
                try(FileInputStream stream = new FileInputStream(cacheFile))
                {
                    StringBuilder resp = new StringBuilder();
                    int i;
                    while((i = stream.read()) != -1) resp.append((char) i);
                    apiCallback.onResponse(resp.toString());
                }
                catch (Exception e)
                {
                    enqueueWeb();
                }
            }).start();
        }
        else enqueueWeb();
    }
    private void enqueueWeb()
    {
        File cacheFile = cacheFile(context, requestUrl());
        client.newCall(requestInit()).enqueue(new Callback()
        {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                try
                {
                    if(response.code() != 200)
                    {
                        //只有200返回码才能触发响应，否则服务端报错也会触发app会有一堆问题
                        callback.onFailure(call, new IOException());
                        Log.e(TAG, "onResponse: 非200响应码：" + response.code());
                    }
                    else if(callback instanceof ApiCallback apiCallback)
                    {
                        String body = response.body().string();
                        Log.i(TAG, "request: " + call.request());
                        Log.i(TAG, "response: " + body);
                        if(cache)       //如果开启了缓存则缓存数据
                        {
                            cacheFile.createNewFile();
                            try(Writer writer = new OutputStreamWriter(new FileOutputStream(cacheFile)))
                            {
                                writer.write(body);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
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
                catch (Exception e)
                {
                    Toast.showInfo(context, "调用api出现错误：" + e.getMessage());
                    Log.e(TAG, "调用api出现错误：", e);
                }
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
    private static File cacheFile(Context context, String name)
    {
        Path path = context.getCacheDir().toPath().resolve("api");
        if(!path.toFile().exists() && !path.toFile().mkdir()) Log.e("ApiClient", "cacheFile: 创建缓存文件夹失败");
        return path.resolve(URLEncoder.encode(name)).toFile();
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
