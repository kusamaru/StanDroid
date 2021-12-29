package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.NGUploaderVideoIdDAO
import io.github.takusan23.tatimidroid.room.entity.NGUploaderVideoIdEntity

/**
 * NG投稿者が投稿した動画IDのデータベース
 *
 * 「NG投稿者」のデータベースはこっち：[io.github.takusan23.tatimidroid.room.database.NGUploaderUserIdDB]
 * */
@Database(entities = [NGUploaderVideoIdEntity::class], version = 1)
abstract class NGUploaderVideoIdDB : RoomDatabase() {
    abstract fun ngUploaderVideoIdDAO(): NGUploaderVideoIdDAO
}