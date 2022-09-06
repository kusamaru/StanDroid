package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * ニコ生おすすめ番組取得
 * */
class NicoLiveRecommendProgramAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコ生のあなたへのおすすめを取得するAPIを叩く
     *
     * @param userId ユーザーのId
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getNicoLiveRecommendProgram(userSession: String, userId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/front/api/v1/recommend-contents?content_meta=true&site=nicolive&recipe=live_top&v=1&user_id=$userId")
            addHeader("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoLiveRecommendProgram]をパースする関数
     * @param response レスポンスボデー
     * @return 番組配列
     * */
    suspend fun parseNicoLiveRecommendProgram(response: String?) = withContext(Dispatchers.Default) {
        // 配列
        val programList = arrayListOf<NicoLiveProgramData>()
        val programJSONArray = JSONObject(response).getJSONObject("data").getJSONArray("values")
        for (i in 0 until programJSONArray.length()) {
            val programJSONObject = programJSONArray.getJSONObject(i).getJSONObject("content_meta")
            val title = programJSONObject.getString("title")
            val communityName = programJSONObject.getString("community_text")
            val openTime = programJSONObject.getString("open_time")
            val endTime = programJSONObject.getString("live_end_time")
            val programId = programJSONObject.getString("content_id")
            val broadCaster = programJSONObject.getString("user_nickname")
            val lifeCycle = if (programJSONObject.getString("live_status") == "onair") "ON_AIR" else "RESERVED"
            val liveScreenShot = programJSONObject.getString("live_screenshot_thumbnail_large")
            val thumb = if (liveScreenShot.isEmpty()) {
                programJSONObject.getString("thumbnail_url")
            } else {
                liveScreenShot
            }
            val isOfficial = programJSONObject.getString("provider_type") == "channel"
            // UnixTimeへ変換
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            val openTimeUnixTime = simpleDateFormat.parse(openTime).time.toString()
            val endTimeUnixTime = simpleDateFormat.parse(endTime).time.toString()
            val data = NicoLiveProgramData(title, communityName, openTimeUnixTime, endTimeUnixTime, programId, broadCaster, lifeCycle, thumb, isOfficial)
            programList.add(data)
        }
        return@withContext programList
    }

}