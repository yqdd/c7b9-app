package com.ow0b.c7b9.app.activity.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class AudioRecorder
{
    private static final String TAG = "AudioRecorder";
    private static final int REQUEST_PERMISSION_CODE = 1000;
    private static MediaRecorder mediaRecorder;
    private static FileOutputStream audioFileStream;
    private static FileDescriptor audioFilePath;
    private static String audioFileName = null;

    public static void startRecording(Activity activity, String fileName)
    {
        requestPermission(activity);
        if (checkPermission(activity))
        {
            try
            {
                audioFileStream = new FileOutputStream(audioFile(activity, fileName));
                audioFilePath = audioFileStream.getFD();
                setupMediaRecorder();
                mediaRecorder.prepare();
                mediaRecorder.start();
                audioFileName = fileName;
            }
            catch (IOException e)
            {
                Log.e(TAG, "startRecording: ", e);
            }
        }
        else
        {
            requestPermission(activity);
        }
    }
    public static String stopRecording()
    {
        try
        {
            mediaRecorder.stop();
            mediaRecorder.release();
            audioFileStream.close();
            String result = audioFileName;
            audioFileName = null;
            return result;
        }
        catch (IOException e)
        {
            Log.e(TAG, "stopRecording: ", e);
            throw new RuntimeException(e);
        }
    }
    public static boolean isRecording()
    {
        return audioFileName != null;
    }

    private static void setupMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(16000);
        mediaRecorder.setAudioEncodingBitRate(32000);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setOutputFile(audioFilePath);
    }
    public static File audioFile(Context context, String fileName)
    {
        return audioCacheDir(context).resolve(fileName).toFile();
    }
    public static Path audioCacheDir(Context context)
    {
        Path path = context.getCacheDir().toPath().resolve("audio");
        if(path.toFile().mkdir()) Log.e(TAG, "audioCacheDir: 创建缓存文件夹失败");
        return path;
    }

    ///需要录音时再检查并弹出申请权限（避免打开应用就需要申请录音权限）
    private static boolean checkPermission(Activity activity)
    {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }
    private static void requestPermission(Activity activity)
    {
        ActivityCompat.requestPermissions(activity, new String[]
        {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }
}
