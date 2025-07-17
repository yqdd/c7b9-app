package com.ow0b.c7b9.app.activity.main.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.AudioPlayer;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.AudioSeekBarListener;
import com.ow0b.c7b9.app.util.ParaType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UserPromptView extends LinearLayout
{
    public UserPromptView(@NonNull Context context)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        ((MarginLayoutParams) getLayoutParams()).bottomMargin = ParaType.toDP(this, 10);
    }

    public TextView newText()
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_prompt_text, this);
        return view.findViewById(R.id.chat_display_prompt_text);
    }
    private final Map<Integer, View> audios = new HashMap<>();
    public View getAudioView(int rid)
    {
        return audios.get(rid);
    }
    public void newAudio(int rid)
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_prompt_audio, this);
        TextView textView = view.findViewById(R.id.chat_display_prompt_audio_time);
        ImageButton playButton = view.findViewById(R.id.chat_display_prompt_audio_button);
        SeekBar seekBar = view.findViewById(R.id.chat_display_prompt_audio_bar);
        ProgressBar progressBar = view.findViewById(R.id.chat_display_prompt_audio_progress);

        seekBar.setOnSeekBarChangeListener(new AudioSeekBarListener(getContext(), String.valueOf(rid), textView,
                () -> playButton.setImageResource(R.drawable.btn_record_play_small)));
        //资源文件不存在时下载
        File file = AudioPlayer.audioFile(getContext(), String.valueOf(rid));
        if(!file.exists())
        {
            progressBar.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.GONE);
            ApiClient.downloadResource(getContext(), progressBar, rid, file, () ->
            {
                playButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                audios.put(rid, view);
            });
        }
        else audios.put(rid, view);
        //TODO 这里直接设置，文件不存在时按钮为GONE应该是点不到的？？
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
                AudioPlayer.playAudio(getContext(), String.valueOf(rid), seekBar, textView, () -> {});
            }
        });
    }
    public boolean skipPlayAudio(int rid, float second, Runnable start)
    {
        if(AudioPlayer.isPlaying()) AudioPlayer.stopPlayAudio();
        if(audios.containsKey(rid))
        {
            View view = audios.get(rid);
            TextView textView = view.findViewById(R.id.chat_display_prompt_audio_time);
            SeekBar seekBar = view.findViewById(R.id.chat_display_prompt_audio_bar);
            start.run();
            AudioPlayer.playAudio(getContext(), String.valueOf(rid), seekBar, second, textView, () -> {});
            return true;
        }
        else return false;
    }
}
