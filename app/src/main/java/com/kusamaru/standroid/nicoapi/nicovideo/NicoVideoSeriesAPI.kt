package com.kusamaru.standroid.nicoapi.nicovideo

import android.os.Build
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import com.kusamaru.standroid.tool.IDRegex
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Duration
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
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
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
     * シリーズの動画一覧へアクセスしてHTMLを取りに行く。スマホ版は申し訳ないが規制が入ってるのでNG。
     * @param seriesId シリーズのID。https://nicovideo.jp/series/{ここの文字}
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getSeriesVideoList(userSession: String, seriesId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nicovideo.jp/series/$seriesId")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getSeriesVideoList]のHTMLをスクレイピングして配列に変換する
     * @param responseHTML [getSeriesVideoList]
     * @return [NicoVideoData]の配列
     * */
    suspend fun parseSeriesVideoList(responseHTML: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        // HTMLスクレイピング
        val document = Jsoup.parse(responseHTML)
        // mapって便利やな
        val titleList = document.getElementsByClass("NC-VideoMediaObject-title").map { it.text() }
        val videoIdList = document.getElementsByClass("NC-Link NC-MediaObject-contents").map { IDRegex(it.attr("href"))!! }
        val thumbUrlList = document.getElementsByClass("NC-Thumbnail-image").map { it.attr("data-background-image") }
        val dateList = document.getElementsByClass("NC-VideoRegisteredAtText-text").map {
            val calendar = Calendar.getInstance()
            when {
                it.text().contains("分前") -> {
                    // 時間操作だるすぎ
                    calendar.add(Calendar.MINUTE, -it.text().replace("分前", "").toInt())
                    calendar.timeInMillis
                }
                it.text().contains("時間前") -> {
                    // 大体の値。スクレイピングだとこの処理がきついがJSONだとなんか取れない
                    calendar.add(Calendar.HOUR_OF_DAY, -it.text().replace("時間前", "").toInt())
                    calendar.timeInMillis
                }
                else -> {
                    // こっちが良いのにね
                    val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
                    simpleDateFormat.parse(it.text()).time
                }
            }
        }
        val viewCountList = document.getElementsByClass("NC-VideoMetaCount NC-VideoMetaCount_view").map { it.text() }
        val mylistCountList = document.getElementsByClass("NC-VideoMetaCount NC-VideoMetaCount_mylist").map { it.text() }
        val commentCountList = document.getElementsByClass("NC-VideoMetaCount NC-VideoMetaCount_comment").map { it.text() }
        val durationList = document.getElementsByClass("NC-VideoLength").map {
            // SimpleDataFormatで(mm:ss)をパースしたい場合はタイムゾーンをUTCにすればいけます。これで動画時間を秒に変換できる
            val simpleDateFormat = SimpleDateFormat("mm:ss").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            simpleDateFormat.parse(it.text()).time / 1000 // 1:00 なら　60 へ
        }

        for (i in titleList.indices) {
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = titleList[i],
                videoId = videoIdList[i],
                thum = thumbUrlList[i],
                date = dateList[i],
                viewCount = viewCountList[i],
                commentCount = commentCountList[i],
                mylistCount = mylistCountList[i],
                duration = durationList[i]
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