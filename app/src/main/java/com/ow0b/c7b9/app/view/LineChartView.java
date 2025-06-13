package com.ow0b.c7b9.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;

import java.util.List;

public class LineChartView extends View implements RecordBackground
{
    private Paint linePaint, pointPaint, recordPaint;
    private Paint textPaint;
    private List<Float> data1;
    private List<Float> data2;
    private String[] labels;
    private int phase, tail;

    public LineChartView(Context context)
    {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    public static ViewGroup getView(Context context, LineChartView instance)
    {
        FrameLayout frame = new FrameLayout(context);
        int dp10 = ParaType.toDP(frame, 10);

        frame.setBackground(context.getDrawable(R.drawable.chart_background));
        frame.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ParaType.toDP(frame, 70))
        {{
            bottomMargin = dp10;
            leftMargin = dp10;
        }});
        frame.setPadding(dp10, dp10, dp10, dp10);
        frame.addView(instance);
        return frame;
    }

    private void init()
    {
        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5);
        /*
        pointPaint = new Paint();
        pointPaint.setColor(color);
        pointPaint.setStyle(Paint.Style.FILL);
         */
        recordPaint = new Paint();
        recordPaint.setColor(getResources().getColor(R.color.translucent_gray));

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<Float> data1, List<Float> data2, int phase, int tail)
    {
        this.data1 = data1;
        this.data2 = data2;
        this.labels = new String[Math.max(data1.size(), data2.size())];
        this.labels[0] = "踏板...";
        this.phase = phase;
        this.tail = tail;
        invalidate();
    }

    float process = 0;
    @Override
    public void setProcess(float process)
    {
        this.process = process;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (data1 == null || data2 == null) return;

        int width = getWidth();
        int height = getHeight();

        canvas.drawRect(0, 0, width * process, height, recordPaint);

        float maxValue1 = data1.stream().max(Float::compareTo).orElse(0f),
                maxValue2 = data2.stream().max(Float::compareTo).orElse(0f);
        float max = Math.max(maxValue1, maxValue2);

        Path path1 = new Path();
        int sum1 = data1.size() + phase + tail;
        for (int i = 0; i < sum1; i++)
        {
            int x = i * (width / (sum1 - 1));
            int y = i >= phase && i - phase < data1.size() ? (int) (height - (data1.get(i - phase) / max) * (height - 5)) : height;
            if (i == 0) path1.moveTo(x, y);
            else path1.lineTo(x, y);
        }


        Path path2 = new Path();
        int widthClip = width - (phase + tail) * (width / (sum1 - 1));
        for (int i = 0; i < data2.size(); i++)
        {
            int x = phase * (width / (sum1 - 1)) + i * (widthClip / (data2.size() - 1));
            int y = (int) (height - (data2.get(i) / max) * (height - 5));

            if (i == 0) path2.moveTo(x, y);
            else path2.lineTo(x, y);
            //绘制备注内容
            if(labels[i] != null) canvas.drawText(labels[i], x, height - 10, textPaint);
        }

        linePaint.setColor(getResources().getColor(R.color.light_red));
        canvas.drawPath(path1, linePaint);
        linePaint.setColor(getResources().getColor(R.color.dark_gray));
        canvas.drawPath(path2, linePaint);
    }
}