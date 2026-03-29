package com.ow0b.c7b9.app.activity.main.chat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatButton;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.midi.Midi;

public class MidiChartPlayerView extends LinearLayout
{
    public MidiChartPlayerView(Context context, String idStr, boolean refMidi)
    {
        super(context);
        int dp10 = ParaType.toDP(this, 10);
        setOrientation(LinearLayout.VERTICAL);
        setBackground(context.getDrawable(R.drawable.bg_chat));
        setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        {{
            bottomMargin = dp10;
            leftMargin = dp10;
        }});
        setPadding(dp10, dp10, dp10, dp10);
        AppCompatButton button = new AppCompatButton(context);
        button.setText("播放");
        addView(button);

        MidiChartView chart = new MidiChartView(context, idStr, refMidi);
        addView(chart);
        button.setOnClickListener(v ->
        {
            Midi midi = new Midi();
            midi.notes.addAll(chart.notes);
            Log.i("TAG", "MidiChartPlayerView: " + midi.notes);
            MidiPlayer.play((Activity) context, midi);
        });
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        invalidate();
    }
}
