package com.ow0b.c7b9.app.activity.main.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.AudioPlayer;
import com.ow0b.c7b9.app.activity.main.AudioRecorder;
import com.ow0b.c7b9.app.activity.main.AudioUtils;
import com.ow0b.c7b9.app.activity.main.MainActivity;
import com.ow0b.c7b9.app.activity.main.UploadResourceListView;
import com.ow0b.c7b9.app.util.ParaType;
import com.ow0b.c7b9.app.util.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ChatContextView extends LinearLayout
{
    private final MainActivity activity;
    public ChatContextView(@NonNull MainActivity activity)
    {
        super(activity);
        this.activity = activity;
        setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);
    }

    public TextView newUserText()
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_prompt_text, this, false);
        addView(view);
        return view.findViewById(R.id.chat_display_prompt_text);
    }
    public AiTextView newAiText()
    {
        AiTextView view = new AiTextView(activity, ChatContextView.this, false, true);
        addView(view);
        return view;
    }
    public AiTextView newAiText(String header)
    {
        AiTextView view = new AiTextView(activity, ChatContextView.this, true, false);
        addView(new ExpandableLayout(activity)
        {{
            setHeaderText(header);
            addComponent(view);
        }});
        return view;
    }

    /// 用于给回复文本内的 \<locateAudio/\> 标签做跳转用
    public final Map<Integer, View> audios = new HashMap<>();
    public View getAudioView(int aid)
    {
        return audios.get(aid);
    }
    @SuppressLint("SetWorldReadable")
    public void newAudio(int aid)
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_prompt_audio, this);
        TextView textView = view.findViewById(R.id.chat_display_prompt_audio_time);
        ImageButton playButton = view.findViewById(R.id.chat_display_prompt_audio_button),
                    saveButton = view.findViewById(R.id.chat_display_prompt_audio_save);
        SeekBar seekBar = view.findViewById(R.id.chat_display_prompt_audio_bar);

        //资源文件不存在时下载
        File file0 = AudioUtils.getAudioFromServer(activity, aid, progress -> {}, f -> audios.put(aid, view));
        playButton.setOnClickListener(v ->
        {
            if(AudioPlayer.isPlaying())
            {
                playButton.setImageResource(R.drawable.btn_record_play_small);
                AudioPlayer.stopPlayAudio();
            }
            else
            {
                playButton.setImageResource(R.drawable.btn_stop_play_record_small);
                AudioPlayer.setComponent(seekBar, textView);
                AudioPlayer.playAudio(activity, file0, 0, () -> playButton.setImageResource(R.drawable.btn_record_play_small));
            }
        });
        saveButton.setOnClickListener(v ->
        {
            File file = AudioUtils.temp(activity);
            try(InputStream input = new FileInputStream(file0);
                OutputStream output = new FileOutputStream(file))
            {
                int b;
                while((b = input.read()) != -1) output.write(b);
                activity.uploadResources.addHistory(activity, file, aid);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        });
    }
    void skipPlayAudio(Context context, int aid, float second)
    {
        if(AudioPlayer.isPlaying()) AudioPlayer.stopPlayAudio();
        ImageButton[] playButton = new ImageButton[1];
        TextView[] textView = new TextView[1];
        SeekBar[] seekBar = new SeekBar[1];
        if(audios.containsKey(aid))
        {
            View view = audios.get(aid);
            playButton[0] = view.findViewById(R.id.chat_display_prompt_audio_button);
            textView[0] = view.findViewById(R.id.chat_display_prompt_audio_time);
            seekBar[0] = view.findViewById(R.id.chat_display_prompt_audio_bar);
            //start.run();
            playButton[0].setImageResource(R.drawable.btn_stop_play_record_small);
        }
        AudioUtils.getAudioFromServer(this.activity, aid, progress -> {}, f ->
        {
            AudioPlayer.setComponent(seekBar[0], textView[0]);
            AudioPlayer.playAudio(activity, f, second, () ->
            {
                if(playButton[0] != null)
                    activity.runOnUiThread(() -> playButton[0].setImageResource(R.drawable.btn_record_play_small));
            });
            activity.runOnUiThread(() -> Toast.showInfo(context, "开始播放音频"));
        });
    }
}
