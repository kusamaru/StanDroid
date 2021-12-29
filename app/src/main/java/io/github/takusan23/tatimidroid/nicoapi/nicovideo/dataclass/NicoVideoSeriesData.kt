package io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass

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