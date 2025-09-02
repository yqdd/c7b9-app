package com.ow0b.c7b9.app.view.chat.sub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.util.ParaType;

import java.util.List;

public class BarChartView extends View implements ProgressGround
{
    private Paint barPaint, linePaint, progressPaint;
    private Paint textPaint;
    private List<Integer> data;
    private List<String> labels;
    public BarChartView(Context context)
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
    public BarChartView(Context context, List<Integer> data)
    {
        this(context);
        setData(data);
    }

    public static ViewGroup getView(Context context, BarChartView instance)
    {
        FrameLayout frame = new FrameLayout(context);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ParaType.toDP(frame, 100));
        layout.bottomMargin = ParaType.toDP(frame, 10);
        layout.leftMargin = ParaType.toDP(frame, 10);
        frame.setLayoutParams(layout);
        frame.setBackground(frame.getResources().getDrawable(R.drawable.bg_chart_gray));

        int padding = ParaType.toDP(frame, 10);
        frame.setPadding(padding, padding, padding, padding);

        instance.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frame.addView(instance);
        return frame;
    }

    public void setData(List<Integer> data)
    {
        this.data = data;
        //this.labels = labels;
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
        if (data == null) return;

        int pLeft = getPaddingLeft(), pRight = getPaddingRight(),
                pTop = getPaddingTop(), pBottom = getPaddingBottom();
        int width = getWidth() - pLeft - pRight;
        int height = getHeight() - pTop - pBottom;
        int barWidth = Math.min(width / data.size(), 100);

        canvas.drawRect(0, 0, getWidth() * process, getHeight(), progressPaint);

        int maxValue = 0;
        for (int value : data)
        {
            if (value > maxValue) maxValue = value;
        }

        for (int i = 0; i < data.size(); i++)
        {
            int barHeight = (int) ((data.get(i) / (float) maxValue) * (height - 100));
            int left = i * barWidth + pLeft;
            int top = height - barHeight + pTop;
            int right = left + barWidth - 20;
            int bottom = height + pTop;

            canvas.drawRect(left, top, right, bottom, barPaint);
            if(labels != null && labels.get(i) != null)
                canvas.drawText(labels.get(i), left + (barWidth / 2), height - 10, textPaint);
        }

        int standHeight = (int) ((100 / (float) maxValue) * (height - 100));
        canvas.drawLine(pLeft, height - standHeight + pTop, width, height - standHeight + pTop, linePaint);
    }
}
