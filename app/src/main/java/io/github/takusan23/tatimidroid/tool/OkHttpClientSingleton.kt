package io.github.takusan23.tatimidroid.tool

import okhttp3.OkHttpClient
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
    val okHttpClient = OkHttpClient()

}