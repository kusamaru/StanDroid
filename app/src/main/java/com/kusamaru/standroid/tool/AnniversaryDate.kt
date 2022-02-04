package com.kusamaru.standroid.tool

import java.text.SimpleDateFormat
import java.util.*

/**
 * 一周年とかを計算するやつ。一周年とかじゃないなら「-1」を返す
 * @param postedUnixTime 比較する時間。UnixTime（ミリ秒）
 * */
fun calcAnniversary(postedUnixTime: Long): Int {
    // 投稿日
    val postedCalendar = Calendar.getInstance()
    postedCalendar.timeInMillis = postedUnixTime
    // 本日
    val todayCalendar = Calendar.getInstance()
    if (postedCalendar[Calendar.MONTH] == todayCalendar[Calendar.MONTH] && postedCalendar[Calendar.DAY_OF_MONTH] == todayCalendar[Calendar.DAY_OF_MONTH]) {
        // 同じ日付。今年から引き算
        return todayCalendar[Calendar.YEAR] - postedCalendar[Calendar.YEAR]
    } else {
        // 違うので -1 返す
        return -1
    }
}

/**
 * 動画投稿日が何日前か数えるやつ。
 * @param upDateTime yyyy/MM/dd HH:mm:ssの形式で。
 * */
fun calcDayCount(upDateTime: String): String {
    // UnixTime（ミリ秒）へ変換
    val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    // 時、分とかは切り捨てる（多分いらない。）
    val calendar = Calendar.getInstance().apply {
        time = simpleDateFormat.parse(upDateTime)
        set(Calendar.HOUR, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    // 現在時刻から引く
    val calc = System.currentTimeMillis() - calendar.time.time
    // 計算で出す。多分もっといい方法ある。
    val second = calc / 1000    // ミリ秒から秒へ
    val minute = second / 60    // 秒から分へ
    val hour = minute / 60      // 分から時間へ
    val day = hour / 24         // 時間から日付へ
    return day.toString()
}

/**
 * UnixTimeをフォーマットする
 * */
fun toFormatTime(unixTime: Long): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(unixTime)
}

class AnniversaryDate {

    companion object {
        /**
         * お祝いテンプレ
         * @param anniversary 一周年なら1など。calcAnniversary()の返り値入れれば良くない？
         * */
        fun makeAnniversaryMessage(anniversary: Int) = "\uD83E\uDD73  本日は $anniversary 回目のお誕生日です \uD83C\uDF89"
    }

}