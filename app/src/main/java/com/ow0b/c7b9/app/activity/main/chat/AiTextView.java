package com.ow0b.c7b9.app.activity.main.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.chord.ChordComposeActivity;
import com.ow0b.c7b9.app.activity.main.AudioPlayer;
import com.ow0b.c7b9.app.activity.main.AudioUtils;
import com.ow0b.c7b9.app.activity.metronome.MetronomeActivity;
import com.ow0b.c7b9.app.activity.piano.PianoToolActivity;
import com.ow0b.c7b9.app.activity.rhythm.RhythmActivity;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.markwon.MarkwonFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.noties.markwon.Markwon;

@SuppressLint("ViewConstructor")
public class AiTextView extends LinearLayout
{
    private final Markwon normal, syntax;
    private final boolean divide, enableXml;
    private final Context context;
    private final LinearLayout layout;
    private final ChatContextView chatContext;

    /// 调用ChatContextView方法创建该对象
    AiTextView(Context context, ChatContextView chatContext, boolean divide, boolean enableXml)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);
        setOrientation(HORIZONTAL);
        this.divide = divide;
        this.enableXml = enableXml;
        this.context = context;
        this.chatContext = chatContext;

        normal = MarkwonFactory.getNormalInstance(context);
        syntax = MarkwonFactory.getSyntaxInstance(context);
        if(divide)
        {
            super.addView(new View(context)
            {{
                setBackgroundColor(getResources().getColor(R.color.light_gray));
                setLayoutParams(new LayoutParams(ParaType.toDP(this, 2), LayoutParams.MATCH_PARENT)
                {{
                    leftMargin = ParaType.toDP(AiTextView.this, 5);
                }});
            }});
        }
        layout = new LinearLayout(context);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.setOrientation(VERTICAL);
        newBlock();
        super.addView(layout);
    }
    /// 应使用 layout.addView
    @Override
    @Deprecated
    public void addView(View child)
    {
        super.addView(child);
    }

    StringBuilder builder = new StringBuilder();
    LinearLayout block;
    public void append(String text)
    {
        builder.append(text);
        rend(builder.toString());
    }
    private void newBlock()
    {
        block = new LinearLayout(context);
        block.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        block.setOrientation(VERTICAL);
        int index = builder.toString().indexOf("/>" + 2);
        builder.delete(0, index == -1 ? builder.length() : index);
        layout.addView(block);
    }

    private void rend(String text)
    {
        block.removeAllViews();
        rendCodeBlock(text, str ->
        {
            if(enableXml)
                rendXmlBlock(str, doc ->
                {
                    Element ele = doc.getDocumentElement();
                    if(ele.getTagName().equals("locateAudio")) block.addView(new LocateAudioXmlBlock(ele));
                    else if(ele.getTagName().equals("intent")) block.addView(new IntentBlock(ele.getAttribute("activity")));
                    else if(ele.getTagName().equals("speedChart")) block.addView(new BarChartView(context, ele.getAttribute("id")));
                    else if(ele.getTagName().equals("forceChart")) block.addView(new LineChartView(context, ele.getAttribute("id"), LineChartView.Type.FORCES));
                    else if(ele.getTagName().equals("rhythmChart")) block.addView(new LineChartView(context, ele.getAttribute("id"), LineChartView.Type.RHYTHMS));
                    else if(ele.getTagName().equals("midiChart")) block.addView(new MidiChartPlayerView(context, ele.getAttribute("id"), false));
                    else if(ele.getTagName().equals("refMidiChart")) block.addView(new MidiChartPlayerView(context, ele.getAttribute("id"), true));
                    else Log.e("AiTextView", "rend: 未识别的tag名：" + ele.getTagName(), new RuntimeException());
                    newBlock();

                }, str2 -> block.addView(new TextBlock(str2)));
            else block.addView(new TextBlock(str));
        });
    }
    private void rendCodeBlock(String block, Consumer<String> other)
    {
        String[] blocks = block.split("```");
        if(block.isEmpty() || blocks.length % 2 == 1)
        {
            other.accept(block);
            return;
        }
        boolean codeBlock = false;
        for(String str : blocks)
        {
            if(codeBlock) this.block.addView(new CodeBlock("```" + str + "```"));
            else other.accept(str);
            codeBlock = !codeBlock;
        }
    }
    private void rendXmlBlock(String block, Consumer<Document> handle, Consumer<String> other)
    {
        if(!(block.contains("<") && block.contains("/>")))
        {
            other.accept(block);
            return;
        }
        try
        {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            while(block.contains("<") && block.contains("/>"))
            {
                int begin = block.indexOf("<"), end = block.indexOf("/>") + 2,
                        begin2 = block.substring(end).indexOf("<");
                if(begin > end) break;

                //先处理第一块文本
                String str = block.substring(0, begin);
                if(!str.replace("\n", "").replace(" ", "").isEmpty())
                    other.accept(str);
                //处理xml
                try
                {
                    Document doc = docBuilder.parse(new ByteArrayInputStream(block.substring(begin, end).getBytes()));
                    handle.accept(doc);
                }
                catch (SAXException ignore)
                {
                    this.block.addView(new ErrorChartView(context));
                }
                block = block.substring(end);
            }
            //处理末尾最后剩的一块文本
            other.accept(block);
        }
        catch (ParserConfigurationException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /// 避免使用Markdown还要调用指定 Markwon 对象，统一设置文本（具体使用哪个Markwon由实现类决定）
    public interface MarkdownText
    {
        void setText(String text);
    }
    public class TextBlock extends androidx.appcompat.widget.AppCompatTextView implements MarkdownText
    {
        public TextBlock()
        {
            super(context);
            setTextIsSelectable(true);
            setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            if(divide)
            {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                setTextColor(getResources().getColor(R.color.middle_gray));
                setLineSpacing(0, 1.3f);
                int contentPadding = ParaType.toDP(this, 20);
                setPadding(0, contentPadding, 0, contentPadding);
            }
            else
            {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                setLineSpacing(8, 1.0f);
            }

            int padding = ParaType.toDP(this, 12),
                    paddingLeft = ParaType.toDP(this, divide ? 10 : 20);
            setPadding(paddingLeft, padding, padding, padding);
        }
        public TextBlock(String text)
        {
            this();
            setText(text);
        }
        public void setText(String text)
        {
            normal.setMarkdown(this, text);
        }
        public void setSimpleText(String text)
        {
            super.setText(text);
        }
    }
    /// 格式：\<locateAudio rid="(资源id)" skip="(秒数位置)" tip="点我播放音频" /\>
    public class LocateAudioXmlBlock extends TextBlock
    {
        public LocateAudioXmlBlock(Element element)
        {
            try
            {
                int aid = Integer.parseInt(element.getAttribute("id"));
                float skip = element.getAttribute("skip").isEmpty() ? 0 : Float.parseFloat(element.getAttribute("skip"));
                String tip = element.getAttribute("tip").isEmpty() ? "点我播放音频" : element.getAttribute("tip");
                setPadding(getPaddingLeft(), ParaType.toDP(this, 10), getPaddingRight(), ParaType.toDP(this, 10));
                getPaint().setFakeBoldText(true);
                getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                setTextIsSelectable(false);
                setTypeface(Typeface.DEFAULT_BOLD);
                setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.light_red)));
                setSimpleText(">>> " + tip + " <<<");
                setOnClickListener(v ->
                {
                    Log.i("AudioPlayer", "LocateAudioXmlBlock: 点击播放音频" + aid);
                    chatContext.skipPlayAudio(context, aid, skip);
                });
            }
            catch (NumberFormatException ignore) {}
        }
    }
    @SuppressLint("ViewConstructor")
    public class CodeBlock extends LinearLayout implements MarkdownText
    {
        private final TextView textView = new TextBlock(), program;
        @SuppressLint("ClickableViewAccessibility")
        public CodeBlock()
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
            program = new TextView(context);
            program.setPadding(padding, padding2, padding, 0);
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
        public CodeBlock(String text)
        {
            this();
            setText(text);
        }
        public void setText(String text)
        {
            int progIndex = text.indexOf("\n");
            program.setText(progIndex == -1 ? text : text.substring(0, progIndex));
            syntax.setMarkdown(textView, text);
        }
    }
    public class IntentBlock extends LinearLayout
    {
        Button text;
        Intent intent;
        @SuppressLint("ClickableViewAccessibility")
        public IntentBlock()
        {
            super(context);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            int padding = ParaType.toDP(this, 3), padding2 = ParaType.toDP(this, 10);
            setLayoutParams(layout);
            setPadding(padding2, 0, 0,0);

            text = new MaterialButton(context);
            text.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(text);

            text.setOnClickListener(v ->
            {
                if(intent != null)
                    context.startActivity(intent);
            });
        }
        public IntentBlock(String intentEnum)
        {
            this();
            text.setText(intentEnum);
            switch (intentEnum)
            {
                case "钢琴窗" -> intent = new Intent(context, PianoToolActivity.class);
                case "节拍器" -> intent = new Intent(context, MetronomeActivity.class);
                case "和弦听辨" -> intent = new Intent(context, ChordComposeActivity.class);
                case "节奏听辨" -> intent = new Intent(context, RhythmActivity.class);
                default -> text.setText("不支持的窗口");
            }
        }
    }
}
