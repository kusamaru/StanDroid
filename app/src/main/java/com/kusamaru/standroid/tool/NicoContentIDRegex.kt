package com.kusamaru.standroid.tool

import java.util.regex.Pattern

/** ニコ動の動画IDの正規表現。smとso */
val NICOVIDEO_ID_REGEX = "(sm|so)([0-9]+)"

/** ニコニコのコミュIDの正規表現。coとch */
val NICOCOMMUNITY_ID_REGEX = "(co|ch)([0-9]+)"

/** ニコ生の番組IDの正規表現 */
val NICOLIVE_ID_REGEX = "(lv)([0-9]+)"

/** ニコ動のマイリストのIDの正規表現 */
val NICOVIDEO_MYLIST_ID_REGEX = "(mylist/)([0-9]+)"

/** ニコ動のシリーズのIDの正規表現 */
val NICOVIDEO_SERIES_ID_REGEX = "(series/)([0-9]+)"

/**
 * 文字列の中からニコニコのID(sm/so/lv/co/ch)を見つける。
 * @param text 文字列
 * @return ID。なければnullです。
 * */
fun IDRegex(text: String): String? {
    // 正規表現
    val nicoIDMatcher = Pattern.compile(NICOLIVE_ID_REGEX)
        .matcher(text)
    val communityIDMatcher = Pattern.compile(NICOCOMMUNITY_ID_REGEX)
        .matcher(text)
    val nicoVideoIdMatcher = Pattern.compile(NICOVIDEO_ID_REGEX)
        .matcher(text)
    return when {
        nicoIDMatcher.find() -> nicoIDMatcher.group()
        communityIDMatcher.find() -> communityIDMatcher.group()
        nicoVideoIdMatcher.find() -> nicoVideoIdMatcher.group()
        else -> null
    }
}


