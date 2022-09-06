package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftHistoryUserData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftItemData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftRankingUserData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * 投げ銭履歴、ランキングAPI を叩くクラス
 * */
class NicoLiveGiftAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ギフトランキングAPIを叩く
     *
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getGiftRanking(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/$liveId/ranking/contribution?limit=50")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftRanking]をパースする
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftRanking(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoLiveGiftRankingUserData>()
        val jsonObject = JSONObject(responseString)
        val rankingJSONArray = jsonObject.getJSONObject("data").getJSONArray("ranking")
        for (i in 0 until rankingJSONArray.length()) {
            val rankingJSONObject = rankingJSONArray.getJSONObject(i)
            val userId = if (rankingJSONObject.has("userId")) {
                rankingJSONObject.getInt("userId")
            } else null
            val advertiserName = rankingJSONObject.getString("advertiserName")
            val totalContribution = rankingJSONObject.getInt("totalContribution")
            val rank = rankingJSONObject.getInt("rank")
            resultList.add(
                NicoLiveGiftRankingUserData(
                    userId = userId,
                    advertiserName = advertiserName,
                    totalContribution = totalContribution,
                    rank = rank
                )
            )
        }
        resultList
    }

    /**
     * ギフト履歴APIを叩く
     *
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getGiftHistory(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/$liveId/histories?limit=50")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftHistory]をパースする
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftHistory(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoLiveGiftHistoryUserData>()
        val jsonObject = JSONObject(responseString)
        val historyJSONArray = jsonObject.getJSONObject("data").getJSONArray("histories")
        for (i in 0 until historyJSONArray.length()) {
            val historyJSONObject = historyJSONArray.getJSONObject(i)
            val advertiserName = historyJSONObject.getString("advertiserName")
            val userId = if (historyJSONObject.has("userId")) {
                historyJSONObject.getInt("userId")
            } else null
            val adPoint = historyJSONObject.getInt("adPoint")
            val itemObject = historyJSONObject.getJSONObject("item")
            val itemName = itemObject.getString("name")
            val itemThumbUrl = itemObject.getString("thumbnailUrl")
            resultList.add(
                NicoLiveGiftHistoryUserData(
                    advertiserName = advertiserName,
                    userId = userId,
                    adPoint = adPoint,
                    itemName = itemName,
                    itemThumbUrl = itemThumbUrl
                )
            )
        }
        resultList
    }

    /**
     * 投げ銭のトータルポイントを返すAPIを叩く
     *
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getGiftTotalPoint(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/contents/nage_agv/$liveId/totalGiftPoint")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftTotalPoint]をパースする関数
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftTotalPoint(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val totalPoint = jsonObject.getJSONObject("data").getInt("totalPoint")
        totalPoint
    }

    /**
     * 投げ銭で投げられたアイテムを取得するAPIを叩く
     *
     * @param liveId 番組ID
     * @param userSession ユーザーセッション
     * */
    suspend fun getGiftItemList(userSession: String, liveId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://api.nicoad.nicovideo.jp/v1/nagenico/nage_agv/$liveId/totalsoldcounts")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getGiftItemList]をパースする関数
     *
     * @param responseString レスポンスボディー
     * */
    suspend fun parseGiftItemList(responseString: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultList = arrayListOf<NicoLiveGiftItemData>()
        val jsonObject = JSONObject(responseString)
        val itemJSONArray = jsonObject.getJSONObject("data").getJSONArray("totalSoldCounts")
        for (i in 0 until itemJSONArray.length()) {
            val itemJSONObject = itemJSONArray.getJSONObject(i)
            val itemId = itemJSONObject.getString("itemId")
            val itemName = itemJSONObject.getString("itemName")
            val thumbnailUrl = itemJSONObject.getString("thumbnailUrl")
            val totalSoldCount = itemJSONObject.getInt("totalSoldCount")
            resultList.add(
                NicoLiveGiftItemData(
                    itemId = itemId,
                    itemName = itemName,
                    thumbnailUrl = thumbnailUrl,
                    totalSoldCount = totalSoldCount
                )
            )
        }
        resultList
    }
}