package com.kusamaru.standroid.nicoapi.nicovideo.dataclass

import dagger.multibindings.IntoMap
import kotlinx.serialization.Serializable

/**
 * シリーズのデータクラス
 * @param itemsCount 動画何件有るか
 * @param seriesId シリーズID
 * @param title シリーズタイトル
 * @param thumbUrl サムネ画像URL
 * */
class NicoVideoSeriesData(
    val title: String,
    val seriesId: String,
    val itemsCount: Int,
    val thumbUrl: String,
)

// こっからSeriesAPI用のデータクラス

/**
 * いろいろ省略しまくってるので呼び出す際は!必ず!ignoreUnknownKeyするように
 */
@Serializable
data class NicoPlayListAPIResponse (
    val meta: NicoPlayListAPIMeta,
    val data: NicoPlayListAPIData?,
)

@Serializable
data class NicoPlayListAPIMeta (
    val status: Int,
    val errorCode: String? = null
)

/**
 * idとmetaは面倒なので省略。必要になったら足す
 */
@Serializable
data class NicoPlayListAPIData (
    val totalCount: Int,
    val items: List<NicoPlayListAPIDataEntry>,
)

@Serializable
data class NicoPlayListAPIDataEntry (
    val watchId: String,
    val content: NicoPlayListAPIDataContent
)

@Serializable
data class NicoPlayListAPIDataContent (
    val type: String,
    val id: String,
    val title: String,
    val registeredAt: String,
    val count: NicoPlayListAPIDataCount,
    val thumbnail: NicoPlaylistAPIDataThumbnail,
    val duration: Long,
    val shortDescription: String,
    val isChannelVideo: Boolean,
    val isPaymentRequired: Boolean,
)

@Serializable
data class NicoPlayListAPIDataCount (
    val view: Int,
    val comment: Int,
    val mylist: Int,
    val like: Int,
)

@Serializable
data class NicoPlaylistAPIDataThumbnail (
    val url: String,
)