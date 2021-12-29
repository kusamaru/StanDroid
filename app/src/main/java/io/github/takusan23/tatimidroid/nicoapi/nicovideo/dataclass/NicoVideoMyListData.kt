package io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass

import java.io.Serializable

/**
 * マイリスト一覧で使うデータクラス
 * @param id マイリストID
 * @param isMe 私のものだったらtrue。APIが違う？ので
 * @param itemsCount 動画数
 * @param title マイリスト名
 * @param isAtodemiru あとでみるならtrue
 * */
data class NicoVideoMyListData(
    val title: String,
    val id: String,
    val itemsCount: Int,
    val isMe: Boolean,
    val isAtodemiru: Boolean = false
) : Serializable