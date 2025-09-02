package com.ow0b.c7b9.app.old.activity.piano;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.activity.metronome.MetronomeActivity;
import com.ow0b.c7b9.app.old.util.ParaType;
import com.ow0b.c7b9.app.old.util.Toast;
import com.ow0b.c7b9.app.old.util.midi.Midi;
import com.ow0b.c7b9.app.old.util.midi.Note;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class PianoToolActivity extends AppCompatActivity
{
    private final static String TAG = "PianoKeys";
    private ImageButton reverbButton;
    private LinearLayout listButton, saveButton, metronomeButton;
    private HorizontalScrollView scroll;
    private ListView recordList;
    private LinearLayout whitesContainer, blacksContainer;
    private final Button[] pianoButtons = new Button[88];

    private final Note[] pianoNotes = new Note[88];
    private long startTime;
    private Midi midi = new Midi();


    private PianoRecordAdapter pianoRecordAdapter;

    @Override
    protected void onResume()
    {
        super.onResume();
        refreshRecord();
    }
    private void refreshRecord()
    {
        startTime = SystemClock.uptimeMillis();
        midi = new Midi();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        try
        {
            //设置布局延伸到刘海屏内，否则小米手机顶部导航栏显示黑色。
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        catch (Exception e)
        {
            Log.e("TAG", "onCreate: ", e);
        }
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);       // 隐藏标题栏

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_piano);

        scroll = findViewById(R.id.piano_container_scroll);
        listButton = findViewById(R.id.piano_record_list_button);
        saveButton = findViewById(R.id.piano_record_save_button);
        metronomeButton = findViewById(R.id.piano_record_metronome_button);
        reverbButton = findViewById(R.id.piano_record_reverb_button);
        whitesContainer = findViewById(R.id.piano_whites_container);
        blacksContainer = findViewById(R.id.piano_blacks_container);
        recordList = findViewById(R.id.piano_record_list);
        recordList.setAdapter(pianoRecordAdapter = new PianoRecordAdapter(this));
        //滚动到中间位置
        scroll.post(() -> scroll.scrollTo((scroll.getChildAt(0).getWidth() - scroll.getWidth()) / 2, 0));

        loadRecords();
        loadKeys();
        listButton.setOnClickListener(v ->
        {
            if(recordList.getVisibility() != View.GONE) recordList.setVisibility(View.GONE);
            else recordList.setVisibility(View.VISIBLE);
        });
        saveButton.setOnClickListener(v -> saveRecord());
        metronomeButton.setOnClickListener(v -> startActivity(new Intent(PianoToolActivity.this, MetronomeActivity.class)));
        reverbButton.setOnClickListener(v ->
        {
            MidiPlayer.reverb = !MidiPlayer.reverb;
            if(!MidiPlayer.reverb)
            {
                MidiPlayer.stopAllKeys();
                Toast.showInfo(this, "踏板已关闭");
            }
            else Toast.showInfo(this, "踏板已开启");
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void loadKeys()
    {
        int[] whites = new int[] {1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1},
                blackOffset = new int[] {0, 0, 1, 1, 2, 2, 2, 3, 3, 4, 4, 5};

        //TODO 两个container合并一下避免按黑键白键不响应
        ((LinearLayout.LayoutParams) blacksContainer.getLayoutParams()).leftMargin = ParaType.toDP(PianoToolActivity.this.blacksContainer, 40);
        for (int i = 0; i < 88; i ++)
        {
            int j = (i + 9) % 12;
            if(whites[j] == 1)
            {
                Button pianoKey = pianoButtons[i] = new Button(this);
                pianoKey.setLayoutParams(new LinearLayout.LayoutParams(ParaType.toDP(pianoKey, 60), ViewGroup.LayoutParams.MATCH_PARENT));
                registerKey(pianoKey, i);
                pianoKey.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.gray)));
                whitesContainer.addView(pianoKey);

                //绘制黑键
                if(i < 88 - 1)
                {
                    boolean internal = ((j - blackOffset[j]) % 7 == 2 || (j - blackOffset[j]) % 7 == 6);
                    View blackKey = internal ? new View(this) : new Button(this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ParaType.toDP(blackKey, 48), ViewGroup.LayoutParams.MATCH_PARENT);
                    layoutParams.leftMargin = layoutParams.rightMargin = ParaType.toDP(blackKey, 6);
                    blackKey.setLayoutParams(layoutParams);
                    blackKey.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.black)));

                    if(!internal)
                    {
                        pianoButtons[i + 1] = (Button) blackKey;
                        registerKey(blackKey, i + 1);
                    }
                    blacksContainer.addView(blackKey);
                }
            }
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private void registerKey(View pianoKey, int i)
    {
        pianoKey.setOnTouchListener((v, event) ->
        {
            switch (event.getAction() == MotionEvent.ACTION_CANCEL ? MotionEvent.ACTION_UP : event.getAction())
            {
                case MotionEvent.ACTION_DOWN ->
                {
                    MidiPlayer.playKey(i);
                    pianoNotes[i] = new Note(getNoteName(i), i, (SystemClock.uptimeMillis() - startTime) / 1000f, 127);
                }
                case MotionEvent.ACTION_UP ->
                {
                    if(!MidiPlayer.reverb) MidiPlayer.stopKey(i);
                    if(pianoNotes[i] != null)
                    {
                        pianoNotes[i].end = Math.max((SystemClock.uptimeMillis() - startTime) / 1000f, pianoNotes[i].start + 0.1f);
                        midi.notes.add(pianoNotes[i]);
                        pianoNotes[i] = null;
                    }
                }
            }
            return false;
        });
    }


    private static String getNoteName(int note)
    {
        String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (note / 12) - 1;   //计算八度数
        int noteInOctave = note % 12;   //获取八度内的音符位置
        return notes[noteInOctave] + octave;
    }
    private void saveRecord()
    {
        //删掉按下音符前空出的片段
        float min = (float) midi.notes.stream().mapToDouble(n -> n.start).min().orElse(0);
        midi.notes.forEach(n ->
        {
            n.start -= min;
            n.end -= min;
        });

        @SuppressLint("SimpleDateFormat")
        String name = new SimpleDateFormat("yy.MM.dd HH:mm:ss").format(Date.from(Instant.now()));
        midi.name = name;
        try(Writer recordsWriter = new OutputStreamWriter(openFileOutput("pianoRecords.json", MODE_PRIVATE));
            Writer writer = new OutputStreamWriter(openFileOutput("records-" + name + ".json", MODE_PRIVATE)))
        {
            Gson gson = new Gson();
            gson.toJson(midi, writer);
            Toast.showInfo(this, "保存成功");
            pianoRecordAdapter.records.add(0, new PianoRecord(name));
            pianoRecordAdapter.notifyDataSetChanged();
            recordsWriter.write(gson.toJson(pianoRecordAdapter.records.stream().map(r -> r.name).toArray()));
            refreshRecord();
            //loadRecords();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    private void loadRecords()
    {
        try(Reader reader = new InputStreamReader(openFileInput("pianoRecords.json")))
        {
            String[] records = new Gson().fromJson(reader, String[].class);
            if(records != null)
            {
                for(String r : records)
                {
                    pianoRecordAdapter.records.add(new PianoRecord(r));
                }
            }
        }
        catch (FileNotFoundException e)
        {
            try(Writer writer = new OutputStreamWriter(openFileOutput("pianoRecords.json", MODE_PRIVATE)))
            {
                new Gson().toJson(new String[0], writer);
            }
            catch (IOException e2)
            {
                throw new RuntimeException(e2);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MidiPlayer.stop();
    }
}