package com.kusamaru.standroid.nicoapi.login

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.text.toLowerCase
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.BuildConfig
import com.kusamaru.standroid.activity.TwoFactorAuthLoginActivity
import com.kusamaru.standroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.*

/**
 * ニコニコにログインする関数。
 * コルーチンで使ってください
 *
 * ```kotlin
 * //ログイン
 * lifecycleScope.launch(Dispatchers.Main) {
 *     // ログインAPIを叩く
 *     val userSession = NicoLogin.secureNicoLogin(requireContext()) ?: return@launch
 * }
 * ```
 * 二段階認証の場合は
 * ログインする
 * ↓
 * ログインAPIを叩いたレスポンスヘッダーにLocation（次ここ行け）ってURLが入るのでそのURLを控える
 * ついでにSet-Cookieに入っている値をこの後のリクエストで使うので控える。mfa_tokenとnicosidが必須？
 * ↓
 * 二段階認証の認証コードがメールで届く（スマホでも変わらん？）
 * ↓
 * https://account.nicovideo.jp/mfa~(ログインAPIのレスポンスヘッダーのLocationの値)
 * へOTP（ワンタイムパスワード等）を入れてAPIを叩く
 * ↓
 * レスポンスヘッダーにまたLocation（最後ここ行け）ってURLが入ってくるのでそのURLへアクセスする
 * ↓
 * レスポンスヘッダーのSet-Cookieにユーザーセッションが入ってログイン成功
 *
 * [NicoLoginTwoFactorAuth]参照
 * */
object NicoLogin {

    /**
     * 二段階認証対応版。本当は通信とUIは別の関数にすべきなんだけどちょっと複雑すぎんよ。
     *
     * SharedPreferenceに値が保存されている必要があるので、初回時は[saveMailPassPreference]を呼んで端末内にメアド、パスワードを保存してください。
     * @param context [android.content.SharedPreferences]を利用するため。
     * @return 失敗時と二段階認証画面に移動する場合はnull。成功時のみユーザーセッションを返します。
     * なお二段階認証時は終了時にPreferenceに保存します。
     * ```
     * val userSession = NicoLogin.secureNicoLogin(requireContext()) ?: return
     * ```
     * */
    suspend fun secureNicoLogin(context: Context?): String? = withContext(Dispatchers.Default) {
        // メアドを取り出す
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        val mail = prefSetting.getString("mail", "")
        val pass = prefSetting.getString("password", "")
        // 二段階認証時以外ならnull。二段階認証時でもデバイスを信頼してない場合はnull。それ以外なら信頼されてるとして、二段階認証時をパスできる。
        val trustDeviceToken = prefSetting.getString("trust_device_token", null)
        if (mail == null && pass == null) {
            withContext(Dispatchers.Main) {
                // メアド設定してね
                Toast.makeText(context, R.string.mail_pass_error, Toast.LENGTH_SHORT).show()
            }
            return@withContext null
        }
        // ログインAPIを叩く
        val nicoLoginResult = nicoLoginCoroutine(mail!!, pass!!, trustDeviceToken)
        if (nicoLoginResult != null) {
            if (!nicoLoginResult.isNeedTwoFactorAuth) {
                // 二段階認証 が　未　設　定
                // 二段階認証未設定時
                prefSetting.edit {
                    putString("user_session", nicoLoginResult.userSession)
                }
                return@withContext nicoLoginResult.userSession!!
            } else {
                withContext(Dispatchers.Main) {
                    // 二段階認証画面へ飛ばす
                    val twoFactorAuthLoginActivity = Intent(context, TwoFactorAuthLoginActivity::class.java)
                    twoFactorAuthLoginActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    twoFactorAuthLoginActivity.putExtra("login", nicoLoginResult)
                    context?.startActivity(twoFactorAuthLoginActivity)
                }
                return@withContext null
            }
        } else {
            withContext(Dispatchers.Main) {
                // しっぱい
                Toast.makeText(context, R.string.login_error, Toast.LENGTH_SHORT).show()
            }
        }
        null
    }

