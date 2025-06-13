package com.ow0b.c7b9.app.view;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;

public class PromptView extends ConstraintLayout
{
    public PromptView(@NonNull Context context, String text)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);

        TextView textView = new TextView(context);
        textView.setBackground(getResources().getDrawable(R.drawable.chat_background));
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setTextIsSelectable(true);

        int padding1 = ParaType.toDP(this, 14), padding2 = ParaType.toDP(this, 10);
        textView.setPadding(padding1, padding2, padding1, padding2);

        ConstraintLayout.LayoutParams constraint = new Constraints.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        constraint.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        constraint.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        addView(textView, constraint);
    }
}
