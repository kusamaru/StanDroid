package com.kusamaru.standroid.nicoapi.nicoad

import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * ニコニ広告APIを叩く。
 *
 * 動画、生放送どっちでも利用可能
 * */
class NicoAdAPI {

    companion object {
        /** [getNicoAd]の第３引数に入れる値。生放送のニコニ広告を取得する場合はこれを入れてください */
        val NICOAD_API_LIVE = "live"

        /** [getNicoAd]の第３引数に入れる値。動画のニコニ広告を取得する場合はこれを入れてください */
        val NICOAD_API_VIDEO = "video"


        /**
         * ニコニ広告ページを開くURLを生成する
         * @param contentId 動画IDか番組ID
         * @return URL。開くとニコニ広告ページに行ける
         * */
        fun generateURL(contentId: String): String {
            // 生放送か動画か
            val type = if (contentId.contains("lv")) "live" else "video"
            return "https://nicoad.nicovideo.jp/$type/publish/$contentId"
        }

    }

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコニ広告のAPIを叩く。
     * @param userSession ユーザーセッション
     * @param contentId 動画IDか生放送ID
     * @param type 生放送なら、[NICOAD_API_LIVE]。動画なら、[NICOAD_API_VIDEO]を入れてください
     * */
    suspend fun getNicoAd(userSession: String, contentId: String, type: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/$type/$contentId")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoAd]のJSONをパースしてデータクラスに詰めて返す関数
     *
     * @param responseString れすぽんすぼでー
     * @return [NicoAdData]
     * */
    suspend fun parseNicoAd(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val dataObject = jsonObject.getJSONObject("data")
        val contentId = dataObject.getString("id")
        val contentTitle = dataObject.getString("title")
        val totalPoint = dataObject.getInt("totalPoint")
        val activePoint = dataObject.getInt("activePoint")
        val thumbnailUrl = dataObject.getString("thumbnailUrl")
        return@withContext NicoAdData(
            contentId = contentId,
            contentTitle = contentTitle,
            totalPoint = totalPoint,
            activePoint = activePoint,
            thumbnailUrl = thumbnailUrl
        )
    }

    /**
     * ニコニ広告の貢献度ランキングを取得するAPIを叩く
     * @param userSession ユーザーセッション
     * @param contentId 動画IDか生放送ID
     * @param type 生放送なら、[NICOAD_API_LIVE]。動画なら、[NICOAD_API_VIDEO]を入れてください
     * */
    suspend fun getNicoAdRanking(userSession: String, contentId: String, type: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/$type/$contentId/ranking/contribution?limit=50")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoAdRanking]のJSONをパースしてデータクラスに詰めて返す関数
     *
     * @param responseString れすぽんすぼでー
     * @return [NicoAdRankingUserData]の配列
     * */
    suspend fun parseNicoAdRanking(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoAdRankingUserData>()
        val jsonObject = JSONObject(responseString)
        val rankingJSONArray = jsonObject.getJSONObject("data").getJSONArray("ranking")
        for (i in 0 until rankingJSONArray.length()) {
            val rankingUserObject = rankingJSONArray.getJSONObject(i)
            val advertiserName = rankingUserObject.getString("advertiserName")
            val totalContribution = rankingUserObject.getInt("totalContribution")
            val rank = rankingUserObject.getInt("rank")
            val userId = if (rankingUserObject.has("userId")) {
                rankingUserObject.getInt("userId")
            } else {
                null
            }
            // データクラスへ
            resultList.add(
                NicoAdRankingUserData(
                    userId = userId,
                    advertiserName = advertiserName,
                    totalContribution = totalContribution,
                    rank = rank
                )
            )
        }
        return@withContext resultList
    }

    /**
     * ニコニ広告の宣伝者一覧（広告履歴）を取得するAPIを叩く
     * @param userSession ユーザーセッション
     * @param contentId 動画IDか生放送ID
     * @param type 生放送なら、[NICOAD_API_LIVE]。動画なら、[NICOAD_API_VIDEO]を入れてください
     * */
    suspend fun getNicoAdHistory(userSession: String, contentId: String, type: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/$type/$contentId/histories?limit=50")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoAdHistory]のJSONをパースしてデータクラスに詰めて返す関数
     *
     * @param responseString れすぽんすぼでー
     * @return [NicoAdHistoryUserData]の配列
     * */
    suspend fun parseNicoAdHistory(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoAdHistoryUserData>()
        val jsonObject = JSONObject(responseString)
        val historyJSONArray = jsonObject.getJSONObject("data").getJSONArray("histories")
        for (i in 0 until historyJSONArray.length()) {
            val historyObject = historyJSONArray.getJSONObject(i)
            val advertiserName = historyObject.getString("advertiserName")
            val contribution = historyObject.getInt("contribution")
            val message = if (historyObject.has("message")) {
                historyObject.getString("message")
            } else {
                null
            }
            val userId = if (historyObject.has("userId")) {
                historyObject.getInt("userId")
            } else {
                null
            }
            resultList.add(
                NicoAdHistoryUserData(
                    userId = userId,
                    advertiserName = advertiserName,
                    contribution = contribution,
                    message = message
                )
            )
        }
        return@withContext resultList
    }

}