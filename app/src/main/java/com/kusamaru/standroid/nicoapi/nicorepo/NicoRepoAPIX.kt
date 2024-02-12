package com.kusamaru.standroid.nicoapi.nicorepo

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.tool.IDRegex
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * ニコレポAPI。動画と生放送どっちも対応。
 * */
class NicoRepoAPIX {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコレポのAPIを叩いてレスポンスを返す関数。
     * @param userSession ユーザーセッション
     * @param userId ゆーざーID。nullで自分のデータを取る
     * @return OkHttpのレスポンス。
     * */
    suspend fun getNicoRepoResponse(userSession: String, userId: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if (userId == null) {
                url("https://api.repoline.nicovideo.jp/v1/timelines/nicorepo/last-1-month/my/pc/entries.json")
            } else {
                url("https://api.repoline.nicovideo.jp/v1/timelines/nicorepo/last-6-months/users/${userId}/pc/entries.json")
            }
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * ニコレポのレスポンスJSONをパースする
     * @param responseString [getNicoRepoResponse]のレスポンスぼでー返り値
     * @return [NicoRepoDataClass]の配列
     * */
    suspend fun parseNicoRepoResponse(responseString: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoRepoDataClass>()
        val jsonObject = JSONObject(responseString)
        val dataList = jsonObject.getJSONArray("data")
        for (i in 0 until dataList.length()) {
            val nicorepoObject = dataList.getJSONObject(i)
            // objectがない時がある？
            if (nicorepoObject.has("object")) {
                val contentObject = nicorepoObject.getJSONObject("object")
                // 動画、生放送のみ
                if (IDRegex(contentObject.getString("url")) != null) {
                    val isVideo = contentObject.getString("type") == "video"
                    val message = nicorepoObject.getString("title")
                    val contentId = IDRegex(contentObject.getString("url"))!!  // トップレベル関数
                    val date = toUnixTime(nicorepoObject.getString("updated"))
                    val thumb = contentObject.getString("image")
                    val title = contentObject.getString("name")
                    // ユーザー情報
                    val userObject = nicorepoObject.getJSONObject("actor")
                    val userName = userObject.getString("name")
                    val userId = userObject.getString("url").replace("https://www.nicovideo.jp/user/", "")
                    val userIcon = userObject.getString("icon")
                    val nicoRepoDataClass = NicoRepoDataClass(
                        isVideo = isVideo,
                        message = message,
                        contentId = contentId,
                        date = date,
                        thumbUrl = thumb,
                        title = title,
                        userName = userName,
                        userId = userId,
                        userIcon = userIcon
                    )
                    list.add(nicoRepoDataClass)
                }
            }
        }
        list
    }

    /**
     * [NicoLiveProgramData]の配列へ変換する
     * */
    fun toProgramDataList(nicoRepoDataClassList: List<NicoRepoDataClass>): List<NicoLiveProgramData> {
        return nicoRepoDataClassList.map { nicoRepoDataClass ->
            NicoLiveProgramData(
                title = nicoRepoDataClass.title,
                communityName = nicoRepoDataClass.userName,
                beginAt = nicoRepoDataClass.date.toString(),
                endAt = nicoRepoDataClass.date.toString(),
                programId = nicoRepoDataClass.contentId,
                broadCaster = nicoRepoDataClass.userName,
                lifeCycle = "ON_AIR",
                thum = nicoRepoDataClass.thumbUrl
            )
        }
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return simpleDateFormat.parse(time).time
    }

}