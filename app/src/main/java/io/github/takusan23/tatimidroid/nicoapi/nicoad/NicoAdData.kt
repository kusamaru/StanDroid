package io.github.takusan23.tatimidroid.nicoapi.nicoad

/**
 * ニコニ広告のAPIを叩いた結果のデータクラス
 *
 * 現在のポイントやトータルポイントなど
 *
 * @param contentId 動画IDか生放送ID
 * @param contentTitle 動画タイトルか生放送タイトル
 * @param totalPoint 累計ポイント
 * @param activePoint 広告期間中ポイント
 * @param thumbnailUrl サムネ画像URL
 * */
data class NicoAdData(
    val contentId: String,
    val contentTitle: String,
    val totalPoint: Int,
    val activePoint: Int,
    val thumbnailUrl: String,
)