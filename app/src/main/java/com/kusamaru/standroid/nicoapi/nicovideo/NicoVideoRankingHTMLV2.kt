package com.kusamaru.standroid.nicoapi.nicovideo

import android.os.Build
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * 新しい仕様に対応したやつ。class名の書き換えだけで動くようにしたいのでなるべく互換性維持
 *
 * ニコ動のランキング取得など
 * スクレイピングをします。
 * */
class NicoVideoRankingHTMLV2 {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /** URLたち */
    companion object {
        /** ランキングURL。ジャンルの並びは本家と同じだと */
        val NICOVIDEO_RANKING_GENRE = arrayListOf(
            "e9uj2uks", // 総合
            "4eet3ca4", // ゲーム
            "zc49b03a", // アニメ
            "dshv5do5", // ボカロ
            "e2bi9pt8", // 音声合成実況・解説・劇場
            "8kjl94d9", // エンタメ
            "wq76qdin", // 音楽
            "1ya6bnqd", // 歌ってみた
            "6yuf530c", // 踊ってみた
            "6r5jr8nd", // 演奏してみた
            "v6wdx6p5", // 解説・講座
            "lq8d5918", // 料理
            "k1libcse", // 旅行・アウトドア
            "24aa8fkw", // 自然
            "3d8zlls9", // 乗り物
            "n46kcz9u", // 技術・工作
            "lzicx0y6", // 社会・政治・時事
            "p1acxuoz", // MMD
            "6mkdo4xd", // VTuber
            "oxzi6bje", // ラジオ
            "4w3p65pf", // スポーツ
            "ne72lua2", // 動物
            "ramuboyn", // その他
            "d2um7mc4", // 例のソレ(これいる？)
        )

        /** ランキング集計期間 */
        val NICOVIDEO_RANKING_TIME = arrayListOf(
            "hour",
            "24h",
            "week",
            "month",
            "total"
        )
    }

    /**
     * ランキングAPIを叩く。
     * @param genre ジャンル。[NICOVIDEO_RANKING_GENRE]から選んで
     * @param time 集計時間。[NICOVIDEO_RANKING_TIME]から選んで
     * @param tag タグ。音楽だとVOCALOIDなど。無くてもいい
     * */
    suspend fun getRankingHTML(genre: String, time: String, tag: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if (tag != null) {
                url("https://nvapi.nicovideo.jp/v1/ranking/teiban/$genre?term=$time&tag=$tag&page=1&pageSize=100")
            } else {
                url("https://nvapi.nicovideo.jp/v1/ranking/teiban/$genre?term=$time&page=1&pageSize=100")
            }
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("X-Frontend-Id", "3") // これないと怒られが発生する

            get()
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * [getRankingHTML]から動画一覧をスクレイピングする関数
     * @param responseString [getRankingHTML]のレスポンス。Response#body#string()は一度しか呼べないのでいったん変数に入れないと
     * @return 何らかの原因で失敗したらnull
     * */
    suspend fun parseRankingVideo(responseString: String?) = withContext(Dispatchers.Default) {
        val videoList = arrayListOf<NicoVideoData>()
        val jsonObject = try {
            JSONObject(responseString)
        } catch (e: JSONException) {
            return@withContext null
        }

//        // headの中の<meta name="server-context">タグのcontentに入ってるっぽい？
//        val jsonElement = html.getElementsByAttributeValue("name", "server-context").find { element ->
//            try {
//                JSONObject(element.attr("content"))
//                true
//            } catch (e: JSONException) {
//                false
//            }
//        } ?: return@withContext null

        val data = jsonObject.getJSONObject("data")
        val jsonVideoList = data.getJSONArray("items")
        for (i in 0 until jsonVideoList.length()) {
            val videoObject = jsonVideoList.getJSONObject(i)
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thumbUrl = videoObject.getJSONObject("thumbnail").getString("url")
            val date = toUnixTime(videoObject.getString("registeredAt"))
            val countObject = videoObject.getJSONObject("count")
            val viewCount = countObject.getInt("view").toString()
            val mylistCount = countObject.getInt("mylist").toString()
            val commentCount = countObject.getInt("comment").toString()
            // val duration = formatISO8601ToSecond(videoObject.getInt("duration"))
            val duration = videoObject.getInt("duration").toLong()
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thumbUrl,
                date = date,
                viewCount = viewCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                duration = duration
            )
            videoList.add(data)
        }
        videoList
    }

    /**
     * [getRankingHTML]をパースする関数
     * 例：その他を選んだ時は {オークション男,BB先輩劇場} など
     * @param responseString [getRankingHTML]のレスポンス
     * */
    suspend fun parseRankingGenreTag(responseString: String?) = withContext(Dispatchers.Default) {
        // TODO: APIわからないので一旦未実装。
        return@withContext arrayListOf<String>()
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