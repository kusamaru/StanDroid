package io.github.takusan23.tatimidroid.room.init

import android.content.Context
import androidx.room.Room
import io.github.takusan23.tatimidroid.room.database.KotehanDB

/**
 * コテハンデータベースを生成する。シングルトン
 * */
object KotehanDBInit {

    private lateinit var kotehanDB: KotehanDB

    /**
     * データベースを返す。シングルトンだって
     * @param context コンテキスト
     * */
    fun getInstance(context: Context): KotehanDB {
        if (!::kotehanDB.isInitialized) {
            // 一度だけ生成
            kotehanDB = Room.databaseBuilder(context, KotehanDB::class.java, "KotehanDB").build()
        }
        return kotehanDB
    }

}