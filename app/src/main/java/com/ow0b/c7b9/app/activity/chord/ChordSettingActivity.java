package com.ow0b.c7b9.app.activity.chord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.android.material.internal.TextWatcherAdapter;
import com.ow0b.c7b9.app.activity.rhythm.RhythmActivity;
import com.ow0b.c7b9.app.databinding.ActivityToolChordSettingBinding;
import com.ow0b.c7b9.app.util.Toast;

public class ChordSettingActivity extends Activity
{
    private ActivityToolChordSettingBinding binding;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityToolChordSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.back.setOnClickListener(v -> finish());
        binding.etNumNotes.setText(String.valueOf(ChordUtil.numNotes));
        binding.etNumNotes.addTextChangedListener(new TextWatcherAdapter()
        {
            @Override
            public void afterTextChanged(Editable s)
            {
                try { ChordUtil.numNotes = Integer.parseInt(binding.etNumNotes.getText().toString()); }
                catch (NumberFormatException ignore) {}
            }
        });
        for(int i = 0; i < binding.intervalCheckboxes.getChildCount(); i ++)
        {
            if(binding.intervalCheckboxes.getChildAt(i) instanceof ViewGroup vg)
            {
                for(int j = 0; j < vg.getChildCount(); j ++)
                {
                    if(vg.getChildAt(j) instanceof CheckBox cb)
                    {
                        int index = getIntervalSemitones(cb.getText().toString());
                        cb.setChecked(ChordUtil.allowedInterval[index]);
                        cb.setOnClickListener(v ->
                        {
                            ChordUtil.allowedInterval[index] = !ChordUtil.allowedInterval[index];
                            cb.setChecked(ChordUtil.allowedInterval[index]);
                        });
                    }
                }
            }
        }
    }
    private int getIntervalSemitones(String intervalType)
    {
        return switch (intervalType)
        {
            case "小二度" -> 0;
            case "大二度" -> 1;
            case "小三度" -> 2;
            case "大三度" -> 3;
            case "纯四度" -> 4;
            case "增四/减五度" -> 5;
            case "纯五度" -> 6;
            case "小六度" -> 7;
            case "大六度" -> 8;
            case "小七度" -> 9;
            case "大七度" -> 10;
            case "纯八度" -> 11;
            default -> throw new RuntimeException(intervalType);
        };
    }
    @Override
    public void finish()
    {
        super.finish();
        ChordUtil.correctChord.clear();
    }
}