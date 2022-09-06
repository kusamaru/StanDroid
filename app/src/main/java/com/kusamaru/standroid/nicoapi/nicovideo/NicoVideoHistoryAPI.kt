package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat


/**
 * 履歴取得API
 * */
class NicoVideoHistoryAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 履歴を取得する。
     * @param userSession ユーザーセッション
     * @return Response
     * */
    suspend fun getHistory(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/watch/history?page=1&pageSize=200") // 最大200件？
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "3")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * 履歴JSONをパースする
     * @param json getHistory()で取得した値
     * @return NicoVideoDataの配列
     * */
    suspend fun parseHistoryJSONParse(json: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val video = items.getJSONObject(i).getJSONObject("video")
            val title = video.getString("title")
            val videoId = video.getString("id")
            val thum = if (video.getJSONObject("thumbnail").isNull("largeUrl")) {
                video.getJSONObject("thumbnail").getString("url")
            } else {
                video.getJSONObject("thumbnail").getString("largeUrl")
            }
            val date = toUnixTime(video.getString("registeredAt"))
            val count = video.getJSONObject("count")
            val viewCount = count.getInt("view").toString()
            val commentCount = count.getInt("comment").toString()
            val mylistCount = count.getInt("mylist").toString()
            val duration = video.getLong("duration")
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thum,
                date = date,
                viewCount = viewCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                mylistItemId = "",
                mylistAddedDate = null,
                duration = duration,
                cacheAddedDate = null
            )
            list.add(data)
        }
        list
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

}