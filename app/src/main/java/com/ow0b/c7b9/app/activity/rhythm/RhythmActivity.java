package com.ow0b.c7b9.app.activity.rhythm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// Import the MidiPlayer from the provided package
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;

public class RhythmActivity extends Activity
{

    private Button btnPlay;
    private Button btnShowAnswer;
    private Button btnPause;
    private TextView txtAnswer;
    private Handler handler = new Handler();

    // For demonstration, we hardcode the answer as "4/4"
    private String answer = "4/4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_rhythm);

        btnPlay = findViewById(R.id.btnPlay);
        btnShowAnswer = findViewById(R.id.btnShowAnswer);
        btnPause = findViewById(R.id.btnPause);
        txtAnswer = findViewById(R.id.txtAnswer);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Clear answer text
                txtAnswer.setText("");

                // Play standard beat immediately using i = 61
                MidiPlayer.playKey(61);

                // After a short delay (e.g., 500 ms), play rhythm note using i = 49
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MidiPlayer.playKey(49);
                    }
                }, 500);

                // Here, you can determine the answer during playback; we set a dummy answer
                answer = "4/4";
            }
        });

        btnShowAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtAnswer.setText("Answer: " + answer);
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MidiPlayer.stop();
            }
        });
    }
}