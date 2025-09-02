package com.ow0b.c7b9.app.activity.main.chat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;

public class ExpandableLayout extends LinearLayout
{
    private LinearLayout headerLayout;
    private LinearLayout contentLayout;
    private boolean expanded = true;
    private int collapsedHeight = 0;

    public ExpandableLayout(Context context)
    {
        super(context);
        init(context);
    }
    public ExpandableLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    private void init(Context context)
    {
        setOrientation(VERTICAL);
        setPadding(ParaType.toDP(this, 20), 0, 0, 0);

        // Header
        headerLayout = new LinearLayout(context);
        headerLayout.setOrientation(HORIZONTAL);
        headerLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        headerLayout.setOnClickListener(v -> toggle());

        // Example header view
        int headerPadding = ParaType.toDP(this, 6);
        TextView headerText = new TextView(context);
        headerText.setPadding(0, headerPadding, 0, headerPadding);
        headerText.setTextColor(getResources().getColor(R.color.middle_gray));
        headerText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        headerText.setText("展开面板");
        headerLayout.addView(headerText);

        // Content
        contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(VERTICAL);
        contentLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //contentLayout.setVisibility(GONE);

        addView(headerLayout);
        addView(contentLayout);
    }

    public void addComponent(View component)
    {
        contentLayout.addView(component);
    }

    public void setHeaderText(String text)
    {
        if (headerLayout.getChildCount() > 0 && headerLayout.getChildAt(0) instanceof TextView)
        {
            TextView tv = (TextView) headerLayout.getChildAt(0);
            tv.setText(text.replace("{", "\n{"));
        }
    }

    public void toggle()
    {
        if (expanded) collapse();
        else expand();
    }

    public void expand()
    {
        if (!expanded)
        {
            expanded = true;
            contentLayout.setVisibility(VISIBLE);
            animateHeight(contentLayout, 0, measureContentHeight());
        }
    }
    public void collapse()
    {
        if (expanded)
        {
            expanded = false;
            animateHeight(contentLayout, contentLayout.getHeight(), 0);
        }
    }

    private void animateHeight(final View view, int start, int end)
    {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.addUpdateListener(animation ->
        {
            int value = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = value;
            view.setLayoutParams(params);
            view.setVisibility(value == 0 ? GONE : VISIBLE);
        });
        animator.setDuration(300);
        animator.start();
    }

    private int measureContentHeight()
    {
        contentLayout.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        return contentLayout.getMeasuredHeight();
    }
}
