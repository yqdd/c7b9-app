package com.ow0b.c7b9.app.activity.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ow0b.c7b9.app.R;

import java.util.List;

public class ConversationsAdapter extends BaseAdapter
{
    private Context context;
    public List<Conversation> conversations;

    public ConversationsAdapter(Context context, List<Conversation> conversations)
    {
        this.context = context;
        this.conversations = conversations;
    }

    @Override
    public int getCount()
    {
        return conversations.size();
    }

    @Override
    public Object getItem(int position)
    {
        return conversations.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        }

        TextView userMessage = convertView.findViewById(R.id.user_message);
        //TextView botResponse = convertView.findViewById(R.id.bot_response);

        Conversation conversation = conversations.get(position);
        userMessage.setText(conversation.refer);
        //botResponse.setText(conversation.getBotResponse());

        return convertView;
    }
}