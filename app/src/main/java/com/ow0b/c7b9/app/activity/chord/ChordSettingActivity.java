package com.ow0b.c7b9.app.activity.chord;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.ow0b.c7b9.app.databinding.ActivityToolChordSettingBinding;
import com.ow0b.c7b9.app.util.Toast;

public class ChordSettingActivity extends Activity
{
    private ActivityToolChordSettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityToolChordSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 加载已保存的设置
        SharedPreferences prefs = getSharedPreferences(ChordUtil.PREFS_NAME, MODE_PRIVATE);
        String intervalsStr = prefs.getString(ChordUtil.KEY_ALLOWED_INTERVALS, "大小三度,减增三度,倍增减三度,大小二度,纯四度");
        int numNotes = prefs.getInt(ChordUtil.KEY_NUM_NOTES, ChordUtil.DEFAULT_NUM_NOTES);
        binding.etNumNotes.setText(String.valueOf(numNotes));

        // 根据保存的 interval 字符串设置复选框状态
        binding.checkboxDaxiaosan.setChecked(intervalsStr.contains("大小三度"));
        binding.checkboxJianzengsan.setChecked(intervalsStr.contains("减增三度"));
        binding.checkboxBeizengjiansan.setChecked(intervalsStr.contains("倍增减三度"));
        binding.checkboxDaxiaoerdu.setChecked(intervalsStr.contains("大小二度"));
        binding.checkboxChunsidu.setChecked(intervalsStr.contains("纯四度"));

        binding.saveSettingsButton.setOnClickListener(v ->
        {
            StringBuilder sb = new StringBuilder();
            if (binding.checkboxDaxiaosan.isChecked()) sb.append("大小三度,");
            if (binding.checkboxJianzengsan.isChecked()) sb.append("减增三度,");
            if (binding.checkboxBeizengjiansan.isChecked()) sb.append("倍增减三度,");
            if (binding.checkboxDaxiaoerdu.isChecked()) sb.append("大小二度,");
            if (binding.checkboxChunsidu.isChecked()) sb.append("纯四度,");

            String intervals = sb.toString();
            if (intervals.endsWith(","))
            {
                intervals = intervals.substring(0, intervals.length()-1);
            }
            int numNotes1;
            try
            {
                numNotes1 = Integer.parseInt(binding.etNumNotes.getText().toString());
            }
            catch (NumberFormatException e)
            {
                Toast.showInfo(ChordSettingActivity.this,  "Invalid number of notes");
                return;
            }
            SharedPreferences.Editor editor = getSharedPreferences(ChordUtil.PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString(ChordUtil.KEY_ALLOWED_INTERVALS, intervals);
            editor.putInt(ChordUtil.KEY_NUM_NOTES, numNotes1);
            editor.apply();
            Toast.showInfo(ChordSettingActivity.this,  "Settings Saved");
            finish();
        });
    }
}