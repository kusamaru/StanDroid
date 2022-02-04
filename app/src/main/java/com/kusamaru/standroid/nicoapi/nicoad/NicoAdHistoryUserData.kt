package com.kusamaru.standroid.nicoapi.nicoad

/**
 * ニコニ広告の広告履歴のユーザーのデータクラス
 *
 * @param userId ユーザーID。匿名で投稿するとnull
 * @param advertiserName 広告した人の名前
 * @param contribution 投げたポイント？
 * @param message 広告メッセージ。未設定時はnull
 * */
data class NicoAdHistoryUserData(
    val userId: Int?,
    val advertiserName: String,
    val contribution: Int,
    val message: String?,
)