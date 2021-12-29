package io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass

/**
 * コミュニティーかチャンネルの情報が入っているデータクラス
 * @param id コミュ、チャンネルID
 * @param name なまえ
 * @param description 説明
 * @param isFollow フォロー中かどうか
 * @param icon アイコン
 * @param isChannel チャンネルの場合はtrue
 * */
data class CommunityOrChannelData(
    val id: String,
    val name: String,
    val description: String,
    val isFollow: Boolean,
    val icon: String,
    val isChannel: Boolean,
)