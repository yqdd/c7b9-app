package com.ow0b.c7b9.app.api;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ChatService
{
    @Streaming
    @GET("/chat")
    Call<ResponseBody> chat(@HeaderMap Map<String, Object> header,
                            @Query("message") String message,
                            @Nullable @Query("id") Integer sid);
    @Streaming
    @POST("/chat")
    Call<ResponseBody> chat(@HeaderMap Map<String, Object> header,
                            @Query("message") String message,
                            @Nullable @Query("id") Integer sid,
                            @Nullable @Body List<Integer> audios);
}
