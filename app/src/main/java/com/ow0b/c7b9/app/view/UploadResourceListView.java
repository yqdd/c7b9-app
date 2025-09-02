package com.ow0b.c7b9.app.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.ow0b.c7b9.app.MainActivity;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.activity.main.AudioPlayerImpl;
import com.ow0b.c7b9.app.old.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.old.util.midi.Midi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class UploadResourceListView extends LinearLayout
{
    private UploadResourceListModel model;
    public UploadResourceListView(Context context)
    {
        super(context);
        init(context);
    }
    public UploadResourceListView(Context context, AttributeSet attr)
    {
        super(context, attr);
        init(context);
    }
    private void init(Context context)
    {
        if (context instanceof Activity)
            model = new ViewModelProvider((MainActivity) context).get(UploadResourceListModel.class);
    }

    public final List<Object> resources = new ArrayList<>();
    public void clear()
    {
        removeAllViews();
        resources.clear();
    }
    public List<File> audios()
    {
        List<File> result = new ArrayList<>();
        for(int i = 0; i < resources.size(); i ++)
        {
            Object res = resources.get(i);
            if(res instanceof File file)
            {
                result.add(file);
            }
        }
        return result;
    }
    public void addResource(Activity activity, Midi midi)
    {
        View item = LayoutInflater.from(getContext()).inflate(R.layout.item_upload_midi, this, false);
        TextView text = item.findViewById(R.id.resource_indicator);
        text.setText(midi.name);
        addView(item);
        resources.add(midi);
        initResource(midi, item, v ->
                {
                    if(MidiPlayer.isPlaying()) MidiPlayer.stop();
                    else MidiPlayer.play(activity, midi);
                },
                v -> item.setVisibility(View.GONE),
                v -> AudioPlayerImpl.cancel(),
                v ->
                {
                    if(AudioPlayerImpl.isPlaying())
                    {
                        MidiPlayer.stop();
                        MidiPlayer.play(activity, midi);
                    }
                    else MidiPlayer.stop();
                });
    }

    private final HashSet<ImageButton> audioPlayingButtons = new HashSet<>();
    public void addResource(File audio)
    {
        View item = LayoutInflater.from(getContext()).inflate(R.layout.item_upload_audio, this, false);
        addView(item);
        resources.add(audio);
        initResource(audio, item, v ->
                {
                    if(model.audioService.isPlaying())
                    {
                        model.audioService.stop();
                        audioPlayingButtons.forEach(b -> b.setImageResource(R.drawable.btn_record_play));
                        audioPlayingButtons.clear();
                    }
                    else
                    {
                        model.audioService.play(getContext(), audio, 0);
                        //AudioPlayerImpl.playAudio(getContext(), audioFileName, v.progressBar, () ->
                        //        v.playButton.setImageResource(R.drawable.btn_record_play));
                        v.playButton.setImageResource(R.drawable.btn_stop_play_record);
                        audioPlayingButtons.add(v.playButton);
                    }
                },
                v ->
                {
                    item.setVisibility(View.GONE);
                    audio.delete();
                },
                v ->
                {
                    //AudioPlayerImpl.cancel();
                },
                v ->
                {
                    if(model.audioService.isPlaying())
                    {
                        AudioPlayerImpl.stopPlayAudio();
                        model.audioService.play(getContext(), audio, 0);
                        //AudioPlayerImpl.playAudio(getContext(), audioFileName, v.progressBar, () ->
                        //        v.playButton.setImageResource(R.drawable.btn_record_play));
                    }
                    else
                    {
                        AudioPlayerImpl.stopPlayAudio();
                        v.playButton.setImageResource(R.drawable.btn_record_play);
                    }
                });
    }

    record ResourceViews(TextView indicator, SeekBar progressBar, ImageButton playButton, ImageButton deleteButton) {}
    @SuppressLint("ClickableViewAccessibility")
    private void initResource(Object data, View view,
                              Consumer<ResourceViews> play, Consumer<ResourceViews> delete,
                              Consumer<ResourceViews> tracking, Consumer<ResourceViews> reset)
    {
        TextView indicator = view.findViewById(R.id.resource_indicator);
        SeekBar progressBar = view.findViewById(R.id.resource_progress_bar);
        ImageButton playButton = view.findViewById(R.id.resource_play_button);
        ImageButton deleteButton = view.findViewById(R.id.resource_delete_button);
        ResourceViews views = new ResourceViews(indicator, progressBar, playButton, deleteButton);

        playButton.setOnClickListener(v -> play.accept(views));
        deleteButton.setOnClickListener(v ->
        {
            delete.accept(views);
            resources.remove(data);
        });
        progressBar.setOnTouchListener((v, event) ->
        {
            //防止ScrollView拦截事件
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) { }
            @Override public void onStartTrackingTouch(SeekBar seekBar)
            {
                tracking.accept(views);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar)
            {
                reset.accept(views);
            }
        });
    }
}
