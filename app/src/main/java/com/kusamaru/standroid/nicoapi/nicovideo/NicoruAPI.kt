package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * ニコるくんAPI。金稼ぎに走ってしまったニコるくんなんて言ってはいけない。
 * nicoru_result.statusが2ならnicorukey切れてる。4ならすでに追加済み。
 *
 * @param userSession ユーザーセッション
 * @param threadId js-initial-watch-dataのdata-api-dataのthread.ids.defaultの値
 * @param userId ユーザーID
 * @param isPremium 現状trueなはず
 * */
class NicoruAPI(val userSession: String, val threadId: String, val isPremium: Boolean, val userId: String) {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    // nicoruKey
    private var nicoruKey: String? = null

    /** NicoruKeyを取得するために最初に呼んでおいてください。 */
    suspend fun init() = withContext(Dispatchers.Default) {
        // NicoruKey取得しておく
        val response = getNicoruKey()
        parseNicoruKey(response.body?.string())
    }

    /**
     * ニコるときに使う「nicorukey」を取得する。コルーチンです。
     * @param userSession ユーザーセッション
     * @param threadId js-initial-watch-dataのdata-api-dataのthread.ids.defaultの値
     * */
    private suspend fun getNicoruKey() = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/nicorukey?language=0&threadId=$threadId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("Content-Type", "application/x-www-form-urlencoded")
            header("X-Frontend-Id", "6")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * NicoruKeyを取得する。
     * 一度取得したら他のにこるでもこのKeyを使い回す。この関数を呼ぶとnicoryKeyが使えるようになります。
     * @param responseString getNicoruKey()のレスポンス
     * @return nicoruKeyに値が入る
     * */
    private suspend fun parseNicoruKey(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        nicoruKey = jsonObject.getJSONObject("data").getString("nicorukey")
    }

    /**
     * ニコるを送信する。コルーチンです。
     * すでにニコってても200が帰ってくる模様。
     * @param id コメントID
     * @param commentText コメントの内容
     * @param postDate コメントの投稿時間（UnixTime）。決してニコった時間ではない。
     * */
    suspend fun postNicoru(id: String, commentText: String, postDate: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            // POSTするJSON
            val postData = JSONArray().apply {
                put(JSONObject().apply {
                    this.put("nicoru", JSONObject().apply {
                        put("thread", threadId)
                        put("user_id", userId)
                        put("premium", if (isPremium) 1 else 0)
                        put("fork", 0)
                        put("language", 0)
                        put("id", id)
                        put("content", commentText)
                        put("postdate", postDate)
                        put("nicorukey", nicoruKey)
                    })
                })
            }
            url("https://nmsg.nicovideo.jp/api.json/")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("Content-Type", "application/x-www-form-urlencoded")
            post(postData.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * ニコるを取り消すAPIを叩く。コルーチンです
     * @param ユーザーセッション
     * @param nicoruId ニコった後に生成されるnicoru_idを使う（nicoruResultIdで取れる）
     * @return okhttpのResponse
     * */
    suspend fun deleteNicoru(userSession: String, nicoruId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/nicoru/send/$nicoruId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("X-Frontend-Id", "6")
            header("X-Frontend-Version", "0")
            header("X-Request-With", "https://www.nicovideo.jp")
            delete()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * ニコるくんの結果を取得する
     * @param responseJSONObject postNicoru()のレスポンスをJSONArrayにして0番目のJSONObject
     * @return status : 0 なら成功？ 1だとnicoruKeyがおかしい 2だとnicoruKey失効、4だとすでにニコり済み
     * */
    fun nicoruResultStatus(responseJSONObject: JSONObject): Int {
        val nicoruResult = responseJSONObject.getJSONObject("nicoru_result").getInt("status")
        return nicoruResult
    }

    /**
     * ニコるくんのニコる数を取得する関数
     * @param responseJSONObject postNicoru()のレスポンスをJSONArrayにして0番目のJSONObject
     * @return ニコる数
     * */
    fun nicoruResultNicoruCount(responseJSONObject: JSONObject): Int {
        val nicoruResult = responseJSONObject.getJSONObject("nicoru_result").getInt("nicoru_count")
        return nicoruResult
    }

    /**
     * ニコった後に発行されるnicoru_idを取得する関数
     * @param responseJSONObject postNicoru()のレスポンスをJSONArrayにして0番目のJSONObject
     * @return
     * */
    fun nicoruResultId(responseJSONObject: JSONObject): String {
        val nicoruId = responseJSONObject.getJSONObject("nicoru_result").getString("nicoru_id")
        return nicoruId
    }

}