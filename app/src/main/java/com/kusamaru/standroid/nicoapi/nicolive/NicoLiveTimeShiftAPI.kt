package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request

/**
 * タイムシフト予約をするAPIまとめ
 * 登録済みか確認する関数は
 * */
class NicoLiveTimeShiftAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * タイムシフト登録APIを叩く。コルーチンです。
     * 注意：このAPIを使うときは登録以外にも登録済みか判断するときに使うっぽい
     *      登録済みの場合はステータスコードが500になる
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun registerTimeShift(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val postFormData = FormBody.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            add("vid", liveId.replace("lv", ""))
            add("overwrite", "0")
        }.build()
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/api/timeshift.reservations")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            post(postFormData)
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * タイムシフト登録リストからタイムシフトを削除するAPIを叩く。コルーチンです。
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun deleteTimeShift(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            // 番組IDからlvを抜いた値を指定する
            url("https://live.nicovideo.jp/api/timeshift.reservations?vid=${liveId.replace("lv", "")}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Origin", "https://live2.nicovideo.jp") // これが必須の模様
            delete()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response
    }

}