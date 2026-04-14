package com.ow0b.c7b9.app.activity.rhythm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.chord.ChordUtil;
import com.ow0b.c7b9.app.databinding.ActivityToolChordBinding;
import com.ow0b.c7b9.app.databinding.ActivityToolRhythmSettingBinding;

import java.util.List;

public class RhythmSettingActivity extends Activity
{
    private ActivityToolRhythmSettingBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView((binding = ActivityToolRhythmSettingBinding.inflate(getLayoutInflater())).getRoot());
        if(RhythmActivity.four) binding.enableRhythm4.setChecked(true);
        else binding.enableRhythm8.setChecked(true);
        binding.enableRhythm4.setOnClickListener(v -> RhythmActivity.four = true);
        binding.enableRhythm8.setOnClickListener(v -> RhythmActivity.four = false);
        binding.speed.setMax(100);
        binding.speed.setProgress((int) ((1 - (RhythmActivity.unitMillis - 50) / 600f) * 100));
        binding.speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                RhythmActivity.unitMillis = (int) ((1 - progress / 100f) * 600 + 50);
            }
        });

        for(int i = 0; i < binding.rhythm4Checkboxes.getChildCount(); i ++)
        {
            if(binding.rhythm4Checkboxes.getChildAt(i) instanceof ViewGroup vg)
            {
                for(int j = 0; j < vg.getChildCount(); j ++)
                {
                    if(vg.getChildAt(j) instanceof CheckBox cb)
                    {
                        List<Integer> l = RhythmUtil.getPoint4Opt(cb.getText().toString());
                        cb.setChecked(!l.isEmpty() && RhythmActivity.enabled4Rhythm.contains(l.get(0)));
                        cb.setOnClickListener(v ->
                        {
                            List<Integer> list = RhythmUtil.getPoint4Opt(cb.getText().toString());
                            if(cb.isChecked()) RhythmActivity.enabled4Rhythm.addAll(list);
                            else list.forEach(RhythmActivity.enabled4Rhythm::remove);
                        });
                    }
                }
            }
        }
        for(int i = 0; i < binding.rhythm8Checkboxes.getChildCount(); i ++)
        {
            if(binding.rhythm8Checkboxes.getChildAt(i) instanceof ViewGroup vg)
            {
                for(int j = 0; j < vg.getChildCount(); j ++)
                {
                    if(vg.getChildAt(j) instanceof CheckBox cb)
                    {
                        List<Integer> l = RhythmUtil.getPoint8Opt(cb.getText().toString());
                        cb.setChecked(!l.isEmpty() && RhythmActivity.enabled8Rhythm.contains(l.get(0)));
                        cb.setOnClickListener(v ->
                        {
                            List<Integer> list = RhythmUtil.getPoint8Opt(cb.getText().toString());
                            if(cb.isChecked()) RhythmActivity.enabled8Rhythm.addAll(list);
                            else list.forEach(RhythmActivity.enabled8Rhythm::remove);
                        });
                    }
                }
            }
        }
    }
}