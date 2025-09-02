package com.ow0b.c7b9.app.view.chat;

import android.view.View;

public interface ChatContext
{
    View newUserText();
    View newAiText();
    View newAiText(String header);
    View newAudio(int aid);
}
