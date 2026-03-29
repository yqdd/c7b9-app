package com.ow0b.c7b9.app.activity.main.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.ParaType;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Call;

public class ErrorChartView extends View
{
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    private Paint barPaint, linePaint, progressPaint;
    private Paint textPaint;
    private List<Integer> data;
    private List<String> labels;
    private boolean error;
    public ErrorChartView(Context context)
    {
        super(context);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ParaType.toDP(this, 100));
        layout.bottomMargin = ParaType.toDP(this, 10);
        layout.leftMargin = ParaType.toDP(this, 10);
        setLayoutParams(layout);
        setBackground(getResources().getDrawable(R.drawable.bg_chat));

        int padding = ParaType.toDP(this, 10);
        setPadding(padding, padding, padding, padding);

        barPaint = new Paint();
        barPaint.setColor(getResources().getColor(R.color.gray));
        barPaint.setStyle(Paint.Style.FILL);

        float internal = ParaType.toDP(this, 5);
        linePaint = new Paint();
        linePaint.setColor(getResources().getColor(R.color.middle_gray));
        linePaint.setPathEffect(new DashPathEffect(new float[] {internal, internal}, 0));
        linePaint.setStrokeWidth(5f);

        progressPaint = new Paint();
        progressPaint.setColor(getResources().getColor(R.color.translucent_gray));

        textPaint = new Paint();
        textPaint.setColor(getResources().getColor(R.color.black));
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Integer> data)
    {
        this.data = data;
        //this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int pLeft = getPaddingLeft(), pRight = getPaddingRight(),
                pTop = getPaddingTop(), pBottom = getPaddingBottom();
        int width = getWidth() - pLeft - pRight;
        int height = getHeight() - pTop - pBottom;
        canvas.drawText("图表渲染格式错误（可以告诉AI重新发一下）", width / 2f, height / 2f + pTop, textPaint);
    }
}
