package com.ow0b.c7b9.app.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.Toast;

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

public class PromptRecordView extends ConstraintLayout
{
    private MediaPlayer mediaPlayer;
    private final Activity activity;
    private AnalyzeView analyzeView;
    private int id = -1;
    public void setId(int id)
    {
        this.id = id;
    }
    public void setAnalyzeView(AnalyzeView analyzeView)
    {
        this.analyzeView = analyzeView;
    }
    public PromptRecordView(@NonNull Activity activity, int audioId)
    {
        this(activity);
        setId(audioId);
    }
    public PromptRecordView(@NonNull Activity activity)
    {
        super(activity);
        this.activity = activity;

        setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);

        LayoutParams constraint = new Constraints.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        constraint.endToEnd = LayoutParams.PARENT_ID;
        constraint.topToTop = LayoutParams.PARENT_ID;
        addView(new Inside(activity), constraint);
    }

    public class Inside extends LinearLayout
    {
        ImageButton playButton;
        Timer timer;
        SeekBar seekBar;
        TextView textView;

        public void updateAnalyzeView()
        {
            analyzeView.backgrounds.forEach(b ->
            {
                b.setProcess((float) seekBar.getProgress() / seekBar.getMax());
                if(b instanceof View view) view.postInvalidate();
            });
        }
        public Inside(Activity activity)
        {
            super(activity);
            setBackground(getResources().getDrawable(R.drawable.bg_chat_record));
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            int padding1 = ParaType.toDP(this, 8), padding2 = ParaType.toDP(this, 10);

            playButton = new ImageButton(activity);
            playButton.setImageResource(R.drawable.btn_play_record_small);
            playButton.setBackgroundColor(getResources().getColor(R.color.empty));
            playButton.setPadding(padding1, padding2, padding1, padding2);
            playButton.setOnClickListener(v ->
            {
                if(id == -1) Toast.showError(activity, "音频暂未初始化");
                else
                {
                    if(mediaPlayer == null) playAudio(activity, id);
                    else stopPlayAudio();
                }
            });
            this.addView(playButton);

            seekBar = new SeekBar(activity);
            seekBar.setLayoutParams(new LinearLayout.LayoutParams(ParaType.toDP(this, 150), ViewGroup.LayoutParams.WRAP_CONTENT));
            seekBar.setPadding(ParaType.toDP(this, 12), padding2, padding1, padding2);
            //设置进度条和按钮颜色
            seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.light_red), PorterDuff.Mode.SRC_IN);
            seekBar.getThumb().setColorFilter(getResources().getColor(R.color.light_red), PorterDuff.Mode.SRC_IN);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b)
                {
                    int seconds = (i / 1000) % 60;
                    int minutes = (i / 1000) / 60;
                    String secondText = seconds < 10 ? "0" + seconds : String.valueOf(seconds),
                            minuteText = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
                    updateAnalyzeView();
                    activity.runOnUiThread(() -> textView.setText(minuteText + ":" + secondText));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar)
                {
                    if(timer != null) timer.cancel();
                }
                @Override public void onStopTrackingTouch(SeekBar seekBar)
                {
                    if(mediaPlayer != null && mediaPlayer.isPlaying())
                    {
                        stopPlayAudio();
                        playAudio(activity, id);
                    }
                    else stopPlayAudio();
                }
            });
            this.addView(seekBar);


            textView = new TextView(activity);
            textView.setText("00:00");
            textView.setTextColor(getResources().getColor(R.color.light_red));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
            textView.setLayoutParams(new LayoutParams(ParaType.toDP(textView, 60), LayoutParams.WRAP_CONTENT));
            //textView.setPadding(padding1, padding2, padding1, padding2);
            this.addView(textView);
        }

        private void playAudio(Context context, int id)
        {
            boolean exist;
            try(FileInputStream stream = new FileInputStream(audioFile(context, id))) { exist = true; }
            catch (IOException e) { exist = false; }

            if(exist) playAudio0(context, id);
            else downloadAudio(context, id, () -> activity.runOnUiThread(() -> playAudio0(context, id)));
        }
        private void playAudio0(Context context, int id)
        {
            try(FileInputStream stream = new FileInputStream(audioFile(context, id)))
            {
                if(mediaPlayer != null) mediaPlayer.release();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.reset();
                //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(stream.getFD());
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mediaPlayer ->
                {
                    stopPlayAudio();
                    seekBar.setProgress(0);
                });
                mediaPlayer.setOnCompletionListener(mediaPlayer ->
                {
                    stopPlayAudio();
                    seekBar.setProgress(0);
                });

                seekBar.setMax(mediaPlayer.getDuration());
                mediaPlayer.seekTo((int) (((float) seekBar.getProgress() / seekBar.getMax()) * mediaPlayer.getDuration()));
                timer = new Timer();
                int[] min = new int[1];
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        if(mediaPlayer != null && mediaPlayer.isPlaying())
                        {
                            int seconds = (mediaPlayer.getCurrentPosition() / 1000) % 60;
                            int minutes = (mediaPlayer.getCurrentPosition() / 1000) / 60;
                            String secondText = seconds < 10 ? "0" + seconds : String.valueOf(seconds),
                                    minuteText = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
                            updateAnalyzeView();

                            activity.runOnUiThread(() -> textView.setText(minuteText + ":" + secondText));
                            seekBar.setProgress(Math.max(mediaPlayer.getCurrentPosition(), min[0]));
                            min[0] = seekBar.getProgress();
                        }
                    }
                }, 10, 10);

                playButton.setImageResource(R.drawable.btn_stop_play_record_small);
            }
            catch (IOException e)
            {
                Log.e("TAG", "playAudio0: ", e);
                Toast.showError(context, "出现未知错误");
            }
        }
        private void stopPlayAudio()
        {
            if(timer != null) timer.cancel();
            if(mediaPlayer != null)
            {
                mediaPlayer.stop();
                mediaPlayer.release();
                playButton.setImageResource(R.drawable.btn_play_record_small);
                textView.setText("00:00");
            }
            timer = null;
            mediaPlayer = null;
        }
        private static void downloadAudio(Context context, int id, Runnable callback)
        {
            ApiClient.getInstance(context).url(context.getResources().getString(R.string.server) + "api/upload")
                    .parameter("id", String.valueOf(id))
                    .get()
                    .callback(new Callback()
                    {
                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                        {
                            byte[] body = response.body().bytes();
                            try(OutputStream output = new FileOutputStream(audioFile(context, id)))
                            {
                                output.write(body);
                            }
                            callback.run();
                        }
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e)
                        {
                            Toast.showError(context, "下载音频文件出现未知错误");
                        }
                    })
                    .enqueue();
        }
        private static File audioFile(Context context, int id)
        {
            return context.getCacheDir().toPath().resolve("audio_" + id + ".m4a").toFile();
        }
    }
}
