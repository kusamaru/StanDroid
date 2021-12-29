package io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass

import java.io.Serializable

/**
 * 番組一覧で表示するRecyclerViewに渡すデータクラス
 *
 * @param title タイトル
 * @param programId 番組ID
 * @param broadCaster 放送者
 * @param communityName コミュ名
 * @param isOfficial 公式かどうか
 * @param lifeCycle ON_AIR が放送中
 * @param thum サムネ
 * @param beginAt 番組開始時間。UnixTime。何故か文字列だけどLongに変換してどうぞ。ミリ秒ではなく秒です。
 * @param endAt UnixTime。これもLongに変換できると思う。ミリ秒ではなく秒。
 * */
data class NicoLiveProgramData(
    val title: String,
    val communityName: String,
    val beginAt: String,
    val endAt: String,
    val programId: String,
    val broadCaster: String,
    val lifeCycle: String,
    val thum: String,
    val isOfficial: Boolean = false
) : Serializable