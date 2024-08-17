package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * 投稿動画取得
 * */
class NicoVideoUpload {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 投稿動画取得APIを叩く。
     * マイページが変わって（というか私GINZA時代だしずっと変わってなかったんやな）投稿動画APIが出現した
     * というわけでスクレイピング回避することに成功しました。
     * @param userId ユーザーID。nullなら自分の投稿動画を取りに行く
     * @param userSession ユーザーセッション
     * @param page ページ。最近のサイトみたいに必要な部分だけAPIを叩いて取得するようになった。
     * */
    suspend fun getUploadVideo(userId: String? = null, userSession: String, page: Int = 1, size: Int = 100) = withContext(Dispatchers.IO) {
        // うらる。v1じゃないv2が存在する
        val url = if (userId == null) {
            // じぶん
            "https://nvapi.nicovideo.jp/v2/users/me/videos?sortKey=registeredAt&sortOrder=desc&pageSize=$size&page=$page"
        } else {
            // ほかのひと
            "https://nvapi.nicovideo.jp/v2/users/$userId/videos?sortKey=registeredAt&sortOrder=desc&pageSize=$size&page=$page"
        }
        val request = Request.Builder().apply {
            url(url)
            addHeader("Cookie", "user_session=${userSession}")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("x-frontend-id", "6")
            addHeader("X-Frontend-Version", "0")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getUploadVideo]のレスポンスぼでーJSONをパースする。
     * @param responseString レスポンス。JSON
     * @return [NicoVideoData]の配列
     * */
    suspend fun parseUploadVideo(responseString: String?) = withContext(Dispatchers.Default) {
        val videoList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        // まわす
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i).getJSONObject("essential")
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thumbnailUrl = videoObject.getJSONObject("thumbnail").getString("url")
            val postDate = videoObject.getString("registeredAt")
            val countObject = videoObject.getJSONObject("count")
            val playCount = countObject.getInt("view").toString()
            val commentCount = countObject.getInt("comment").toString()
            val mylistCount = countObject.getInt("mylist").toString()
            val duration = videoObject.getInt("duration").toLong()
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thumbnailUrl,
                date = toUnixTime(postDate),
                viewCount = playCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                mylistItemId = "",
                mylistAddedDate = null,
                duration = duration,
                cacheAddedDate = null
            )
            videoList.add(data)
        }
        videoList
    }

    /**
     * 投稿動画をすべて取得する
     *
     * わざと遅延させているので時間がかかると思う
     *
     * @param userId 投稿者のユーザーID
     * @param userSession ユーザーセッション
     * @return 動画一覧
     * */
    suspend fun getAllUploadVideo(userId: String? = null, userSession: String) = withContext(Dispatchers.Default) {
        // 現在のページ
        var currentPage = 1
        // 返す配列
        val resultVideoList = arrayListOf<NicoVideoData>()
        while (isActive) {
            val response = getUploadVideo(userId = userId, userSession = userSession, page = currentPage)
            if (response.isSuccessful) {
                // 成功？
                val videoList = parseUploadVideo(response.body?.string())
                // もうない場合は戻る
                if (videoList.isEmpty()) {
                    break
                }
                resultVideoList.addAll(videoList)
                currentPage++
            } else {
                // もうない
                break
            }
            delay(500) // 0.5秒待ってから次の動画
        }
        return@withContext resultVideoList
    }

    // UnixTime（ミリ秒）に変換する関数
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
        return simpleDateFormat.parse(time).time
    }

}