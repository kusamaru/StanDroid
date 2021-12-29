package io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass

/**
 * ニコ動の視聴ページのHTML内にあるシリーズのデータをまとめたデータクラス
 *
 * [NicoVideoSeriesData]との違いは次の動画、前の動画、最初の動画の情報が入っているかどうか。こっちは入ってる。
 *
 * nullの場合はデータが無いんだなって。(最新話の場合は次の動画（[nextVideoData]）がnullになる)
 *
 * [io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoHTML.getSeriesHTMLData] 参照
 *
 * @param seriesData シリーズのデータクラス。タイトルなど
 * @param firstVideoData 最初の動画の情報が入ったデータクラス
 * @param nextVideoData 次の動画の情報が入ったデータクラス
 * @param prevVideoData 前の動画の情報が入ったデータクラス
 * */
data class NicoVideoHTMLSeriesData(
    val seriesData: NicoVideoSeriesData,
    val firstVideoData: NicoVideoData?,
    val nextVideoData: NicoVideoData?,
    val prevVideoData: NicoVideoData?
)