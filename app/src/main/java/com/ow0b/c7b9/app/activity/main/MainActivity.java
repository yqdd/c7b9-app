package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.content.Context;
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
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.piano.MidiPlayer;
import com.ow0b.c7b9.app.activity.piano.PianoToolActivity;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.midi.Midi;
import com.ow0b.c7b9.app.view.AnalyzeView;
import com.ow0b.c7b9.app.view.ExpandableLayout;
import com.ow0b.c7b9.app.view.PromptRecordView;
import com.ow0b.c7b9.app.activity.main.chat.UserPromptView;
import com.ow0b.c7b9.app.activity.main.chat.AiResponseView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSource;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPreferences;
    private EditText userInput;
    public FrameLayout contentFrame;
    public TextView titleText;
    public LinearLayout chatDisplay, welcomeDisplay;
    private ScrollView chatDisplayScroll;
    private ImageButton sendButton, recordAudioButton;
    private MaterialButton audioLLMButton, midiAnalyzeButton;
    private Button toolSelectionButton, drawerButton;
    public Button newChatButton;
    private UploadResourceListView uploadResources;
    public DrawerLayout drawerLayout;
    public int audioLLMModel = 1;        //0为无，1为Qwen，2为Gemini
    public boolean isMidiOn = false;
    public boolean isNewChat = false;       //这个用来减少打开Fragment需要的网络请求
    public int chatContextId = -1;
    private boolean isGenerating = false;
    private Call generateCall = null;

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

                    UserPromptView promptView = new UserPromptView(this);
                    AiResponseView responseView = new AiResponseView(this, promptView);
                    chatDisplay.addView(promptView);
                    chatDisplay.addView(responseView);

                    promptView.newText().setText(text);
                    responseView.rend(this, """
                                在Spring MVC集成MyBatis的场景下，事务的提交时机**不会**在`@ModelAttribute`方法调用后自动触发，而是由Spring的事务管理机制控制。以下是关键点的详细说明：
                                
                                ---
                                
                                ### 1. **默认情况下，`@ModelAttribute`方法不开启事务**
                                   - `@ModelAttribute`方法通常用于准备模型数据，与数据库的交互通常是**只读操作**。
                                   - 即使方法中包含MyBatis的写操作（如`insert/update`），如果没有显式配置事务，MyBatis会默认**自动提交**（前提是使用的`SqlSession`是非事务性的，例如`SqlSessionTemplate`配置为`ExecutorType.SIMPLE`）。
                                   - 但这种情况不推荐，实际项目中应通过Spring事务管理控制写操作。
                                
                                {"playAudio":0, "skip":4.6}
                                
                                ---
                                
                                ### 2. **事务提交的触发条件**
                                   - 事务的提交或回滚由**`@Transactional`注解**或**声明式事务配置**决定，通常作用在Service层方法上。
                                   - 只有当标记了`@Transactional`的方法**成功执行完毕**时，事务才会提交。如果方法抛出未捕获的异常，事务会回滚。
                                   - **示例：**
                                     ```java
                                     @Service
                                     public class UserService {
                                         @Transactional
                                         public void updateUser(User user) {
                                             userMapper.update(user); // 此处的MyBatis操作会在方法成功后提交
                                         }
                                     }
                                     ```
                                
                                ---
                                
                                ### 3. **`@ModelAttribute`方法的特殊情况**
                                   - 如果`@ModelAttribute`方法被意外标记了`@Transactional`，则事务会在该方法执行完成后提交（但这是**不推荐的实践**，因为`@ModelAttribute`应专注于模型准备，而非业务逻辑）。
                                   - 如果Controller类全局使用了`@Transactional`（极端不推荐），则所有请求处理方法（包括`@ModelAttribute`）会共享同一事务，事务会在请求处理完成后提交。
                                
                                ---
                                
                                ### 4. **MyBatis与Spring事务的协作**
                                   - MyBatis通过`SqlSessionTemplate`与Spring事务管理器（如`DataSourceTransactionManager`）集成。
                                   - 事务的生命周期由Spring管理，MyBatis仅作为执行SQL的工具，**不直接控制事务边界**。
                                
                                ---
                                
                                ### 5. **验证事务行为的方法**
                                   - 在`@ModelAttribute`方法中插入数据后，**主动抛出异常**，观察数据是否回滚。
                                   - 检查日志中是否有`Committing JDBC transaction`或`Rolling back JDBC transaction`（需开启Spring事务日志：`logging.level.org.springframework.transaction=DEBUG`）。
                                
                                ---
                                
                                ### 总结
                                - **正常情况**：`@ModelAttribute`方法中的MyBatis操作不会自动触发事务提交，除非显式配置了错误的事务管理。
                                - **最佳实践**：将数据库写操作放在Service层的`@Transactional`方法中，确保事务可控。
                                
                                如果有其他特殊配置（如手动事务管理或自定义AOP），需要结合具体代码分析。
                                """);

                    for(Object res : uploadResources.resources)
                    {
                        if(res instanceof String fileName)
                        {
                            File file = AudioPlayer.audioFile(this, fileName);
                            try(FileInputStream stream = new FileInputStream(file);
                                FileOutputStream outTest = new FileOutputStream(AudioPlayer.audioFile(this, "0")))
                            {
                                int b;
                                while((b = stream.read()) != -1) outTest.write(b);
                                promptView.newAudio(0);
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    /*
                    try(FileInputStream stream = openFileInput(audioFileName))
                    {
                        PromptRecordView recordView = new PromptRecordView(this);
                        chatDisplay.addView(recordView);
                        sendMediaToServer(stream, response ->
                        {
                            deleteFile(audioFileName);
                            runOnUiThread(() ->
                            {
                                chatDisplay.addView(new UserPromptView(this));     // text
                                JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
                                int id = obj.get("id").getAsInt();
                                recordView.setId(id);
                                sendMessageToAI(text, id);
                            });
                        });
                        //uploadResources.setVisibility(View.GONE);
                    }
                    catch (IOException ignore)
                    {
                        //音频文件不存在则只发文本
                        chatDisplay.addView(new UserPromptView(this));      //, text
                        sendMessageToAI(text, -1);
                    }
                     */
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
                        //加载完前文后再加载没有生成完的内容
                        ApiClient.getInstance(MainActivity.this).url(getResources().getString(R.string.server) + "api/context/reconnect")
                                .parameter("id", String.valueOf(id))
                                .get()
                                .callback(new Callback()
                                {
                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                                    {
                                        if(response.code() == 200) sendMessageResponse(response.body().source());
                                    }
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                                    {
                                        Toast.showError(MainActivity.this, "连接服务器失败，请稍后再试");
                                    }
                                })
                                .enqueue();
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
                        //addComponent(new AiResponseView(MainActivity.this, audioContent, true));
                    }});
                    if(reasoning != null) chatDisplay.addView(new ExpandableLayout(this)
                    {{
                        setHeaderText("深度思考");
                        //addComponent(new AiResponseView(MainActivity.this, reasoning, true));
                    }});
                    //chatDisplay.addView(new AiResponseView(MainActivity.this, content));
                }
                else if(role.equals("user"))
                {
                    if(audioId >= 0) chatDisplay.addView(promptRecord[0] = new PromptRecordView(MainActivity.this, audioId));
                    chatDisplay.addView(new UserPromptView(MainActivity.this));     //, content
                }
                else Toast.showError(MainActivity.this, "加载对话失败");

            });
        });
    }

    private void sendMediaToServer(FileInputStream stream, Consumer<String> callback)
    {
        /*
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
         */
    }
    private void sendMessageToAICancel()
    {
        ApiClient.getInstance(this, 120).url(getResources().getString(R.string.server) + "api/context/cancel")
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
    private void sendMessageToAI(String message, int audioId)
    {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        if(audioId != -1) json.addProperty("audioId", audioId);
        if(chatContextId != -1) json.addProperty("id", chatContextId);

        Runnable generateFinish = () ->
        {
            runOnUiThread(() ->
            {
                sendButton.setImageResource(R.drawable.btn_send);
                isGenerating = false;
            });
        };
        ApiClient.getInstance(this, 120).url(getResources().getString(R.string.server) + "api/chat")
                .method("POST", json)
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        generateCall = call;
                        BufferedSource source = response.body().source();
                        sendMessageResponse(source);
                        isNewChat = true;
                        generateFinish.run();
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        runOnUiThread(() -> Toast.showError(MainActivity.this, "连接服务器失败，请稍后再试"));
                        generateFinish.run();
                    }
                })
                .enqueue();
    }
    private void sendMessageResponse(BufferedSource source) throws IOException
    {
        AnalyzeView[] analyzeView = new AnalyzeView[1];
        AiResponseView[] audioView = new AiResponseView[1], reasoningView = new AiResponseView[1], responseView = new AiResponseView[1];
        ExpandableLayout[] audioExpandable = new ExpandableLayout[1], reasoningExpandable = new ExpandableLayout[1];
        runOnUiThread(() ->
        {
            analyzeView[0] = new AnalyzeView(this);
            //audioView[0] = new AiResponseView(this, "", true);
            //reasoningView[0] = new AiResponseView(this, "", true);
            //responseView[0] = new AiResponseView(this);
            audioExpandable[0] = new ExpandableLayout(MainActivity.this)
            {{
                setHeaderText("音频理解（QWEN）");
                addComponent(audioView[0]);
            }};
            reasoningExpandable[0] = new ExpandableLayout(MainActivity.this)
            {{
                setHeaderText("深度思考");
                addComponent(reasoningView[0]);
            }};
            audioExpandable[0].setVisibility(View.GONE);
            reasoningExpandable[0].setVisibility(View.GONE);

            chatDisplay.addView(analyzeView[0]);
            chatDisplay.addView(audioExpandable[0]);
            chatDisplay.addView(reasoningExpandable[0]);
            chatDisplay.addView(responseView[0]);
        });

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
                        case "id" ->
                        {
                            chatContextId = json.get("id").getAsInt();
                            //新对话需要等待 chatContextId 赋值后才能取消
                            runOnUiThread(() ->
                            {
                                sendButton.setImageResource(R.drawable.btn_send_cancel);
                                isGenerating = true;
                            });
                        }
                        case "message" ->
                        {
                            analyzeView[0].compileJsonObject(MainActivity.this, json);
                            if(json.get("content") != null)
                            {
                                contentBuilder.append(json.get("content").getAsString());
                                responseView[0].rend(MainActivity.this, contentBuilder.toString());
                            }
                            if(json.get("reasoning") != null)
                            {
                                reasoningExpandable[0].setVisibility(View.VISIBLE);
                                reasoningBuilder.append(json.get("reasoning").getAsString());
                                reasoningView[0].rend(MainActivity.this, reasoningBuilder.toString());
                            }
                            if(json.get("audioContent") != null)
                            {
                                audioExpandable[0].setVisibility(View.VISIBLE);
                                audioBuilder.append(json.get("audioContent").getAsString());
                                audioView[0].rend(MainActivity.this, audioBuilder.toString());
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

    /*
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
     */
}