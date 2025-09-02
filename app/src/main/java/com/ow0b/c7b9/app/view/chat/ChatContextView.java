package com.ow0b.c7b9.app.view.chat;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.activity.main.AudioPlayerImpl;
import com.ow0b.c7b9.app.old.activity.main.AudioService;
import com.ow0b.c7b9.app.view.ExpandableLayout;
import com.ow0b.c7b9.app.view.chat.sub.AiTextView;
import com.ow0b.c7b9.app.old.util.AudioSeekBarListener;
import com.ow0b.c7b9.app.old.util.ParaType;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChatContextView extends LinearLayout implements ChatContext
{
    private final Context context;
    private final AudioService audioService;
    public ChatContextView(@NonNull Context context, @NotNull AudioService audioService)
    {
        super(context);
        this.context = context;
        this.audioService = audioService;
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
        AiTextView view = new AiTextView(context, ChatContextView.this, false);
        addView(view);
        return view;
    }
    public AiTextView newAiText(String header)
    {
        AiTextView view = new AiTextView(context, ChatContextView.this, true);
        addView(new ExpandableLayout(context)
        {{
            setHeaderText(header);
            addComponent(view);
        }});
        return view;
    }

    public View newAudio(int aid)
    {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_prompt_audio, this);
        TextView textView = view.findViewById(R.id.chat_display_prompt_audio_time);
        ImageButton playButton = view.findViewById(R.id.chat_display_prompt_audio_button);
        SeekBar seekBar = view.findViewById(R.id.chat_display_prompt_audio_bar);
        ProgressBar progressBar = view.findViewById(R.id.chat_display_prompt_audio_progress);

        seekBar.setOnSeekBarChangeListener(new AudioSeekBarListener(getContext(), String.valueOf(aid), textView,
                () -> playButton.setImageResource(R.drawable.btn_record_play_small)));
        //资源文件不存在时下载
        File file = audioService.download(getContext(), aid);
        audioViews.put(aid, view);
        //TODO 这里直接设置，文件不存在时按钮为GONE应该是点不到的？？
        playButton.setOnClickListener(v ->
        {
            if(AudioPlayerImpl.isPlaying())
            {
                playButton.setImageResource(R.drawable.btn_record_play_small);
                AudioPlayerImpl.stopPlayAudio();
            }
            else
            {
                playButton.setImageResource(R.drawable.btn_stop_play_record_small);
                MediaPlayer player = audioService.play(getContext(), file, 0);
                player.setOnCompletionListener(mediaPlayer ->
                {
                    audioService.stop();
                    seekBar.setProgress(0);
                });
                seekBar.setMax(player.getDuration());
                seekBar.setProgress(0);
                player.seekTo((int) (((float) seekBar.getProgress() / seekBar.getMax()) * player.getDuration()));
            }
        });
        return view;
    }

    public final Map<Integer, View> audioViews = new HashMap<>();
    public View getAudioView(int rid)
    {
        return audioViews.get(rid);
    }
    public boolean skipPlayAudio(int rid, float second, Runnable start)
    {
        if(AudioPlayerImpl.isPlaying()) AudioPlayerImpl.stopPlayAudio();
        if(audioViews.containsKey(rid))
        {
            View view = audioViews.get(rid);
            TextView textView = view.findViewById(R.id.chat_display_prompt_audio_time);
            SeekBar seekBar = view.findViewById(R.id.chat_display_prompt_audio_bar);
            start.run();
            AudioPlayerImpl.playAudio(getContext(), String.valueOf(rid), seekBar, second, textView, () -> {});
            return true;
        }
        else return false;
    }
}
