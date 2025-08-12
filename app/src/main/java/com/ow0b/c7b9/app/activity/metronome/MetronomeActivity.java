package com.ow0b.c7b9.app.activity.metronome;

import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.R;

public class MetronomeActivity extends AppCompatActivity
{
    public LinearLayout container;

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

        container = findViewById(R.id.metronome_container);




        container.addView(new MetronomeView(this, LinearLayout.HORIZONTAL));
        container.addView(new MetronomeView(this, LinearLayout.HORIZONTAL));
    }
}
