package com.ow0b.c7b9.app.old.activity.main;

import android.content.Context;
import android.media.MediaPlayer;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface AudioService
{
    //TODO 不要Timer可以吗？（这个貌似MediaPlayer就可以直接停止，接口不需要stop()？）
    //TODO Midi包也不要了？
    /// 播放音频
    MediaPlayer play(@NotNull Context context, @NotNull File file, float start);
    /// 停止播放音频
    void stop();
    /// 是否正在播放音频
    boolean isPlaying();

    /// 从服务器或本地下载获取音频，会缓存且在已存在时从本地获取
    File download(Context context, int aid);
    /// 将音频上传到服务器（返回资源id）
    int upload(Context context, File audio);
    /// 从服务器或本地下载获取Midi Json，会缓存且在已存在时从本地获取
    String downloadMidiJson(Context context, int aid);
    /// 将Midi Json上传到服务器（返回资源id）
    int uploadMidiJson(Context context, String json);

    /// 开始录音
    void startRecording(@NotNull Context context);
    /// 结束录音，在临时文件夹保存录音文件并返回（需要及时删除）
    File stopRecording(@NotNull Context context);
    /// 返回当前是否正在录音
    boolean isRecording();
}
