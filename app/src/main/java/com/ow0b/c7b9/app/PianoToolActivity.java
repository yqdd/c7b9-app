package com.ow0b.c7b9.app;

import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.view.PianoRollView;

public class PianoToolActivity extends AppCompatActivity
{
    private LinearLayout whitesContainer, blacksContainer;
    private SoundPool soundPool;
    private int[] pianoSounds;

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

        whitesContainer = findViewById(R.id.piano_whites_container);
        blacksContainer = findViewById(R.id.piano_blacks_container);
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        pianoSounds = new int[88];

        /*
        pianoSounds[0] = soundPool.load(this, R.raw.piano_c, 1);
        pianoSounds[1] = soundPool.load(this, R.raw.piano_d, 1);
        pianoSounds[2] = soundPool.load(this, R.raw.piano_e, 1);
        pianoSounds[3] = soundPool.load(this, R.raw.piano_f, 1);
        pianoSounds[4] = soundPool.load(this, R.raw.piano_g, 1);
        pianoSounds[5] = soundPool.load(this, R.raw.piano_a, 1);
        pianoSounds[6] = soundPool.load(this, R.raw.piano_b, 1);
         */

        for (int i = 0; i < 88; i++)
        {
            Button pianoKey = new Button(this);
            pianoKey.setLayoutParams(new LinearLayout.LayoutParams(ParaType.toDP(pianoKey, 80), ViewGroup.LayoutParams.MATCH_PARENT));
            //pianoKey.setText("Key " + (i + 1));
            final int soundId = pianoSounds[i];
            pianoKey.setOnClickListener(v -> soundPool.play(soundId, 1, 1, 0, 0, 1));
            pianoKey.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.gray)));
            whitesContainer.addView(pianoKey);
        }

        ((LinearLayout.LayoutParams) blacksContainer.getLayoutParams()).leftMargin = ParaType.toDP(PianoToolActivity.this.blacksContainer, 40);
        for (int i = 0; i < 88; i++)
        {
            boolean internal = (i % 7 == 2 || i % 7 == 6);
            View pianoKey = internal ? new View(this) : new Button(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ParaType.toDP(pianoKey, 64), ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.leftMargin = layoutParams.rightMargin = ParaType.toDP(pianoKey, 8);
            pianoKey.setLayoutParams(layoutParams);

            //pianoKey.setText("Key " + (i + 1));
            int soundId = pianoSounds[i];
            if(!internal) pianoKey.setOnClickListener(v -> soundPool.play(soundId, 1, 1, 0, 0, 1));
            pianoKey.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.black)));
            blacksContainer.addView(pianoKey);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        soundPool.release();
        soundPool = null;
    }
}