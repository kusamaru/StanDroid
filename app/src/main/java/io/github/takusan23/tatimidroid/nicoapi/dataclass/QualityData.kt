package io.github.takusan23.tatimidroid.nicoapi.dataclass

/**
 * 画質データクラス
 *
 * @param title タイトル。6Mbpsとか
 * @param isSelected 選択中ならtrue
 * @param isAvailable 利用可能ならtrue。一般会員等で利用できない場合はfalse
 * @param id 画質ID。
 * */
data class QualityData(
    val title: String,
    val id: String,
    val isSelected: Boolean,
    val isAvailable: Boolean,
)