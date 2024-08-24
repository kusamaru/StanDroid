package com.kusamaru.standroid.nicoapi.nicofeed

/**
 * @param data フォロー新着の本体
 * @param nextCursor 続きを持ってきたいときはこれを使う
 */
data class NicoFeedResponse(
    val data: ArrayList<NicoFeedDataClass>,
    val nextCursor: String?,
)

data class NicoFeedDataClass(
    val isVideo: Boolean,
    val message: String,
    val contentId: String,
    val date: Long,
    val thumbUrl: String,
    val title: String,
    val userName: String,
    val userId: String,
    val userIcon: String,
)