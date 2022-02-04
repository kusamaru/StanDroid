package com.kusamaru.standroid.tool

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.kusamaru.standroid.R

/**
 * ダークモードなど
 * DarkModeUtilとかDarkModeToolとかにしとけばよかった。
 * */
class DarkModeSupport(val context: Context) {

    /**
     * ダークモードテーマ。setContentView()の前に書いてね
     * */
    fun setActivityTheme(activity: AppCompatActivity) {
        if (isDarkMode(activity)) {
            activity.setTheme(R.style.OLEDTheme)
        } else {
            activity.setTheme(R.style.AppTheme)
        }
    }

    /**
     * MainActivity用。TitleBarが表示されていない。setContentView()の前に書いてね
     *
     * Material Youに対応した
     * */
    fun setMainActivityTheme(activity: AppCompatActivity) {
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(activity)
        if (prefSetting.getBoolean("setting_material_you", false)) {
            // Material You有効
            activity.setTheme(R.style.MainActivity_MaterialYou)
            DynamicColors.applyIfAvailable(activity)
        } else {
            if (isDarkMode(activity)) {
                activity.setTheme(R.style.MainActivity_OLEDTheme)
            } else {
                activity.setTheme(R.style.MainActivity_AppTheme)
            }
        }
    }


    /**
     * 二窓Activity用ダークモード切り替えるやつ。setContentView()の前に書いてね
     * */
    fun setNimadoActivityTheme(activity: AppCompatActivity) {
        if (isDarkMode(activity)) {
            activity.setTheme(R.style.NimadoOLEDTheme)
        } else {
            activity.setTheme(R.style.NimadoTheme)
        }
    }

    /**
     * 色を返す。白か黒か
     * ダークモード->黒
     * それ以外->白
     * */
    @Deprecated("DarkModeSupport#getThemeColor(context)を使ってください。", ReplaceWith("com.kusamaru.standroid.Tool.getThemeColor(context)", "io"))
    fun getThemeColor(): Int = getThemeColor(context)

}

/**
 * ダークモードかどうか。
 * アプリの設定に従って返します。
 * - 端末の設定に従う
 *     - 端末がダークモードならダークモードにする
 * - ダークモード
 *     - 端末に関係なくダークモード
 * - ライトモード
 *     - 端末に関係なくライトモード
 * @param context こんてきすと
 * @return ダークモードならtrue。そうじゃなければfalse。
 * */
fun isDarkMode(context: Context?): Boolean {
    if (context == null) return false
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    // ダークモードの設定を　「端末の設定に従う」「ダークモード」「ライトテーマ」　から選べるように。
    return when (prefSetting.getString("setting_darkmode_app", "device")) {
        "device" -> {
            // 端末の設定に従う。よく考えたらAndroid 9ってダークモードまだ普及して無くね？
            //ダークモードかどうか
            val conf = context.resources.configuration
            val nightMode = conf.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES // ダークモードなら true
        }
        "dark" -> true // 設定に関係なくライトモード
        "light" -> false // 設定に関係なくライトモード
        else -> false // ありえる？
    }
}

/**
 * テーマに合わせた色を返す関数。
 * @param context こんてきすと
 * @return ダークモードなら黒、それ以外なら白です。
 * */
fun getThemeColor(context: Context?): Int = if (isDarkMode(context)) {
    Color.parseColor("#000000")
} else {
    Color.parseColor("#ffffff")
}

/**
 * テーマに合ったテキスト色を返す関数。
 * ダークモード時は白を返します。
 * @param context こんてきすと
 * @return ダークモードなら白
 * */
fun getThemeTextColor(context: Context?): Int = if (isDarkMode(context)) {
    Color.parseColor("#ffffff")
} else {
    Color.parseColor("#000000")
}