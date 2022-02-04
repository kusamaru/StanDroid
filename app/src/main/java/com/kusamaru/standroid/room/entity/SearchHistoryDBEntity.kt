package com.kusamaru.standroid.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 検索履歴データベースの中身
 *
 * @param id 主キーです
 * @param addTime 追加日時です
 * @param pin ピン止めした場合はtrue。ところでSQLiteってBooleanあったっけ？
 * @param text 検索ワード
 * @param service 今のところ「video」のみ
 * @param sort ソート。[com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER]を参照して。
 * @param isTagSearch タグ検索ならtrue
 * @param description 将来使う？
 * */
@Entity(tableName = "search_history")
data class SearchHistoryDBEntity(
    @ColumnInfo(name = "_id") @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "add_time") val addTime: Long,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "service") val service: String,
    @ColumnInfo(name = "sort") val sort: String,
    @ColumnInfo(name = "tag_search") val isTagSearch: Boolean,
    @ColumnInfo(name = "pin") val pin: Boolean,
    @ColumnInfo(name = "description") val description: String
)