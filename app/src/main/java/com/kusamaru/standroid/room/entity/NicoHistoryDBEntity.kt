package com.kusamaru.standroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 端末内履歴のデータベースに入ってる中身のデータクラス。カラム
 * @param id 主キー
 * @param type live (生放送) か video (動画)
 * @param serviceId 生放送ID か 動画ID
 * @param userId コミュID（生放送） か ユーザーID（動画。SQLite時代は何も保存していない。）
 * @param title タイトル
 * @param unixTime 追加時間。System.currentTimeMillis() / 1000 した値。
 * @param description 将来的に使うかも
 * */
@Entity(tableName = "history")
data class NicoHistoryDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "service_id") val serviceId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "date") val unixTime: Long,
    @ColumnInfo(name = "description") val description: String
)
