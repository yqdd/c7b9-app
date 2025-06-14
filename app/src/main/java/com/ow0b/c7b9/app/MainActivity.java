package com.ow0b.c7b9.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.view.AnalyzeView;
import com.ow0b.c7b9.app.view.ExpandableLayout;
import com.ow0b.c7b9.app.view.PromptRecordView;
import com.ow0b.c7b9.app.view.PromptView;
import com.ow0b.c7b9.app.view.ResponseView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSource;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity INSTANCE;
    private SharedPreferences sharedPreferences;
    private EditText userInput;
    public FrameLayout contentFrame;
    public TextView titleText;
    public LinearLayout chatDisplay, welcomeDisplay;
    private ScrollView chatDisplayScroll;
    private TextView audioIndicator;
    private ImageButton sendButton, recordAudioButton, playAudioButton, deleteAudioButton;
    private MaterialButton audioLLMButton, midiAnalyzeButton;
    private Button toolSelectionButton, drawerButton;
    public Button newChatButton;
    private ConstraintLayout audioBar;
    private SeekBar audioProgressBar;
    public DrawerLayout drawerLayout;
    private MediaPlayer mediaPlayer;
    private Timer mediaPlayerTimer;
    private MediaRecorder mediaRecorder;
    private FileOutputStream audioFileStream;
    private FileDescriptor audioFilePath;
    private final String audioFileName = "audio.m4a";
    public int audioLLMModel = 1;        //0为无，1为Qwen，2为Gemini
    public boolean isMidiOn = false;
    private boolean isRecording = false;
    public boolean isNewChat = false;       //这个用来减少打开Fragment需要的网络请求
    public int chatContextId = -1;

    private static final int REQUEST_PERMISSION_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        INSTANCE = this;

        setContentView(R.layout.activity_main);
        deleteFile(audioFileName);
        try(InputStream input = getAssets().open("testAudio.m4a");
            OutputStream output = openFileOutput(audioFileName, MODE_PRIVATE))
        {
            int b;
            while((b = input.read()) != -1) output.write(b);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        findViewById(R.id.audio_bar).setVisibility(View.VISIBLE);

        //sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userInput = findViewById(R.id.user_input);
        titleText = findViewById(R.id.title_text);
        chatDisplay = findViewById(R.id.chat_display);
        welcomeDisplay = findViewById(R.id.welcome_display);
        chatDisplayScroll = findViewById(R.id.chat_display_scroll);
        audioIndicator = findViewById(R.id.audio_indicator);
        sendButton = findViewById(R.id.send_button);
        recordAudioButton = findViewById(R.id.record_audio_button);
        playAudioButton = findViewById(R.id.play_audio_button);
        deleteAudioButton = findViewById(R.id.delete_audio_button);
        audioBar = findViewById(R.id.audio_bar);
        audioProgressBar = findViewById(R.id.audio_progress_bar);
        audioLLMButton = findViewById(R.id.audio_llm_button);
        midiAnalyzeButton = findViewById(R.id.midi_analyze_button);
        toolSelectionButton = findViewById(R.id.tool_selection_button);
        drawerButton = findViewById(R.id.drawer_button);
        newChatButton = findViewById(R.id.new_chat_button);
        contentFrame = findViewById(R.id.content_frame);
        drawerLayout = findViewById(R.id.drawer_layout);

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
            String text = userInput.getText().toString();
            if(!text.isEmpty())
            {
                welcomeDisplay.setVisibility(View.GONE);
                newChatButton.setVisibility(View.VISIBLE);
                userInput.setText("");

                try(FileInputStream stream = openFileInput(audioFileName))
                {
                    PromptRecordView recordView = new PromptRecordView(this);
                    chatDisplay.addView(recordView);
                    sendMediaToServer(stream, response ->
                    {
                        deleteFile(audioFileName);
                        runOnUiThread(() ->
                        {
                            chatDisplay.addView(new PromptView(this, text));
                            JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                            int id = obj.get("id").getAsInt();
                            recordView.setId(id);
                            sendMessageToAI(text, id);
                        });
                    });
                    audioBar.setVisibility(View.GONE);
                }
                catch (IOException ignore)
                {
                    //音频文件不存在则只发文本
                    chatDisplay.addView(new PromptView(this, text));
                    sendMessageToAI(text, -1);
                }
            }
        });
        recordAudioButton.setOnClickListener(v ->
        {
            if (isRecording)
            {
                stopRecording();
                recordAudioButton.setImageResource(R.drawable.btn_start_record);
            }
            else
            {
                startRecording();
                recordAudioButton.setImageResource(R.drawable.btn_stop_record);
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
        playAudioButton.setOnClickListener(v ->
        {
            if(mediaPlayer == null) playAudio();
            else stopPlayAudio();
        });
        deleteAudioButton.setOnClickListener(v ->
        {
            audioBar.setVisibility(View.GONE);
            deleteFile(audioFileName);
        });
        audioProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override public void onProgressChanged(SeekBar seekBar, int i, boolean b) { }
            @Override public void onStartTrackingTouch(SeekBar seekBar)
            {
                if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar)
            {
                if(mediaPlayer != null && mediaPlayer.isPlaying())
                {
                    stopPlayAudio();
                    playAudio();
                }
                else stopPlayAudio();
            }
        });

        toolSelectionButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, ToolSelectionActivity.class);
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

    public void loadContext(int id)
    {
        try(BufferedReader reader = new BufferedReader(new FileReader(contextFile(this, id))))
        {
            StringBuilder builder = new StringBuilder();
            String str;
            while((str = reader.readLine()) != null) builder.append(str);
            loadContext(builder.toString());
        }
        catch (IOException e) { }
        loadContextFromWeb(id);
    }
    private static File contextFile(Context context, int id)
    {
        return context.getCacheDir().toPath().resolve("context_" + id + ".json").toFile();
    }
    private void loadContextFromWeb(int id)
    {
        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "api/context")
                .parameter("id", String.valueOf(id))
                .get()
                .callback(new ApiCallback(this)
                {
                    @Override
                    public void onResponse(String response)
                    {
                        loadContext(response);
                        //保存到本地
                        try(FileWriter writer = new FileWriter(contextFile(MainActivity.this, id)))
                        {
                            writer.write(response);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .enqueue();
    }
    private void loadContext(String response)
    {
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonArray messages = json.get("data").getAsJsonArray();
        runOnUiThread(() ->
        {
            PromptRecordView[] promptRecord = new PromptRecordView[1];
            chatDisplay.removeAllViews();
            messages.forEach(element ->
            {
                AnalyzeView analyzeView = new AnalyzeView(this);
                if(promptRecord[0] != null)
                {
                    promptRecord[0].setAnalyzeView(analyzeView);
                    promptRecord[0] = null;
                }
                chatDisplay.addView(analyzeView);

                JsonObject obj = element.getAsJsonObject();
                String role = obj.get("role").getAsString();
                String content = obj.get("content").getAsString();
                String reasoning = analyzeView.jsonNullGet(obj, "reasoning", JsonElement::getAsString);
                String audioContent = analyzeView.jsonNullGet(obj, "audioContent", JsonElement::getAsString);
                int audioId = obj.get("audioId").getAsInt();

                analyzeView.compileJsonObject(this, obj);
                if(role.equals("assistant"))
                {
                    if(audioContent != null) chatDisplay.addView(new ExpandableLayout(this)
                    {{
                        setHeaderText("音频理解（QWEN）");
                        addComponent(new ResponseView(MainActivity.this, audioContent, true));
                    }});
                    if(reasoning != null) chatDisplay.addView(new ExpandableLayout(this)
                    {{
                        setHeaderText("深度思考");
                        addComponent(new ResponseView(MainActivity.this, reasoning, true));
                    }});
                    chatDisplay.addView(new ResponseView(MainActivity.this, content));
                }
                else if(role.equals("user"))
                {
                    if(audioId >= 0) chatDisplay.addView(promptRecord[0] = new PromptRecordView(MainActivity.this, audioId));
                    chatDisplay.addView(new PromptView(MainActivity.this, content));
                }
                else Toast.showError(MainActivity.this, "加载对话失败");

            });
        });
    }

    private void sendMediaToServer(FileInputStream stream, Consumer<String> callback)
    {
        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "api/upload")
                .method("POST", encodeAudioFileToStr(audioFileName, stream), "audio/m4a")
                .callback(new ApiCallback(this)
                {
                    @Override
                    public void onResponse(String response)
                    {
                        callback.accept(response);
                    }
                })
                .enqueue();
    }
    private void sendMessageToAI(String message, int audioId)
    {
        AnalyzeView analyzeView = new AnalyzeView(this);
        chatDisplay.addView(analyzeView);

        ResponseView audioView = new ResponseView(this, "", true),
                reasoningView = new ResponseView(this, "", true),
                responseView = new ResponseView(this);
        ExpandableLayout audioExpandable = new ExpandableLayout(this)
        {{
            setHeaderText("音频理解（QWEN）");
            addComponent(audioView);
        }}, reasoningExpandable = new ExpandableLayout(this)
        {{
            setHeaderText("深度思考");
            addComponent(reasoningView);
        }};
        audioExpandable.setVisibility(View.GONE);
        reasoningExpandable.setVisibility(View.GONE);
        chatDisplay.addView(audioExpandable);
        chatDisplay.addView(reasoningExpandable);
        chatDisplay.addView(responseView);

        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        if(audioId != -1) json.addProperty("audioId", audioId);
        if(chatContextId != -1) json.addProperty("id", chatContextId);

        ApiClient.getInstance(this, 120).url(getResources().getString(R.string.server) + "api/chat")
                .method("POST", json)
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        BufferedSource source = response.body().source();
                        StringBuilder contentBuilder = new StringBuilder(),
                                reasoningBuilder = new StringBuilder(),
                                audioBuilder = new StringBuilder();
                        while(!source.exhausted())
                        {
                            String line = source.readUtf8Line();
                            Log.i("TAG", "onResponse: " + line);
                            runOnUiThread(() -> ApiClient.check(MainActivity.this, line));
                            if(line != null && !line.isEmpty())
                            {
                                JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                                runOnUiThread(() ->
                                {
                                    switch (json.get("type").getAsString())
                                    {
                                        case "id" -> chatContextId = json.get("id").getAsInt();
                                        case "message" ->
                                        {
                                            analyzeView.compileJsonObject(MainActivity.this, json);
                                            if(json.get("content") != null)
                                            {
                                                contentBuilder.append(json.get("content").getAsString());
                                                responseView.rend(MainActivity.this, contentBuilder.toString());
                                            }
                                            if(json.get("reasoning") != null)
                                            {
                                                reasoningExpandable.setVisibility(View.VISIBLE);
                                                reasoningBuilder.append(json.get("reasoning").getAsString());
                                                reasoningView.rend(MainActivity.this, reasoningBuilder.toString());
                                            }
                                            if(json.get("audioContent") != null)
                                            {
                                                audioExpandable.setVisibility(View.VISIBLE);
                                                audioBuilder.append(json.get("audioContent").getAsString());
                                                audioView.rend(MainActivity.this, audioBuilder.toString());
                                            }
                                        }
                                        /*
                                        case "process" ->
                                        {
                                            audioView.rend(MainActivity.this, audioView.getText() + json.get("text").getAsString() + "\n");
                                        }
                                         */
                                    }
                                    chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                                });
                            }
                        }
                        isNewChat = true;
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        runOnUiThread(() -> Toast.showError(MainActivity.this, "连接服务器失败，请稍后再试"));
                    }
                })
                .enqueue();
    }
    private String encodeAudioFileToStr(String filePath, FileInputStream stream)
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

    private void playAudio()
    {
        if(mediaPlayer != null) mediaPlayer.release();
        mediaPlayer = new MediaPlayer();
        try(FileInputStream stream = openFileInput(audioFileName))
        {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(stream.getFD());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mediaPlayer ->
            {
                stopPlayAudio();
                audioProgressBar.setProgress(0);
            });

            audioProgressBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.seekTo((int) (((float) audioProgressBar.getProgress() / audioProgressBar.getMax()) * mediaPlayer.getDuration()));
            mediaPlayerTimer = new Timer();
            int[] min = new int[1];
            mediaPlayerTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if(mediaPlayer != null && mediaPlayer.isPlaying())
                    {
                        audioProgressBar.setProgress(Math.max(mediaPlayer.getCurrentPosition(), min[0]));
                        min[0] = audioProgressBar.getProgress();
                    }
                }
            }, 10, 10);

            playAudioButton.setImageResource(R.drawable.btn_stop_play_record);
        }
        catch (IOException e)
        {
            Toast.showError(MainActivity.this, "当前未录制音频");
        }
    }
    private void stopPlayAudio()
    {
        if(mediaPlayerTimer != null) mediaPlayerTimer.cancel();
        if(mediaPlayer != null)
        {
            mediaPlayer.stop();
            mediaPlayer.release();
            playAudioButton.setImageResource(R.drawable.btn_play_record);
        }
        mediaPlayerTimer = null;
        mediaPlayer = null;
    }
    private void startRecording()
    {
        requestPermission();
        if (checkPermission())
        {
            try
            {
                audioFileStream = openFileOutput(audioFileName, MODE_PRIVATE);
                audioFilePath = audioFileStream.getFD();
                setupMediaRecorder();
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                //recordAudioButton.setText("停止");
                audioIndicator.setText("音频录制中...");
                //Toast.makeText(MainActivity.this, "Recording started", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            requestPermission();
        }
    }
    private void stopRecording()
    {
        try
        {
            mediaRecorder.stop();
            mediaRecorder.release();
            audioFileStream.close();
            isRecording = false;
            //recordAudioButton.setText("录制");
            audioIndicator.setText("音频已录制");
            audioBar.setVisibility(View.VISIBLE);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    private void setupMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setOutputFile(audioFilePath);
    }


    private boolean checkPermission()
    {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission()
    {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE)
        {
            if (grantResults.length > 0)
            {
                boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean recordAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (storageAccepted && recordAccepted) {} //Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                //else //Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}