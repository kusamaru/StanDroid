package com.kusamaru.standroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NGデータベースのEntity。からむ
 * @param id 主キーです
 * @param type comment か user
 * @param value コメントならコメント本文。ユーザーならユーザーID
 * @param description 将来使うかも
 * */
@Entity(tableName = "ng_list")
data class NGDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "description") val description: String
)