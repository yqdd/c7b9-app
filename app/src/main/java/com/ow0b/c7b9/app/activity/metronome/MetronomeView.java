package com.ow0b.c7b9.app.activity.metronome;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;

import java.util.WeakHashMap;

public class MetronomeView extends LinearLayout
{
    public static WeakHashMap<MetronomeView, Integer> all = new WeakHashMap<>();
    public EditText speed, n1, n2;
    public ImageButton speedUp, speedDown;
    public TextView play;
    public View left, right;

    public MetronomeView(Context context, int orientation)
    {
        super(context);
        //相当于设置为0dp，按照weight权重来排版
        if(orientation == LinearLayout.HORIZONTAL) setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        if(orientation == LinearLayout.VERTICAL) setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        View view = LayoutInflater.from(context).inflate(R.layout.item_metronome, this, false);
        addView(view);
        speed = view.findViewById(R.id.metronome_bpm);
        n1 = view.findViewById(R.id.metronome_beat_n1);
        n2 = view.findViewById(R.id.metronome_beat_n2);
        speedUp = view.findViewById(R.id.metronome_speed_up);
        speedDown = view.findViewById(R.id.metronome_speed_down);
        play = view.findViewById(R.id.metronome_play);
        left = view.findViewById(R.id.metronome_play_left);
        right = view.findViewById(R.id.metronome_play_right);

        all.put(this, 0);
        speedUp.setOnClickListener(v -> speed.setText(String.valueOf(getSpeed() + 10)));
        speedDown.setOnClickListener(v -> speed.setText(String.valueOf(Math.max(getSpeed() - 10, 10))));
        play.setOnClickListener(v ->
        {
            if(play.getText().toString().equals("停止"))
            {
                play.setText("开始");
                play.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                MetronomePlayer.stop();
            }
            else if(context instanceof Activity activity)
            {
                setAllBackground(getResources().getColor(R.color.black));
                MetronomeView.all.keySet().forEach(m -> m.play.setText("开始"));
                try
                {
                    int n1 = Integer.parseInt(this.n1.getText().toString()),
                            n2 = Integer.parseInt(this.n2.getText().toString());
                    MetronomePlayer.play(activity, (int) (60d / getSpeed() * 1000), n1, n2, left, right);
                }
                catch (NumberFormatException e)
                {
                    MetronomePlayer.play(activity, (int) (60d / getSpeed() * 1000), left, right);
                }
                play.setText("停止");
            }
        });
        view.addOnLayoutChangeListener((view1, i, i1, i2, i3, i4, i5, i6, i7) ->
        {
            int visibility = view1.getWidth() < 550 ? View.GONE : View.VISIBLE;
            speedUp.setVisibility(visibility);
            speedDown.setVisibility(visibility);
            speed.requestLayout();
        });
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
    public static void setAllBackground(int color)
    {
        all.keySet().forEach(m ->
        {
            m.findViewById(R.id.metronome_play).setBackgroundTintList(ColorStateList.valueOf(color));
        });
    }
}
