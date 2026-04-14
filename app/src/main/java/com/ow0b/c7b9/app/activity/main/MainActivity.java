package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.chord.ChordComposeActivity;
import com.ow0b.c7b9.app.activity.leaderboard.LeaderboardActivity;
import com.ow0b.c7b9.app.activity.metronome.MetronomeActivity;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.activity.piano.PianoToolActivity;
import com.ow0b.c7b9.app.activity.rhythm.RhythmActivity;
import com.ow0b.c7b9.app.databinding.ActivityMainBinding;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.activity.main.chat.ChatContextView;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private ActivityMainBinding binding;
    private EditText userInput;
    public FrameLayout contentFrame;
    public TextView titleText;
    public LinearLayout chatDisplay, welcomeDisplay;
    public ConstraintLayout chatPoster;
    public ScrollView chatDisplayScroll;
    ImageButton sendButton;
    private ImageButton recordAudioButton;
    private Button toolSelectionButton, drawerButton;
    public Button newChatButton;
    public UploadResourceListView uploadResources;
    public DrawerLayout drawerLayout;
    public boolean isThinkingOn = true, isMidiOn = true;
    public boolean isNewChat = false;       //这个用来减少打开Fragment需要的网络请求
    public int chatContextId = -1;
    boolean isGenerating = false;
    Call generateCall = null;

    @Override
    @SuppressLint("NewApi")
    protected void onStart()
    {
        super.onStart();
        //加载其他activity启动main传入的资源
        Intent intent = getIntent();
        if(intent.getExtras() != null)
        {
            Midi midi = intent.getExtras().getSerializable("midi", Midi.class);
            Log.d(TAG, "onResume: " + midi);
            if (midi != null) uploadResources.addMidi(this, midi);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView((binding = ActivityMainBinding.inflate(getLayoutInflater())).getRoot());
        clearAudioCache();
        MidiPlayer.init(this);

        //sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userInput = findViewById(R.id.user_input);
        titleText = findViewById(R.id.title_text);
        chatPoster = findViewById(R.id.chat_poster);
        chatDisplay = findViewById(R.id.chat_display);
        welcomeDisplay = findViewById(R.id.welcome_display);
        chatDisplayScroll = findViewById(R.id.chat_display_scroll);
        sendButton = findViewById(R.id.send_button);
        recordAudioButton = findViewById(R.id.record_audio_button);
        uploadResources = findViewById(R.id.upload_bar);
        toolSelectionButton = findViewById(R.id.tool_selection_button);
        drawerButton = findViewById(R.id.drawer_button);
        newChatButton = findViewById(R.id.new_chat_button);
        contentFrame = findViewById(R.id.content_frame);
        drawerLayout = findViewById(R.id.drawer_layout);
        ApiClient.pingServer(this);

        //添加测试用的音频
        /*
        File file = AudioRecorder.audioFile(this, "testAudio.m4a");
        try(InputStream input = getAssets().open("testAudio.m4a");
            OutputStream output = new FileOutputStream(file))
        {
            int b;
            while((b = input.read()) != -1) output.write(b);
            uploadResources.addAudio(this, file);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
         */

        binding.fragmentDrawerToolMetronome.setOnClickListener(v ->
                startActivity(new Intent(this, MetronomeActivity.class)));
        binding.fragmentDrawerToolChord.setOnClickListener(v ->
                startActivity(new Intent(this, ChordComposeActivity.class)));
        binding.fragmentDrawerToolRhythm.setOnClickListener(v ->
                startActivity(new Intent(this, RhythmActivity.class)));
        binding.fragmentDrawerToolLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderboardActivity.class)));


        chatDisplayScroll.setVerticalScrollBarEnabled(false);
        chatDisplay.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
        {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7)
            {
                chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                chatDisplay.removeOnLayoutChangeListener(this);
            }
        });

        sendButton.setOnClickListener(v ->
        {
            if(!ApiClient.isLogin(this)) Toast.showInfo(this, "请先登录");
            else if(isGenerating) sendMessageToAICancel();
            else
            {
                String text = userInput.getText().toString();
                if(!text.isEmpty())
                {
                    welcomeDisplay.setVisibility(View.GONE);
                    binding.toolScroll.setVisibility(View.GONE);
                    newChatButton.setVisibility(View.VISIBLE);
                    userInput.setText("");
                    ChatContextView promptView = new ChatContextView(this);

                    chatDisplay.addView(promptView);
                    if(uploadResources.resources.isEmpty())
                    {
                        //音频文件不存在则只发文本
                        /*
                        chatDisplay.addView(new ChatContextView(this)
                        {{
                            AiTextView aiText = newAiText();
                            aiText.append("""
                                    测试123
                                    <locateAudio id="1" skip="2" tip="点我播放音频" />
                                    123123
                                    <intent activity="节拍器" />
                                    <intent activity="钢琴窗" />
                                    <forceChart />
                                    <speedChart id="65" />
                                    <midiChart />
                                    123123
                                    """);
                        }});
                         */
                        promptView.newUserText().setText(text);
                        ChatUtils.sendMessageToAI(this, promptView, isThinkingOn, isMidiOn, text, chatContextId, null);
                    }
                    else
                    {
                        AudioUtils.sendAudioToServer(this, aids ->
                        {
                            runOnUiThread(() ->
                            {
                                uploadResources.clear();
                                aids.forEach(promptView::newAudio);
                                //chatDisplay.addView(new UserPromptView(this));     // text
                                //JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                                //int id = obj.get("id").getAsInt();
                                //recordView.setId(id);
                                promptView.newUserText().setText(text);
                                ChatUtils.sendMessageToAI(this, promptView, isThinkingOn, isMidiOn, text, chatContextId, aids);
                            });
                        });
                        //uploadResources.setVisibility(View.GONE);
                    }
                    if(!ApiClient.serverAlive)
                    {
                        chatDisplay.addView(new ChatContextView(this)
                        {{
                            newAiText().append("无法连接到服务器");
                        }});
                    }
                }
            }
        });
        recordAudioButton.setOnClickListener(v ->
        {
            if (AudioRecorder.isRecording())
            {
                uploadResources.addAudio(this, AudioRecorder.stopRecording());
                recordAudioButton.setImageResource(R.drawable.btn_record_start);
            }
            else
            {
                AudioRecorder.startRecording(this);
                recordAudioButton.setImageResource(R.drawable.btn_record_stop);
            }
        });
        updateSwitchButtonState(binding.thinkButton, isThinkingOn);
        binding.thinkButton.setOnClickListener(v ->
        {
            isThinkingOn = !isThinkingOn;
            updateSwitchButtonState(binding.thinkButton, isThinkingOn);
        });
        updateSwitchButtonState(binding.midiMatchButton, isMidiOn);
        binding.midiMatchButton.setOnClickListener(v ->
        {
            isMidiOn = !isMidiOn;
            updateSwitchButtonState(binding.midiMatchButton, isMidiOn);
        });
        toolSelectionButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, PianoToolActivity.class);
            startActivity(intent);
        });


        drawerButton.setOnClickListener(v ->
        {
            userInput.clearFocus();
            drawerLayout.openDrawer(findViewById(R.id.fragment_drawer));      //findViewById(R.id.fragment_drawer)
        });

        newChatButton.setOnClickListener(v ->
        {
            chatContextId = -1;
            isNewChat = true;
            titleText.setText("新对话");
            welcomeDisplay.setVisibility(View.VISIBLE);
            binding.toolScroll.setVisibility(View.VISIBLE);
            chatDisplay.removeAllViews();
            newChatButton.setVisibility(View.GONE);
        });
    }

    private void updateSwitchButtonState(MaterialButton button, boolean on)
    {
        if(on)
        {
            button.setAlpha(1);
            button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
            button.setTextColor(getResources().getColor(R.color.white));
            button.setRippleColor(ColorStateList.valueOf(getResources().getColor(R.color.translucent_black)));
        }
        else
        {
            button.setAlpha(0.5f);
            button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.empty)));
            button.setTextColor(getResources().getColor(R.color.black));
            button.setRippleColor(ColorStateList.valueOf(getResources().getColor(R.color.translucent_white)));
        }
    }




    private void sendMessageToAICancel()
    {
        ApiClient.getInstance(this, 120).url(getResources().getString(R.string.server) + "/chat/cancel")
                .parameter("id", String.valueOf(chatContextId))
                .get()
                .callback(new ApiCallback(this)
                {
                    @Override public void onResponse(@NonNull String response)
                    {
                        MainActivity.this.runOnUiThread(() ->
                        {
                            sendButton.setImageResource(R.drawable.btn_send);
                            ApiClient.check(MainActivity.this, response);
                        });
                        isNewChat = true;
                        isGenerating = false;
                        if(generateCall != null)
                        {
                            generateCall.cancel();
                            generateCall = null;
                        }
                    }
                })
                .enqueue();
    }




    private void clearAudioCache()
    {
        try
        {
            Files.walkFileTree(AudioRecorder.audioCacheDir(this), new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException
                {
                    path.toFile().deleteOnExit();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            Log.e(TAG, "clearAudioCache: ", e);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MidiPlayer.stop();
    }
}