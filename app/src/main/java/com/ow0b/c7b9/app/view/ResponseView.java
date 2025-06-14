package com.ow0b.c7b9.app.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.markwon.MarkwonFactory;

import io.noties.markwon.Markwon;

public class ResponseView extends LinearLayout
{
    private final Markwon normal, syntax;
    private boolean defaultDivide = false;
    public ResponseView(Context context)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);
        setOrientation(HORIZONTAL);
        textView = getTextView(context, true, false);

        normal = MarkwonFactory.getNormalInstance(context);
        syntax = MarkwonFactory.getSyntaxInstance(context);
    }
    public ResponseView(Context context, String text)
    {
        this(context);
        rend(context, text);
    }
    public ResponseView(Context context, String text, boolean divide)
    {
        this(context);
        rend(context, text, defaultDivide = divide);
    }
    public TextView textView;

    public CharSequence getText()
    {
        return textView.getText();
    }
    public void rend(Context context, String text)
    {
        rend(context, text, defaultDivide);
    }
    public void rend(Context context, String text, boolean divide)
    {
        removeAllViews();
        if(divide)
        {
            addView(new View(context)
            {{
                setBackgroundColor(getResources().getColor(R.color.light_gray));
                setLayoutParams(new LayoutParams(ParaType.toDP(this, 2), LayoutParams.MATCH_PARENT)
                {{
                    leftMargin = ParaType.toDP(ResponseView.this, 5);
                }});
            }});
        }

        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(VERTICAL);
        this.addView(layout);

        boolean codeBlock = false;
        String[] responses = text.split("```");
        for(String str : responses)
        {
            TextView textView = getTextView(context, responses.length == 1, divide);
            if(codeBlock)
            {
                syntax.setMarkdown(textView, "```" + str + "```");
                int index = str.indexOf("\n");
                layout.addView(new CodeBlock(context, textView, index == -1 ? str : str.substring(0, index)));
            }
            else
            {
                normal.setMarkdown(textView, str);
                layout.addView(textView);
            }
            codeBlock = !codeBlock;
        }
    }
    private TextView getTextView(Context context, boolean selectable, boolean divide)
    {
        textView = new TextView(context);
        textView.setTextIsSelectable(selectable);
        textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if(divide)
        {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            textView.setTextColor(getResources().getColor(R.color.middle_gray));
            textView.setLineSpacing(0, 1.3f);
            int contentPadding = ParaType.toDP(this, 20);
            textView.setPadding(0, contentPadding, 0, contentPadding);
        }
        else
        {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            textView.setLineSpacing(8, 1.0f);
        }

        int padding = ParaType.toDP(this, 12),
                paddingLeft = ParaType.toDP(this, divide ? 10 : 20);
        textView.setPadding(paddingLeft, padding, padding, padding);
        return textView;
    }

    public static class CodeBlock extends LinearLayout
    {
        public CodeBlock(Context context, TextView textView, String prog)
        {
            super(context);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            layout.setMargins
                    (textView.getPaddingLeft(), textView.getPaddingTop(),
                    textView.getPaddingRight(), textView.getPaddingBottom());
            setLayoutParams(layout);
            setBackground(getResources().getDrawable(R.drawable.bg_chat));
            setOrientation(VERTICAL);

            int padding = ParaType.toDP(this, 5), padding2 = ParaType.toDP(this, 10);
            TextView program = new TextView(context);
            program.setPadding(padding, padding2, padding, 0);
            program.setText(prog);
            this.addView(program);

            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            scroll.setPadding(0, 0, 0, padding2);
            scroll.setHorizontalScrollBarEnabled(false);
            this.addView(scroll);


            textView.setPadding(padding, padding2, padding, padding2);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            scroll.addView(textView);
        }
    }
}
