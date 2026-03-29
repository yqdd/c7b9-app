package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiClient;
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
    @SuppressLint("StaticFieldLeak")
    private static SeekBar seekBar = null;
    @SuppressLint("StaticFieldLeak")
    private static TextView time = null;
    private static Runnable onCompletion = null;


    /// 获取MediaPlayer
    public static MediaPlayer getMediaPlayer()
    {

        return mediaPlayer;
    }
    /// 获取Timer
    public static Timer getMediaPlayerTimer()
    {

        return mediaPlayerTimer;
    }
    /// 获取当前MediaPlayer播放到的时间
    public static String getMediaPlayerTime()
    {
        int seconds = (mediaPlayer.getCurrentPosition() / 1000) % 60;
        int minutes = (mediaPlayer.getCurrentPosition() / 1000) / 60;
        String secondText = seconds < 10 ? "0" + seconds : String.valueOf(seconds),
                minuteText = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
        return minuteText + ":" + secondText;
    }
    /// 播放音频
    public static void playAudio(Activity activity, File file, float start, Runnable onCompletion)
    {
        try(FileInputStream stream = new FileInputStream(file))
        {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(stream.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();
            AudioPlayer.onCompletion = onCompletion;
            mediaPlayer.setOnCompletionListener(mp ->
            {
                stopPlayAudio();
                if(seekBar != null) seekBar.setProgress(0);
                onCompletion.run();
            });
            if(seekBar != null)
            {
                seekBar.setMax(mediaPlayer.getDuration());
                if(start > 0) seekBar.setProgress((int) (start * 1000));
                mediaPlayer.seekTo((int) (((float) seekBar.getProgress() / seekBar.getMax()) * mediaPlayer.getDuration()));
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
                    @Override public void onStartTrackingTouch(SeekBar seekBar)
                    {
                        if(seekBar == AudioPlayer.seekBar) AudioPlayer.cancel();
                    }
                    @Override public void onStopTrackingTouch(SeekBar seekBar)
                    {
                        if(seekBar == AudioPlayer.seekBar)
                        {
                            if(AudioPlayer.isPlaying())
                            {
                                AudioPlayer.skipPlayAudio();
                                AudioPlayer.playAudio(activity, file, 0, onCompletion);
                            }
                            else AudioPlayer.stopPlayAudio();
                        }
                    }
                });
            }
            else mediaPlayer.seekTo((int) (start * 1000));

            if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
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
                        activity.runOnUiThread(() ->
                        {
                            if(time != null)
                                time.setText(minuteText + ":" + secondText);
                        });
                    }
                }
            }, 10, 10);
        }
        catch (IOException e)
        {
            Toast.showError(activity, "当前未录制音频");
            Log.e("AudioPlayer", "playAudio: ", e);
        }
    }
    /// 设置播放时更新的组件
    public static void setComponent(@Nullable SeekBar seekBar, @Nullable TextView time)
    {
        AudioPlayer.seekBar = seekBar;
        AudioPlayer.time = time;
    }


    /// 注意：这个方法没有初始化seekBar的事件，需要手动初始化（可以使用AudioSeekBarListener类）
    @Deprecated
    public static void playAudio(Context context, String fileName, SeekBar seekBar, float startSecond, TextView i18nTextView, Runnable onCompletion)
    {
        audioFileName = fileName;
        try(FileInputStream stream = new FileInputStream(AudioRecorder.audioFile(context, fileName)))
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
            Log.e("AudioPlayer", "playAudio: ", e);
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
    //不执行 onCompletion 的stopPlayAudio，用于给seekBar做跳转的
    private static void skipPlayAudio()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
        mediaPlayer.stop();
        mediaPlayerTimer = null;
        audioFileName = null;
    }
    public static void stopPlayAudio()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
        mediaPlayer.stop();
        if(onCompletion != null) onCompletion.run();
        onCompletion = null;
        mediaPlayerTimer = null;
        audioFileName = null;
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
