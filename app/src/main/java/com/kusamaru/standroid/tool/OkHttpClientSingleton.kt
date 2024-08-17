package com.kusamaru.standroid.tool

import com.kusamaru.standroid.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * OkHttp曰く、「OkHttpClient」を使いまわし、すべてのリクエストで同じOkHttpClientを使うと最高のパフォーマンスが出る
 *
 * とのことなので使いまわしてみる
 * */
object OkHttpClientSingleton {

    /**
     * これをすべてのリクエストで使う共通のOkHttpClient
     * */
    val okHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(20, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        readTimeout(30, TimeUnit.SECONDS)
        // ログを出力させる設定
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        // .addNetworkInterceptor(FixJsonContentTypeInterceptor())
    }.build()
}

class FixJsonContentTypeInterceptor: Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val orig = chain.request()

        val fixed = orig.newBuilder()
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(fixed)
    }

}