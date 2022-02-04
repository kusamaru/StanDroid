package com.kusamaru.standroid.setting.dbclear

import android.content.Context
import androidx.room.RoomDatabase
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.init.*

/**
 * データベース削除で使うデータクラス
 *
 * @param database データベース
 * @param databaseDescription データベースの説明
 * @param isDelete 削除する場合はtrue
 * */
data class DatabaseClearData(
    val database: RoomDatabase,
    val databaseDescription: String,
    val isDelete: Boolean,
) {

    companion object {

        /**
         * データベース削除一覧で表示する配列を返す関数
         * @param context Context
         * */
        fun getDatabaseList(context: Context): List<DatabaseClearData> {
            return listOf(
                DatabaseClearData(KotehanDBInit.getInstance(context), context.getString(R.string.database_kotehan), false),
                DatabaseClearData(NGDBInit.getInstance(context), context.getString(R.string.database_ng_user), false),
                DatabaseClearData(NGUploaderUserIdDBInit.getInstance(context), context.getString(R.string.database_ng_uploader), false),
                DatabaseClearData(NGUploaderVideoIdDBInit.getInstance(context), context.getString(R.string.database_ng_video), false),
                DatabaseClearData(NicoHistoryDBInit.getInstance(context), context.getString(R.string.database_local_history), false),
                DatabaseClearData(SearchHistoryDBInit.getInstance(context), context.getString(R.string.database_search_history), false),
            )
        }

    }

}