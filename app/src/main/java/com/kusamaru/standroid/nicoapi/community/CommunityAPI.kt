package com.kusamaru.standroid.nicoapi.community

import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup

/**
 * コミュをフォローしたりフォローしなかったりする関数があるクラス。classじゃなくてobjectでも良かった感
 * */
class CommunityAPI {

    /** OkHttpClientを使い回す */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * コミュをフォローする関数。リクエストヘッダーに「X-Requested-By」が必要な模様。
     *
     * すでにフォロー中の場合は409が帰ってくる模様。
     *
     * @param communityId coから始まる数字。co6246とか
     * @param userSession ユーザーセッション
     * @return [okhttp3.Response.isSuccessful]ならフォロー成功だと思う
     * */
    suspend fun requestCommunityFollow(userSession: String, communityId: String) = withContext(Dispatchers.Default) {
        val request = Request.Builder().apply {
            // coを抜いたコミュIDをURLの中に組み込む
            url("https://com.nicovideo.jp/api/v1/communities/${communityId.replace("co", "")}/follows.json")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            // これがないと失敗する
            header("X-Requested-By", "https://com.nicovideo.jp/motion/$communityId")
            // Chromeで通信追いかけたけどContent-Length=0で何も送ってなかったので
            post(byteArrayOf(0).toRequestBody())
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * フォローを解除する
     *
     * こっちは一筋縄では行かない模様。
     * @param communityId コミュID
     * @param userSession ユーザーセッション
     * @return nullなら途中で失敗してる。[okhttp3.Response.isSuccessful]
     * */
    suspend fun requestRemoveCommunityFollow(userSession: String, communityId: String) = withContext(Dispatchers.Default) {
        // 1.解除ページをリクエスト
        val request = Request.Builder().apply {
            url("https://com.nicovideo.jp/leave/$communityId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        // HTMLを解析する。最後のformのタグを取る
        val form = Jsoup.parse(response.body?.string()).getElementsByTag("form").last()
        // POSTに必要な情報を集める。HTMLのFormよくわがんね
        val time = form.getElementsByTag("input")[0].attr("value")
        val commitKey = form.getElementsByTag("input")[1].attr("value")
        val commit = form.getElementsByTag("input")[2].attr("value")
        // POSTするFormを作成
        val postFormData = FormBody.Builder().apply {
            add("time", time)
            add("commit_key", commitKey)
            add("commit", commit)
        }.build()
        // コミュ解除APIを叩く。こっちには「Referer」が必要
        val unfollowRequest = Request.Builder().apply {
            url("https://com.nicovideo.jp/leave/$communityId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            header("Referer", "https://com.nicovideo.jp/leave/$communityId")
            // URL同じだけどPOSTリクエストになる。DELETEではなかった
            post(postFormData)
        }.build()
        // これで最後
        return@withContext okHttpClient.newCall(unfollowRequest).execute()
    }
}