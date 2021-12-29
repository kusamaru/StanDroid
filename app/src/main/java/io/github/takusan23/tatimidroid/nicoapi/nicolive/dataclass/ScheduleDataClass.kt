package io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass

/**
 * 延長JSONのデータクラス。
 * @param beginTime 番組開始時間？
 * @param endTime 番組終了時間
 * */
data class ScheduleDataClass(
    val beginTime: Long,
    val endTime: Long
)