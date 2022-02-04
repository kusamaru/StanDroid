package com.kusamaru.standroid.nguploader.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nguploader.NGUploaderTool

/**
 * NG投稿者に追加したときに、その投稿者が投稿した動画を取得する。
 *
 * setInputData()でユーザーIDを入れてください
 *
 * user_id | String | NG投稿者のユーザーID
 *
 * ViewModelで取得してもいいんだけど、多分時間がかかるので
 *
 * （多分Flowでデータベースの変更は受け取ることができます）
 * */
class AddNGUploaderVideoListWork(private val context: Context, private val params: WorkerParameters) : CoroutineWorker(context, params) {

    /** 通知ID */
    private val NOTIFIACTION_ID = 334

    /**
     * NG投稿者の動画をすべて取得してデータベースに押し込む
     * */
    override suspend fun doWork(): Result {
        // ユーザーID
        val userId = params.inputData.getString("user_id") ?: return Result.failure() // この書き方好き
        // データベース関連クラス
        val ngUploaderTool = NGUploaderTool(context)
        val count = ngUploaderTool.addNGUploaderAllVideoList(userId)
        // 成功を返す
        showNotification(count)
        return Result.success()
    }

    /** 通知を送信する */
    private fun showNotification(count: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 通知ちゃんねるで分岐
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ng_uploader_notification"
            NotificationChannel(channelId, "NG投稿者/投稿動画データベース通知", NotificationManager.IMPORTANCE_LOW).let { channel ->
                // なければ追加
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    notificationManager.createNotificationChannel(channel)
                }
            }
            NotificationCompat.Builder(context, channelId)
        } else {
            NotificationCompat.Builder(context)
        }
        notification.apply {
            setContentTitle("NG投稿者追加通知")
            setContentText("動画をNGに追加しました。${count}件")
            setSmallIcon(R.drawable.ng_uploader_icon)
        }
        // 投稿
        notificationManager.notify(NOTIFIACTION_ID, notification.build())
    }

}