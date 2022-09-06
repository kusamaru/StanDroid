package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

/**
 * いいね♡機能。コルーチンで使ってね
 * ニコニ広告に次ぐ動画を評価する機能らしい。
 * ランキングに貢献できるって。まーたランキングが；；
 * */
class NicoLikeAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * いいね♡
     * APIを叩く関数。
     * 登録の際はステータスコードが201になり、
     * 解除の際はステータスコードが200になります。
     * @param userSession ユーザーセッション
     * @param videoId 動画ID
     * @return OkHttpのレスポンス
     * */
    suspend fun postLike(userSession: String, videoId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/likes/items?videoId=$videoId")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("x-frontend-id", "6")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("X-Frontend-Version", "0")
            addHeader("X-Request-With", "https://www.nicovideo.jp")
            post(FormBody.Builder().build()) // POST
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * いいね♡
     * APIのレスポンスJSONをパースする関数。なんとなくコルーチン
     * @param responseString [postLike]のレスポンスぼでー
     * @return お礼メッセージ。未設定の場合は「文字列の"null"」が帰ってくるから気をつけて。例：sm37252062
     * */
    suspend fun parseLike(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        return@withContext jsonObject.optJSONObject("data")?.getString("thanksMessage")
    }

    /**
     * いいね♡を解除するAPIを叩く関数。
     * いいね♡登録APIとの差はPOSTかDELETEかだと思う
     * @param userSession ユーザーセッション
     * @param videoId 動画ID
     * */
    suspend fun deleteLike(userSession: String, videoId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/likes/items?videoId=$videoId")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("x-frontend-id", "6")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("X-Frontend-Version", "0")
            addHeader("X-Request-With", "https://www.nicovideo.jp")
            delete() // Delete
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }

}