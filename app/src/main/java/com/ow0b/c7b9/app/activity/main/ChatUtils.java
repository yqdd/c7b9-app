package com.ow0b.c7b9.app.activity.main;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.api.Api;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.chat.AiTextView;
import com.ow0b.c7b9.app.activity.main.chat.ChatContextView;
import com.ow0b.c7b9.app.api.ChatService;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.activity.main.chat.ExpandableLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

public class ChatUtils
{
    private static final String TAG = "ChatUtils";
    public static ChatService chatService = Api.create(ChatService.class);
    public static Gson gson = new GsonBuilder().create();
    public static void sendMessageToAI(MainActivity activity, ChatContextView chatContext, String message, Integer sid, List<Integer> audios)
    {
        Runnable generateFinish = () ->
        {
            activity.runOnUiThread(() ->
            {
                activity.sendButton.setImageResource(R.drawable.btn_send);
                activity.isGenerating = false;
            });
        };
        /*
        Log.i(TAG, "sendMessageToAI: " + Api.header(activity));
        retrofit2.Call<ResponseBody> call = audios == null ?
                chatService.chat(Api.header(activity), message, sid) :
                chatService.chat(Api.header(activity), message, sid, audios);
        call.enqueue(new retrofit2.Callback<>() { });
        */
        ApiClient.getInstance(activity, 120).url(activity.getResources().getString(R.string.server) + "/chat")
                .parameter("id", String.valueOf(sid))
                .parameter("message", message)
                .method("POST", audios)
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        Log.i("ChatUtils", "request: " + call.request());
                        try
                        {
                            activity.generateCall = call;
                            sendMessageResponse(activity, chatContext, response.body().charStream());
                            generateFinish.run();
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        activity.runOnUiThread(() -> Toast.showError(activity, "连接服务器失败，请稍后再试"));
                        generateFinish.run();
                    }
                })
                .enqueue();
    }

    private static void sendMessageResponse(MainActivity activity, ChatContextView chatContext, BufferedSource source) throws IOException
    {
        AiTextView[] reasoningView = new AiTextView[1], contentView = new AiTextView[1];
        activity.runOnUiThread(() ->
        {
            reasoningView[0] = chatContext.newAiText("深度思考");
            contentView[0] = chatContext.newAiText();
            reasoningView[0].setVisibility(View.GONE);
        });

        while(!source.exhausted())
        {
            String line = source.readUtf8Line();
            Log.i("ChatUtils", "line: " + line);
            activity.runOnUiThread(() -> ApiClient.check(activity, line));
            if(line != null && !line.isEmpty())
            {
                Log.i("ChatUtils", "line: " + line);
                try
                {
                    JsonElement element = JsonParser.parseString(line);
                    if(element.isJsonObject())
                    {
                        JsonObject json = element.getAsJsonObject();
                        activity.runOnUiThread(() ->
                        {
                            switch (json.get("type").getAsString())
                            {
                                case "id" ->
                                {
                                    activity.chatContextId = json.get("id").getAsInt();
                                    //新对话需要等待 chatContextId 赋值后才能取消
                                    activity.sendButton.setImageResource(R.drawable.btn_send_cancel);
                                    activity.isGenerating = true;
                                }
                                case "message" ->
                                {
                                    if(json.get("content") != null)
                                        contentView[0].append(json.get("content").getAsString());
                                    if(json.get("reasoning") != null)
                                    {
                                        reasoningView[0].setVisibility(View.VISIBLE);
                                        reasoningView[0].append(json.get("reasoning").getAsString());
                                    }
                                }
                            }
                            activity.chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                        });
                    }
                }
                catch (Exception e)
                {
                    Log.i(TAG, "出现错误: " + line);
                }



                /*
                JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                activity.runOnUiThread(() ->
                {
                    switch (json.get("type").getAsString())
                    {
                        case "id" ->
                        {
                            activity.chatContextId = json.get("id").getAsInt();
                            //新对话需要等待 chatContextId 赋值后才能取消
                            activity.sendButton.setImageResource(R.drawable.btn_send_cancel);
                            activity.isGenerating = true;
                        }
                        case "message" ->
                        {
                            if(json.get("content") != null)
                                contentView.append(json.get("content").getAsString());
                            if(json.get("reasoning") != null)
                                reasoningView.append(json.get("reasoning").getAsString());
                        }
                    }
                    activity.chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                });

                 */
            }
        }
    }
    @Deprecated
    private static void sendMessageResponse(MainActivity activity, ChatContextView chatContext, Reader reader) throws IOException
    {
        boolean[] reasoning = new boolean[] {true};
        AiTextView[] reasoningView = new AiTextView[1], contentView = new AiTextView[1];
        activity.runOnUiThread(() ->
        {
            reasoningView[0] = chatContext.newAiText("深度思考");
            contentView[0] = chatContext.newAiText();
        });
        /*
        activity.runOnUiThread(() ->
        {
            activity.chatDisplay.addView(new ExpandableLayout(activity)
            {{
                setHeaderText("深度思考");
                addComponent(reasoningView);
            }});
            activity.chatDisplay.addView(contentView);
        });
         */
        BufferedReader bufferedReader = new BufferedReader(reader);
        String line;
        while((line = bufferedReader.readLine()) != null)
        {
            Log.i("ChatUtils", "line: " + line);
            String finalLine = line;
            activity.runOnUiThread(() -> ApiClient.check(activity, finalLine));
            if(!line.isEmpty())
            {
                try
                {
                    JsonElement element = JsonParser.parseString(line);
                    if(element.isJsonObject())
                    {
                        JsonObject json = element.getAsJsonObject();
                        activity.runOnUiThread(() ->
                        {
                            switch (json.get("type").getAsString())
                            {
                                case "context" ->
                                {
                                    if(json.get("id") != null)
                                    {
                                        activity.chatContextId = json.get("id").getAsInt();
                                        //新对话需要等待 chatContextId 赋值后才能取消
                                        activity.sendButton.setImageResource(R.drawable.btn_send_cancel);
                                        activity.isGenerating = true;
                                    }
                                }
                                case "message" ->
                                {
                                    if(json.get("content") != null)
                                    {
                                        if(reasoning[0]) contentView[0] = chatContext.newAiText();
                                        reasoning[0] = false;
                                        contentView[0].append(json.get("content").getAsString());
                                    }
                                    if(json.get("reasoning") != null)
                                    {
                                        if(!reasoning[0]) reasoningView[0] = chatContext.newAiText("深度思考");
                                        reasoning[0] = true;
                                        reasoningView[0].setVisibility(View.VISIBLE);
                                        reasoningView[0].append(json.get("reasoning").getAsString());
                                    }
                                }
                            }
                            activity.chatDisplayScroll.fullScroll(View.FOCUS_DOWN);
                        });
                    }
                }
                catch (Exception e)
                {
                    Log.i(TAG, "出现错误: " + line);
                }
            }
        }
    }


    private static void loadContextFromWeb(MainActivity activity, ChatContextView promptView, int id)
    {
        ApiClient.getInstance(activity).url(activity.getResources().getString(R.string.server) + "/context")
                .parameter("id", String.valueOf(id))
                .get()
                .callback(new ApiCallback(activity)
                {
                    @Override
                    public void onResponse(String response)
                    {
                        loadContext(activity, response);
                        //保存到本地
                        try(FileWriter writer = new FileWriter(contextFile(activity, id)))
                        {
                            writer.write(response);
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                        //加载完前文后再加载没有生成完的内容
                        ApiClient.getInstance(activity).url(activity.getResources().getString(R.string.server) + "/chat/reconnect")
                                .parameter("id", String.valueOf(id))
                                .get()
                                .callback(new Callback()
                                {
                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                                    {
                                        if(response.code() == 200) sendMessageResponse(activity, promptView, response.body().source());
                                    }
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                                    {
                                        Toast.showError(activity, "连接服务器失败，请稍后再试");
                                    }
                                })
                                .enqueue();
                    }
                })
                .enqueue();
    }
    public static void loadContext(MainActivity activity, ChatContextView promptView, int id)
    {
        try(BufferedReader reader = new BufferedReader(new FileReader(contextFile(activity, id))))
        {
            StringBuilder builder = new StringBuilder();
            String str;
            while((str = reader.readLine()) != null) builder.append(str);
            loadContext(activity, builder.toString());
        }
        catch (IOException e) { }
        loadContextFromWeb(activity, promptView, id);
    }
    public static void loadContext(MainActivity activity, String response)
    {
        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        JsonArray array = obj.get("data").getAsJsonArray();
        ChatContextView chatContext = new ChatContextView(activity);
        activity.runOnUiThread(() ->
        {
            activity.chatDisplay.removeAllViews();
            activity.chatDisplay.addView(chatContext);
            array.forEach(ele ->
            {
                JsonObject json = ele.getAsJsonObject();
                /*
                AnalyzeView analyzeView = new AnalyzeView(activity);
                if(promptRecord[0] != null)
                {
                    promptRecord[0].setAnalyzeView(analyzeView);
                    promptRecord[0] = null;
                }
                activity.chatDisplay.addView(analyzeView);
                 */

                String role = json.get("role").getAsString().toLowerCase();
                String content = json.get("content").getAsString();
                String reasoning = json.get("reasoning").getAsString();
                //String audioContent = analyzeView.jsonNullGet(obj, "audioContent", JsonElement::getAsString);
                JsonArray aids = json.get("audios").isJsonNull() ? null : json.get("audios").getAsJsonArray();

                //analyzeView.compileJsonObject(activity, obj);
                if(role.equals("assistant") || role.equals("tool"))
                {
                    if(!reasoning.isEmpty())
                        chatContext.newAiText("深度思考").append(reasoning);
                    //if(!audioContent.isEmpty())
                    //    chatContext.newAiText("音频理解").append(audioContent);

                    chatContext.newAiText().append(content);
                }
                else if(role.equals("user"))
                {
                    if(aids != null)
                        aids.forEach(id -> chatContext.newAudio(id.getAsInt()));
                    chatContext.newUserText().setText(content);
                }
                else Toast.showError(activity, "加载对话失败：role=" + role);
            });
        });
    }
    private static File contextFile(Context context, int id)
    {
        return context.getCacheDir().toPath().resolve("context_" + id + ".json").toFile();
    }
}
