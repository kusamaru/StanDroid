package com.kusamaru.standroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NG投稿者IDデータベースに入れるデータのデータクラス
 *
 * @param id 主キー
 * @param userId NGにした投稿者のユーザーID
 * @param addDateTime 追加日時（UnixTimeのミリ秒）
 * @param latestUpdateTime 最新更新日時（UnixTimeのミリ秒）
 * @param videoCount NG動画数
 * @param description 将来的に使う？
 * */
@Entity(tableName = "ng_uploader_user_id")
data class NGUploaderUserIdEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "latest_update_time") val latestUpdateTime: Long,
    @ColumnInfo(name = "add_date_time") val addDateTime: Long,
    @ColumnInfo(name = "video_count") val videoCount: Int,
    @ColumnInfo(name = "description") val description: String,
)