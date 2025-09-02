package com.ow0b.c7b9.app.api;

import okhttp3.Callback;

public interface ApiClient
{
    /// 设置请求的URL
    ApiClient url(String url);
    /// 设置请求的URL参数
    ApiClient parameter(String key, String value);

    /// 设置使用GET请求调用
    ApiClient get();
    /// 设置指定请求方法（没有body）
    ApiClient method(String method);
    /// 设置指定请求方法和数据（contentType为application/json）
    ApiClient method(String method, Object body);
    /// 设置请求方法、body 和 contentType
    ApiClient method(String method, String body, String type);
    /// 设置请求成功或失败的回调操作
    ApiClient callback(Callback callback);

    /// 异步调用请求
    void enqueue();
    /// 阻塞调用请求
    void execute();
    /// 关闭callback，不触发onFailure()
    void dispose();
}
