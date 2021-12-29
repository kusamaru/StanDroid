package io.github.takusan23.tatimidroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NG投稿者が投稿した動画の動画IDデータベースの中に入れるデータのデータクラス
 *
 * @param id 主キー
 * @param videoId NGにした動画ID
 * @param userId ユーザーID
 * @param addDateTime 追加日時（UnixTimeのミリ秒）
 * @param description 将来的に使う？
 * */
@Entity(tableName = "ng_uploader_video_id")
data class NGUploaderVideoIdEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "video_id") val videoId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "add_date_time") val addDateTime: Long,
    @ColumnInfo(name = "description") val description: String,
)