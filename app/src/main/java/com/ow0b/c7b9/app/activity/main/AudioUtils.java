package com.ow0b.c7b9.app.activity.main;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AudioUtils
{
    /// 在缓存目录中创建一个临时File
    public static File temp(Context context)
    {
        try
        {
            return File.createTempFile("temp-", ".audio", context.getCacheDir());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    /// 将音频上传到服务器
    public static void sendAudioToServer(MainActivity activity, Consumer<List<Integer>> callback)
    {
        int[] over = new int[1];
        List<Integer> aids = new ArrayList<>();
        List<File> files = new ArrayList<>();
        for(int i = 0; i < activity.uploadResources.resources.size(); i ++)
        {
            Object res = activity.uploadResources.resources.get(i);
            if(res instanceof File file) files.add(file);
            if(res instanceof Integer aid) aids.add(aid);
        }
        if(files.isEmpty()) callback.accept(aids);
        else
        {
            for(File file : files)
            {
                try(FileInputStream stream = new FileInputStream(file))
                {
                    String body = encodeAudioFileToStr(stream);
                    ApiClient.getInstance(activity).url(activity.getResources().getString(R.string.server) + "/audio/upload")
                            .method("POST", body, "audio/m4a")
                            .parameter("type", "m4a")
                            .callback(new ApiCallback(activity)
                            {
                                @Override
                                public void onResponse(@NonNull String response)
                                {
                                    try
                                    {
                                        String aid = JsonParser.parseString(response).getAsJsonObject().get("id").getAsString();
                                        aids.add(Integer.parseInt(aid));
                                        stream.close();
                                        over[0] ++;
                                        if(over[0] == files.size()) callback.accept(aids);
                                    }
                                    catch (IOException e)
                                    {
                                        Log.e("AudioUtils", "onResponse: ", e);
                                    }
                                }
                            })
                            .enqueue();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    /// 从服务器下载音频，同时缓存到本地
    public static File getAudioFromServer(Context context, int aid, Consumer<Float> onDownloading, Consumer<File> onCompletion)
    {
        File file = audioFile(context, aid + ".m4a");
        if(file.exists())
        {
            onCompletion.accept(file);
            return file;
        }
        //如果能连接上服务器就从服务器下载更新本地缓存
        ApiClient.getInstance(context).url(context.getResources().getString(R.string.server) + "/audio")
                .parameter("id", String.valueOf(aid))
                .get()
                .callback(new Callback()
                {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
                    {
                        InputStream body = Objects.requireNonNull(response.body()).byteStream();
                        long length = response.body().contentLength();
                        long current = 0;
                        try(FileOutputStream output = new FileOutputStream(file))
                        {
                            int b;
                            while((b = body.read()) != -1)
                            {
                                output.write(b);
                                current ++;
                                onDownloading.accept((float) (current / length));
                            }
                            onCompletion.accept(file);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e)
                    {
                        if(file.exists()) onCompletion.accept(file);
                        else Toast.showError(context, "下载音频文件出现未知错误");
                    }
                })
                .enqueue();
        return file;
    }
    /// 获取音频缓存文件
    public static File audioFile(Context context, String name)
    {
        Path path = context.getCacheDir().toPath().resolve("audio");
        if(!path.toFile().exists() && !path.toFile().mkdir()) Log.e("AudioUtils", "audioCacheDir: 创建缓存文件夹失败");
        return path.resolve(name).toFile();
    }


    private static String encodeAudioFileToStr(FileInputStream stream)
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
}
