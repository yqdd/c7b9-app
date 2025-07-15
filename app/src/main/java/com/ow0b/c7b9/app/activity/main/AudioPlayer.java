package com.ow0b.c7b9.app.activity.main;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.SeekBar;

import com.ow0b.c7b9.app.util.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class AudioPlayer
{
    private static final MediaPlayer mediaPlayer = new MediaPlayer();
    private static Timer mediaPlayerTimer;
    private static String audioFileName;

    public static void playAudio(Context context, String fileName, SeekBar progress, Runnable onCompletion)
    {
        audioFileName = fileName;
        try(FileInputStream stream = new FileInputStream(audioFile(context, fileName)))
        {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(stream.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mediaPlayer ->
            {
                stopPlayAudio();
                progress.setProgress(0);
                onCompletion.run();
            });

            progress.setMax(mediaPlayer.getDuration());
            mediaPlayer.seekTo((int) (((float) progress.getProgress() / progress.getMax()) * mediaPlayer.getDuration()));
            mediaPlayerTimer = new Timer();
            int[] min = new int[1];
            mediaPlayerTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if(mediaPlayer.isPlaying())
                    {
                        progress.setProgress(Math.max(mediaPlayer.getCurrentPosition(), min[0]));
                        min[0] = progress.getProgress();
                    }
                }
            }, 10, 10);
        }
        catch (IOException e)
        {
            Toast.showError(context, "当前未录制音频");
        }
    }
    public static void stopPlayAudio()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
        mediaPlayer.stop();
        mediaPlayerTimer = null;
        audioFileName = null;
    }
    public static File audioFile(Context context, String fileName)
    {
        return AudioRecorder.audioFile(context, fileName);
    }

    public static boolean isPlaying()
    {
        return mediaPlayer.isPlaying();
    }
    public static void cancel()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
    }
}
