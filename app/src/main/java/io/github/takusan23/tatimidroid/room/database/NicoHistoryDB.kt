package io.github.takusan23.tatimidroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.takusan23.tatimidroid.room.dao.NicoHistoryDBDAO
import io.github.takusan23.tatimidroid.room.entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.NicoHistoryDBInit

/**
 * 端末内履歴データベース
 * 使う際は[NicoHistoryDBInit.getInstance]を経由してね（SQLite->Room移行時にバージョンを上げるコードが書いてある）
 * */
@Database(entities = [NicoHistoryDBEntity::class], version = 4)
abstract class NicoHistoryDB : RoomDatabase() {
    abstract fun nicoHistoryDBDAO(): NicoHistoryDBDAO
}