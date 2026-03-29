package com.ow0b.c7b9.app.activity.metronome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.ow0b.c7b9.app.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MetronomePlayer
{
    private static final String TAG = "MetronomePlayer";
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private static SoundPool soundPool;
    private static int sound1, sound2;
    private static long last, lastInterval;
    private static int stream1 = -1, stream2 = -1;
    private static WeakReference<View> left, right;

    public static void init(Activity activity)
    {
        try
        {
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
            sound1 = soundPool.load(activity.getResources().getAssets().openFd("metronome1.mp3"), 1);
            sound2 = soundPool.load(activity.getResources().getAssets().openFd("metronome2.mp3"), 1);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void play(Activity activity, long milli, View left, View right)
    {
        stop();
        MetronomePlayer.left = new WeakReference<>(left);
        MetronomePlayer.right = new WeakReference<>(right);
        executor.scheduleWithFixedDelay(() ->
        {
            stream1 = soundPool.play(sound1, 1, 1, 0, 0, 1);
            last = SystemClock.uptimeMillis();
            activity.runOnUiThread(() ->
            {
                left.setVisibility(View.VISIBLE);
                right.setVisibility(View.VISIBLE);
            });
            executor.schedule(() ->
            {
                activity.runOnUiThread(() ->
                {
                    left.setVisibility(View.INVISIBLE);
                    right.setVisibility(View.INVISIBLE);
                });
            }, milli / 2, TimeUnit.MILLISECONDS);

        }, lastInterval - (SystemClock.uptimeMillis() - last), milli, TimeUnit.MILLISECONDS);
        lastInterval = milli;
    }
    public static void play(Activity activity, long milli, int n1, int n2, View left, View right)
    {
        stop();
        MetronomePlayer.left = new WeakReference<>(left);
        MetronomePlayer.right = new WeakReference<>(right);
        long m = milli / (n1 + n2);
        int[] count = new int[1];
        executor.scheduleWithFixedDelay(() ->
        {
            last = SystemClock.uptimeMillis();
            if(count[0] % n2 == 0)
            {
                stream1 = soundPool.play(sound1, 1, 1, 0, 0, 1);
                activity.runOnUiThread(() -> left.setVisibility(View.VISIBLE));
                executor.schedule(() -> activity.runOnUiThread(() -> left.setVisibility(View.INVISIBLE)), m * n2 / 2, TimeUnit.MILLISECONDS);
            }
            if(count[0] % n1 == 0)
            {
                stream2 = soundPool.play(sound2, 1, 1, 0, 0, 1);
                activity.runOnUiThread(() -> right.setVisibility(View.VISIBLE));
                executor.schedule(() -> activity.runOnUiThread(() -> right.setVisibility(View.INVISIBLE)), m * n1 / 2, TimeUnit.MILLISECONDS);
            }
            count[0] ++;

        }, lastInterval - (SystemClock.uptimeMillis() - last), m, TimeUnit.MILLISECONDS);
        lastInterval = m;
    }
    public static void stop()
    {
        if(executor != null)
        {
            executor.shutdownNow();
            executor = Executors.newScheduledThreadPool(3);
            stream1 = -1;
            stream2 = -1;
        }
        if(left != null)
        {
            if(left.get() != null) left.get().setVisibility(View.INVISIBLE);
            left = null;
        }
        if(right != null)
        {
            if(right.get() != null) right.get().setVisibility(View.INVISIBLE);
            right = null;
        }
    }
    public static boolean isPlaying()
    {
        return stream1 != -1 || stream2 != -1;
    }
}
