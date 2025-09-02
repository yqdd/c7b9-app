package com.ow0b.c7b9.app.activity.metronome;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.util.Toast;

public class MetronomeActivity extends AppCompatActivity
{
    public ConstraintLayout container;
    public LinearLayout views;
    public TextView add, del;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        try
        {
            //设置布局延伸到刘海屏内，否则小米手机顶部导航栏显示黑色。
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        catch (Exception e)
        {
            Log.e("TAG", "onCreate: ", e);
        }
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);       // 隐藏标题栏

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_metronome);
        MetronomePlayer.init(this);

        container = findViewById(R.id.metronome_container);
        views = findViewById(R.id.metronome_views);
        add = findViewById(R.id.metronome_add);
        del = findViewById(R.id.metronome_del);

        add.setOnClickListener(v ->
        {
            if(views.getChildCount() < 4)
            {
                views.addView(new MetronomeView(this, LinearLayout.HORIZONTAL));
                MetronomePlayer.stop();
                MetronomeView.all.keySet().forEach(m ->
                {
                    m.findViewById(R.id.metronome_play).
                            setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
                    m.play.setText("开始");
                });
            }
        });
        del.setOnClickListener(v ->
        {
            if(views.getChildCount() > 1)
            {
                views.removeViewAt(views.getChildCount() - 1);
                MetronomePlayer.stop();
                MetronomeView.all.keySet().forEach(m -> m.play.setText("开始"));
                MetronomeView.setAllBackground(getResources().getColor(R.color.black));
            }
        });

        views.addView(new MetronomeView(this, LinearLayout.HORIZONTAL));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MetronomePlayer.stop();
    }
}
