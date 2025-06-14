package com.ow0b.c7b9.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.midi.Midi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PianoRollView extends View implements RecordBackground
{

    private Paint notePaint, scrollBarPaint, recordPaint;

    private List<Note> notes = new ArrayList<>(); // 音符列表
    private int keyHeight = 2; // 每个音高的高度（像素）
    private float timeWidth = 30; // 每个时间单位的宽度（像素）
    private float minTimeWidth = 1; // 最小时间单位宽度
    private float maxTimeWidth = 400; // 最大时间单位宽度
    private float totalTime;

    private float offsetX = 0; // 当前水平滚动偏移
    private float lastTouchX = 0; // 上一次触摸位置
    private OverScroller scroller; // 用于实现惯性滚动
    private ScaleGestureDetector scaleGestureDetector; // 用于检测缩放手势
    private VelocityTracker velocityTracker; // 用于计算手势速度

    private int scrollBarHeight = 10; // 滚动条高度（像素）

    public PianoRollView(Context context)
    {
        super(context);
        init();
    }

    public PianoRollView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }
    public static ViewGroup getView(Context context, PianoRollView instance)
    {
        return getView(context, null, instance);
    }
    public static ViewGroup getView(Context context, String text, PianoRollView instance)
    {
        LinearLayout layout = new LinearLayout(context);
        int dp10 = ParaType.toDP(layout, 10);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(context.getDrawable(instance.colorRed ? R.drawable.bg_chart_red : R.drawable.bg_chart_gray));
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        {{
            bottomMargin = dp10;
            leftMargin = dp10;
        }});
        layout.setPadding(dp10, dp10, dp10, dp10);

        if (text != null)
        {
            TextView title = new TextView(context);
            title.setText("测试测试.mid");
            layout.addView(title);
        }
        layout.addView(instance);
        return layout;
    }

    private void init()
    {
        notePaint = new Paint();
        notePaint.setStyle(Paint.Style.FILL);

        scrollBarPaint = new Paint();
        scrollBarPaint.setStyle(Paint.Style.FILL);
        scrollBarPaint.setColor(Color.GRAY);

        recordPaint = new Paint();
        recordPaint.setColor(getResources().getColor(R.color.translucent_gray));

        // 初始化 Scroller
        scroller = new OverScroller(getContext());

        // 初始化手势检测器
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
    }
    private boolean colorRed = false;
    public void setColorRed(boolean value)
    {
        colorRed = value;
        if(colorRed) scrollBarPaint.setColor(getResources().getColor(R.color.light_red));
    }

    public void setMidi(Midi midi)
    {
        this.notes = new ArrayList<>();
        this.totalTime = midi.totalTime;
        for(int i = 0; i < midi.noteGroup.size(); i++)
        {
            HashSet<com.ow0b.c7b9.app.util.midi.Note> group = midi.noteGroup.get(i);
            for(com.ow0b.c7b9.app.util.midi.Note n : group)
            {
                this.notes.add(new Note(n.pitch, n.start, n.end - n.start, midi.indexes != null && midi.indexes.contains(i)));
            }
        }
        requestLayout(); // 重新计算布局
        invalidate(); // 重新绘制视图
    }
    float process = 0;
    @Override
    public void setProcess(float process)
    {
        this.process = process;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // 根据音符的最大时长计算宽度
        /*
        float maxEndTime = 0;
        for (Note note : notes)
        {
            float noteEndTime = note.startTime + note.duration;
            if (noteEndTime > maxEndTime)
            {
                maxEndTime = noteEndTime;
            }
        }
         */

        // 根据最大时长设置宽度
        int desiredWidth = (int) (totalTime * timeWidth); // 每个时间单位宽度动态调整
        int desiredHeight = 128 * keyHeight + scrollBarHeight; // MIDI 音高范围 + 滚动条高度

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        // 使用水平偏移量绘制音符
        canvas.translate(-offsetX, 0);
        canvas.drawRect(0, 0, process * totalTime * timeWidth, getHeight(), recordPaint);

        // 绘制音符
        for (Note note : notes)
        {
            float left = note.startTime * timeWidth; // 每个时间单位宽度动态调整
            float top = (127 - note.pitch) * keyHeight; // MIDI 音高范围：0-127
            float right = left + note.duration * timeWidth;
            float bottom = top + keyHeight;

            if(colorRed || note.colorRed) notePaint.setColor(getResources().getColor(R.color.light_red));
            else notePaint.setColor(getResources().getColor(R.color.dark_gray));
            canvas.drawRect(left, top, right, bottom, notePaint);
        }

        // 恢复画布状态
        canvas.translate(offsetX, 0);

        // 绘制滚动条
        drawScrollBar(canvas);
    }

    private void drawScrollBar(Canvas canvas)
    {
        float contentWidth = computeContentWidth();
        float viewWidth = getWidth();

        if (contentWidth > viewWidth)
        {
            float scrollBarWidth = viewWidth * (viewWidth / contentWidth);
            float scrollBarLeft = offsetX * (viewWidth / contentWidth);
            float scrollBarTop = getHeight() - scrollBarHeight;

            canvas.drawRect(scrollBarLeft, scrollBarTop, scrollBarLeft + scrollBarWidth, scrollBarTop + scrollBarHeight, scrollBarPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // 将触摸事件传递给 ScaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                // 初始化 VelocityTracker
                if (velocityTracker == null)
                {
                    velocityTracker = VelocityTracker.obtain();
                }
                else
                {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);

                // 停止当前滚动
                if (!scroller.isFinished())
                {
                    scroller.abortAnimation();
                }
                // 记录初始触摸位置
                lastTouchX = event.getX();
                return true;

            case MotionEvent.ACTION_MOVE:
                velocityTracker.addMovement(event);

                // 计算水平滑动距离
                float dx = lastTouchX - event.getX();
                lastTouchX = event.getX();

                // 更新水平偏移量
                offsetX += dx;

                // 限制偏移范围
                offsetX = Math.max(0, Math.min(offsetX, computeMaxScrollOffset()));

                // 重新绘制视图
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000); // 计算速度，单位为像素/秒
                float velocityX = velocityTracker.getXVelocity();

                // 惯性滚动
                scroller.fling(
                        (int) offsetX, 0, // 起始点
                        (int) -velocityX, 0, // 初速度
                        0, (int) computeMaxScrollOffset(), // 滚动范围
                        0, 0 // Y轴不滚动
                );

                // 回收 VelocityTracker
                if (velocityTracker != null)
                {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }

                invalidate(); // 开始惯性滚动
                return true;

            case MotionEvent.ACTION_CANCEL:
                // 回收 VelocityTracker
                if (velocityTracker != null)
                {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
        }
        return true;
    }

    @Override
    public void computeScroll()
    {
        if (scroller.computeScrollOffset())
        {
            // 更新滚动位置
            offsetX = scroller.getCurrX();
            invalidate();
        }
    }

    /**
     * 计算最大滚动偏移量
     */
    private float computeMaxScrollOffset()
    {
        float contentWidth = computeContentWidth();
        return Math.max(0, contentWidth - getWidth());
    }

    /**
     * 计算内容总宽度
     */
    private float computeContentWidth()
    {
        float maxEndTime = 0;
        for (Note note : notes)
        {
            float noteEndTime = note.startTime + note.duration;
            if (noteEndTime > maxEndTime)
            {
                maxEndTime = noteEndTime;
            }
        }
        return maxEndTime * timeWidth;
    }

    // 手势监听器，用于处理双指缩放
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            // 根据缩放比例调整 timeWidth
            timeWidth *= detector.getScaleFactor();

            // 限制 timeWidth 的最小值和最大值
            timeWidth = Math.max(minTimeWidth, Math.min(timeWidth, maxTimeWidth));

            // 更新偏移量以保持视图中心不变
            offsetX = Math.max(0, Math.min(offsetX, computeMaxScrollOffset()));

            // 请求重新布局和绘制
            requestLayout();
            invalidate();

            return true;
        }
    }

    /**
     * 音符类表示一个 MIDI 音符
     */
    public static class Note
    {
        public final int pitch; // 音高（MIDI 音符值：0-127）
        public final float startTime; // 开始时间（单位时间）
        public final float duration; // 持续时间（单位时间）
        public final boolean colorRed;

        public Note(int pitch, float startTime, float duration, boolean red)
        {
            this.pitch = pitch;
            this.startTime = startTime;
            this.duration = duration;
            this.colorRed = red;
        }
    }
}