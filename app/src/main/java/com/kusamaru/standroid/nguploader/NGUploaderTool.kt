package com.kusamaru.standroid.nguploader

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kusamaru.standroid.nguploader.work.AddNGUploaderVideoListWork
import com.kusamaru.standroid.nguploader.work.UpdateNGUploaderVideoListWork
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoUpload
import com.kusamaru.standroid.room.entity.NGUploaderUserIdEntity
import com.kusamaru.standroid.room.entity.NGUploaderVideoIdEntity
import com.kusamaru.standroid.room.init.NGUploaderUserIdDBInit
import com.kusamaru.standroid.room.init.NGUploaderVideoIdDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * NG投稿者、NG投稿者が投稿した動画のデータベース関係のクラス
 *
 * NG機能概要 ---
 *
 * 検索結果スクレイピングでは、投稿者IDまでは取れないので、
 * 代わりに予めNG投稿者の動画IDをデータベース（NG投稿者が投稿した動画データベース）に保存じます
 *
 * NG投稿者が投稿した動画データベースに一致した動画IDがあった場合はその動画を除外します。[NGUploaderTool.filterNGUploaderVideoId]参照
 *
 * */
class NGUploaderTool(val context: Context) {

    companion object {

        /**
         * 引数の動画配列の中からNG投稿者が投稿した動画IDがあればそれを取り除いて返す
         *
         * @param videoList 動画配列
         * @return NGを適用した動画配列
         * */
        suspend fun filterNGUploaderVideoId(context: Context, videoList: List<NicoVideoData>) = withContext(Dispatchers.Default) {
            // 有効かどうか
            val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefSetting.getBoolean("nicovideo_ng_uploader_enable", false)) {
                // 動画DBを取り出す
                val ngVideoId = NGUploaderVideoIdDBInit
                    .getInstance(context)
                    .ngUploaderVideoIdDAO()
                    .getAll()
                    .map { ngUploaderVideoIdEntity -> ngUploaderVideoIdEntity.videoId }
                // NG投稿者の投稿した動画を取り除いて返す
                videoList.filter { nicoVideoData -> !ngVideoId.contains(nicoVideoData.videoId) }
            } else {
                // そのまま
                videoList
            }
        }
    }

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", null) ?: ""

    /** 投稿動画API */
    private val nicoVideoUpload = NicoVideoUpload()

    /** NG投稿者IDデータベース */
    private val ngUserIdDB = NGUploaderUserIdDBInit.getInstance(context)

    /** NG投稿者が投稿した動画IDデータベース */
    private val ngVideoIdDB = NGUploaderVideoIdDBInit.getInstance(context)

    /** NG投稿者一覧をFlowで返す */
    fun getNGUploaderRealTime() = ngUserIdDB.ngUploaderUserIdDAO().getAllRealTime()

    /** NG投稿者一覧を返す */
    fun getNGUploader() = ngUserIdDB.ngUploaderUserIdDAO().getAll()

    /**
     * NG投稿者機能を有効、無効にする
     *
     * WorkManagerの定期実行をセットするのに使います。
     *
     * @param isEnable 有効にするならtrue。そうじゃないならfalse
     * */
    fun setNGUploaderEnable(isEnable: Boolean) {
        // WorkManagerに渡す仕事につけるTag
        val workTag = "nicovideo_ng_uploader_update"
        if (isEnable) {
            // 定期実行。一日ごと
            val updateNGUploaderVideoListWork = PeriodicWorkRequestBuilder<UpdateNGUploaderVideoListWork>(
                1, TimeUnit.DAYS
            ).addTag(workTag).build()
            WorkManager.getInstance(context).enqueue(updateNGUploaderVideoListWork)
        } else {
            // 取りやめ
            WorkManager.getInstance(context).cancelAllWorkByTag(workTag)
        }
        prefSetting.edit { putBoolean("nicovideo_ng_uploader_enable", isEnable) }
    }

    /** NG投稿者機能が有効かどうかを返す */
    fun getEnableNGUploader(): Boolean {
        return prefSetting.getBoolean("nicovideo_ng_uploader_enable", false)
    }

    /**
     * NG投稿者として登録する
     *
     * @param userId ユーザーID
     * */
    suspend fun addNGUploaderId(userId: String) = withContext(Dispatchers.Default) {
        val ngUserData = NGUploaderUserIdEntity(
            userId = userId,
            latestUpdateTime = System.currentTimeMillis(),
            addDateTime = System.currentTimeMillis(),
            videoCount = 0,
            description = ""
        )
        ngUserIdDB.ngUploaderUserIdDAO().insert(ngUserData)
        // NGユーザーの投稿動画を取得してデータベースへ。一度だけ実行
        val addNGUploaderVideoListWork = OneTimeWorkRequestBuilder<AddNGUploaderVideoListWork>()
            .setInputData(
                workDataOf(
                    "user_id" to userId
                )
            )
            .build()
        WorkManager.getInstance(context).enqueue(addNGUploaderVideoListWork)
    }

    /**
     * 指定した動画IDを使って、NG投稿者を追加する
     *
     * @param videoId 動画ID
     * */
    suspend fun addNGUploaderIdFromVideoId(videoId: String) = withContext(Dispatchers.Default) {
        // 動画情報取得
        val nicoVideoHTML = NicoVideoHTML()
        nicoVideoHTML.getJSON(videoId, userSession).let { response ->
            if (response.isSuccessful) {
                // JSONパース
                val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())!!
                // ユーザーID取る
                val userData = nicoVideoHTML.parseUserData(jsonObject)
                if (userData != null) {
                    addNGUploaderId(userData.userId)
                }
            }
        }
    }

    /**
     * NG投稿者を削除する
     *
     * @param userId ユーザーID
     * */
    suspend fun deleteNGUploaderId(userId: String) = withContext(Dispatchers.Default) {
        ngUserIdDB.ngUploaderUserIdDAO().deleteFromUserId(userId)
        // 投稿動画の方も消す
        ngVideoIdDB.ngUploaderVideoIdDAO().deleteFromUserId(userId)
    }

    /**
     * 指定したユーザーの投稿動画をすべて取得してデータベースに格納する
     *
     * わざと遅延させて取得させているので時間がかかる
     *
     * @param userId ユーザーID
     * @return NG動画件数
     * */
    suspend fun addNGUploaderAllVideoList(userId: String) = withContext(Dispatchers.Default) {
        // 投稿動画をすべて取得する。ちょっと時間がかかる
        val allVideoList = nicoVideoUpload
            .getAllUploadVideo(userId = userId, userSession = userSession)
            .map { nicoVideoData ->
                NGUploaderVideoIdEntity(
                    videoId = nicoVideoData.videoId,
                    userId = userId,
                    addDateTime = System.currentTimeMillis(),
                    description = ""
                )
            }
        // 保存
        allVideoList.forEach { ngUploaderVideoIdEntity ->
            ngVideoIdDB.ngUploaderVideoIdDAO().insert(ngUploaderVideoIdEntity)
        }
        // 最終更新日時を更新
        updateUserIdDBLatestUpdate(userId)
        // 動画件数を更新
        updateUserIdDBVideoCountUpdate(userId)
        // 件数を返す
        allVideoList.size
    }

    /**
     * 定期実行用。引数に入れたユーザーIDの動画を取得してNG投稿者が投稿した動画IDデータベースを更新する
     *
     * @param userId 投稿者のユーザーID
     * @param maxCount 更新数
     * */
    suspend fun updateNGUploaderVideoList(userId: String, maxCount: Int = 5) = withContext(Dispatchers.Default) {
        val response = nicoVideoUpload.getUploadVideo(
            userId = userId,
            page = 1,
            size = maxCount,
            userSession = userSession
        )
        if (!response.isSuccessful) return@withContext
        // パース
        val videoList = nicoVideoUpload.parseUploadVideo(response.body?.string())
        videoList.forEach { nicoVideoData ->
            // すでに追加済みなら何もしない
            val isAddedVideoId = ngVideoIdDB.ngUploaderVideoIdDAO().isAddedVideoFromId(nicoVideoData.videoId)
            if (!isAddedVideoId) {
                // 登録
                val ngUploaderVideoIdEntity = NGUploaderVideoIdEntity(
                    userId = userId,
                    videoId = nicoVideoData.videoId,
                    addDateTime = System.currentTimeMillis(),
                    description = ""
                )
                ngVideoIdDB.ngUploaderVideoIdDAO().insert(ngUploaderVideoIdEntity)
            }
        }
        // 最終更新日時を更新
        updateUserIdDBLatestUpdate(userId)
        // 動画件数を更新
        updateUserIdDBVideoCountUpdate(userId)
    }


    /**
     * NG投稿者DBの最終更新日時（[NGUploaderUserIdEntity.latestUpdateTime]）を更新する
     *
     * @param userId ユーザーID
     * */
    private suspend fun updateUserIdDBLatestUpdate(userId: String) = withContext(Dispatchers.Default) {
        // データ取得
        val ngUserIdItem = ngUserIdDB.ngUploaderUserIdDAO().getItemFromUserId(userId)
        // コピーして書き換え
        val updateNGUploaderUserIdEntity = ngUserIdItem.first().copy(latestUpdateTime = System.currentTimeMillis())
        // 更新
        ngUserIdDB.ngUploaderUserIdDAO().update(updateNGUploaderUserIdEntity)
    }

    /**
     * NG投稿者DBのNG動画数（[NGUploaderUserIdEntity.videoCount]）を更新する
     *
     * @param userId ユーザーID
     * */
    private suspend fun updateUserIdDBVideoCountUpdate(userId: String) = withContext(Dispatchers.Default) {
        // 件数取得
        val count = ngVideoIdDB.ngUploaderVideoIdDAO().getAllFromUserId(userId).size
        // データ取得
        val ngUserIdItem = ngUserIdDB.ngUploaderUserIdDAO().getItemFromUserId(userId)
        // コピーして書き換え
        val updateNGUploaderUserIdEntity = ngUserIdItem.first().copy(videoCount = count)
        // 更新
        ngUserIdDB.ngUploaderUserIdDAO().update(updateNGUploaderUserIdEntity)
    }
}