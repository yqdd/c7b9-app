package com.ow0b.c7b9.app.view.chat.sub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.util.ParaType;

import java.util.List;

public class LineChartView extends View implements ProgressGround
{
    private Paint linePaint, pointPaint, progressPaint;
    private Paint textPaint;
    private List<Float> data1;
    private List<Float> data2;
    private String[] labels;
    private int phase, tail;

    public LineChartView(Context context)
    {
        super(context);
        int dp10 = ParaType.toDP(this, 10);
        setBackground(context.getDrawable(R.drawable.bg_chat));
        setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ParaType.toDP(this, 100))
        {{
            bottomMargin = dp10;
            leftMargin = dp10;
        }});
        setPadding(dp10, dp10, dp10, dp10);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5);
        /*
        pointPaint = new Paint();
        pointPaint.setColor(color);
        pointPaint.setStyle(Paint.Style.FILL);
         */
        progressPaint = new Paint();
        progressPaint.setColor(getResources().getColor(R.color.translucent_gray));

        textPaint = new Paint();
        textPaint.setColor(getResources().getColor(R.color.middle_gray));
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
    public LineChartView(Context context, List<Float> data1, List<Float> data2, int phase, int tail)
    {
        this(context);
        setData(data1, data2, phase, tail);
    }

    public void setData(List<Float> data1, List<Float> data2, int phase, int tail)
    {
        this.data1 = data1;
        this.data2 = data2;
        this.labels = new String[Math.max(data1.size(), data2.size())];
        //this.labels[0] = "过重";
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

        int fontOffset = ParaType.toDP(this, 10);
        int pLeft = getPaddingLeft(), pRight = getPaddingRight(),
                pTop = getPaddingTop(), pBottom = getPaddingBottom();
        int width = getWidth() - pLeft - pRight;
        int height = getHeight() - pTop - pBottom;

        canvas.drawRect(0, 0, getWidth() * process, getHeight(), progressPaint);

        float maxValue1 = data1.stream().max(Float::compareTo).orElse(0f),
                maxValue2 = data2.stream().max(Float::compareTo).orElse(0f);
        float max = Math.max(maxValue1, maxValue2);

        Path path1 = new Path();
        int sum1 = data1.size() + phase + tail;
        for (int i = 0; i < sum1; i++)
        {
            int x = i * (width / (sum1 - 1)) + pLeft;
            int y = (i >= phase && i - phase < data1.size() ? (int) (height - (data1.get(i - phase) / max) * (height - 5)) : height) + pTop;
            if (i == 0) path1.moveTo(x, y);
            else path1.lineTo(x, y);
        }


        Path path2 = new Path();
        int widthClip = width - (phase + tail) * (width / (sum1 - 1));
        for (int i = 0; i < data2.size(); i++)
        {
            int x = phase * (width / (sum1 - 1)) + i * (widthClip / (data2.size() - 1)) + pLeft;
            int y = (int) (height - (data2.get(i) / max) * (height - 5)) + pTop;

            if (i == 0) path2.moveTo(x, y);
            else path2.lineTo(x, y);
            //绘制备注内容
            if(labels[i] != null) canvas.drawText(labels[i], x, height, textPaint);
        }

        linePaint.setColor(getResources().getColor(R.color.gray));
        canvas.drawPath(path1, linePaint);
        linePaint.setColor(getResources().getColor(R.color.middle_gray));
        canvas.drawPath(path2, linePaint);
    }
}