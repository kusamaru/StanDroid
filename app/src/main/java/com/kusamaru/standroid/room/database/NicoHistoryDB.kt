package com.kusamaru.standroid.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kusamaru.standroid.room.dao.NicoHistoryDBDAO
import com.kusamaru.standroid.room.entity.NicoHistoryDBEntity
import com.kusamaru.standroid.room.init.NicoHistoryDBInit

/**
 * 端末内履歴データベース
 * 使う際は[NicoHistoryDBInit.getInstance]を経由してね（SQLite->Room移行時にバージョンを上げるコードが書いてある）
 * */
@Database(entities = [NicoHistoryDBEntity::class], version = 4, exportSchema = false)
abstract class NicoHistoryDB : RoomDatabase() {
    abstract fun nicoHistoryDBDAO(): NicoHistoryDBDAO
}