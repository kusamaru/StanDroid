package io.github.takusan23.tatimidroid.room.init

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.takusan23.tatimidroid.room.database.NicoHistoryDB

/**
 * 端末内履歴データベース初期化（いやデータベース吹っ飛ばすって意味じゃなくて初期設定的な）
 * */
object NicoHistoryDBInit {
    private lateinit var nicoHistoryDB: NicoHistoryDB

    /**
     * しんぐるとん
     * */
    fun getInstance(context: Context): NicoHistoryDB {
        if (!::nicoHistoryDB.isInitialized) {
            nicoHistoryDB = Room.databaseBuilder(context, NicoHistoryDB::class.java, "NicoHistory.db")
                //.setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // SQLite->Room移行。移行後のデータベースを作成する。カラムは移行前と同じ
                        database.execSQL(
                            """
                                CREATE TABLE history_tmp (
                                _id INTEGER NOT NULL PRIMARY KEY, 
                                type TEXT NOT NULL,
                                service_id TEXT NOT NULL,
                                user_id TEXT NOT NULL,
                                title TEXT NOT NULL,
                                date INTEGER NOT NULL,
                                description TEXT NOT NULL
                                )
                                """
                        )
                        // 移行後のデータベースへデータを移す
                        database.execSQL(
                            """
                                INSERT INTO history_tmp (_id, type, service_id, user_id, title, date, description)
                                SELECT _id, type, service_id, user_id, title, date, description FROM history
                                """
                        )
                        // 前あったデータベースを消す
                        database.execSQL("DROP TABLE history")
                        // 移行後のデータベースの名前を移行前と同じにして移行完了
                        database.execSQL("ALTER TABLE history_tmp RENAME TO history")
                    }
                })
                .addMigrations(object : Migration(2, 3) {
                    // なんかデータベースに入れる中身間違えた（生放送なのにtypeが動画になっちゃった）
                    // のでバージョンを3へ上げるマイグレーション。
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // service_id に lv が含まれている場合は データの type の部分を live に変更する
                        database.execSQL("UPDATE history SET type = 'live' WHERE service_id LIKE '%lv%'")
                    }
                })
                .addMigrations(object : Migration(3, 4) {
                    // また同じ間違いをしたのでまーた修正
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // service_id に lv が含まれている場合は データの type の部分を live に変更する
                        database.execSQL("UPDATE history SET type = 'live' WHERE service_id LIKE '%lv%'")
                    }
                })
                .build()
        }
        return nicoHistoryDB
    }

}