package io.github.takusan23.tatimidroid.room.dao

import androidx.room.*
import io.github.takusan23.tatimidroid.room.entity.NGUploaderUserIdEntity
import kotlinx.coroutines.flow.Flow

/**
 * NG投稿者のユーザーIDが格納されたデータベースへアクセスするときに使う
 * */
@Dao
interface NGUploaderUserIdDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_uploader_user_id")
    fun getAll(): List<NGUploaderUserIdEntity>

    /** データベースの変更検知機能がついたやつ */
    @Query("SELECT * FROM ng_uploader_user_id")
    fun getAllRealTime(): Flow<List<NGUploaderUserIdEntity>>

    /** データ更新 */
    @Update
    fun update(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データ追加 */
    @Insert
    fun insert(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データ削除 */
    @Delete
    fun delete(ngUploaderUserIdEntity: NGUploaderUserIdEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM ng_uploader_user_id")
    fun deleteAll()

    /**
     * 引数に入れたユーザーIDを削除する
     * @param userId ユーザーID
     * */
    @Query("DELETE FROM ng_uploader_user_id WHERE user_id = :userId")
    fun deleteFromUserId(userId: String)

    /**
     * 指定したユーザーIDのデータを返す
     * @param userId ユーザーID
     * @return 配列だけど最初の要素がそれ
     * */
    @Query("SELECT * FROM ng_uploader_user_id WHERE user_id = :userId")
    fun getItemFromUserId(userId: String): List<NGUploaderUserIdEntity>


}