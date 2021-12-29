package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.SearchHistoryDAO
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity

/**
 * 検索履歴DB
 * 利用する際は
 * */
@Database(entities = [SearchHistoryDBEntity::class], version = 1)
abstract class SearchHistoryDB : RoomDatabase() {
    abstract fun searchHistoryDAO(): SearchHistoryDAO
}