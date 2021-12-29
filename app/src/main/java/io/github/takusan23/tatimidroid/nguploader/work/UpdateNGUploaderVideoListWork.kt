package io.github.takusan23.tatimidroid.nguploader.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nguploader.NGUploaderTool

/**
 * NG投稿者の投稿した動画一覧を更新する
 *
 * WorkManagerを使って定期実行させる
 * */
class UpdateNGUploaderVideoListWork(private val context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    /** 通知ID */
    private val NOTIFIACTION_ID = 334

    /**
     * 仕事内容。コルーチン対応！
     * */
    override suspend fun doWork(): Result {
        // データベース関連クラス
        val ngUploaderTool = NGUploaderTool(context)
        // NG動画数を計算
        ngUploaderTool.getNGUploader().forEach { userIdData ->
            // 更新用関数を呼ぶ
            ngUploaderTool.updateNGUploaderVideoList(userIdData.userId)
        }
        // 成功を返す
        showNotification()
        return Result.success()
    }

    /** 通知を送信する */
    private fun showNotification() {
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
            setContentTitle("NG投稿者更新通知")
            setContentText("定期実行が完了しました。")
            setSmallIcon(R.drawable.ng_uploader_icon)
        }
        // 投稿
        notificationManager.notify(NOTIFIACTION_ID, notification.build())
    }
}