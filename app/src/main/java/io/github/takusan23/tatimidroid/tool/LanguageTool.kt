package io.github.takusan23.tatimidroid.tool

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import java.util.*

/**
 * 言語変更で使う関数がある。
 * ActivityとServiceで利用する必要があります。
 * (FragmentはなんかActivityのを勝手に使ってくれるっぽい？)
 *
 * 以下例
 * ```kotlin
 * override fun attachBaseContext(newBase: Context?) {
 *     super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
 * }
 * ```
 * */
object LanguageTool {

    /**
     * アプリ内設定の言語から言語指定Contextをセットする
     * @param baseContext attachBaseContextの引数にあるやつ
     * */
    fun setLanguageContext(newBase: Context?): Context? {
        // 設定取得
        val preference = PreferenceManager.getDefaultSharedPreferences(newBase)
        val settingLanguage = preference.getString("setting_language", "default") ?: "default"
        return if (settingLanguage == "default") {
            getLanguageContext(newBase) // 端末の設定に従う
        } else {
            getLanguageContext(newBase, settingLanguage) // 言語指定
        }
    }

    /**
     * 言語を指定したContextを生成する。
     * @param baseContext Contextを作るためにContextがいる
     * @param lang 日本語なら「ja」。英語なら「en」。それ以外はサポートしてない(strings.xmlを用意してない)。省略時は端末の設定を使う？
     * */
    fun getLanguageContext(baseContext: Context?, lang: String = Locale.getDefault().language): Context? {
        val configuration = Configuration()
        configuration.setLocale(Locale(lang))
        return baseContext?.createConfigurationContext(configuration)
    }

}