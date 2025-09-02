package com.ow0b.c7b9.app.activity.main;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.main.chat.AiTextView;
import com.ow0b.c7b9.app.activity.main.chat.ChatContextView;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.activity.main.chat.ExpandableLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okio.BufferedSource;

public class ChatUtils
{
    public static Gson gson = new GsonBuilder().create();
    public static void sendMessageToAI(MainActivity activity, ChatContextView chatContext, String message, int chatContextId, int[] res)
    {
        Runnable generateFinish = () ->
        {
            activity.runOnUiThread(() ->
            {
                activity.sendButton.setImageResource(R.drawable.btn_send);
                activity.isGenerating = false;
            });
        };
        ApiClient.getInstance(activity, 120).url(activity.getResources().getString(R.string.server) + "/chat")
                .parameter("id", String.valueOf(chatContextId))
                .parameter("message", message)
                .method("POST", res)
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        Log.i("ChatUtils", "request: " + call.request());
                        activity.generateCall = call;
                        BufferedSource source = response.body().source();
                        sendMessageResponse(activity, chatContext, source);
                        activity.isNewChat = true;
                        generateFinish.run();
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

        AiTextView reasoningView = chatContext.newAiText("深度思考"), contentView = chatContext.newAiText();
        activity.runOnUiThread(() ->
        {
            activity.chatDisplay.addView(new ExpandableLayout(activity)
            {{
                setHeaderText("深度思考");
                addComponent(reasoningView);
            }});
            activity.chatDisplay.addView(contentView);
        });
        while(!source.exhausted())
        {
            String line = source.readUtf8Line();
            Log.i("ChatUtils", "line: " + line);
            activity.runOnUiThread(() -> ApiClient.check(activity, line));
            if(line != null && !line.isEmpty())
            {
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

                String role = json.get("role").getAsString();
                String content = json.get("content").getAsString();
                String reasoning = json.get("reasoning").getAsString();
                //String audioContent = analyzeView.jsonNullGet(obj, "audioContent", JsonElement::getAsString);
                //int audioId = obj.get("audioId").getAsInt();

                //analyzeView.compileJsonObject(activity, obj);
                if(role.equals("assistant"))
                {
                    if(!reasoning.isEmpty())
                        chatContext.newAiText("深度思考").append(reasoning);
                    //if(!audioContent.isEmpty())
                    //    chatContext.newAiText("音频理解").append(audioContent);

                    chatContext.newAiText().append(content);
                }
                else if(role.equals("user")) chatContext.newUserText().setText(content);
                else Toast.showError(activity, "加载对话失败");
            });
        });
    }
    private static File contextFile(Context context, int id)
    {
        return context.getCacheDir().toPath().resolve("context_" + id + ".json").toFile();
    }
}
