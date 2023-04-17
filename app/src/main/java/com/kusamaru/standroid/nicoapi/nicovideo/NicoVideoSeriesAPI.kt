package com.kusamaru.standroid.nicoapi.nicovideo

import android.os.Build
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoPlayListAPIResponse
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import com.kusamaru.standroid.tool.IDRegex
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * ニコ動のシリーズ取得。PC版をスクレイピング
 *
 * スマホ版は規制がかかった動画を非表示にするのでPC版をスクレイピングすることに。
 * */
class NicoVideoSeriesAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * シリーズ一覧を取得する。こいつはAPIが存在する
     * @param userId ユーザーID。nullの場合は自分のを取得します
     * @param userSession ユーザーセッション
     * */
    suspend fun getSeriesList(userSession: String, userId: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if (userId == null) {
                url("https://nvapi.nicovideo.jp/v1/users/me/series")
            } else {
                url("https://nvapi.nicovideo.jp/v1/users/${userId}/series")
            }
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("x-frontend-id", "6")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * シリーズ一覧をパースする
     * @param responseString [getSeriesList]で取得したれすぽんすぼでー
     * @return [NicoVideoSeriesData]の配列
     * */
    suspend fun parseSeriesList(responseString: String?) = withContext(Dispatchers.Default) {
        val seriesList = arrayListOf<NicoVideoSeriesData>()
        val jsonObject = JSONObject(responseString)
        val itemsJSONArray = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until itemsJSONArray.length()) {
            val seriesObject = itemsJSONArray.getJSONObject(i)
            val title = seriesObject.getString("title")
            val itemsCount = seriesObject.getInt("itemsCount")
            val thumbUrl = seriesObject.getString("thumbnailUrl")
            val seriesId = seriesObject.getInt("id").toString()
            val seriesData = NicoVideoSeriesData(title, seriesId, itemsCount, thumbUrl)
            seriesList.add(seriesData)
        }
        seriesList
    }

    /**
     * Playlist APIを叩いてシリーズ情報をもらってくる。
     * @param seriesId シリーズのID。https://nicovideo.jp/series/{ここの文字}
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス. [NicoPlayListAPIResponse]
     * */
    suspend fun getSeriesVideoList(userSession: String, seriesId: String) = withContext(Dispatchers.IO) {
        // ここに置いとくの絶対によくない
        val FRONTEND_ID = 11
        val FRONTEND_VERSION = 0

        val url = "https://nvapi.nicovideo.jp/v1/playlist/series/$seriesId"
        val request = Request.Builder().apply {
            url(url)
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("X-Frontend-Id", FRONTEND_ID.toString())
            addHeader("X-Frontend-Version", FRONTEND_VERSION.toString())
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.let {
                val fmt = Json { ignoreUnknownKeys = true }
                fmt.decodeFromString<NicoPlayListAPIResponse>(it.string())
            }
        } else {
            null
        }
    }

    /**
     * [getSeriesVideoList]のresponseを[NicoVideoData]に変換する
     * @param responseData [getSeriesVideoList]
     * @return [NicoVideoData]の配列
     * */
    suspend fun parseSeriesVideoList(responseData: NicoPlayListAPIResponse) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        // メインの情報～
        val entries = if (responseData.meta.status == 200 && responseData.data != null) {
            responseData.data.items
        } else {
            // エラーだったわ
            return@withContext list
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
        entries.forEach { entry ->
            val c = entry.content
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = c.title,
                videoId = c.id,
                thum = c.thumbnail.url,
                date = sdf.parse(c.registeredAt)?.time ?: 0,
                viewCount = c.count.view.toString(),
                commentCount = c.count.comment.toString(),
                mylistCount = c.count.mylist.toString(),
                duration = c.duration
            )
            list.add(data)
        }

        list
    }

    /** UnixTime変換 */
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

    /**
     * なんかしらんけど、「PT176S」みたいな文字列も時間を表してるらしく、それをパースする（ISO8601 Durationで検索）
     * なおAndroid 8以上のAPIを利用しているため8未満のデバイスは0を返します。
     * @return 時間（秒）を返します。なおAndroid 8未満は対応してないので0です
     * */
    private fun formatISO8601ToSecond(duration: String): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Duration.parse(duration).get(ChronoUnit.SECONDS)
        } else {
            0
        }
    }

}