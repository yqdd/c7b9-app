package com.ow0b.c7b9.app.activity.main.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.MainActivity;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.util.midi.Note;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MidiChartView extends LinearLayout implements PlayProgressBackground
{
    private Paint notePaint, scrollBarPaint, recordPaint;

    private Midi midi, red;
    private float keyHeight = 4; // 每个音高的高度（像素）
    private float timeWidth = -1; // 每个时间单位的宽度（像素）
    private float minTimeWidth = 1; // 最小时间单位宽度
    private float maxTimeWidth = 400; // 最大时间单位宽度

    private float offsetX = 0; // 当前水平滚动偏移
    private float lastTouchX = 0; // 上一次触摸位置
    private OverScroller scroller; // 用于实现惯性滚动
    private ScaleGestureDetector scaleGestureDetector; // 用于检测缩放手势
    private VelocityTracker velocityTracker; // 用于计算手势速度

    private int scrollBarHeight = 10; // 滚动条高度（像素）

    public MidiChartView(Context context)
    {
        super(context);
        int dp10 = ParaType.toDP(this, 10);
        setOrientation(LinearLayout.VERTICAL);
        setBackground(context.getDrawable(R.drawable.bg_chat));
        setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        {{
            bottomMargin = dp10;
            leftMargin = dp10;
        }});
        setPadding(dp10, dp10, dp10, dp10);
        /*
        if (text != null)
        {
            TextView title = new TextView(context);
            title.setText("测试测试.mid");
            layout.addView(title);
        }
         */
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


        setMidi(new Midi()
        {{
            totalTime = 41;
            notes.add(new Note(null, 60, 0, 20) {{ end = 1; }});
            notes.add(new Note(null, 60, 1, 20) {{ end = 2; }});
            notes.add(new Note(null, 66, 2, 20) {{ end = 3; }});
            notes.add(new Note(null, 26, 35, 20) {{ end = 41; }});
            notes.add(new Note(null, 40, 40, 20) {{ end = 41; }});
        }});
        setRedMidi(new Midi()
        {{
            totalTime = 5;
            notes.add(new Note(null, 0, 0, 20) {{ end = 1; }});
            notes.add(new Note(null, 127, 1, 20) {{ end = 2; }});
        }});
    }

    private boolean colorRed = false;
    public void setColorRed(boolean value)
    {
        colorRed = value;
        if(colorRed) scrollBarPaint.setColor(getResources().getColor(R.color.light_red));
    }

    public void setMidi(Midi midi)
    {
        this.midi = midi;
        requestLayout(); // 重新计算布局
        invalidate(); // 重新绘制视图
    }
    public void setRedMidi(Midi midi)
    {
        this.red = midi;
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
        Log.i("TAG", "setMidi: " + timeWidth + " " + getWidth() + " " + horizontalPadding());
        // 根据最大时长设置宽度
        int desiredWidth = (int) (midi.totalTime * timeWidth); // 每个时间单位宽度动态调整
        float desiredHeight = 127 * keyHeight + scrollBarHeight + verticalPadding(); // MIDI 音高范围 + 滚动条高度

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize((int) desiredHeight, heightMeasureSpec);
        if(timeWidth <= 0) timeWidth = (width - getPaddingLeft() - horizontalPadding()) / midi.totalTime;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int pLeft = getPaddingLeft(), pRight = getPaddingRight(),
                pTop = getPaddingTop(), pBottom = getPaddingBottom();

        // 绘制滚动条（在clip之前）
        drawScrollBar(canvas);
        // 使用水平偏移量绘制音符
        //canvas.clipRect(pLeft, pTop, getWidth() - pRight, getHeight() - pBottom);
        canvas.translate(-offsetX, 0);
        canvas.drawRect(0, 0, process * midi.totalTime * timeWidth, getHeight(), recordPaint);

        // 绘制音符
        for (Note note : midi.notes)
        {
            float left = note.start * timeWidth + pLeft; // 每个时间单位宽度动态调整
            float top = (127 - note.pitch) * keyHeight + pTop; // MIDI 音高范围：0-127
            float right = left + (note.end - note.start) * timeWidth + pLeft;
            float bottom = top + keyHeight;

            notePaint.setColor(getResources().getColor(R.color.dark_gray));
            canvas.drawRect(left, top, right, bottom, notePaint);
        }
        if(red != null)
        {
            for (Note note : red.notes)
            {
                float left = note.start * timeWidth + pLeft; // 每个时间单位宽度动态调整
                float top = (127 - note.pitch) * keyHeight + pTop; // MIDI 音高范围：0-127
                float right = left + (note.end - note.start) * timeWidth + pLeft;
                float bottom = top + keyHeight;

                notePaint.setColor(getResources().getColor(R.color.light_red));
                canvas.drawRect(left, top, right, bottom, notePaint);
            }
        }

        // 恢复画布状态
        canvas.translate(offsetX, 0);
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
    private int horizontalPadding()
    {
        return getPaddingLeft() + getPaddingRight();
    }
    private int verticalPadding()
    {
        return getPaddingTop() + getPaddingBottom();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // 将触摸事件传递给 ScaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);
        if(getContext() instanceof MainActivity main)
            main.chatDisplayScroll.requestDisallowInterceptTouchEvent(true);

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                // 初始化 VelocityTracker
                if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
                else velocityTracker.clear();

                velocityTracker.addMovement(event);
                // 停止当前滚动
                if (!scroller.isFinished()) scroller.abortAnimation();
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
    /// 计算最大滚动偏移量
    private float computeMaxScrollOffset()
    {
        float contentWidth = computeContentWidth();
        return Math.max(0, contentWidth - (getWidth() - horizontalPadding() - getPaddingLeft()));
    }
    /// 计算内容总宽度
    private float computeContentWidth()
    {
        return (float) midi.notes.stream().mapToDouble(n -> n.end).max().orElse(0) * timeWidth;
    }

    // 手势监听器，用于处理双指缩放
    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            // 根据缩放比例调整 timeWidth
            float scale = detector.getScaleFactor();
            if (computeContentWidth() + horizontalPadding() > getWidth() - getPaddingLeft() || scale > 1)
            {
                timeWidth *= scale;
                //keyHeight *= detector.getScaleFactor();
                // 限制 timeWidth 的最小值和最大值
                timeWidth = Math.max(Math.max(minTimeWidth, (getWidth() - getPaddingLeft() - horizontalPadding()) / midi.totalTime),
                        Math.min(timeWidth, maxTimeWidth));
            }

            // 更新偏移量以保持视图中心不变
            //offsetX = Math.max(0, Math.min(offsetX, computeMaxScrollOffset()));
            // 请求重新布局和绘制
            requestLayout();
            invalidate();

            return true;
        }
    }
}