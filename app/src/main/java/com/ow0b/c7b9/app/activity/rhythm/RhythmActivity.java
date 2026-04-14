package com.ow0b.c7b9.app.activity.rhythm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

// Import the MidiPlayer from the provided package
import com.ow0b.c7b9.app.activity.chord.ChordSettingActivity;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.databinding.ActivityToolRhythmBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RhythmActivity extends Activity
{
    private ActivityToolRhythmBinding binding;
    private Handler handler = new Handler();

    private String answer;
    public static List<Integer> rhythm = new ArrayList<>();
    public static Set<Integer> enabled4Rhythm = new HashSet<>(Set.of(1, 2, 3, 4, 5, 17));
    public static Set<Integer> enabled8Rhythm = new HashSet<>(Set.of(8, 9, 10, 19, 20));
    public static boolean four = true, next = true;
    public static int unitMillis = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityToolRhythmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.setting.setOnClickListener(v -> startActivity(new Intent(this, RhythmSettingActivity.class)));
        binding.back.setOnClickListener(v -> finish());
        binding.btnPlay.setOnClickListener(view ->
        {
            binding.btnPause.performClick();
            if(rhythm.isEmpty()) generateRhythm();
            if(next) binding.txtAnswer.setText("");
            playRhythm();
        });
        binding.btnShowAnswer.setOnClickListener(view ->
        {
            if(answer != null)
            {
                binding.txtAnswer.setText(answer);
                next = false;
            }
        });
        binding.btnNext.setOnClickListener(v -> generateRhythm());
        binding.btnPause.setOnClickListener(v ->
        {
            handler.removeCallbacksAndMessages(null);
            MidiPlayer.stopAllKeys();
        });
    }
    private void generateRhythm()
    {
        float[][] data = four ? RhythmUtil.point4RhythmsData : RhythmUtil.point8RhythmsData;
        Random random = new Random();
        rhythm.clear();
        next = true;
        StringBuilder answer = new StringBuilder();
        for(int k = 0; k < 4; k ++)
        {
            int surplus = 4;
            while(surplus > 0)
            {
                List<Integer> list = four ? new ArrayList<>(enabled4Rhythm) : new ArrayList<>(enabled8Rhythm);
                int index = list.get(random.nextInt(list.size()));
                float[] r = data[index];
                if(surplus - (int) r[14] < 0) continue;
                else surplus -= (int) r[14];
                rhythm.add(index);
                answer.append(RhythmUtil.nameOf(four, (int) r[15])).append("  ");
            }
            answer.append("\n");
        }
        this.answer = answer.toString();
    }
    private void playRhythm()
    {
        float[][] data = four ? RhythmUtil.point4RhythmsData : RhythmUtil.point8RhythmsData;
        int curMillis = 0;
        for(int i = 0; i < 4; i ++)
        {
            handler.postDelayed(() -> MidiPlayer.playKey(69), curMillis);
            curMillis += (four ? 8 : 12) * unitMillis;
        }
        handler.postDelayed(() -> MidiPlayer.stopKey(69, 50), curMillis);
        for(int index : rhythm)
        {
            for(int i = 1; i <= data[index][0]; i ++)
            {
                int finalI = i;
                handler.postDelayed(() ->
                {
                    if(data[index][finalI] > 0) MidiPlayer.playKey(57);
                    else MidiPlayer.stopKey(57, 50);
                }, curMillis);
                curMillis += (int) (Math.abs(data[index][i]) * unitMillis);
            }
        }
        handler.postDelayed(() -> MidiPlayer.stopKey(57, 50), curMillis);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        MidiPlayer.stopAllKeys();
    }
    @Override
    public void finish()
    {
        super.finish();
        handler.removeCallbacksAndMessages(null);
        MidiPlayer.stopAllKeys();
    }
}