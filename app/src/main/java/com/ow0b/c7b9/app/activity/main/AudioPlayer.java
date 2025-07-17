package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.view.PromptRecordView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AudioPlayer
{
    private static final MediaPlayer mediaPlayer = new MediaPlayer();
    private static Timer mediaPlayerTimer;
    private static String audioFileName;
    public MediaPlayer getMediaPlayer()
    {
        return mediaPlayer;
    }
    public Timer getMediaPlayerTimer()
    {
        return mediaPlayerTimer;
    }

    /// 注意：这个方法没有初始化seekBar的事件，需要手动初始化（可以使用AudioSeekBarListener类）
    public static void playAudio(Context context, String fileName, SeekBar seekBar, float startSecond, TextView i18nTextView, Runnable onCompletion)
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
                if(seekBar != null) seekBar.setProgress(0);
                onCompletion.run();
            });
            //TODO 这里还有一种seekbar的更新？（在PromptRecordView里的）

            if(seekBar != null)
            {
                seekBar.setMax(mediaPlayer.getDuration());
                if(startSecond > 0) seekBar.setProgress((int) (startSecond * 1000));
                mediaPlayer.seekTo((int) (((float) seekBar.getProgress() / seekBar.getMax()) * mediaPlayer.getDuration()));
            }
            else
            {
                mediaPlayer.seekTo((int) (startSecond * 1000));
            }
            mediaPlayerTimer = new Timer();
            int[] min = new int[1];
            mediaPlayerTimer.schedule(new TimerTask()
            {
                @SuppressLint("SetTextI18n")
                @Override
                public void run()
                {
                    if(mediaPlayer.isPlaying())
                    {
                        int seconds = (mediaPlayer.getCurrentPosition() / 1000) % 60;
                        int minutes = (mediaPlayer.getCurrentPosition() / 1000) / 60;
                        String secondText = seconds < 10 ? "0" + seconds : String.valueOf(seconds),
                                minuteText = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
                        if(seekBar != null)
                        {
                            seekBar.setProgress(Math.max(mediaPlayer.getCurrentPosition(), min[0]));
                            min[0] = seekBar.getProgress();
                        }
                        if(i18nTextView != null && context instanceof Activity activity)
                        {
                            activity.runOnUiThread(() -> i18nTextView.setText(minuteText + ":" + secondText));
                        }
                    }
                }
            }, 10, 10);
        }
        catch (IOException e)
        {
            Toast.showError(context, "当前未录制音频");
        }
    }
    public static void playAudio(Context context, String fileName, SeekBar seekBar, TextView i18nTextView, Runnable onCompletion)
    {
        playAudio(context, fileName, seekBar, 0, i18nTextView, onCompletion);
    }
    public static void playAudio(Context context, String fileName, SeekBar seekBar, Runnable onCompletion)
    {
        playAudio(context, fileName, seekBar, 0, null, onCompletion);
    }
    public static void playAudio(Context context, String fileName, Runnable onCompletion)
    {
        playAudio(context, fileName, null, 0, null, onCompletion);
    }
    public static void stopPlayAudio()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
        mediaPlayer.stop();
        mediaPlayerTimer = null;
        audioFileName = null;
    }
    public static void downloadAudio(Context context, ProgressBar progressBar, int rid, String audioFileName, Runnable callback)
    {
        ApiClient.downloadResource(context, progressBar, rid, audioFile(context, audioFileName), callback);
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
