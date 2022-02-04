package com.kusamaru.standroid.nicoapi.nicolive.dataclass

/**
 * 投げ銭のアイテムのデータクラス
 *
 * @param itemId アイテムID？
 * @param itemName アイテムの名前
 * @param thumbnailUrl アイテムの画像
 * @param totalSoldCount 売れた数
 * */
data class NicoLiveGiftItemData(
    val itemId: String,
    val itemName: String,
    val thumbnailUrl: String,
    val totalSoldCount: Int,
)