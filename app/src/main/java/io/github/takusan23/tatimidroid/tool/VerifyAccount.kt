package io.github.takusan23.tatimidroid.tool

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Preferenceからメール、パスワードがあるかを確認する関数。
 * @param context Context
 * @return メール、パスワードがあればtrue
 * */
fun hasMailPass(context: Context?): Boolean {
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    return prefSetting.getString("mail", "")
        ?.isNotEmpty() == true && prefSetting.getString("password", "")?.isNotEmpty() == true
}

/**
 * ログインしないで利用する設定が有効かどうか確認する関数。有効時はtrue
 * @param context Context
 * @return 有効時はtrueです
 * */
fun isNotLoginMode(context: Context?): Boolean {
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    return prefSetting.getBoolean("setting_no_login", false)
}

/**
 * isNotLoginMode()の反転版。ログインする場合はtrue
 * */
fun isLoginMode(context: Context?): Boolean {
    return !isNotLoginMode(context)
}