package io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass

/**
 * タグAPI叩いたときに帰ってくるJSON解析結果を入れるデータクラス
 *
 * 動画でも使ってる
 *
 * @param tagName タグ名
 * @param isLocked ロックされてる場合。[type]が"category"の時はtrue？
 * @param type "category"？
 * @param isDeletable 消せるか。
 * @param hasNicoPedia 二コ百あってもfalseじゃね？
 * @param nicoPediaUrl 二コ百URL。
 * */
data class NicoTagItemData(
    val tagName: String,
    val isLocked: Boolean,
    val type: String,
    val isDeletable: Boolean,
    val hasNicoPedia: Boolean,
    val nicoPediaUrl: String,
)