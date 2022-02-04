package com.kusamaru.standroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kusamaru.standroid.room.dao.NGUploaderVideoIdDAO
import com.kusamaru.standroid.room.entity.NGUploaderVideoIdEntity

/**
 * NG投稿者が投稿した動画IDのデータベース
 *
 * 「NG投稿者」のデータベースはこっち：[com.kusamaru.standroid.room.database.NGUploaderUserIdDB]
 * */
@Database(entities = [NGUploaderVideoIdEntity::class], version = 1)
abstract class NGUploaderVideoIdDB : RoomDatabase() {
    abstract fun ngUploaderVideoIdDAO(): NGUploaderVideoIdDAO
}