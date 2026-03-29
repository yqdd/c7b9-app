package com.ow0b.c7b9.app.activity.piano;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.ow0b.c7b9.app.activity.main.MainActivity;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.midi.Midi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class PianoRecordAdapter extends BaseAdapter
{
    private Activity activity;
    public List<PianoRecord> records = new ArrayList<>();
    private LinearLayout recordLayout;
    private ImageButton deleteButton, playButton, uploadButton;


    public PianoRecordAdapter(Activity activity)
    {
        this.activity = activity;
    }

    @Override
    public int getCount()
    {
        return records.size();
    }
    @Override
    public Object getItem(int position)
    {
        return records.get(position);
    }
    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(activity).inflate(R.layout.item_piano_record, parent, false);
        }
        TextView name = convertView.findViewById(R.id.piano_record_name);
        recordLayout = convertView.findViewById(R.id.piano_record_layout);
        deleteButton = convertView.findViewById(R.id.piano_delete_button);
        playButton = convertView.findViewById(R.id.piano_play_button);
        uploadButton = convertView.findViewById(R.id.piano_upload_button);

        PianoRecord record = records.get(position);
        name.setText(record.name);

        deleteButton.setOnClickListener(v -> deleteRecords(position));
        playButton.setOnClickListener(v -> playRecords(position));
        uploadButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra("midi", getMidi(record.name));
            activity.startActivity(intent);
        });
        return convertView;
    }

    private void deleteRecords(int position)
    {
        String name = records.get(position).name;
        try(Writer writer = new OutputStreamWriter(activity.openFileOutput("pianoRecords.json", Context.MODE_PRIVATE)))
        {
            records.removeIf(r -> r.name.equals(name));
            new Gson().toJson(records.stream().map(r -> r.name).toArray(), writer);
            if(!activity.getDir("records-" + name + ".json", Context.MODE_PRIVATE).delete())
            {
                Toast.showError(activity, "存档文件删除失败");
                Log.e("TAG", "deleteRecords: ", new RuntimeException());
            }
            notifyDataSetChanged();
            Toast.showInfo(activity, "删除成功");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    private void playRecords(int position)
    {
        String name = records.get(position).name;
        MidiPlayer.playInExecutor(activity, getMidi(name));
    }
    private Midi getMidi(String name)
    {
        try(Reader reader = new InputStreamReader(activity.openFileInput("records-" + name + ".json")))
        {
            Gson gson = new Gson();
            return gson.fromJson(reader, Midi.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
