package com.ow0b.c7b9.app.old.util;

import android.util.TypedValue;
import android.view.View;

import org.jetbrains.annotations.NotNull;

public class ParaType
{
    public static int toDP(@NotNull View view, int px)
    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, view.getResources().getDisplayMetrics());
    }
}
