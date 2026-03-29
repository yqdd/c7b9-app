package com.ow0b.c7b9.app.activity.main;

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

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.util.midi.Midi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public class UploadResourceListView extends LinearLayout
{
    private final Context context;
    public UploadResourceListView(Context context)
    {
        super(context);
        this.context = context;
    }
    public UploadResourceListView(Context context, AttributeSet attr)
    {
        super(context, attr);
        this.context = context;
    }

    public final List<Object> resources = new ArrayList<>();
    public void clear()
    {
        removeAllViews();
        resources.clear();
    }
    public void addMidi(Activity activity, Midi midi)
    {
        View item = LayoutInflater.from(context).inflate(R.layout.item_upload_midi, this, false);
        TextView text = item.findViewById(R.id.resource_indicator);
        text.setText(midi.name);
        addView(item);
        resources.add(midi);
        initResource(midi, item, v ->
                {
                    if(MidiPlayer.isPlaying()) MidiPlayer.stop();
                    else MidiPlayer.playInExecutor(activity, midi);
                },
                v -> item.setVisibility(View.GONE));
    }

    private final HashSet<ImageButton> audioPlayingButtons = new HashSet<>();
    public void addAudio(Activity activity, File file)
    {
        View item = LayoutInflater.from(context).inflate(R.layout.item_upload_audio, this, false);
        addView(item);
        resources.add(file);
        initResource(file, item, v ->
                {
                    if(AudioPlayer.isPlaying())
                    {
                        AudioPlayer.stopPlayAudio();
                        audioPlayingButtons.forEach(b -> b.setImageResource(R.drawable.btn_record_play));
                        audioPlayingButtons.clear();
                    }
                    else
                    {
                        AudioPlayer.setComponent(v.progressBar, null);
                        AudioPlayer.playAudio(activity, file, 0, () -> v.playButton.setImageResource(R.drawable.btn_record_play));
                        v.playButton.setImageResource(R.drawable.btn_stop_play_record);
                        audioPlayingButtons.add(v.playButton);
                    }
                },
                v ->
                {
                    item.setVisibility(View.GONE);
                    file.delete();
                });
    }
    public void addHistory(Activity activity, File file, int aid)
    {
        View item = LayoutInflater.from(context).inflate(R.layout.item_upload_history, this, false);
        addView(item);
        resources.add(aid);
        initResource(aid, item, v ->
                {
                    if(AudioPlayer.isPlaying())
                    {
                        AudioPlayer.stopPlayAudio();
                        audioPlayingButtons.forEach(b -> b.setImageResource(R.drawable.btn_history_play));
                        audioPlayingButtons.clear();
                    }
                    else
                    {
                        AudioPlayer.setComponent(v.progressBar, null);
                        AudioPlayer.playAudio(activity, file, 0, () -> v.playButton.setImageResource(R.drawable.btn_history_play));
                        v.playButton.setImageResource(R.drawable.btn_stop_play_history);
                        audioPlayingButtons.add(v.playButton);
                    }
                },
                v ->
                {
                    item.setVisibility(View.GONE);
                    file.delete();
                });
    }

    record ResourceViews(TextView indicator, SeekBar progressBar, ImageButton playButton, ImageButton deleteButton) {}
    @SuppressLint("ClickableViewAccessibility")
    private void initResource(Object data, View view,
                              Consumer<ResourceViews> play, Consumer<ResourceViews> delete)
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
    }
}
