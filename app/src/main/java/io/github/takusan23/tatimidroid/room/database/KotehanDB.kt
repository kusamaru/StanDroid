package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.KotehanDBDAO
import io.github.takusan23.tatimidroid.room.entity.KotehanDBEntity

/**
 * コテハンデータベース
 * */
@Database(entities = [KotehanDBEntity::class], version = 1)
abstract class KotehanDB : RoomDatabase() {
    abstract fun kotehanDBDAO(): KotehanDBDAO
}