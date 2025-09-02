package com.ow0b.c7b9.app.old.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ow0b.c7b9.app.old.activity.main.AudioPlayerImpl;

import java.util.HashSet;

public class AudioSeekBarListener implements SeekBar.OnSeekBarChangeListener
{
    private static final HashSet<Runnable> stops = new HashSet<>();
    private final Context context;
    private final TextView i18nTextView;
    private String audioFileName;

    public AudioSeekBarListener(Context context, String audioFileName, TextView i18nTextView, Runnable stop)
    {
        this.context = context;
        this.i18nTextView = i18nTextView;
        this.audioFileName = audioFileName;
        stops.add(stop);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b)
    {
        int seconds = (i / 1000) % 60;
        int minutes = (i / 1000) / 60;
        String secondText = seconds < 10 ? "0" + seconds : String.valueOf(seconds),
                minuteText = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
        //TODO updateAnalyzeView();
        if(i18nTextView != null && context instanceof Activity activity)
        {
            activity.runOnUiThread(() -> i18nTextView.setText(minuteText + ":" + secondText));
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
        AudioPlayerImpl.cancel();
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        if(AudioPlayerImpl.isPlaying())
        {
            AudioPlayerImpl.stopPlayAudio();
            AudioPlayerImpl.playAudio(context, audioFileName, seekBar, () ->
            {
                stops.forEach(Runnable::run);
                stops.clear();
            });
        }
        else
        {
            AudioPlayerImpl.stopPlayAudio();
            stops.forEach(Runnable::run);
            stops.clear();
        }
    }
}
