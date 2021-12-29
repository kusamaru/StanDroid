package io.github.takusan23.tatimidroid.room.init

import android.content.Context
import androidx.room.Room
import io.github.takusan23.tatimidroid.room.database.NGUploaderVideoIdDB

/**
 * NG投稿者が投稿した動画IDデータベースをインスタンス化する
 * */
object NGUploaderVideoIdDBInit {

    /** NG動画ID */
    private lateinit var ngUploaderVideoIdDB: NGUploaderVideoIdDB

    /** しんぐるとん */
    fun getInstance(context: Context): NGUploaderVideoIdDB {
        if (!::ngUploaderVideoIdDB.isInitialized) {
            ngUploaderVideoIdDB = Room.databaseBuilder(context, NGUploaderVideoIdDB::class.java, "NGUploaderVideoId.db").build()
        }
        return ngUploaderVideoIdDB
    }

}