    /**
     * メアド、パスワードをPreferenceへ保存する。初回実行時は[secureNicoLogin]が利用できないので、まずこれを使って
     * */
    fun saveMailPassPreference(context: Context?, mail: String, pass: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString("mail", mail)
            putString("password", pass)
        }
    }

    /**
     * ニコニコにログインする関数。二段階認証対応？
     * OkHttpで書き直した。リダイレクト禁止すればできます。Set-Cookieが複数あるので注意
     * @param mail メアド
     * @param pass パスワード
     * @param trustDeviceToken 二段階認証時で、このデバイスが信頼されている場合は、mfa_trusted_device_tokenの値を入れてください。（二段階認証時に信頼するにチェックを入れるとSet-Cookieにmfa_trusted_device_tokenのあたいが入る）
     * @return [NicoLoginDataClass]を返します。
     *          二段階認証が必要な場合は[NicoLoginDataClass.isTwoFactor]がtrueになってます。
     *          [NicoLoginDataClass.isTwoFactor]がfalseの場合は[NicoLoginDataClass.userSession]にユーザーセッションが入っています。
     * */
    suspend fun nicoLoginCoroutine(mail: String, pass: String, trustDeviceToken: String? = null): NicoLoginDataClass? = withContext(Dispatchers.Default) {
        val url = "https://account.nicovideo.jp/login/redirector"
        val postData = "mail_tel=$mail&password=$pass"
        val request = Request.Builder().apply {
            url(url)
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            if (trustDeviceToken != null) {
                addHeader("Cookie", trustDeviceToken)
            }
            post(postData.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())) // 送信するデータ。
        }.build()
        // リダイレクト禁止（そうしないとステータスコードが302にならない）
        val okHttpClient = OkHttpClient().newBuilder().apply {
            followRedirects(false)
            followSslRedirects(false)
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            }
        }.build()
        val response = okHttpClient.newCall(request).execute()
        // 成功時
        if (response.code == 302) {
            // 二段階認証がかかっているかどうか
            var userSession = ""
            // Set-Cookieを探す。
            // なんか複雑なことしてるけどおそらくヘッダーSet-Cookieが複数あるせいで最後のSet-Cookieの値しか取れないのでめんどい
            response.headers.filter { pair ->
                pair.second.contains("user_session") && !pair.second.contains("secure") && !pair.second.contains("deleted")
            }.forEach { header ->
                // user_session
                userSession = header.second.split(";")[0].replace("user_session=", "")
                return@withContext NicoLoginDataClass(false, userSession = userSession)
            }
            // mfa_sessionがあったので二段階認証が必須
            if (response.headers.any { pair -> pair.second.contains("mfa_session") }) {
                // 設定されてる。二段階認証のためにCookieも取得する
                val loginCookie = getLoginCookie(response.headers)
                return@withContext NicoLoginDataClass(true, twoFactorURL = response.headers["Location"], twoFactorCookie = loginCookie)
            } else {
                // なかった
                return@withContext null
            }
        } else {
            // そもそも失敗
            return@withContext null
        }
    }

    /**
     * 二段階認証に必要なCookieを取得する関数
     * @param responseHeaders [nicoLoginCoroutine]の内部で使ってるので。ログインのレスポンスヘッダー
     * @return Cookie。mfa_session=なんとか;nicosid=なんとか
     * */
    private fun getLoginCookie(responseHeaders: Headers?): String {
        // Set-Cookieを解析
        var mfaSession = ""
        var nicosid = ""
        responseHeaders?.forEach {
            // Set-Cookie に入ってる mfa_session と nicosid を控える
            // レスポンスのSet-Cookieが小文字になってたせいで動かなくなったっぽい。なぜ……
            if (it.first.lowercase() == "set-cookie") {
                if (it.second.contains("mfa_session")) {
                    mfaSession = it.second.split(";")[0]
                }
                if (it.second.contains("nicosid")) {
                    nicosid = it.second.split(";")[0]
                }
            }
        }
        return "$mfaSession;$nicosid"
    }
}