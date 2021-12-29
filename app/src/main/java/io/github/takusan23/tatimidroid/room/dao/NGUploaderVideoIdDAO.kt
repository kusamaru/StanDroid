package io.github.takusan23.tatimidroid.room.dao

import androidx.room.*
import io.github.takusan23.tatimidroid.room.entity.NGUploaderVideoIdEntity
import kotlinx.coroutines.flow.Flow

/**
 * NG投稿者が投稿した動画IDが入ったデータベースにアクセスするときに使う
 * */
@Dao
interface NGUploaderVideoIdDAO {
    /** 全データ取得 */
    @Query("SELECT * FROM ng_uploader_video_id")
    fun getAll(): List<NGUploaderVideoIdEntity>

    /** データベースの変更検知機能がついたやつ */
    @Query("SELECT * FROM ng_uploader_video_id")
    fun getAllRealTime(): Flow<List<NGUploaderVideoIdEntity>>

    /** 指定したユーザーの動画を返す */
    @Query("SELECT * FROM ng_uploader_video_id WHERE user_id = :userId")
    fun getAllFromUserId(userId: String): List<NGUploaderVideoIdEntity>

    /** データ更新 */
    @Update
    fun update(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データ追加 */
    @Insert
    fun insert(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データ削除 */
    @Delete
    fun delete(ngUploaderVideoIdEntity: NGUploaderVideoIdEntity)

    /** データベースを吹っ飛ばす。全削除 */
    @Query("DELETE FROM ng_uploader_video_id")
    fun deleteAll()

    /**
     * 引数に入れたユーザーIDが投稿した動画を削除する
     * @param userId ユーザーID
     * */
    @Query("DELETE FROM ng_uploader_video_id WHERE user_id = :userId")
    fun deleteFromUserId(userId: String)

    /**
     * 指定した動画IDがすでに登録されているかどうかを返す
     *
     * @param videoId 動画ID
     * @return 登録済みならtrue
     * */
    @Query("SELECT EXISTS(SELECT * FROM ng_uploader_video_id WHERE video_id = :videoId)")
    fun isAddedVideoFromId(videoId: String): Boolean

}