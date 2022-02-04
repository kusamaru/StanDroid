package com.kusamaru.standroid.nicoapi.nicolive.dataclass

/**
 * 投げ銭のランキングAPIのユーザーのデータクラス
 *
 * @param userId ユーザーID
 * @param advertiserName ユーザー名
 * @param totalContribution　貢献度？よくわからん
 * @param rank 何位か
 * */
data class NicoLiveGiftRankingUserData(
    val userId: Int?,
    val advertiserName: String,
    val totalContribution: Int,
    val rank: Int,
)