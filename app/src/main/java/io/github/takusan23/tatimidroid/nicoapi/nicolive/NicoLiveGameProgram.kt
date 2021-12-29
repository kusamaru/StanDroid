package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ生ゲームの番組のやつ。
 * まじでエモーションだけAkashic別にしてほしかったのに
 * */
class NicoLiveGameProgram {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    companion object {
        /** ニコ生ゲームプレイ中の番組取得API */
        const val NICONAMA_GAME_PLAYING = "https://api.spi.nicovideo.jp/v1/matching/profiles/targets/frontend/statuses/playing?limit=30"

        /** ニコ生ゲーム募集中番組取得API */
        const val NICONAMA_GAME_MATCHING = "https://api.spi.nicovideo.jp/v1/matching/profiles/targets/frontend/statuses/matching?limit=20"
    }

    /**
     * ニコ生ゲームプレイ中/募集中番組一覧取得API
     * タイムアウトの事を考えてtry/catch書いたほうが良い気がする
     * @param url ニコ生ゲームプレイ中の番組取得は[NICONAMA_GAME_PLAYING]。視聴者を募集している番組取得は[NICONAMA_GAME_MATCHING]
     * @return OkHttpのレスポンス
     * */
    suspend fun getNicoNamaGameProgram(userSession: String, url: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url(url) // なんと！APIがある！
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoNamaGameProgram]のレスポンスJSONをパースする
     * @param html [getNicoNamaGameProgram]のレスポンス。response#body#string()
     * @return ProgramDataの配列
     * */
    suspend fun parseJSON(html: String?) = withContext(Dispatchers.Default) {
        val dataList = arrayListOf<NicoLiveProgramData>()
        // JSONパース
        val jsonObject = JSONObject(html)
        val data = jsonObject.getJSONArray("data")
        for (i in 0 until data.length()) {
            val program = data.getJSONObject(i)
            val contentId = program.getString("contentId") // 番組ID
            val contentTitle = program.getString("contentTitle") // 番組名
            val startedAt = program.getString("startedAt") // 番組開始時間
            val providerName = program.getString("providerName") // 放送者
            val productName = program.getString("productName") // ゲーム名
            val contentThumbnailUrl = program.getString("contentThumbnailUrl")
            // ISO 8601の形式からUnixTimeへ変換（Adapterの方で必要）
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
            val date_calender = simpleDateFormat.parse(startedAt)
            val calender = Calendar.getInstance(TimeZone.getDefault())
            calender.time = date_calender
            // データクラス
            val data =
                NicoLiveProgramData("$contentTitle\n\uD83C\uDFAE：$productName", providerName, calender.time.time.toString(), calender.time.time.toString(), contentId, providerName, "ON_AIR", contentThumbnailUrl)
            dataList.add(data)
        }
        dataList
    }

}