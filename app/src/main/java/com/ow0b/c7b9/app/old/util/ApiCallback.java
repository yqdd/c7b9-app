package com.ow0b.c7b9.app.old.util;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public abstract class ApiCallback implements Callback
{
    final Activity activity;
    public ApiCallback(Activity activity)
    {
        this.activity = activity;
    }
    public void onResponse(@NonNull String response) {}
    public void onResponse(@NonNull Response resp, @NonNull String body) {}

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
    {

    }
    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e)
    {
        activity.runOnUiThread(() -> Toast.showError(activity, "连接服务器失败，请稍后再试"));
    }
}
