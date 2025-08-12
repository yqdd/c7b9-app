package com.ow0b.c7b9.app.activity.metronome;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.ow0b.c7b9.app.R;

public class MetronomeView extends LinearLayout
{
    public EditText speed;
    public ImageButton speedUp, speedDown;

    public MetronomeView(Context context, int orientation)
    {
        super(context);
        //相当于设置为0dp，按照weight权重来排版
        if(orientation == LinearLayout.HORIZONTAL) setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        if(orientation == LinearLayout.VERTICAL) setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        View view = LayoutInflater.from(context).inflate(R.layout.item_metronome, this, false);
        addView(view);
        speed = view.findViewById(R.id.metronome_bpm);
        speedUp = view.findViewById(R.id.metronome_speed_up);
        speedDown = view.findViewById(R.id.metronome_speed_down);

        speedUp.setOnClickListener(v -> speed.setText(String.valueOf(getSpeed() + 10)));
        speedDown.setOnClickListener(v -> speed.setText(String.valueOf(Math.max(getSpeed() - 10, 10))));
    }
    public int getSpeed()
    {
        try
        {
            return Math.max(Integer.parseInt(speed.getText().toString()), 10);
        }
        catch (NumberFormatException e)
        {
            return 100;
        }
    }
}
