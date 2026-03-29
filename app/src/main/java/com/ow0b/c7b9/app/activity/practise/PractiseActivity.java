package com.ow0b.c7b9.app.activity.practise;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.leaderboard.LeaderboardAdapter;
import com.ow0b.c7b9.app.activity.leaderboard.UserStats;
import com.ow0b.c7b9.app.databinding.ActivityToolPractiseBinding;

import java.util.ArrayList;
import java.util.List;

public class PractiseActivity extends AppCompatActivity
{
    private ActivityToolPractiseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView((binding = ActivityToolPractiseBinding.inflate(getLayoutInflater())).getRoot());

    }
}