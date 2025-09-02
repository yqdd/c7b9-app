package com.ow0b.c7b9.app.old;

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.ow0b.c7b9.app.R;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity
{

    private ListView leaderboardList;
    private LeaderboardAdapter leaderboardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardList = findViewById(R.id.leaderboard_list);

        // Fetch leaderboard data
        List<UserStats> userStatsList = getUserStats();


        LineChart chart = findViewById(R.id.board_speed);

        LineData data = new LineData();
        data.addDataSet(new LineDataSet(List.of(new Entry(0, 0), new Entry(100, 100)), "测试"));
        chart.setData(data);



        // Set up leaderboard list
        leaderboardAdapter = new LeaderboardAdapter(this, userStatsList);
        leaderboardList.setAdapter(leaderboardAdapter);
    }

    private List<UserStats> getUserStats()
    {
        // Dummy data for user stats
        List<UserStats> userStatsList = new ArrayList<>();
        userStatsList.add(new UserStats("User1", 10));
        userStatsList.add(new UserStats("User2", 8));
        userStatsList.add(new UserStats("User3", 15));
        return userStatsList;
    }
}