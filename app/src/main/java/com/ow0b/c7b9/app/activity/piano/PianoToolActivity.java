package com.ow0b.c7b9.app.activity.piano;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.util.midi.Note;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class PianoToolActivity extends AppCompatActivity
{
    private final static String TAG = "PianoKeys";
    private ImageButton listButton, saveButton, reverbButton;
    private ListView recordList;
    private LinearLayout whitesContainer, blacksContainer;
    private SoundPool soundPool;
    private final int[] pianoSounds = new int[88];
    private final Integer[] pianoStreams = new Integer[88];
    private final ValueAnimator[] pianoAnims = new ValueAnimator[88];
    private final Button[] pianoButtons = new Button[88];
    private boolean reverb = false;

    private Note[] pianoNotes = new Note[88];
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
        startTime = System.currentTimeMillis();
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

        listButton = findViewById(R.id.piano_record_list_button);
        saveButton = findViewById(R.id.piano_record_save_button);
        reverbButton = findViewById(R.id.piano_record_reverb_button);
        whitesContainer = findViewById(R.id.piano_whites_container);
        blacksContainer = findViewById(R.id.piano_blacks_container);
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        recordList = findViewById(R.id.piano_record_list);
        recordList.setAdapter(pianoRecordAdapter = new PianoRecordAdapter(this));

        loadRecords();
        loadKeys();
        listButton.setOnClickListener(v ->
        {
            if(recordList.getVisibility() != View.GONE) recordList.setVisibility(View.GONE);
            else recordList.setVisibility(View.VISIBLE);
        });
        saveButton.setOnClickListener(v -> saveRecord());
        reverbButton.setOnClickListener(v ->
        {
            reverb = !reverb;
            if(!reverb)
            {
                for(int i = 0; i < 88; i ++)
                {
                    if(pianoStreams[i] != null)
                        stopKey(i, 100);
                }
                Toast.showInfo(this, "踏板已关闭");
            }
            else Toast.showInfo(this, "踏板已开启");
        });
    }

    private void loadKeys()
    {
        try
        {
            for(int i = 1; i <= 88; i ++)
            {
                pianoSounds[i - 1] = soundPool.load(getResources().getAssets().openFd("grand/tone (" + i + ").wav"), 1);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

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
                case MotionEvent.ACTION_DOWN -> startKey(i);
                case MotionEvent.ACTION_UP ->
                {
                    if(!reverb) stopKey(i);
                }
            }
            return false;
        });
    }
    private void startKey(int i)
    {
        if(pianoStreams[i] != null) soundPool.stop(pianoStreams[i]);
        if(pianoAnims[i] != null) pianoAnims[i].end();

        pianoStreams[i] = soundPool.play(pianoSounds[i], 1, 1, 0, 0, 1);
        pianoNotes[i] = new Note(getNoteName(i), i, (System.currentTimeMillis() - startTime) / 1000f, 127);
    }
    private void stopKey(int i)
    {
        stopKey(i, 500);
    }
    private void stopKey(int i, int millis)
    {
        @SuppressLint("Recycle")
        ValueAnimator anim = pianoAnims[i] = ValueAnimator.ofFloat(1, 0);
        anim.addUpdateListener(value ->
        {
            if(pianoStreams[i] != null)
            {
                soundPool.setVolume(pianoStreams[i], (float) value.getAnimatedValue(), (float) value.getAnimatedValue());
            }
        });
        anim.addListener(new AnimatorListenerAdapter()
        {

            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(pianoStreams[i] != null)
                {
                    soundPool.stop(pianoStreams[i]);
                    pianoStreams[i] = null;
                    pianoAnims[i] = null;
                }
            }
        });
        anim.setDuration(millis);
        anim.start();
        if(pianoNotes[i] != null)
        {
            pianoNotes[i].end = (System.currentTimeMillis() - startTime) / 1000f;
            midi.notes.add(pianoNotes[i]);
            pianoNotes[i] = null;
        }
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
        @SuppressLint("SimpleDateFormat")
        String name = new SimpleDateFormat("yy.MM.dd HH:mm:ss").format(Date.from(Instant.now()));
        try(Reader recordsReader = new InputStreamReader(openFileInput("pianoRecords.json"));
            Writer recordsWriter = new OutputStreamWriter(openFileOutput("pianoRecords.json", MODE_PRIVATE));
            Writer writer = new OutputStreamWriter(openFileOutput("records-" + name + ".json", MODE_PRIVATE)))
        {
            Gson gson = new Gson();
            gson.toJson(midi, writer);
            Toast.showInfo(this, "保存成功");
            String[] records = gson.fromJson(recordsReader, String[].class);
            gson.toJson(Stream.concat((records == null ? Stream.of() : Arrays.stream(records)), Stream.of(name)).toArray(), recordsWriter);
            refreshRecord();
            loadRecords();
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
    /*
    private void loadRecord(int index)
    {
        String name = pianoRecordAdapter.records.get(index).name;
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput("records-" + name + ".json"))))
        {
            Midi midi = new Gson().fromJson(reader, Midi.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
     */

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        soundPool.release();
        soundPool = null;
    }
}