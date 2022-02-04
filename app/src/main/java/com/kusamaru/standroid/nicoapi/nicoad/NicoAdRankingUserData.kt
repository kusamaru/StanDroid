package com.kusamaru.standroid.nicoapi.nicoad

/**
 * ニコニ広告の貢献度ランキングのユーザー配列に詰めるデータクラス
 *
 * @param userId ユーザーID。匿名で投稿するとnull
 * @param advertiserName 広告した人の名前
 * @param totalContribution どんだけ貢いだか
 * @param rank ランキング
 * */
data class NicoAdRankingUserData(
    val userId: Int?,
    val advertiserName: String,
    val totalContribution: Int,
    val rank: Int,
)