package io.github.takusan23.tatimidroid.nicoapi.user

import io.github.takusan23.tatimidroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * ニコニコのユーザー情報を取得するAPI
 * コルーチン版のみ
 * */
class UserAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ユーザー情報を取得する。コルーチン版。自分の情報を取得する[getMyAccountUserData]もあります。
     * @param userId ユーザーID。作者は「40210583」。nullの場合は自分のアカウント情報を取りに行く
     * @param userSession ユーザーセッション
     * */
    suspend fun getUserData(userSession: String, userId: String? = null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if (userId == null) {
                url("https://nvapi.nicovideo.jp/v1/users/me")
            } else {
                url("https://nvapi.nicovideo.jp/v1/users/$userId")
            }
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3") // これ必要。
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * 自分のアカウントの情報を取得する。
     * @param userSession ユーザーセッション
     * */
    suspend fun getMyAccountUserData(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3") // これ必要。
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getUserData]のレスポンスぼでーをパースする
     * @param responseString [okhttp3.ResponseBody.string]
     * @return データクラス
     * */
    suspend fun parseUserData(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val user = jsonObject.getJSONObject("data").getJSONObject("user")
        val description = user.getString("description")
        val isPremium = user.getBoolean("isPremium")
        val niconicoVersion = user.getString("registeredVersion") // GINZA とか く とか
        val followeeCount = user.getInt("followeeCount")
        val followerCount = user.getInt("followerCount")
        val userLevel = user.getJSONObject("userLevel")
        val currentLevel = userLevel.getInt("currentLevel") // ユーザーレベル。大人数ゲームとかはレベル条件ある
        val userId = user.getString("id")
        val nickName = user.getString("nickname")
        val isFollowing = if (jsonObject.getJSONObject("data").has("relationships")) {
            jsonObject.getJSONObject("data").getJSONObject("relationships")
                .getJSONObject("sessionUser").getBoolean("isFollowing") // フォロー中かどうか
        } else {
            false
        }
        val largeIcon = user.getJSONObject("icons").getString("large")
        UserData(description, isPremium, niconicoVersion, followeeCount, followerCount, userId, nickName, isFollowing, currentLevel, largeIcon)
    }

    /**
     * ユーザーをフォローする関数。
     * @param userSession ユーザーセッション
     * @param userId ユーザーID
     * */
    suspend fun postUserFollow(userSession: String, userId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://public.api.nicovideo.jp/v1/user/followees/niconico-users/${userId}.json")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "6") // これ必要。
            // これがないと 200 が帰ってこない
            header("X-Request-With", "https://www.nicovideo.jp/user/${userId}?ref=pc_mypage_follow_following")
            // POSTリクエスト。Content-Lengthが0なので特に何も送ってない
            post(byteArrayOf(0).toRequestBody())
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * ユーザーのフォローを解除する関数。Unfollowにしなかった理由はRemoveのほうが長くて間違いにくいかなってそれぐらい。
     * @param userSession ユーザーセッション
     * @param userId ユーザーID
     * */
    suspend fun postRemoveUserFollow(userSession: String, userId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://public.api.nicovideo.jp/v1/user/followees/niconico-users/${userId}.json")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "6") // これ必要。
            // これがないと 200 が帰ってこない
            header("X-Request-With", "https://www.nicovideo.jp/user/${userId}?ref=pc_mypage_follow_following")
            // Deleteリクエスト
            delete()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [postUserFollow]、[postRemoveUserFollow]が成功したかどうかを返す関数
     *
     * @param responseString [postUserFollow]、[postRemoveUserFollow]のれすぽんすぼでー
     * */
    suspend fun isSuccessfulFollowRequest(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val status = jsonObject.getJSONObject("meta").getInt("status")
        status == 200
    }

}