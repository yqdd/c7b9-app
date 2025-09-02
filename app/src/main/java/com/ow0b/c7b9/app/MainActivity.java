package com.ow0b.c7b9.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.ow0b.c7b9.app.api.ApiClientFactory;
import com.ow0b.c7b9.app.api.ApiClientImpl;
import com.ow0b.c7b9.app.old.activity.main.AudioPlayerImpl;
import com.ow0b.c7b9.app.old.activity.main.AudioService;
import com.ow0b.c7b9.app.old.activity.main.ChatService;
import com.ow0b.c7b9.app.old.activity.main.ChatUtils;
import com.ow0b.c7b9.app.old.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.old.activity.piano.PianoToolActivity;
import com.ow0b.c7b9.app.databinding.ActivityMainBinding;
import com.ow0b.c7b9.app.old.util.midi.Midi;
import com.ow0b.c7b9.app.view.chat.ChatContext;
import com.ow0b.c7b9.app.view.chat.ChatContextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements TempDir
{
    private static final String TAG = "Chat";
    public ActivityMainBinding binding;
    public int sid = -1;

    @Inject
    private Gson gson;
    @Inject
    private AudioService audioService;
    @Inject
    private ChatService chatService;

    @Override
    @SuppressLint("NewApi")
    protected void onStart()
    {
        super.onStart();
        //加载其他activity启动main传入的资源
        Bundle bundle = getIntent().getExtras();
        Midi midi;
        if(bundle != null && (midi = bundle.getSerializable("midi", Midi.class)) != null)
        {
            Log.d(TAG, "onResume: " + midi);
            audioService.uploadMidiJson(this, gson.toJson(midi));
        }
    }

    private void ping()
    {
        ApiClientFactory.ping(this, getString(R.string.server) + "/alive", alive ->
        {
            if(!alive)
            {
                runOnUiThread(() ->
                {
                    //显示无法连接到服务器提示窗
                    View view = View.inflate(this, R.layout.layout_server_dead, null);
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setView(view)
                            .create();
                    TextView ok = view.findViewById(R.id.server_dead_ok);
                    ok.setOnClickListener(v -> dialog.cancel());

                    dialog.show();
                });
            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(R.layout.activity_main);
        ping();

        binding.chatDisplayScroll.setVerticalScrollBarEnabled(false);
        binding.chatDisplay.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
        {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7)
            {
                binding.chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                binding.chatDisplay.removeOnLayoutChangeListener(this);
            }
        });
        binding.sendButton.setOnClickListener(v ->
        {
            String text = binding.userInput.getText().toString();
            if(!text.isEmpty())
            {
                binding.welcomeDisplay.setVisibility(View.GONE);
                binding.newChatButton.setVisibility(View.VISIBLE);
                binding.userInput.setText("");

                ChatContextView chatContext = new ChatContextView(this, audioService);
                List<File> audios = binding.uploadBar.audios();
                audios.forEach(f -> chatContext.newAudio(audioService.upload(this, f)));
                chatContext.newUserText().setText(text);
                binding.chatDisplay.addView(chatContext);

                    if(audios.isEmpty())
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
                        chatService.send();
                        //ChatUtils.sendMessageToAI(this, promptView, text, chatContextId, new int[0]);
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
                    if(!ApiClientImpl.serverAlive)
                    {
                        chatDisplay.addView(new ChatContextView(this)
                        {{
                            newAiText().append("无法连接到服务器");
                        }});
                    }
                }
        });
        binding.recordAudioButton.setOnClickListener(v ->
        {
            if (audioService.isRecording())
            {
                //TODO 这里要做个删除（现在多文件要保存多个文件名删）
                //TODO 或者上传后直接用rid保存到本地？？？？
                String fileName = audioService.stopRecording();
                binding.uploadBar.addResource(fileName);
                binding.recordAudioButton.setImageResource(R.drawable.btn_record_start);
            }
            else
            {
                audioService.startRecording(this, "record:" + Instant.now() + ".m4a");
                binding.recordAudioButton.setImageResource(R.drawable.btn_record_stop);
            }
        });
        binding.toolSelectionButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, PianoToolActivity.class);
            startActivity(intent);
        });
        binding.drawerButton.setOnClickListener(v ->
        {
            binding.drawerLayout.openDrawer(findViewById(R.id.fragment_drawer));      //findViewById(R.id.fragment_drawer)
        });
        binding.newChatButton.setOnClickListener(v ->
        {
            sid = -1;
            binding.titleText.setText("新对话");
            binding.welcomeDisplay.setVisibility(View.VISIBLE);
            binding.chatDisplay.removeAllViews();
            binding.newChatButton.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        MidiPlayer.stop();
    }
}