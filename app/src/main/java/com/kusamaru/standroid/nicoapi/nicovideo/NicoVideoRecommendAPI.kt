package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * 関連動画のAPIを叩く・パースする関数
 * */
class NicoVideoRecommendAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 関連動画取得APIを叩く。
     * @param channelId 公式動画の場合は入れてください。
     * @param videoId 動画ID
     * */
    suspend fun getVideoRecommend(userSession:String,videoId: String, channelId: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            // recipeIdには、video_channel_watch_recommendation（公式）とvideo_watch_recommendation（公式じゃない）がある。
            if (videoId.contains("so")) {
                // 公式
                url("https://nvapi.nicovideo.jp/v1/recommend?recipeId=video_channel_watch_recommendation&videoId=$videoId&channelId=$channelId&site=nicovideo&_frontendId=6")
            } else {
                // 公式じゃない
                url("https://nvapi.nicovideo.jp/v1/recommend?recipeId=video_watch_recommendation&videoId=$videoId&site=nicovideo&_frontendId=6")
            }
            header("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * getVideoRecommend()のレスポンスをパースする。
     * @param responseString getVideoRecommend()の返り値
     * @return NicoVideoDataの配列
     * */
    suspend fun parseVideoRecommend(responseString: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i)
            val contentType = videoObject.getString("contentType")
            val recommendType = videoObject.getString("recommendType")
            // 動画のみ
            if (contentType == "video") {
                val contentObject = videoObject.getJSONObject("content")
                val videoId = contentObject.getString("id")
                val videoTitle = contentObject.getString("title")
                val registeredAt = toUnixTime(contentObject.getString("registeredAt"))
                val thumb = if (contentObject.getJSONObject("thumbnail").isNull("largeUrl")) {
                    contentObject.getJSONObject("thumbnail").getString("url")
                } else {
                    contentObject.getJSONObject("thumbnail").getString("largeUrl")
                }
                val countObject = contentObject.getJSONObject("count")
                val viewCount = countObject.getString("view")
                val commentCount = countObject.getString("comment")
                val mylistCount = countObject.getString("mylist")
                val duration = contentObject.getLong("duration")
                val data = NicoVideoData(isCache = false, isMylist = false, title = videoTitle, videoId = videoId, thum = thumb, date = registeredAt, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = duration, cacheAddedDate = null)
                list.add(data)
            }
        }
        list
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

}