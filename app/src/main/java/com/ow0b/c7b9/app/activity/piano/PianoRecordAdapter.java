package com.ow0b.c7b9.app.activity.piano;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.UserStats;

import java.util.ArrayList;
import java.util.List;

public class PianoRecordAdapter extends BaseAdapter
{
    private Context context;
    public List<PianoRecord> records = new ArrayList<>();

    public PianoRecordAdapter(Context context)
    {
        this.context = context;
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
            convertView = LayoutInflater.from(context).inflate(R.layout.item_piano_record, parent, false);
        }
        TextView name = convertView.findViewById(R.id.piano_record_name);
        //Button deleteButton = convertView.findViewById(R.id.piano_delete_button);
        //Button playButton = convertView.findViewById(R.id.piano_play_button);

        PianoRecord record = records.get(position);
        name.setText(record.name);

        return convertView;
    }
}
