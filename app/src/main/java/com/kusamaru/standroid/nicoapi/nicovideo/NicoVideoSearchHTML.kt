package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動の検索結果をスクレイピングする
 * */
class NicoVideoSearchHTML {

    companion object {

        /**
         * 並び替え一覧。
         * */
        val NICOVIDEO_SEARCH_ORDER = arrayListOf<String>(
            "人気が高い順",
            "あなたへのおすすめ順",
            "投稿日時が新しい順",
            "再生数が多い順",
            "マイリスト数が多い順",
            "コメントが新しい順",
            "コメントが古い順",
            "再生数が少ない順",
            "コメント数が多い順",
            "コメント数が少ない順",
            "マイリスト数が少ない順",
            "投稿日時が古い順",
            "再生時間が長い順",
            "再生時間が短い順"
        )
    }

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 検索結果のHTMLを返す。コルーチン
     * @param searchText 検索する文字列
     * @param userSession ユーザーセッション
     * @param tagOrSearch "tag" か "search" のどちらか。"tag"はタグ検索。"search"はキーワード検索。
     * @param order "d" か "a" のどちらか。 "d"は新しい順。 "a"は古い順。
     * @param page ページ数
     * @param sort ↓これ見て
     *              h 人気の高い順
     *              f 投稿日時
     *              v 再生回数
     *              m マイリスト数
     *              n コメント数
     * */
    suspend fun getHTML(userSession: String, searchText: String, tagOrSearch: String, sort: String, order: String, page: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/$tagOrSearch/$searchText?page=$page&sort=$sort&order=$order")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * HTMLをパースする
     * そういえば検索結果HTMLにJSONが入るようになったけどなんかコメント投稿数だけ取れないんだよね。はい？
     * */
    suspend fun parseHTML(html: String?): ArrayList<NicoVideoData> = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        val document = Jsoup.parse(html)
        val li = document.getElementsByTag("li")
        li.forEach {
            // ニコニ広告はのせない？
            if (it.attr("data-video-id").isNotEmpty()) {
                val title = it.getElementsByClass("itemTitle")[0].text()
                val videoId = it.attr("data-video-id")
                val thum = it.getElementsByTag("img")[0].attr("src")
                val date = toUnixTime(it.getElementsByClass("time")[0].text())
                val viewCount =
                    it.getElementsByClass("count view")[0].getElementsByClass("value")[0].text()
                val commentCount =
                    it.getElementsByClass("count comment")[0].getElementsByClass("value")[0].text()
                val mylistCount =
                    it.getElementsByClass("count mylist")[0].getElementsByClass("value")[0].text()
                val videoLength = it.getElementsByClass("videoLength")[0].text()
                // SimpleDataFormatで(mm:ss)をパースしたい場合はタイムゾーンをUTCにすればいけます。これで動画時間を秒に変換できる
                val simpleDateFormat = SimpleDateFormat("mm:ss").apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val duration = simpleDateFormat.parse(videoLength).time / 1000
                val data = NicoVideoData(isCache = false, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = duration, cacheAddedDate = null)
                list.add(data)
            }
        }
        list
    }

    /**
     * 検索結果の関連タグの項目をスクレイピングする
     *
     * @param html HTML。はい
     * @return 関連タグの文字が入った文字列を返す
     * */
    suspend fun parseTag(html: String?) = withContext(Dispatchers.Default) {
        val document = Jsoup.parse(html)
        if (document.getElementsByClass("tags").isNotEmpty()) {
            val ul = document.getElementsByClass("tags")[0]
            val tagElements = ul.getElementsByTag("a")
            tagElements.map { element -> element.text() }
        } else arrayListOf() // タグの件数0なら空を返す
    }

    // 投稿時間をUnixTimeへ変換
    fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
        return simpleDateFormat.parse(time).time
    }

    /**
     * ソートの文字列から検索内容を生成。返り値はPairで最初がsort,二個目がorderになります。
     * 例：コメントの新しい順→Pair("n","d")
     * @param sort コメントが新しい順、再生数が多い順など。
     * */
    fun makeSortOrder(sort: String): Pair<String, String> {
        return when (sort) {
            "人気が高い順" -> Pair("h", "d")
            "あなたへのおすすめ順" -> Pair("p", "d")
            "投稿日時が新しい順" -> Pair("f", "d")
            "再生数が多い順" -> Pair("v", "d")
            "マイリスト数が多い順" -> Pair("m", "d")
            "コメントが新しい順" -> Pair("n", "d")
            "コメントが古い順" -> Pair("n", "a")
            "再生数が少ない順" -> Pair("v", "a")
            "コメント数が多い順" -> Pair("r", "d")
            "コメント数が少ない順" -> Pair("r", "a")
            "マイリスト数が少ない順" -> Pair("m", "a")
            "投稿日時が古い順" -> Pair("f", "a")
            "再生時間が長い順" -> Pair("l", "d")
            "再生時間が短い順" -> Pair("l", "a")
            else -> Pair("h", "d")
        }
    }

    /**
     * 検索候補、サジェストを取得するAPIを叩く関数
     *
     * @param searchText 検索ワード
     * @param userSession ユーザーセッション
     * */
    suspend fun getSearchSuggest(userSession: String, searchText: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://sug.search.nicovideo.jp/suggestion/expand/$searchText")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getSearchSuggest]で取得したJSONをパースする関数
     *
     * @param responseString レスポンスボデー
     * @return 文字列配列
     * */
    suspend fun parseSearchSuggest(responseString: String?) = withContext(Dispatchers.Default) {
        // サジェスト結果
        val suggestList = arrayListOf<String>()
        val jsonObject = JSONObject(responseString)
        val jsonArray = jsonObject.getJSONArray("candidates")
        for (i in 0 until jsonArray.length()) {
            suggestList.add(jsonArray.getString(i))
        }
        suggestList
    }

}