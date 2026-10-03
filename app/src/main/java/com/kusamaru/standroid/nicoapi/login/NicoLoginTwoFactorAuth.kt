package com.kusamaru.standroid.nicoapi.login

import android.os.Build
import com.kusamaru.standroid.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

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
    private val twoFactorAuthAPIURL = requireNotNull(nicoLoginDataClass.twoFactorURL)

    /** リダイレクトを禁止にした[OkHttpClient] */
    private val okHttpClient = OkHttpClient.Builder().apply {
        // リダイレクトを禁止する
        followRedirects(false)
        followSslRedirects(false)
        // Debugビルドならログを出す
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
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
        try {
            // 認証コードを入れてAPIを叩く
            val (finalAPIURL, trustDeviceToken) = postOneTimePassword(twoFactorAuthAPIURL, otp, isTrustDevice)
            // ラスト、ユーザーセッションを取得する
            if (finalAPIURL == null) return@withContext null
            val userSession = getUserSession(finalAPIURL) ?: return@withContext null
            Pair(userSession, trustDeviceToken)
        } catch (_: IOException) {
            null
        }
    }

    /**
     * 認証コードを入れて、二段階認証APIを叩く。
     * 成功するとステータスコードが302になる。
     * @param otp 認証コード。メールで届いてるはず
     * @param refererUrl リファラに指定するURL。どこ指したらいいんだこれ
     * @param isTrustDevice このデバイスを信頼する場合はtrue。信頼すると次回から二段階認証をスキップできる（クライアント側で対応必須）。せーのでほっぴんジャンプ♪
     * @return Pair<String,String>を返します。
     * 一個目は最後に叩くAPIのURLです。
     * 二個目は[isTrustDevice]がtrue(このデバイスを信頼する場合)なら、mfa_trusted_device_tokenの値を入れてます。無いならnullです
     * */
    private suspend fun postOneTimePassword(refererUrl: String, otp: String, isTrustDevice: Boolean) = withContext(Dispatchers.Default) {
        val formData = FormBody.Builder().apply {
            add("otp", otp) // メールで送られてきた認証コード
            add("loginBtn", "ログイン")
            add("is_mfa_trusted_device", isTrustDevice.toString()) // これ true だとレスポンスSet-Cookieになんか信用してます的な内容が入る。これをログイン時のCookieにくっつけることでパスできる
            add("device_name", "Stan-Droid（${Build.MODEL}）") // デバイス名
        }.build()
        val request = Request.Builder().apply {
            url(twoFactorAuthAPIURL)
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("Cookie", loginCookie)
            addHeader("Referer", refererUrl)
            addHeader("Origin", "https://account.nicovideo.jp")
            addHeader("Upgrade-Insecure-Requests", "1")
            addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
            post(formData)
        }.build()
        okHttpClient.newCall(request).execute().use { response ->
            if (response.code != 302) return@withContext Pair(null, null)
            val finalAPIURL = response.headers["Location"]?.let { response.request.url.resolve(it) }
                ?: return@withContext Pair(null, null)
            val trustDeviceToken = if (isTrustDevice) {
                findLoginCookie(response.headers, request.url, "mfa_trusted_device_token")
                    ?.let { "${it.name}=${it.value}" }
            } else null
            Pair(finalAPIURL.toString(), trustDeviceToken)
        }
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
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("Cookie", loginCookie)
            get()
        }.build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 302) return@withContext null
            findLoginCookie(response.headers, request.url, "user_session")?.value
        }
    }

}
