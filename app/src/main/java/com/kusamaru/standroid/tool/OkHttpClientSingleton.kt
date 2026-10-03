package com.kusamaru.standroid.tool

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

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
        // Authenticated requests carry session cookies and temporary keys; never log bodies/headers.
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
