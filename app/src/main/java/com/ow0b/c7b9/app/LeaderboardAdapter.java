package com.ow0b.c7b9.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class LeaderboardAdapter extends BaseAdapter {

    private Context context;
    private List<UserStats> userStatsList;

    public LeaderboardAdapter(Context context, List<UserStats> userStatsList) {
        this.context = context;
        this.userStatsList = userStatsList;
    }

    @Override
    public int getCount() {
        return userStatsList.size();
    }

    @Override
    public Object getItem(int position) {
        return userStatsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_user_stats, parent, false);
        }

        TextView userName = convertView.findViewById(R.id.user_name);
        TextView aiCallCount = convertView.findViewById(R.id.ai_call_count);

        UserStats userStats = userStatsList.get(position);
        userName.setText(userStats.getUserName());
        aiCallCount.setText(String.valueOf(userStats.getAiCallCount()));

        return convertView;
    }
}