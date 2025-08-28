package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.chat.AiTextView;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.activity.piano.PianoToolActivity;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.activity.main.chat.ChatContextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private EditText userInput;
    public FrameLayout contentFrame;
    public TextView titleText;
    public LinearLayout chatDisplay, welcomeDisplay;
    public ScrollView chatDisplayScroll;
    ImageButton sendButton;
    private ImageButton recordAudioButton;
    private MaterialButton audioLLMButton, midiAnalyzeButton;
    private Button toolSelectionButton, drawerButton;
    public Button newChatButton;
    private UploadResourceListView uploadResources;
    public DrawerLayout drawerLayout;
    public int audioLLMModel = 1;        //0为无，1为Qwen，2为Gemini
    public boolean isMidiOn = false;
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
            if (midi != null) uploadResources.addResource(this, midi);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clearAudioCache();
        MidiPlayer.init(this);

        //sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userInput = findViewById(R.id.user_input);
        titleText = findViewById(R.id.title_text);
        chatDisplay = findViewById(R.id.chat_display);
        welcomeDisplay = findViewById(R.id.welcome_display);
        chatDisplayScroll = findViewById(R.id.chat_display_scroll);
        sendButton = findViewById(R.id.send_button);
        recordAudioButton = findViewById(R.id.record_audio_button);
        uploadResources = findViewById(R.id.upload_bar);
        audioLLMButton = findViewById(R.id.audio_llm_button);
        midiAnalyzeButton = findViewById(R.id.midi_analyze_button);
        toolSelectionButton = findViewById(R.id.tool_selection_button);
        drawerButton = findViewById(R.id.drawer_button);
        newChatButton = findViewById(R.id.new_chat_button);
        contentFrame = findViewById(R.id.content_frame);
        drawerLayout = findViewById(R.id.drawer_layout);
        ApiClient.pingServer(this);


        //TODO 用户页的滚动token数与使用时长切换
        //添加测试用的音频
        try(InputStream input = getAssets().open("testAudio.m4a");
            OutputStream output = new FileOutputStream(AudioRecorder.audioFile(this, "testAudio.m4a")))
        {
            int b;
            while((b = input.read()) != -1) output.write(b);
            uploadResources.addResource("testAudio.m4a");
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

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
            if(isGenerating) sendMessageToAICancel();
            else
            {
                String text = userInput.getText().toString();
                if(!text.isEmpty())
                {
                    welcomeDisplay.setVisibility(View.GONE);
                    newChatButton.setVisibility(View.VISIBLE);
                    userInput.setText("");

                    ChatContextView promptView = new ChatContextView(this);
                    List<FileInputStream> streams = new ArrayList<>();
                    for(int i = 0; i < uploadResources.resources.size(); i ++)
                    {
                        Object res = uploadResources.resources.get(i);
                        if(res instanceof String fileName)
                        {
                            File file = AudioPlayer.audioFile(this, fileName);
                            try(FileInputStream stream = new FileInputStream(file);
                                FileOutputStream outTest = new FileOutputStream(AudioPlayer.audioFile(this, String.valueOf(i))))
                            {
                                int b;
                                while((b = stream.read()) != -1) outTest.write(b);
                                streams.add(new FileInputStream(AudioPlayer.audioFile(this, String.valueOf(i))));
                                promptView.newAudio(i);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    promptView.newUserText().setText(text);
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
                                    <locateAudio rid="1" skip="2" tip="点我播放音频" />
                                    123123
                                    <forceChart />
                                    <speedChart />
                                    <midiChart />
                                    123123
                                    """);
                        }});
                         */
                        ChatUtils.sendMessageToAI(this, promptView, text, chatContextId, new int[0]);
                    }
                    else
                    {
                        uploadResources.clear();
                        sendAudioToServer(streams, response ->
                        {
                            runOnUiThread(() ->
                            {
                                //chatDisplay.addView(new UserPromptView(this));     // text
                                //JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                                //int id = obj.get("id").getAsInt();
                                //recordView.setId(id);
                                //ChatUtils.sendMessageToAI(text, id);
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
                //TODO 这里要做个删除（现在多文件要保存多个文件名删）
                //TODO 或者上传后直接用rid保存到本地？？？？
                String fileName = AudioRecorder.stopRecording();
                uploadResources.addResource(fileName);
                recordAudioButton.setImageResource(R.drawable.btn_record_start);
            }
            else
            {
                AudioRecorder.startRecording(this, "record:" + Instant.now() + ".m4a");
                recordAudioButton.setImageResource(R.drawable.btn_record_stop);
            }
        });
        updateSwitchButtonState(audioLLMButton, true);
        audioLLMButton.setOnClickListener(v ->
        {
            switch (audioLLMModel)
            {
                case 0 :
                    audioLLMModel = 1;
                    audioLLMButton.setText("音频理解 (QWEN)");
                    break;
                case 1 :
                    audioLLMModel = 2;
                    audioLLMButton.setText("音频理解 (GEMINI)");
                    break;
                case 2 :
                    audioLLMModel = 0;
                    audioLLMButton.setText("音频理解");
                    break;
            }
            updateSwitchButtonState(audioLLMButton, audioLLMModel > 0);
        });
        updateSwitchButtonState(midiAnalyzeButton, isMidiOn);
        midiAnalyzeButton.setOnClickListener(v ->
        {
            isMidiOn = !isMidiOn;
            updateSwitchButtonState(midiAnalyzeButton, isMidiOn);
        });

        toolSelectionButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, PianoToolActivity.class);
            startActivity(intent);
        });


        drawerButton.setOnClickListener(v ->
        {
            drawerLayout.openDrawer(findViewById(R.id.fragment_drawer));      //findViewById(R.id.fragment_drawer)
        });

        newChatButton.setOnClickListener(v ->
        {
            chatContextId = -1;
            isNewChat = true;
            titleText.setText("新对话");
            welcomeDisplay.setVisibility(View.VISIBLE);
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



    private void sendAudioToServer(Collection<FileInputStream> streams, Consumer<String> callback)
    {
        int[] over = new int[1];
        for(FileInputStream stream : streams)
        {
            ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "/resource/upload")
                    .method("POST", encodeAudioFileToStr(stream), "audio/m4a")
                    .parameter("type", "audio")
                    .callback(new ApiCallback(this)
                    {
                        @Override
                        public void onResponse(String response)
                        {
                            try
                            {
                                stream.close();
                                over[0] ++;
                                if(over[0] == streams.size()) callback.accept(response);
                            }
                            catch (IOException e)
                            {
                                Log.e(TAG, "onResponse: ", e);
                            }
                        }
                    })
                    .enqueue();
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


    private String encodeAudioFileToStr(FileInputStream stream)
    {
        try
        {
            byte[] bytes = new byte[stream.available()];
            int length = stream.read(bytes);
            return Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, length));
        }
        catch (IOException e)
        {
            return null;
        }
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