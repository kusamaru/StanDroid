package io.github.takusan23.tatimidroid.tool

import android.content.Context
import androidx.preference.PreferenceManager
import java.io.File
import java.text.SimpleDateFormat


/**
 * クラッシュレポートを回収する（テキストファイルに書き込む）
 *
 * 外出先でクラッシュしたとき用
 *
 * MediaStore APIではDocumentsフォルダへアクセスできないらしいので、仕方なく固有外部ストレージへ保存（Android/data）
 * */
object CrashReportGenerator {

    /**
     * クラッシュレポートを回収する設定が有効かどうか
     * @param context Context
     * @return 有効ならtrue
     * */
    fun isEnableSaveCrashReport(context: Context) = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dev_setting_save_crash_report", false)

    /**
     * これを[io.github.takusan23.tatimidroid.MainActivity]なんかに置いておけばいいと思います。
     *
     * クラッシュした際にレポートを保存する処理が書いてあります。
     *
     * @param context Context
     * */
    fun initCrashReportGenerator(context: Context) {
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            // 保存先
            val crashReportFolder = File(context.getExternalFilesDir(null), "crash_report").apply {
                if (!exists()) {
                    mkdir()
                }
            }
            val crashReportDate = SimpleDateFormat("yyyyMMdd-HHmmss").format(System.currentTimeMillis())
            // ファイルに書き込む
            File(crashReportFolder, "crash_$crashReportDate.txt").apply {
                createNewFile()
                val crashReport = e.stackTrace.joinToString(separator = "\n")
                writeText(crashReport)
            }
            // Androidのクラッシュダイアログを表示
            defaultUncaughtExceptionHandler?.uncaughtException(t, e)
        }
    }

}