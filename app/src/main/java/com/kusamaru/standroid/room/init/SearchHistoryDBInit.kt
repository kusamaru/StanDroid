package com.kusamaru.standroid.room.init

import android.content.Context
import androidx.room.Room
import com.kusamaru.standroid.room.database.SearchHistoryDB

/**
 * 検索履歴DBへアクセスする
 * */
object SearchHistoryDBInit {

    private lateinit var searchHistoryDB: SearchHistoryDB

    /**
     * データベースのインスタンスを返す。シングルとん
     *
     * @param context Context
     * */
    fun getInstance(context: Context): SearchHistoryDB {
        if (!::searchHistoryDB.isInitialized) {
            // 一度だけ生成
            searchHistoryDB = Room.databaseBuilder(context, SearchHistoryDB::class.java, "SearchHistoryDB.db").build()
        }
        return searchHistoryDB
    }

}