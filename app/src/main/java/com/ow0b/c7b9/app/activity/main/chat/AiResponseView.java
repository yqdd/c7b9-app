package com.ow0b.c7b9.app.activity.main.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.markwon.MarkwonFactory;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;

public class AiResponseView extends LinearLayout
{
    private final Markwon normal, syntax;
    private UserPromptView userPromptView;

    public AiResponseView(Context context, UserPromptView promptView)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);
        setOrientation(HORIZONTAL);
        userPromptView = promptView;
        textView = getTextView(context, false);

        normal = MarkwonFactory.getNormalInstance(context);
        syntax = MarkwonFactory.getSyntaxInstance(context, userPromptView);
    }
    public TextView textView;

    public CharSequence getText()
    {
        return textView.getText();
    }
    public void rend(Context context, String text)
    {
        rend(context, text, false);
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
                    leftMargin = ParaType.toDP(AiResponseView.this, 5);
                }});
            }});
        }
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(VERTICAL);
        this.addView(layout);

        rendBlockText(context, layout, text, divide);
    }
    private void rendBlockText(Context context, LinearLayout layout, String text, boolean divide)
    {
        boolean codeBlock = false;
        String[] responses = text.split("```");
        for(String str : responses)
        {
            TextView textView = getTextView(context, divide);
            if(codeBlock)
            {
                //渲染代码块
                syntax.setMarkdown(textView, "```" + str);      // + "```"
                int index = str.indexOf("\n");
                layout.addView(new CodeBlock(context, textView, index == -1 ? str : str.substring(0, index)));
            }
            else
            {
                while(str.contains("{") && str.contains("}"))
                {
                    TextView t1 = getTextView(context, divide);
                    int begin = str.indexOf("{"), end = str.indexOf("}") + 1,
                        begin2 = str.substring(end).indexOf("{");
                    //先添加第一块文本
                    normal.setMarkdown(t1, str.substring(0, begin));
                    layout.addView(t1);

                    //处理json
                    JsonObject json = JsonParser.parseString(str.substring(begin, end)).getAsJsonObject();
                    if(json.get("playAudio") != null)
                    {
                        TextView t2 = getTextView(context, divide);
                        int rid = json.get("playAudio").getAsInt();
                        float skip = json.get("skip").getAsFloat();
                        t2.setOnClickListener(v ->
                        {
                            ImageButton imageButton = userPromptView.getAudioView(rid).findViewById(R.id.chat_display_prompt_audio_button);
                            if(!userPromptView.skipPlayAudio(rid, skip, () -> imageButton.setImageResource(R.drawable.btn_stop_play_record_small)))
                                Toast.showError(getContext(), "音频不存在");
                        });
                        t2.setText(">>> 点我播放音频 >>>");
                        t2.setPadding(t2.getPaddingLeft(), ParaType.toDP(this, 8), getPaddingRight(), ParaType.toDP(this, 8));
                        t2.getPaint().setFakeBoldText(true);
                        t2.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                        t2.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.light_red)));
                        layout.addView(t2);
                    }
                    //添加第三块文本
                    if(begin2 != -1)
                    {
                        TextView t3 = getTextView(context, divide);
                        normal.setMarkdown(t3, str.substring(end, begin2));
                        layout.addView(t3);
                    }
                    str = str.substring(end);
                }
                normal.setMarkdown(textView, str);
                layout.addView(textView);
            }
            codeBlock = !codeBlock;
        }
    }
    private TextView getTextView(Context context, boolean divide)
    {
        textView = new TextView(context);
        textView.setTextIsSelectable(true);
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

    @SuppressLint("ViewConstructor")
    public static class CodeBlock extends LinearLayout
    {
        @SuppressLint("ClickableViewAccessibility")
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
            scroll.setOnTouchListener((v, event) ->
            {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            this.addView(scroll);


            textView.setPadding(padding, padding2, padding, padding2);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            scroll.addView(textView);
        }
    }
}
