package com.kusamaru.standroid.service

import android.app.*
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.NicoVideoCache
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.tool.LanguageTool
import com.kusamaru.standroid.tool.isLoginMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.File

/**
 * キャッシュ取得サービス。Serviceに移管した。
 * 必要なもの↓
 *       id|   String|     動画ID
 *   is_eco|  Boolean|     エコノミーならtrue
 * */
class GetCacheService : Service() {
    /** フォアグラウンドサービス通知ID */
    private val FOREGROUND_NOTIFICATION_ID = 816

    /** ダウンロード進捗通知ID */
    private val DOWNLOAD_PROGRESS_NOTIFICATION_ID = 1919

    /** 通知出すやつ */
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    /** 設定 */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    /** ユーザーセッション */
    private val userSession by lazy {
        if (isLoginMode(this)) {
            prefSetting.getString("user_session", "") ?: "" // ログインする
        } else {
            ""  // ログインしない
        }
    }

    /** 並列数、分割数 */
    private val splitCount by lazy { prefSetting.getInt("setting_cache_split_count", 1) }

    /** 動画キャッシュ予約リスト。キャッシュ取得成功すればここの配列の中身が使われていく */
    private val cacheList = arrayListOf<Pair<String, Boolean>>()

    /** 終了済みリスト */
    private val cacheDLFinishedList = arrayListOf<String>()

    /** コルーチンキャンセル用。launch{ }の戻り値 */
    private var cacheCoroutineJob: Job? = null

    /** 現在取得している動画ID */
    private var currentCacheVideoId = ""

    /** キャンセル用Broadcast */
    private lateinit var broadcastReceiver: BroadcastReceiver

