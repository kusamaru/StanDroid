package io.github.takusan23.tatimidroid.nicoapi.login

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

/**
 * 二段階認証に対応したログイン関数たち。
 *
 * リダイレクトを禁止しないとうまく動きません。
 *
 * 二段階認証に対応した話はここ：https://takusan.negitoro.dev/posts/nicovideo_two_factor
 *
 * @param nicoLoginDataClass [NicoLogin.nicoLoginCoroutine]の戻り値。
 * [NicoLoginDataClass.isNeedTwoFactorAuth]がtrueである必要があります。([NicoLoginDataClass.userSession]以外がnull以外になっている必要がある)
 * */
class NicoLoginTwoFactorAuth(private val nicoLoginDataClass: NicoLoginDataClass) {

    /** ログインで使うAPI全てに設定するCookie。 */
    private val loginCookie = nicoLoginDataClass.twoFactorCookie!!

    /** 二段階認証のWebページへのURL */
    private val twoFactorAuthAPIURL = nicoLoginDataClass.twoFactorURL!!

    /** リダイレクトを禁止にした[OkHttpClient] */
    private val okHttpClient = OkHttpClient.Builder().apply {
        // リダイレクトを禁止する
        followRedirects(false)
        followSslRedirects(false)
    }.build()


    /**
     * 二段階認証を完了させる関数。
     *
     * 二段階認証のWEBページ取得→二段階認証APIを叩く→ユーザーセッションを取得
     *
     * これを一つの関数にまとめた。
     * @param otp ワンタイムパスワード。メールで送られてくる。ドコモ口座の被害を回避できたのはこの仕組を導入していた銀行（それ以外にもあるけど）
     * @return Pair<String,String>を返します。
     * 一個目はユーザーセッションです。大切にしてね
     * 二個目は[isTrustDevice]がtrueの場合は次回から二段階認証をスキップできる値が入っています。この値をログイン時のCookieに詰めることで省略ができます。
     * */
    suspend fun twoFactorAuth(otp: String, isTrustDevice: Boolean) = withContext(Dispatchers.Default) {
        // 二段階認証のAPIを取得しに行く
        val twoFactorAuthAPIURL = getTwoFactorAPIURL()
        // 認証コードを入れてAPIを叩く
        val (finalAPIURL, trustDeviceToken) = postOneTimePassword(twoFactorAuthAPIURL, otp, isTrustDevice)
        // ラスト、ユーザーセッションを取得する
        val userSession = getUserSession(finalAPIURL!!)
        return@withContext Pair(userSession, trustDeviceToken)
    }

    /**
     * 二段階認証のWebページへアクセスして、二段階認証APIのURLを返す。
     * 成功するとステータスコードが200になる
     * @return 二段階認証APIのURL
     * */
    private suspend fun getTwoFactorAPIURL() = withContext(Dispatchers.Default) {
        val request = Request.Builder().apply {
            url(twoFactorAuthAPIURL)
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            addHeader("Cookie", loginCookie)
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        val responseString = response.body?.string()
        // HTML内からURLを探す
        val document = Jsoup.parse(responseString)
        val path = document.getElementsByTag("form")[0].attr("action")
        // 二段階認証をするAPIのURLを返す
        "https://account.nicovideo.jp$path"
    }

    /**
     * 認証コードを入れて、二段階認証APIを叩く。
     * 成功するとステータスコードが302になる。
     * @param otp 認証コード。メールで届いてるはず
     * @param twoFactorAuthAPIURL [getTwoFactorAPIURL]の戻り値
     * @param isTrustDevice このデバイスを信頼する場合はtrue。信頼すると次回から二段階認証をスキップできる（クライアント側で対応必須）。せーのでほっぴんジャンプ♪
     * @return Pair<String,String>を返します。
     * 一個目は最後に叩くAPIのURLです。
     * 二個目は[isTrustDevice]がtrue(このデバイスを信頼する場合)なら、mfa_trusted_device_tokenの値を入れてます。無いならnullです
     * */
    private suspend fun postOneTimePassword(twoFactorAuthAPIURL: String, otp: String, isTrustDevice: Boolean) = withContext(Dispatchers.Default) {
        val formData = FormBody.Builder().apply {
            add("otp", otp) // メールで送られてきた認証コード
            add("loginBtn", "ログイン")
            add("is_mfa_trusted_device", isTrustDevice.toString()) // これ true だとレスポンスSet-Cookieになんか信用してます的な内容が入る。これをログイン時のCookieにくっつけることでパスできる
            add("device_name", "たちみどろいど（${Build.MODEL}）") // デバイス名
        }.build()
        val request = Request.Builder().apply {
            url(twoFactorAuthAPIURL)
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            addHeader("Cookie", loginCookie)
            post(formData)
        }.build()
        val response = okHttpClient.newCall(request).execute()
        // デバイスを信頼している場合
        val trustDeviceToken = if (isTrustDevice) {
            response.headers.filter { pair -> pair.second.contains("mfa_trusted_device_token") }[0].second.split(";")[0]
        } else {
            null
        }
        val finalAPIURL = response.headers["Location"]
        Pair(finalAPIURL, trustDeviceToken)
    }

    /**
     * ユーザーセッションを取得するAPIを叩く。最終章。三年生。
     * @param location [postOneTimePassword]の戻り値
     * @return ユーザーセッション！！！。長かったね。Avaritia(マイクラのMOD)かよ
     * */
    private suspend fun getUserSession(location: String) = withContext(Dispatchers.Default) {
        val url = location // URLを完成させる
        val request = Request.Builder().apply {
            url(url)
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            addHeader("Cookie", loginCookie)
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response.headers.filter { pair -> pair.second.contains("user_session") }[1].second.split(";")[0].replace("user_session=", "")
    }

}