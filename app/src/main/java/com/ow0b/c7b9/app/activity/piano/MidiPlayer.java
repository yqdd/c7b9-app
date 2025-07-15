package com.ow0b.c7b9.app.activity.piano;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.util.midi.Note;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MidiPlayer
{
    private final static String TAG = "MidiPlayer";
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(88);
    private static SoundPool soundPool;
    private static final int[] pianoSounds = new int[88];
    private static final Integer[] pianoStreams = new Integer[88];
    private static final ValueAnimator[] pianoAnims = new ValueAnimator[88];
    public static boolean reverb = false;

    public static void init(Activity activity)
    {
        soundPool = new SoundPool(88, AudioManager.STREAM_MUSIC, 0);
        try
        {
            for(int i = 1; i <= 88; i ++)
            {
                pianoSounds[i - 1] = soundPool.load(activity.getResources().getAssets().openFd("grand/tone (" + i + ").wav"), 1);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    public static void play(Activity activity, Midi midi)
    {
        for(Note note : midi.notes)
        {
            executor.schedule(() ->
            {
                try { activity.runOnUiThread(() -> playKey(note.pitch)); }
                catch (Exception e) { Log.e(TAG, "playKey: ", e); }

                executor.schedule(() ->
                {
                    try { activity.runOnUiThread(() -> stopKey(note.pitch)); }
                    catch (Exception e) { Log.e(TAG, "stopKey: ", e); }

                }, (long) ((note.end - note.start) * 1000), TimeUnit.MILLISECONDS);

            }, (long) (note.start * 1000), TimeUnit.MILLISECONDS);
        }
    }
    public static void stop()
    {
        if(executor != null)
        {
            stopAllKeys();
            executor.shutdownNow();
            executor = Executors.newScheduledThreadPool(88);
        }
    }
    public static boolean isPlaying()
    {
        if(executor != null) return ((ScheduledThreadPoolExecutor) executor).getActiveCount() > 0;
        else return false;
    }

    public static void playKey(int i)
    {
        if(pianoStreams[i] != null) soundPool.stop(pianoStreams[i]);
        if(pianoAnims[i] != null) pianoAnims[i].end();

        pianoStreams[i] = soundPool.play(pianoSounds[i], 1, 1, 0, 0, 1);
    }
    public static void stopKey(int i)
    {
        stopKey(i, 500);
    }
    public static void stopKey(int i, int millis)
    {
        ValueAnimator anim = pianoAnims[i] = ValueAnimator.ofFloat(1, 0);
        anim.addUpdateListener(value ->
        {
            if(pianoStreams[i] != null)
            {
                soundPool.setVolume(pianoStreams[i], (float) value.getAnimatedValue(), (float) value.getAnimatedValue());
            }
        });
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(pianoStreams[i] != null)
                {
                    soundPool.stop(pianoStreams[i]);
                    pianoStreams[i] = null;
                    pianoAnims[i] = null;
                }
            }
        });
        anim.setDuration(millis);
        anim.start();
        Log.d(TAG, "stopKey: " + anim.isStarted() + "  id:" + i);
    }
    public static void stopAllKeys()
    {
        for(int i = 0; i < 88; i ++)
        {
            if(pianoStreams[i] != null)
                stopKey(i, 100);
        }
    }
}
