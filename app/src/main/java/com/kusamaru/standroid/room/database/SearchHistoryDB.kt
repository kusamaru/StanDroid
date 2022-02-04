package com.kusamaru.standroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kusamaru.standroid.room.dao.SearchHistoryDAO
import com.kusamaru.standroid.room.entity.SearchHistoryDBEntity

/**
 * 検索履歴DB
 * 利用する際は
 * */
@Database(entities = [SearchHistoryDBEntity::class], version = 1)
abstract class SearchHistoryDB : RoomDatabase() {
    abstract fun searchHistoryDAO(): SearchHistoryDAO
}