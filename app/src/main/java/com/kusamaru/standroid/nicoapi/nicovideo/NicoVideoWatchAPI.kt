package com.kusamaru.standroid.nicoapi.nicovideo

import androidx.compose.runtime.Composable
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Watch API扱うよ
 */
class NicoVideoWatchAPI {
    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    private val actionTrackIdChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    private val regexNicosId = "nicosid=[0-9.]+;".toRegex()

    fun getActionTrackId(): String {
        return (0..9)
            .map { actionTrackIdChars.random() }
            .joinToString("")
            .plus("_")
            .plus(System.currentTimeMillis())
    }

    /**
     * Watch API V3を叩いてデータ持ってきます。
     * @param videoId smなんたらかんたら
     * @param userSession いるかわからん。
     * @return
     */
    @Deprecated("動画によっては400(HARMFUL_VIDEO)が帰ってくる。HTMLスクレイピングした方がいい")
    suspend fun getVideoDataV3(
        videoId: String,
        userSession: String? = null,
    ) = withContext(Dispatchers.IO) {
        val actionTrackId = getActionTrackId()
        val url = if (userSession != null) {
            "https://www.nicovideo.jp/api/watch/v3/${videoId}?actionTrackId=${actionTrackId}&_frontendId=6&_frontendVersion=0"
        } else {
            "https://www.nicovideo.jp/api/watch/v3_guest/${videoId}?actionTrackId=${actionTrackId}&_frontendId=192&_frontendVersion=0"
        }
        val request = Request.Builder().apply {
            url(url)
            get()
            if (userSession != null) {
                addHeader("Cookie", "user_session=$userSession")
            }
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            // header("X-Frontend-Id", "192")
            // header("X-Frontend-Version", "0")
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val cookie = response.headers["set-cookie"]?.let { chars ->
                regexNicosId.find(chars)?.value
            }
            val body = response.body?.string()
            val json = JSONObject(body)
             Pair(json.getJSONObject("data"), cookie)
        } else {
            null
        }
    }
}