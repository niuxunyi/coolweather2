package com.example.coolweather2.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    // 将通用的网络操作提取到一个公共的类里，并提供一个静态方法，
    // 当想要发起网络请求的时候，只需要简单的调用一下这个方法即可。

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback){ // 静态方法
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }



}
