package com.kusamaru.standroid.nicoapi.user

import java.io.Serializable

/**
 * ユーザー取得APIのレスポンス。
 * @param description 説明文。どうやらHTML
 * @param isPremium プレ垢ならtrue。価値は人それぞれ
 * @param niconicoVersion 登録時のバージョン。GINZAとか
 * @param followeeCount フォロー中数
 * @param followerCount フォロワーの数
 * @param userId ユーザーID。チャンネルの場合はchが含まれるため文字列になります
 * @param nickName ユーザー名
 * @param isFollowing フォロー中？
 * @param currentLevel 現在のレベル
 * @param largeIcon アイコンのURL
 * @param isNotAPICallVer APIを叩かずに取得した場合はtrue。どういうことかって言うと動画視聴ページから取得するとフォロー人数とかは取れない。そのため。[nickName]と[userId]、[largeIcon]は取得できるはず
 * @param isChannel チャンネルの場合はtrue。[isNotAPICallVer]もtrueになると思う。
 * */
data class UserData(
    val description: String,
    val isPremium: Boolean,
    val niconicoVersion: String,
    val followeeCount: Int,
    val followerCount: Int,
    val userId: String,
    val nickName: String,
    val isFollowing: Boolean,
    val currentLevel: Int,
    val largeIcon: String,
    val isNotAPICallVer: Boolean = false,
    val isChannel: Boolean = false,
) : Serializable