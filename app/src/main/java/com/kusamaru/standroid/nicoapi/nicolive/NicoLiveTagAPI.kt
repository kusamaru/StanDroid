package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveTagData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoTagItemData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * ニコ生のタグを取得する関数
 * コルーチンで呼んでね
 * */
class NicoLiveTagAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * タグを返すAPIを叩く関数
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getTags(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * getTags()のレスポンスをパースする関数
     * @param responseString getTags()のレスポンス
     * @return NicoLiveTagItemDataの配列
     * */
    suspend fun parseTags(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString).getJSONObject("data")
        // タグ編集可能か
        val isLocked = jsonObject.getBoolean("isLocked")
        // タグ配列
        val tags = jsonObject.getJSONArray("tags")
        val list = arrayListOf<NicoTagItemData>()
        for (i in 0 until tags.length()) {
            val tagObject = tags.getJSONObject(i)
            val title = tagObject.getString("tag")
            val isLocked = tagObject.getBoolean("isLocked")
            val type = tagObject.getString("type")
            val isDeletable = tagObject.getBoolean("isDeletable")
            val hasNicoPedia = tagObject.getBoolean("hasNicopedia")
            val nicoPediaUrl = tagObject.getString("nicopediaUrl")
            val data = NicoTagItemData(title, isLocked, type, isDeletable, hasNicoPedia, nicoPediaUrl)
            list.add(data)
        }
        NicoLiveTagData(isLocked, list)
    }

    /**
     * タグを追加する。コルーチン
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * @param tagName 追加するタグの名前
     * */
    suspend fun addTag(liveId: String, userSession: String, tagName: String) = withContext(Dispatchers.IO) {
        // なんかAPI変わってトークンが要らなくなってJSONで送信するように
        val sendJSON = JSONObject().apply {
            put("tag", tagName)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/unama/api/v2/programs/$liveId/livetags")
            header("Cookie", "user_session=$userSession")
            // なんかログイン情報を渡す方法もCookieからヘッダーに付ける方式に変わってる
            header("X-niconico-session", userSession)
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            put(sendJSON) // 追加はput
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * タグを削除する。
     * @param liveId 番組ID
     * @param tagName 削除するタグ名
     * @param userSession ユーザーセッション
     * */
    suspend fun deleteTag(liveId: String, userSession: String, tagName: String) = withContext(Dispatchers.IO) {
        // なんかAPI変わってトークンが要らなくなってJSONで送信するように
        val sendJSON = JSONObject().apply {
            put("tag", tagName)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/unama/api/v2/programs/$liveId/livetags")
            header("Cookie", "user_session=$userSession")
            // なんかログイン情報を渡す方法もCookieからヘッダーに付ける方式に変わってる
            header("X-niconico-session", userSession)
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            delete(sendJSON) // 削除はdelete
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

}