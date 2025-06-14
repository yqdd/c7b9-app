package com.ow0b.c7b9.app.util;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;

public class Toast
{
    public static final int LENGTH_LONG = android.widget.Toast.LENGTH_LONG;
    public static final int LENGTH_SHORT = android.widget.Toast.LENGTH_SHORT;

    public enum Type { INFO, ERROR }
    public static void show(Context context, Type type, String message, int duration)
    {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = switch (type)
        {
            case INFO -> inflater.inflate(R.layout.layout_toast_info, null);
            case ERROR -> inflater.inflate(R.layout.layout_toast_error, null);
        };

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        android.widget.Toast toast = new android.widget.Toast(context);
        toast.setDuration(duration);
        toast.setView(layout);
        toast.setGravity(Gravity.CENTER, 0, 0); //显示在屏幕中间
        toast.show();
    }
    public static void showInfo(Context context, String message)
    {
        show(context, Type.INFO, message, LENGTH_SHORT);
    }
    public static void showError(Context context, String message)
    {
        show(context, Type.ERROR, message, LENGTH_SHORT);
    }
}