    /** APIまとめ */
    private val nicoVideoHTML = NicoVideoHTML()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        // 通知出す
        showNotification()
        // ブロードキャスト初期化
        initBroadcastReceiver()
    }

    /** 通知ボタンのBroadCastReceiver */
    private fun initBroadcastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("cache_service_stop")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "cache_service_stop" -> {
                        // DL中に中断したらファイルを消すように
                        NicoVideoCache(this@GetCacheService).deleteCache(currentCacheVideoId)
                        // Service強制終了
                        stopSelf()
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 動画ID
        val id = intent?.getStringExtra("id")
        // エコノミーなら
        val isEco1 = intent?.getBooleanExtra("is_eco", false) ?: false
        if (id != null) {
            // すでにあるか
            if (!cacheList.contains(Pair(id, isEco1))) {
                // 予約に追加
                cacheList.add(Pair(id, isEco1))
                // 一度だけ実行
                // Serviceは多重起動できないけど起動中ならonStartCommandが呼ばれる
                if (cacheCoroutineJob == null) {
                    // 取得
                    coroutine()
                } else {
                    // 予定に追加したよ！
                    showToast("${getString(R.string.cache_get_list_add)}。$id\n${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheDLFinishedList.size}")
                }
                showNotification("${getString(R.string.loading)}：$currentCacheVideoId / ${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheDLFinishedList.size}")
            } else {
                showToast(getString(R.string.cache_get_list_contains))
            }
        }
        return START_NOT_STICKY
    }

    // コルーチン
    private fun coroutine(position: Int = 0) {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        cacheCoroutineJob = GlobalScope.launch(errorHandler) {
            // キャッシュ取得クラス
            val nicoVideoCache = NicoVideoCache(this@GetCacheService)
            // ID
            val videoId = cacheList[position].first
            currentCacheVideoId = videoId
            // エコノミーか
            val isEco1 = cacheList[position].second
            val eco = if (isEco1) "1" else "0"
            // 進捗通知
            showNotification("${getString(R.string.loading)}：$currentCacheVideoId / ${getString(R.string.cache_get_list_size)}：${cacheList.size - cacheDLFinishedList.size}")
            // リクエスト
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)} : $videoId\n${response.code}")
                return@launch
            }
            val nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
            if (!nicoVideoHTML.isEncryption(jsonObject.toString())) {
                // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
                var contentUrl = ""
                // https://api.dmc.nico/api/sessions のレスポンス
                val sessionAPIJSONObject = nicoVideoHTML.getSessionAPI(jsonObject)
                if (sessionAPIJSONObject != null) {
                    // 動画URL
                    contentUrl = nicoVideoHTML.parseContentURI(sessionAPIJSONObject)
                    // ハートビート処理
                    nicoVideoHTML.startHeartBeat(sessionAPIJSONObject)
                }
                /**
                 * キャッシュ取得
                 *
                 * コルーチンのasyncで並列にリクエストを飛ばす
                 * */
                val json = jsonObject.toString()
                // 動画IDフォルダー作成
                val videoIdFolder = File(nicoVideoCache.getCacheFolderPath(), videoId)
                videoIdFolder.mkdir()
                // コメント取得
                val asyncComment = async { nicoVideoCache.getCacheComment(videoIdFolder, videoId, json, userSession) }
                // 動画情報取得
                val asyncVideoInfo = async { nicoVideoCache.saveVideoInfo(videoIdFolder, videoId, json) }
                // 動画のサ胸取得
                val asyncThumb = async { nicoVideoCache.getThumbnail(videoIdFolder, videoId, json, userSession) }
                // 動画取得で使う一時的に持っておくフォルダ
                val tmpVideoFileFolder = File(nicoVideoCache.getCacheTempFolderPath(), "${videoId}_pocket").apply { mkdir() }
                // 動画取得用クラス
                val pocket = nicoVideoCache.getVideoDownloader(tmpVideoFileFolder, videoIdFolder, videoId, contentUrl, userSession, nicoHistory, splitCount)
                val asyncVideo = async { pocket.start() }
                // 通知更新
                launch {
                    pocket.progressFlow.collect { progress -> showDownloadProgressNotification(videoId, progress) }
                }
                // 終了を待つ
                asyncComment.await()
                asyncVideoInfo.await()
                asyncThumb.await()
                asyncVideo.await()
            } else {
                showToast(getString(R.string.encryption_not_download))
            }
            // 取得完了したら呼ばれる。
            nicoVideoHTML.destroy()
            // 次の要素へ
            if (position + 1 < cacheList.size) {
                coroutine(position + 1)
            } else {
                // 終了
                showToast(getString(R.string.cache_get_list_all_complete))
                stopSelf()
            }
        }
    }

    // サービス実行中通知出す
    private fun showNotification(contentText: String = getString(R.string.loading)) {
        val foregroundNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val notificationChannelId = "cache_service"
            val notificationChannel = NotificationChannel(notificationChannelId, getString(R.string.cache_service_title), NotificationManager.IMPORTANCE_HIGH)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, notificationChannelId)
        } else NotificationCompat.Builder(this)
        // 通知の中身
        foregroundNotification.apply {
            setContentTitle(getString(R.string.cache_get_notification_title))
            setContentText(contentText)
            setSmallIcon(R.drawable.ic_cache_progress_icon)
            // 強制終了ボタン置いておく
            addAction(R.drawable.ic_outline_delete_24px, getString(R.string.cache_get_service_stop), PendingIntent.getBroadcast(this@GetCacheService, 811, Intent("cache_service_stop"), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT))
            setStyle(NotificationCompat.InboxStyle().also { inboxStyle ->
                // キャッシュ予約リストを表示させるなど、キャッシュ取得済みは表示させない
                cacheList.filter { pair: Pair<String, Boolean> -> !cacheDLFinishedList.contains(pair.first) }.forEach { inboxStyle.addLine(it.first) }
            })
        }
        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification.build())
    }

    /**
     * 動画ダウンロード進捗通知を飛ばす
     * @param videoId 動画ID
     * @param progress 0から100
     * */
    private fun showDownloadProgressNotification(videoId: String, progress: Int) {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val notificationChannelId = "cache_download_progress"
            val notificationChannel = NotificationChannel(notificationChannelId, getString(R.string.cache_progress_notification), NotificationManager.IMPORTANCE_LOW)
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            Notification.Builder(this, notificationChannelId)
        } else {
            Notification.Builder(this)
        }
        notification.apply {
            setContentTitle("${getString(R.string.cache_progress_now)} : $progress %")
            setContentText(videoId)
            setSmallIcon(R.drawable.ic_cache_progress_icon)
            setProgress(100, progress, false) // プログレスバー
        }
        if (progress == 100) {
            // 消す
            notificationManager.cancel(DOWNLOAD_PROGRESS_NOTIFICATION_ID)
        } else {
            // 表示
            notificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION_ID, notification.build().apply { flags = Notification.FLAG_NO_CLEAR })
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** お片付け */
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        // コルーチン終了
        cacheCoroutineJob?.cancel()
        // 通知さんも退場
        notificationManager.cancel(FOREGROUND_NOTIFICATION_ID)
        notificationManager.cancel(DOWNLOAD_PROGRESS_NOTIFICATION_ID)
        // 一時保管フォルダも一応消す
        NicoVideoCache(this).getCacheTempFolderPath()?.let { path -> File(path).deleteRecursively() }
        // ハートビート切断
        nicoVideoHTML.destroy()
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}

/**
 * キャッシュ取得サービス起動関数。
 * 注意：すでに起動している場合は今のキャッシュ取得が終わり次第取得します。
 * @param context Context
 * @param videoId 動画ID
 * @param isEco エコノミーで取得する場合はtrue。省略時はfalse（通常の画質）
 * @param overwrite すでに取得済みでも取得する場合はtrue。強制取得的な。デフォtrue
 * @return キャッシュ取得するならtrue。そうじゃなければfalse
 * */
fun startCacheService(context: Context?, videoId: String, isEco: Boolean = false, overwrite: Boolean = true): Boolean {
    val nicoVideoCache = NicoVideoCache(context)
    // 強制取得 か まだキャッシュ未取得時 は取得する。
    if (overwrite || !nicoVideoCache.hasCacheVideoFile(videoId)) {
        val intent = Intent(context, GetCacheService::class.java).apply {
            putExtra("id", videoId)
            putExtra("is_eco", isEco)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(intent)
        } else {
            context?.startService(intent)
        }
        return true
    } else {
        return false
    }
}
