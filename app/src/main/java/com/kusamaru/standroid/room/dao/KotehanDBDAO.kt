package com.kusamaru.standroid.room.dao

import androidx.room.*
import com.kusamaru.standroid.room.entity.KotehanDBEntity
import kotlinx.coroutines.flow.Flow

/**
 * コテハンDBを操作する関数。
 * */
@Dao
interface KotehanDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM kotehan")
    fun getAll(): List<KotehanDBEntity>

    /** データ更新 */
    @Update
    fun update(kotehanDBEntity: KotehanDBEntity)

    /** データ追加 */
    @Insert
    fun insert(kotehanDBEntity: KotehanDBEntity)

    /** データ削除 */
    @Delete
    fun delete(kotehanDBEntity: KotehanDBEntity)

    /** ユーザーIDからコテハンを取り出す */
    @Query("SELECT * FROM kotehan WHERE user_id = :userId")
    fun findKotehanByUserId(userId: String): KotehanDBEntity?

    /** データベースをリアルタイムで監視するとき使う。これでDBへ追加/削除等の変更を検知できる。コルーチンで使ってね */
    @Query("SELECT * FROM kotehan")
    fun flowGetKotehanAll(): Flow<List<KotehanDBEntity>>

}
