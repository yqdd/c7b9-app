package com.ow0b.c7b9.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ToolSelectionActivity extends AppCompatActivity
{
    private TextView pianoToolButton, leaderboardToolButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool);

        pianoToolButton = findViewById(R.id.piano_tool_button);
        leaderboardToolButton = findViewById(R.id.leaderboard_tool_button);

        pianoToolButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(ToolSelectionActivity.this, PianoToolActivity.class);
            startActivity(intent);
        });

        leaderboardToolButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(ToolSelectionActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });
    }
}
