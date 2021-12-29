package io.github.takusan23.tatimidroid.room.dao

import androidx.room.*
import io.github.takusan23.tatimidroid.room.entity.NGDBEntity
import kotlinx.coroutines.flow.Flow

/**
 * データベースへアクセスするときに使う関数を定義する
 * */
@Dao
interface NGDBDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_list")
    fun getAll(): List<NGDBEntity>

    /** データ更新 */
    @Update
    fun update(ngdbEntity: NGDBEntity)

    /** データ追加 */
    @Insert
    fun insert(ngdbEntity: NGDBEntity)

    /** データ削除 */
    @Delete
    fun delete(ngdbEntity: NGDBEntity)

    /**  指定したコメント/ユーザーIDを消す。コメント/ユーザー共通でこの関数を通じて削除できます。 */
    @Query("DELETE FROM ng_list WHERE value = :value")
    fun deleteByValue(value: String)

    /** NGコメント一覧を取得する */
    @Query("SELECT * FROM ng_list WHERE type = 'comment'")
    fun getNGCommentList(): List<NGDBEntity>

    /** NGユーザー一覧を取得する */
    @Query("SELECT * FROM ng_list WHERE type = 'user'")
    fun getNGUserList(): List<NGDBEntity>

    /** データベースに追加があった時に変更を検知できる。コルーチンで使ってね。Flowはデータベースをシングルトンにしないと動かない */
    @Query("SELECT * FROM ng_list")
    fun flowGetNGAll(): Flow<List<NGDBEntity>>

}