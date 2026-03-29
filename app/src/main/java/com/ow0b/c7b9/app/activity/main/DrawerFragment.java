package com.ow0b.c7b9.app.activity.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.ow0b.c7b9.app.AccountOptionsActivity;
import com.ow0b.c7b9.app.LoginActivity;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.activity.chord.ChordComposeActivity;
import com.ow0b.c7b9.app.activity.leaderboard.LeaderboardActivity;
import com.ow0b.c7b9.app.activity.main.chat.ChatContextView;
import com.ow0b.c7b9.app.activity.metronome.MetronomeActivity;
import com.ow0b.c7b9.app.activity.practise.PractiseActivity;
import com.ow0b.c7b9.app.activity.rhythm.RhythmActivity;
import com.ow0b.c7b9.app.databinding.FragmentDrawerBinding;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DrawerFragment extends Fragment
{
    private static final Gson gson = new GsonBuilder().serializeNulls().create();
    public static DrawerFragment INSTANCE;
    private FragmentDrawerBinding binding;
    public String TAG = "侧边栏";
    private ConstraintLayout fragmentHeader;
    private ImageView userAvatar;
    private TextView username, token;
    private ListView conversationsList;
    private ConversationsAdapter conversationsAdapter;
    private SharedPreferences sharedPreferences;

    private int lastTouchRawX = 0;
    private int lastTouchRawY = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        INSTANCE = this;
        binding = FragmentDrawerBinding.inflate(inflater, container, false);
        View view = binding.getRoot();


        fragmentHeader = view.findViewById(R.id.fragment_header);
        userAvatar = view.findViewById(R.id.fragment_user_avatar);
        username = view.findViewById(R.id.fragment_username);
        token = view.findViewById(R.id.fragment_token);
        conversationsList = view.findViewById(R.id.conversations_list);
        sharedPreferences = ApiClient.getSharedPreferences(getContext());

        conversationsAdapter = new ConversationsAdapter(getContext(), new ArrayList<>());
        conversationsList.setAdapter(conversationsAdapter);
        conversationsList.setOnItemClickListener((adapterView, view1, i, l) ->
        {
            if(adapterView.getAdapter() instanceof ConversationsAdapter adapter &&
                    getActivity() instanceof MainActivity activity)
            {
                activity.newChatButton.setVisibility(View.VISIBLE);
                Conversation conversation = adapter.conversations.get(i);
                activity.chatContextId = conversation.id;
                activity.titleText.setText(conversation.refer);
                activity.welcomeDisplay.setVisibility(View.GONE);
                activity.chatDisplay.removeAllViews();
                Log.i(TAG, "context id:" + conversation.id);
                ChatUtils.loadContext(activity, new ChatContextView(activity), conversation.id);
                activity.drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
        // 记录最近一次触摸的全局坐标
        conversationsList.setOnTouchListener((v, event) ->
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                lastTouchRawX = (int) event.getRawX();
                lastTouchRawY = (int) event.getRawY();
            }
            return false; // 不能消费事件，否则ListView无法正常响应点击和长按
        });
        conversationsList.setOnItemLongClickListener((parent, view2, position, id) ->
        {
            showPopupMenu(view, conversationsAdapter.conversations.get(position).id);
            return true;
        });

        fragmentHeader.setOnClickListener(v ->
        {
            if (isLoggedIn()) navigateToAccountOptions();
            else navigateToLogin();
        });

        //if(sharedPreferences.contains("permit"))
        init();

        return view;
    }
    public void init()
    {
        loadConversations();
        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "/user/info")
                .get()
                .callback(new ApiCallback(getActivity())
                {
                    @Override
                    public void onResponse(@NonNull String response)
                    {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        getActivity().runOnUiThread(() ->
                        {
                            if(json.get("name") != null)
                                username.setText(json.get("name").getAsString());
                            if(json.get("token") != null)
                                token.setText("消耗token：" + json.get("token").getAsInt());
                        });
                    }
                })
                .enqueue();

        userAvatar.setImageResource(R.drawable.ic_user_avatar);
    }

    private void showPopupMenu(View anchorView, int id)
    {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.layout_popupmenu, null);
        int width = (int) (getResources().getDisplayMetrics().density * 120 + 0.5f);
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setOutsideTouchable(true);

        TextView rename = popupView.findViewById(R.id.menu_rename);
        TextView delete = popupView.findViewById(R.id.menu_delete);

        rename.setOnClickListener(v ->
        {
            popupWindow.dismiss();
            showRenameDialog(id);
        });
        delete.setOnClickListener(v ->
        {
            popupWindow.dismiss();
            ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "/context/delete")
                    .get()
                    .parameter("id", String.valueOf(id))
                    .callback(new ApiCallback(getActivity())
                    {
                        @Override
                        public void onResponse(String response)
                        {
                            getActivity().runOnUiThread(() ->
                            {
                                if(ApiClient.check(getContext(), response).equals("error")) return;
                                loadConversations();
                            });
                        }
                    })
                    .enqueue();
        });

        //显示PopupWindow
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, lastTouchRawX, lastTouchRawY);
    }
    // 弹出对话框修改名字
    private void showRenameDialog(int id)
    {
        Context context = getContext();
        if (context == null) return;

        EditText editText = new EditText(context);
        // 获取当前会话名称并设置到输入框
        Conversation conversation = conversationsAdapter.conversations.stream()
                .filter(conv -> conv.id == id)
                .findAny()
                .orElse(new Conversation(id, ""));
        editText.setText(conversation.refer);
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(context)
                .setTitle("重命名对话")
                .setView(editText)
                .setPositiveButton("确认", (dialog, which) ->
                {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty())
                    {
                        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "/context/rename")
                                .get()
                                .parameter("id", String.valueOf(id))
                                .parameter("name", newName)
                                .callback(new ApiCallback(getActivity())
                                {
                                    @Override
                                    public void onResponse(String response)
                                    {
                                        getActivity().runOnUiThread(() ->
                                        {
                                            if(ApiClient.check(getContext(), response).equals("error")) return;
                                            loadConversations();
                                        });
                                    }
                                })
                                .enqueue();
                    }
                    else Toast.showError(getContext(), "名称不能为空");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        MainActivity activity = (MainActivity) getActivity();
        View shadow = activity.findViewById(R.id.frame_shadow);
        activity.drawerLayout.setScrimColor(Color.TRANSPARENT);
        activity.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener()
        {
            @Override
            public void onDrawerOpened(View drawerView)
            {
                if(activity.isNewChat)
                {
                    loadConversations();
                    activity.isNewChat = false;
                }
            }
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                // 动态移动主内容视图
                float moveX = drawerView.getWidth() * slideOffset;
                activity.contentFrame.setTranslationX(moveX);
                shadow.setAlpha(slideOffset * 0.3f);
            }
            @Override
            public void onDrawerClosed(View drawerView) { }
            @Override
            public void onDrawerStateChanged(int newState) { }
        });
    }


    private static File conversationsFile(Context context)
    {
        return context.getCacheDir().toPath().resolve("conversations.json").toFile();
    }
    private void loadConversations()
    {
        try(BufferedReader reader = new BufferedReader(new FileReader(conversationsFile(getContext()))))
        {
            StringBuilder builder = new StringBuilder();
            String str;
            while((str = reader.readLine()) != null) builder.append(str);
            loadConversations(builder.toString());
        }
        catch (IOException ignore) { }
        loadConversationsFromWeb();
    }
    private void loadConversationsFromWeb()
    {
        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "/conversations")
                .get()
                .callback(new ApiCallback(getActivity())
                {
                    @Override
                    public void onResponse(String response)
                    {
                        loadConversations(response);
                        //保存到本地
                        try(FileWriter writer = new FileWriter(conversationsFile(getContext())))
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
    private void loadConversations(String response)
    {
        Map<String, String> map = gson.fromJson(response, TypeToken.getParameterized(Map.class, String.class, String.class).getType());
        List<Map.Entry<String, String>> conv = map.entrySet()
                .stream()
                .sorted((m1, m2) -> -Integer.compare(Integer.parseInt(m1.getKey()), Integer.parseInt(m2.getKey())))
                .collect(Collectors.toList());
        getActivity().runOnUiThread(() ->
        {
            conversationsAdapter.conversations.clear();
            for(Map.Entry<String, String> entry : conv)
            {
                try
                {
                    int id = Integer.parseInt(entry.getKey());
                    String refer = entry.getValue();
                    conversationsAdapter.conversations.add(new Conversation(id, refer));
                }
                catch (NumberFormatException ignore) { }
            }
            conversationsAdapter.notifyDataSetChanged();
        });
    }

    private boolean isLoggedIn()
    {
        return sharedPreferences.contains("permit");
    }

    private void navigateToAccountOptions()
    {
        Intent intent = new Intent(getActivity(), AccountOptionsActivity.class);
        startActivity(intent);
    }

    private void navigateToLogin()
    {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
    }
}