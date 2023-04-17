package com.kusamaru.standroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kusamaru.standroid.room.dao.NGDBDAO
import com.kusamaru.standroid.room.entity.NGDBEntity

/**
 * NGのデータベース。
 * クラス名が略しすぎてわからんて
 * */
@Database(entities = [NGDBEntity::class], version = 2, exportSchema = false)
abstract class NGDB : RoomDatabase() {
    abstract fun ngDBDAO(): NGDBDAO
}