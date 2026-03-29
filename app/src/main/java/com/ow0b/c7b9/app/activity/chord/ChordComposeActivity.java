package com.ow0b.c7b9.app.activity.chord;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.databinding.ActivityToolChordBinding;
import com.ow0b.c7b9.app.util.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChordComposeActivity extends Activity
{
    private ActivityToolChordBinding binding;
    private Handler handler = new Handler();
    private List<Integer> userChord = new ArrayList<>();
    private boolean rootSelected = false;
    
    // 12 个音按钮名称（对应 C, C#, D, ... B）
    private String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityToolChordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MidiPlayer.init(this);

        binding.playChordButton.setOnClickListener(v -> playChord());

        // 设置 12 个根音按钮
        for (int i = 0; i < 12; i++)
        {
            final int noteIndex = i;
            Button btn = new Button(this);
            btn.setText(noteNames[i]);
            btn.setOnClickListener(v ->
            {
                if (!rootSelected)
                {
                    // 设定根音，映射为 60 + noteIndex（确保与播放页面的和弦生成一致）
                    int root = 60 + noteIndex;
                    userChord.add(root);
                    rootSelected = true;
                    updateChordDisplay();
                    Toast.showInfo(ChordComposeActivity.this, "Root note selected: " + noteNames[noteIndex]);
                }
                else Toast.showInfo(ChordComposeActivity.this, "Root already selected");
            });
            binding.noteButtonsContainer.addView(btn);
        }
        
        // 从 SharedPreferences 中获取允许的音程，并动态创建相应的间隔按钮
        SharedPreferences prefs = getSharedPreferences(ChordUtil.PREFS_NAME, MODE_PRIVATE);
        String intervalsStr = prefs.getString(ChordUtil.KEY_ALLOWED_INTERVALS, "大小三度,减增三度,倍增减三度,大小二度,纯四度");
        final String[] allowedIntervals = intervalsStr.split(",");
        for (String interval : allowedIntervals)
        {
            final String trimmedInterval = interval.trim();
            Button btn = new Button(this);
            btn.setText(trimmedInterval);
            btn.setOnClickListener(v ->
            {
                if (!rootSelected)
                {
                    Toast.showInfo(ChordComposeActivity.this, "Please select a root note first");
                    return;
                }
                // 使用与 ChordUtil 中相同的映射规则
                int semitones = getIntervalSemitones(trimmedInterval, new Random());
                int lastNote = userChord.get(userChord.size()-1);
                int nextNote = lastNote + semitones;
                if (nextNote > 87) {
                    nextNote = 87;
                }
                userChord.add(nextNote);
                updateChordDisplay();
            });
            binding.intervalButtonsContainer.addView(btn);
        }
        
        binding.submitChordButton.setOnClickListener(v ->
        {
            int expectedNotes = getExpectedNumNotes();
            if (userChord.size() != expectedNotes)
            {
                Toast.showInfo(ChordComposeActivity.this, "Chord must have " + expectedNotes + " notes.");
                return;
            }
            // 判断用户拼合的和弦是否与播放的和弦一致
            if (userChord.equals(ChordUtil.correctChord))
                Toast.showInfo(ChordComposeActivity.this, "Correct chord!");
            else Toast.showInfo(ChordComposeActivity.this, "Incorrect chord.");
        });
    }
    
    private void updateChordDisplay()
    {
        StringBuilder sb = new StringBuilder();
        for (int note : userChord)
            sb.append(note).append(" ");

        binding.chordDisplay.setText(sb.toString());
    }
    
    private int getExpectedNumNotes()
    {
        SharedPreferences prefs = getSharedPreferences(ChordUtil.PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(ChordUtil.KEY_NUM_NOTES, ChordUtil.DEFAULT_NUM_NOTES);
    }
    
    // 与 ChordUtil 中的音程映射保持一致
    private int getIntervalSemitones(String intervalType, Random random)
    {
        return switch (intervalType)
        {
            case "大小二度" -> random.nextBoolean() ? 1 : 2;
            case "大小三度" -> random.nextBoolean() ? 3 : 4;
            case "减增三度" -> random.nextBoolean() ? 2 : 5;
            case "倍增减三度" -> 6;
            case "纯四度" -> 5;
            default -> 3;
        };
    }

    /// 根据设置生成和弦，并依次播放和弦中的每个音（延时 300ms 播放下一个音）。
    private void playChord()
    {
        List<Integer> chord = ChordUtil.generateChord(this);
        android.widget.Toast.makeText(this, "Playing chord...", android.widget.Toast.LENGTH_SHORT).show();
        int delay = 0;
        for (final int note : chord)
        {
            handler.postDelayed(() -> MidiPlayer.playKey(note), delay);
            delay += 300;
        }
    }
}