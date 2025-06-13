package com.ow0b.c7b9.app;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class PianoToolActivity extends AppCompatActivity {

    private LinearLayout pianoKeysContainer;
    private SoundPool soundPool;
    private int[] pianoSounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piano_tool);

        pianoKeysContainer = findViewById(R.id.piano_keys_container);
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        pianoSounds = new int[7];

        /*
        pianoSounds[0] = soundPool.load(this, R.raw.piano_c, 1);
        pianoSounds[1] = soundPool.load(this, R.raw.piano_d, 1);
        pianoSounds[2] = soundPool.load(this, R.raw.piano_e, 1);
        pianoSounds[3] = soundPool.load(this, R.raw.piano_f, 1);
        pianoSounds[4] = soundPool.load(this, R.raw.piano_g, 1);
        pianoSounds[5] = soundPool.load(this, R.raw.piano_a, 1);
        pianoSounds[6] = soundPool.load(this, R.raw.piano_b, 1);
         */

        for (int i = 0; i < 7; i++) {
            Button pianoKey = new Button(this);
            pianoKey.setLayoutParams(new LinearLayout.LayoutParams(200, ViewGroup.LayoutParams.MATCH_PARENT));
            //pianoKey.setText("Key " + (i + 1));
            final int soundId = pianoSounds[i];
            pianoKey.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    soundPool.play(soundId, 1, 1, 0, 0, 1);
                }
            });
            pianoKeysContainer.addView(pianoKey);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPool.release();
        soundPool = null;
    }
}