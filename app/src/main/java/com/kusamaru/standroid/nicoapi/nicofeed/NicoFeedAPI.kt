package com.kusamaru.standroid.nicoapi.nicofeed

import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoDataClass
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * ニコレポの後継(らしい)、フォロー新着を扱うAPI
 */
class NicoFeedAPI {
    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * フォロー新着APIを叩いてレスポンスを返す関数。
     * @param userSession ユーザーセッション
     * @param cursor フォロー新着の続きを取りたいときに渡す
     * @return OkHttpのレスポンス。
     * */
    suspend fun getNicoFeedResponse(userSession: String, nextCursor: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if (nextCursor == null) {
                url("https://api.feed.nicovideo.jp/v1/activities/followings/publish?context=my_timeline")
            } else {
                url("https://api.feed.nicovideo.jp/v1/activities/followings/publish?context=my_timeline&cursor=${nextCursor}")
            }
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("X-Frontend-Id", "6")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * レスポンスbodyをparseする。
     * 見た感じActivity Streamsっぽいけどなんかやる予定あるんだろうか
     * @return 今はNicoRepoDataClassにする。
     */
    suspend fun parseNicoFeedResponse(responseString: String?) = withContext(Dispatchers.Default) {
        var list = arrayListOf<NicoRepoDataClass>()
        val jsonObject = JSONObject(responseString)
        val activities = jsonObject.getJSONArray("activities")
        for (i in 0 until activities.length()) {
            val obj = activities.getJSONObject(i)
            val content = obj.getJSONObject("content")
            // kindが便利
            val kind = obj.getString("kind")
            if ((kind != NicoFeedKinds.Video.kind) && (kind != NicoFeedKinds.Live.kind)) {
                // 動画でも生放送でもないものは無視する
                continue
            }
            val isVideo = kind == NicoFeedKinds.Video.kind
            val message = obj.getJSONObject("message").getString("text")
            val contentId = content.getString("id")
            val date = toUnixTime(content.getString("startedAt"))
            val thumbUrl = obj.getString("thumbnailUrl")
            val title = content.getString("title")
            // actorはユーザー情報を教えてくれる
            val actor = obj.getJSONObject("actor")
            val userName = actor.getString("name")
            val userId = actor.getString("id")
            val userIcon = actor.getString("iconUrl")
            val nicoRepoDataClass = NicoRepoDataClass(
                isVideo = isVideo,
                message = message,
                contentId = contentId,
                date = date,
                thumbUrl = thumbUrl,
                title = title,
                userName = userName,
                userId = userId,
                userIcon = userIcon
            )
            list.add(nicoRepoDataClass)
        }

        val cursor =jsonObject.optString("nextCursor")
        Pair(
            list,
            when (cursor) { // cursorが無い場合はnullを返す
                "" -> null
                else -> cursor
            }
        )
    }

    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
        return simpleDateFormat.parse(time).time
    }
}