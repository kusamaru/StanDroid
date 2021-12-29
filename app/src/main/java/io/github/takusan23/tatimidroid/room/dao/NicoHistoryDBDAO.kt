package io.github.takusan23.tatimidroid.room.dao

import androidx.room.*
import io.github.takusan23.tatimidroid.room.entity.NicoHistoryDBEntity

/**
 * 端末内履歴データベースにアクセスするための関数。
 * */
@Dao
interface NicoHistoryDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM history")
    fun getAll(): List<NicoHistoryDBEntity>

    /** データ更新 */
    @Update
    fun update(nicoHistoryDBEntity: NicoHistoryDBEntity)

    /** データ追加 */
    @Insert
    fun insert(nicoHistoryDBEntity: NicoHistoryDBEntity)

    /** データ削除 */
    @Delete
    fun delete(nicoHistoryDBEntity: NicoHistoryDBEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM history")
    fun deleteAll()

}