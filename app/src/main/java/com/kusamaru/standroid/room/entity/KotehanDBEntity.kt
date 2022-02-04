package com.kusamaru.standroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * コテハンDBカラム。コテハンを永続化したいというお声があったので
 * @param id 主キー
 * @param addTime 追加日時。毎週水曜だっけ？木曜だっけ？にメンテで184がリセットされるから。UnixTimeで頼んだ
 * @param userId ユーザーID
 * @param kotehan コテハン名
 * */
@Entity(tableName = "kotehan")
data class KotehanDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "add_time") val addTime: Long,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "kotehan") val kotehan: String
)