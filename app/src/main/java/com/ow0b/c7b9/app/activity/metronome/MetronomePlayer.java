package com.ow0b.c7b9.app.activity.metronome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;
import android.view.View;

import com.ow0b.c7b9.app.R;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MetronomePlayer
{
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private static SoundPool soundPool;
    private static int sound;
    private static long last, lastInterval;
    private static int stream = -1;

    public static void init(Activity activity)
    {
        try
        {
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
            sound = soundPool.load(activity.getResources().getAssets().openFd("grand/tone (61).wav"), 1);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void play(long milli, View playView)
    {
        stop();
        executor.scheduleWithFixedDelay(() ->
        {
            stream = soundPool.play(sound, 1, 1, 0, 0, 1);
            last = SystemClock.uptimeMillis();

            playView.setBackgroundTintList(ColorStateList.valueOf(playView.getContext().getResources().getColor(R.color.white)));
            executor.schedule(() -> playView.setBackgroundTintList(ColorStateList.valueOf(playView.getContext().getResources().getColor(R.color.black))),
                    200, TimeUnit.MILLISECONDS);

        }, lastInterval - (SystemClock.uptimeMillis() - last), milli, TimeUnit.MILLISECONDS);
        lastInterval = milli;
    }
    public static void stop()
    {
        if(executor != null)
        {
            executor.shutdownNow();
            executor = Executors.newScheduledThreadPool(3);
            stream = -1;
        }
    }
    public static boolean isPlaying()
    {
        return stream != -1;
    }
}
