package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * ニコ生のトップページから
 * フォロー中
 * 一般
 * ルーキー
 * 等を取得するクラス。
 * コルーチンですねえ！
 *
 * **んなことよりニコ生TOPページの一番上に朝鮮中央テレビミラーとか馬の放送とかいらんやろ。ランキングとか置けよ**
 * */
class NicoLiveProgram {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * NicoLiveProgram#parseJSON()の二番目の引数に入れる値。const val と val って何が違うんだ？
     * */
    companion object {
        /** フォロー中番組。 */
        const val FAVOURITE_PROGRAM = "favoriteProgramListState"

        /** あなたへのおすすめ。～歳 無職 みたいなタイトルばっか出てくるのニコ生っぽくて好き */
        @Deprecated("NicoLiveRecommendProgram クラス参照")
        const val RECOMMEND_PROGRAM = "recommendedProgramListState"

        /** 放送中の注目番組取得。ニコニコニュースがある部分 */
        @Deprecated("多分なくなった")
        const val FORCUS_PROGRAM = "organizationProgramListCarouselState"

        /** これからの注目番組。*/
        const val RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM = "organizationProgramListCarouselState"

        /** 人気の予約されている番組取得。アニメ一挙とか */
        const val POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM = "popularBeforeOpenBroadcastStatusProgramListState"

        /** ルーキー番組 */
        const val ROOKIE_PROGRAM = "rookieProgramListState"
    }

    /**
     * ニコ生TOPページを取得する関数。
     * コルーチンできるマン助けてこれ例外のときどうすればええん？
     * 注意：というわけで例外処理しないとたまによくタイムアウトします。CoroutineExceptionHandler等使ってね
     * */
    suspend fun getNicoLiveTopPageHTML(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/?header")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * HTMLからJSONを取り出して、番組情報の配列を取得する関数。
     * @param html [getNicoLiveTopPageHTML]のレスポンス。response#body#string()
     * @param jsonObjectName JSONObjectの名前。フォロー中番組なら[FAVOURITE_PROGRAM]です。
     * */
    suspend fun parseJSON(html: String?, jsonObjectName: String) = withContext(Dispatchers.Default) {
        // JSONっぽいのがあるので取り出す。すくれいぴんぐ
        val document = Jsoup.parse(html)
        val json = document.getElementById("embedded-data").getElementsByAttribute("data-props")
        val jsonString = json.attr("data-props")
        val jsonObject = JSONObject(jsonString)
        // JSON解析
        val programs = jsonObject.getJSONObject("view").getJSONObject(jsonObjectName).getJSONArray("programList")
        // 番組取得
        val dataList = arrayListOf<NicoLiveProgramData>()
        for (i in 0 until programs.length()) {
            val programJSONObject = programs.getJSONObject(i)
            val programId = programJSONObject.getString("id")
            val title = programJSONObject.getString("title")
            val beginAt = programJSONObject.getString("beginAt")
            val endAt = programJSONObject.getString("endAt")
            val communityName = programJSONObject.getJSONObject("socialGroup").getString("name")
            val liveCycle = programJSONObject.getString("liveCycle") //放送中か？
            val official = programJSONObject.getString("providerType") == "official" // community / channel は false
            // サムネ。放送中はスクショを取得するけどそれ以外はアイコン取得？
            val hasThumbnailUrl = programJSONObject.getString("thumbnailUrl") != "null"
            val hasLiveScreenshotThumbnailUrl = programJSONObject.getJSONObject("screenshotThumbnail").has("liveScreenshotThumbnailUrl")
            val thumb = when {
                !hasLiveScreenshotThumbnailUrl -> programJSONObject.getString("thumbnailUrl")
                liveCycle == "ON_AIR" -> programJSONObject.getJSONObject("screenshotThumbnail").getString("liveScreenshotThumbnailUrl")
                !hasThumbnailUrl -> programJSONObject.getJSONObject("socialGroup").getString("thumbnailUrl")
                else -> programJSONObject.getString("thumbnailUrl")
            }
            // データクラス
            val data = NicoLiveProgramData(title, communityName, beginAt, endAt, programId, "", liveCycle, thumb, official)
            dataList.add(data)
        }
        dataList
    }

}