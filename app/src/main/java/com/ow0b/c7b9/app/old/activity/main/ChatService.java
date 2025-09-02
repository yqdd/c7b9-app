package com.ow0b.c7b9.app.old.activity.main;

import com.ow0b.c7b9.app.view.chat.ChatContextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ChatService
{
    void send(@NotNull ChatContextView view, @NotNull String message, @Nullable Integer sid, @Nullable int[] audios);

    void cancel(@NotNull ChatContextView view, int sid);

    void load(@NotNull ChatContextView view, int sid);
}
