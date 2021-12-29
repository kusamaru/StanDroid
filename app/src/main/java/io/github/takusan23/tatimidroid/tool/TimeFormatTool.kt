package io.github.takusan23.tatimidroid.tool

import android.text.format.DateUtils

/**
 * 時間をHH:mm:ssに変換する
 * */
object TimeFormatTool {

    /**
     * 変換する関数
     * @param position 時間（秒）
     * @return HH:mm:ss。0以下の場合は空文字
     * */
    fun timeFormat(position: Long): String {
        return if (position < 0) {
            ""
        }else{
            DateUtils.formatElapsedTime(position)
        }
    }

}