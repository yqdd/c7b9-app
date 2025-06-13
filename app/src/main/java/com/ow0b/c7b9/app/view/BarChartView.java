package com.ow0b.c7b9.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;

import java.util.List;

public class BarChartView extends View
{
    private Paint barPaint, linePaint;
    private Paint textPaint;
    private List<Integer> data;
    private List<String> labels;
    public BarChartView(Context context)
    {
        super(context);
        init();
    }
    public BarChartView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    public static ViewGroup getView(Context context, BarChartView instance)
    {
        FrameLayout frame = new FrameLayout(context);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ParaType.toDP(frame, 100));
        layout.bottomMargin = ParaType.toDP(frame, 10);
        layout.leftMargin = ParaType.toDP(frame, 10);
        frame.setLayoutParams(layout);
        frame.setBackground(frame.getResources().getDrawable(R.drawable.chart_background));

        int padding = ParaType.toDP(frame, 10);
        frame.setPadding(padding, padding, padding, padding);

        instance.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.addView(instance);
        return frame;
    }

    private void init()
    {
        barPaint = new Paint();
        barPaint.setColor(getResources().getColor(R.color.light_red));
        barPaint.setStyle(Paint.Style.FILL);

        float internal = ParaType.toDP(this, 5);
        linePaint = new Paint();
        linePaint.setColor(getResources().getColor(R.color.dark_gray));
        linePaint.setPathEffect(new DashPathEffect(new float[] {internal, internal}, 0));
        linePaint.setStrokeWidth(5f);

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
        if (data == null) return;

        int width = getWidth();
        int height = getHeight();
        int barWidth = Math.min(width / data.size(), 100);

        int maxValue = 0;
        for (int value : data)
        {
            if (value > maxValue) maxValue = value;
        }

        for (int i = 0; i < data.size(); i++)
        {
            int barHeight = (int) ((data.get(i) / (float) maxValue) * (height - 100));
            int left = i * barWidth;
            int top = height - barHeight;
            int right = left + barWidth - 20;
            int bottom = height;

            canvas.drawRect(left, top, right, bottom, barPaint);
            if(labels != null && labels.get(i) != null)
                canvas.drawText(labels.get(i), left + (barWidth / 2), height - 10, textPaint);
        }

        int standHeight = (int) ((100 / (float) maxValue) * (height - 100));
        canvas.drawLine(0, height - standHeight, width, height - standHeight, linePaint);
    }
}
