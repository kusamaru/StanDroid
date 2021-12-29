package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.NGUploaderUserIdDAO
import io.github.takusan23.tatimidroid.room.entity.NGUploaderUserIdEntity

/**
 * NG投稿者のデータベース。
 *
 * これとは別に「NG投稿者が投稿した動画ID」データベースが存在する
 * */
@Database(entities = [NGUploaderUserIdEntity::class], version = 1)
abstract class NGUploaderUserIdDB : RoomDatabase() {
    abstract fun ngUploaderUserIdDAO(): NGUploaderUserIdDAO
}