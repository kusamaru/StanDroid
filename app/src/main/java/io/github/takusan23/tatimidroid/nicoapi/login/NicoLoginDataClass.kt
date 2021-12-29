package io.github.takusan23.tatimidroid.nicoapi.login

import java.io.Serializable

/**
 * ニコ動にログインした際の情報を入れているデータクラス。[NicoLogin.nicoLoginCoroutine]の戻り値
 * @param isNeedTwoFactorAuth 二段階認証が必須の場合はtrue
 * @param userSession [isNeedTwoFactorAuth]がfalseのとき（二段階認証未設定時）はここにユーザーセッションが入る
 * @param twoFactorURL 二段階認証WebページのURL
 * @param twoFactorCookie 二段階認証でCookieが必要なのでそれ
 * */
data class NicoLoginDataClass(
    val isNeedTwoFactorAuth: Boolean = false,
    val userSession: String? = null,
    val twoFactorURL: String? = null,
    val twoFactorCookie: String? = null,
) : Serializable