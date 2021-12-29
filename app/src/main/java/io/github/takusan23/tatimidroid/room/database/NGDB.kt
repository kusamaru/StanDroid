package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.NGDBDAO
import io.github.takusan23.tatimidroid.room.entity.NGDBEntity

/**
 * NGのデータベース。
 * クラス名が略しすぎてわからんて
 * */
@Database(entities = [NGDBEntity::class], version = 2)
abstract class NGDB : RoomDatabase() {
    abstract fun ngDBDAO(): NGDBDAO
}