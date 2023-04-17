package com.kusamaru.standroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kusamaru.standroid.room.dao.KotehanDBDAO
import com.kusamaru.standroid.room.entity.KotehanDBEntity

/**
 * コテハンデータベース
 * */
@Database(entities = [KotehanDBEntity::class], version = 1, exportSchema = false)
abstract class KotehanDB : RoomDatabase() {
    abstract fun kotehanDBDAO(): KotehanDBDAO
}