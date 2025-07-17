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
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.AccountOptionsActivity;
import com.ow0b.c7b9.app.LoginActivity;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DrawerFragment extends Fragment
{
    public static DrawerFragment INSTANCE;
    public String TAG = "侧边栏";
    private ConstraintLayout fragmentHeader;
    private ImageView userAvatar;
    private TextView username;
    private ListView conversationsList;
    private HorizontalScrollView toolScroll;
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
        View view = inflater.inflate(R.layout.fragment_drawer, container, false);

        fragmentHeader = view.findViewById(R.id.fragment_header);
        userAvatar = view.findViewById(R.id.fragment_user_avatar);
        username = view.findViewById(R.id.fragment_username);
        conversationsList = view.findViewById(R.id.conversations_list);
        toolScroll = view.findViewById(R.id.fragment_drawer_tool_scroll);
        sharedPreferences = ApiClient.getSharedPreferences(getContext());

        toolScroll.setOnTouchListener((v, event) ->
        {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

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
                activity.loadContext(conversation.id);
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
        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "api/userinfo")
                .get()
                .callback(new ApiCallback(getActivity())
                {
                    @Override
                    public void onResponse(@NonNull String response)
                    {
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        if(json.get("name") != null)
                            getActivity().runOnUiThread(() -> username.setText(json.get("name").getAsString()));
                    }
                })
                .enqueue();
        //TODO 可以本地存优化下
        //String savedUsername = sharedPreferences.getString("username", "未登录");

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
            ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "api/context")
                    .parameter("id", String.valueOf(id))
                    .method("DELETE")
                    .callback(new ApiCallback(getActivity())
                    {
                        @Override
                        public void onResponse(String response)
                        {
                            getActivity().runOnUiThread(() -> Toast.showInfo(getContext(), "删除成功"));
                            loadConversations();
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
                        JsonObject unit = new JsonObject();
                        unit.addProperty("id", id);
                        unit.addProperty("refer", newName);

                        JsonArray body = new JsonArray();
                        body.add(unit);
                        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "api/conversation")
                                .parameter("id", String.valueOf(id))
                                .method("PUT", body)
                                .callback(new ApiCallback(getActivity())
                                {
                                    @Override
                                    public void onResponse(String response)
                                    {
                                        getActivity().runOnUiThread(() -> Toast.showInfo(getContext(), "重命名成功"));
                                        loadConversations();
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
        ApiClient.getInstance(getContext()).url(getResources().getString(R.string.server) + "api/conversation")
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
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if(json.get("data") != null)
        {
            JsonArray infos = json.get("data").getAsJsonArray();
            getActivity().runOnUiThread(() ->
            {
                conversationsAdapter.conversations.clear();
                infos.forEach(element ->
                {
                    JsonObject obj = element.getAsJsonObject();
                    int id = obj.get("id").getAsInt();
                    String refer = obj.get("refer").getAsString();
                    conversationsAdapter.conversations.add(new Conversation(id, refer));
                });
                conversationsAdapter.notifyDataSetChanged();
            });
        }
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