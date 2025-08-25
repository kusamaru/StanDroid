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
 * ニコ動の検索結果を用意する。
 * API叩きますよAPI
 * */
class NicoVideoSearchHTMLV2 {

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

        // ソート文字の変換用。時間あるときにこんなもん使わなくてもいいようにしたいけど面倒だ
        val NICOVIDEO_SORT_MAP = mapOf(
            "h" to "hotLikeAndMylist",
            "p" to "personalized",
            "f" to "registeredAt",
            "v" to "viewCount",
            "m" to "mylistCount",
            "n" to "commentCount",
        )

        val NICOVIDEO_ORDER_MAP = mapOf(
            "d" to "desc",
            "a" to "asc"
        )
    }

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 検索結果のJSONを返す。コルーチン
     * @param searchText 検索する文字列
     * @param userSession ユーザーセッション
     * @param tagOrSearch "tag" か "search" のどちらか。"tag"はタグ検索。"search"はキーワード検索。
     * @param order "d" か "a" のどちらか。 "d"は新しい順。 "a"は古い順。
     * @param page ページ数
     * @param sort ↓これ見て
     *              h 人気の高い順
     *              p おすすめ順
     *              f 投稿日時
     *              v 再生回数
     *              m マイリスト数
     *              n コメント数
     * */
    suspend fun getHTML(userSession: String, searchText: String, tagOrSearch: String, sort: String, order: String, page: String) = withContext(Dispatchers.IO) {
        val sortValue = NICOVIDEO_SORT_MAP.get(sort) ?: "hotLikeAndMylist"
        val orderValue = NICOVIDEO_ORDER_MAP.get(order) ?: "asc"

//        val requestUrl = if (tagOrSearch == "search") {
//            // 一般検索
//            "https://nvapi.nicovideo.jp/search/list?keyword=$searchText&sortKey=$sortKey&page=$page&pageSize=3&searchByUser=false"
//        } else {
//            // タグ検索。なんでこっちだけAPIないんだ……
//            // 末尾に`responseType=json`クエリを付加することでjsonのお返事が帰ってきます
//            "https://www.nicovideo.jp/tag/$searchText?page=$page&responseType=json"
//        }

        val request = Request.Builder().apply {
            // 末尾に`responseType=json`クエリを付加することでjsonでお返事が帰ってくるみたい
            url("https://www.nicovideo.jp/$tagOrSearch/$searchText?page=$page&order=$orderValue&sort=$sortValue&responseType=json")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            // たぶんなくても通る
            // addHeader("X-Frontend-Id", "3")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * JSONをパースする
     * */
    suspend fun parseHTML(html: String?): ArrayList<NicoVideoData> = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(html)
        val data = jsonObject.getJSONObject("data")
        val items = data.getJSONObject("response")
            .getJSONObject("\$getSearchVideoV2")
            .getJSONObject("data")
            .getJSONArray("items")
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i)
            // ニコニ広告はのせない？
            // if (it.attr("data-video-id").isNotEmpty()) {
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thum = videoObject.getJSONObject("thumbnail").getString("url")
            val date = toUnixTime(videoObject.getString("registeredAt"))

            val countObject = videoObject.getJSONObject("count")
            val viewCount = countObject.getInt("view").toString()
            val commentCount = countObject.getInt("comment").toString()
            val mylistCount = countObject.getInt("mylist").toString()
            val videoLength = videoObject.getInt("duration").toLong()
//                // SimpleDataFormatで(mm:ss)をパースしたい場合はタイムゾーンをUTCにすればいけます。これで動画時間を秒に変換できる
//                val simpleDateFormat = SimpleDateFormat("mm:ss").apply {
//                    timeZone = TimeZone.getTimeZone("UTC")
//                }
            val data = NicoVideoData(isCache = false, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = videoLength, cacheAddedDate = null)
            list.add(data)
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
//        val document = Jsoup.parse(html)
//        if (document.getElementsByClass("tags").isNotEmpty()) {
//            val ul = document.getElementsByClass("tags")[0]
//            val tagElements = ul.getElementsByTag("a")
//            tagElements.map { element -> element.text() }
//        } else arrayListOf() // タグの件数0なら空を返す

        // TODO: いずれやる
        return@withContext arrayListOf<String>()
    }

    // 投稿時間をUnixTimeへ変換
    fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
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