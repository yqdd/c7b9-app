package com.ow0b.c7b9.app.activity.rhythm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ow0b.c7b9.app.R;

public class RhythmSettingActivity extends Activity
{
    private RadioGroup radioGroup;
    private Button btnSave;
    private static final String PREFS_NAME = "RhythmPrefs";
    private static final String KEY_RHYTHM_PATTERN = "rhythm_pattern";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_rhythm_setting);

        radioGroup = findViewById(R.id.radioGroupRhythm);
        btnSave = findViewById(R.id.btnSave);

        // Load saved rhythm pattern from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedPattern = prefs.getInt(KEY_RHYTHM_PATTERN, 0); // default pattern 0

        // Set the radio button selection based on the saved value
        switch(savedPattern) {
            case 0:
                radioGroup.check(R.id.radioPattern0);
                break;
            case 1:
                radioGroup.check(R.id.radioPattern1);
                break;
            case 2:
                radioGroup.check(R.id.radioPattern2);
                break;
            default:
                break;
        }

        btnSave.setOnClickListener(v ->
        {
            int checkedId = radioGroup.getCheckedRadioButtonId();
            int pattern = 0;
            if(checkedId == R.id.radioPattern0) {
                pattern = 0;
            } else if(checkedId == R.id.radioPattern1) {
                pattern = 1;
            } else if(checkedId == R.id.radioPattern2) {
                pattern = 2;
            }
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putInt(KEY_RHYTHM_PATTERN, pattern);
            editor.apply();
            Toast.makeText(RhythmSettingActivity.this, "Rhythm pattern saved", Toast.LENGTH_SHORT).show();
        });
    }
}