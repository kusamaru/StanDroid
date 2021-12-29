package io.github.takusan23.tatimidroid.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.OverlayVideoPlayerLayoutBinding
import io.github.takusan23.tatimidroid.nicoapi.NicoVideoCache
import io.github.takusan23.tatimidroid.nicoapi.XMLCommentJSON
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.tool.DisplaySizeTool
import io.github.takusan23.tatimidroid.tool.InternetConnectionCheck
import io.github.takusan23.tatimidroid.tool.LanguageTool
import io.github.takusan23.tatimidroid.tool.isLoginMode
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * ニコ生をポップアップ、バックグラウンドで再生するやつ。
 * FragmentからServiceに移動させる
 *
 * Intentに詰めてほしいもの↓
 * mode           |String |"popup"（ポップアップ再生）か"background"(バッググラウンド再生)のどっちか。
 * video_id       |String |動画ID
 * is_cache       |Boolean|キャッシュ再生ならtrue
 * オプション
 * seek           |Long   |シークする場合は値（ms）を入れてね。
 * video_quality  |String |画質指定したい場合は入れてね。
 * audio_quality  |String |音質指定したい場合は入れてね。
 * playlist       |Serialize([NicoVideoData])の配列| 連続再生する場合。連続再生の場合、video_idの動画から再生を始めます
 * */
class NicoVideoPlayService : Service() {

    // 通知ID
    private val NOTIFICAION_ID = 865

    private lateinit var prefSetting: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private lateinit var windowManager: WindowManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var mediaSessionCompat: MediaSessionCompat

    // 再生するやつ
    private lateinit var exoPlayer: SimpleExoPlayer

    /** これらはポップアップ再生時のみ初期化されます。ので lateinit ではなくnull許容 */
    private var viewBinding: OverlayVideoPlayerLayoutBinding? = null
    private var commentCanvas: CommentCanvas? = null

    /** ニコ動のデータ取得からハートビートまで */
    private val nicoVideoHTML = NicoVideoHTML()

    /** 視聴モード */
    var playMode = "popup"

    /** 連続再生リスト */
    private val playlist = arrayListOf<NicoVideoData>()

    /** 連続再生かどうか */
    private val isPlayList: Boolean
        get() {
            return playlist.size > 1
        }

    /** 連続再生で現在の位置 */
    private var currentPlaylistPos = 0

    /** 現在の動画ID */
    private var currentVideoId = ""

    /** 現在の動画タイトル */
    private var currentVideoTitle = ""

    /** 現在キャッシュで再生しているかどうか */
    private var isCurrentVideoCache = false

    /** コメント配列 */
    private var currentVideoCommentList = arrayListOf<CommentJSONParse>()

    /** シークする場合 */
    private var seekMs = 0L

    /** シークバーを動かすタイマー */
    private var seekTimer = Timer()

    /** シークバー操作中ならtrue */
    private var isTouchingSeekBar = false

    // アスペクト比（横 / 縦）。なんか21:9並のほっそ長い動画があるっぽい？
    // 4:3 = 1.3 / 16:9 = 1.7
    private var aspect = 1.7
    private lateinit var popupLayoutParams: WindowManager.LayoutParams

    // コメント描画改善。drawComment()関数でのみ使う（0秒に投稿されたコメントが重複して表示される対策）
    private var drewedList = arrayListOf<String>() // 描画したコメントのNoが入る配列。一秒ごとにクリアされる
    private var tmpPosition = 0L // いま再生している位置から一秒引いた値が入ってる。

    // 視聴開始画質。こいつらnullの可能性あるからな
    var startVideoQuality: String? = ""
    var startAudioQuality: String? = ""

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        showNotification(getString(R.string.loading))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 受け取り
        playMode = intent?.getStringExtra("mode") ?: "popup"
        val videoId = intent?.getStringExtra("video_id") ?: ""
        val isCache = intent?.getBooleanExtra("is_cache", false) ?: false
        seekMs = intent?.getLongExtra("seek", 0L) ?: 0L
        // 画質が指定されている場合
        startVideoQuality = intent?.getStringExtra("video_quality")
        startAudioQuality = intent?.getStringExtra("audio_quality")

        // 連続再生？
        if (intent?.getSerializableExtra("playlist") != null) {
            playlist.addAll(intent.getSerializableExtra("playlist") as ArrayList<NicoVideoData>)
        }

        // ポップアップ再生ならプレイヤーを用意しておく
        if (isPopupPlay()) {
            initPlayerUI()
        }

        // ExoPlayer用意
        initExoPlayer()

        if (playlist.isNotEmpty()) {
            // 連続再生。まず位置特定
            currentPlaylistPos = playlist.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == videoId }
            val playlistStartId = playlist[currentPlaylistPos].videoId
            val playlistStartCanUseCache = playlist[currentPlaylistPos].isCache // キャッシュ再生が使えるか
            if (playlistStartCanUseCache) {
                // キャッシュ再生
                cachePlay(playlistStartId)
            } else {
                // データ取得など
                coroutine(playlistStartId)
            }
        } else {
            // 通常
            if (isCache) {
                // キャッシュ再生
                cachePlay(videoId)
            } else {
                // データ取得など
                coroutine(videoId)
            }
        }

        initBroadcast()
        return START_NOT_STICKY
    }

    /** ExoPlayerを用意する */
    private fun initExoPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // ポップアップのみ
                viewBinding?.apply {
                    // シークの最大値設定。
                    val videoLengthFormattedTime = DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
                    overlayVideoControlInclude.playerControlDuration.text = videoLengthFormattedTime
                    overlayVideoControlInclude.playerControlSeek.max = (exoPlayer.duration / 1000L).toInt()
                    // 動画のシーク
                    overlayVideoControlInclude.playerControlSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            isTouchingSeekBar = true
                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            // 動画シークする
                            exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
                            isTouchingSeekBar = false
                        }
                    })
                    // ローディング
                    overlayVideoLoadingProgressBar.isVisible = !(state == Player.STATE_READY)
                }
                // 再生終了。次の動画
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    // 動画おわった。連続再生時なら次の曲へ
                    nextPlaylistVideo()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                // 動画のアイコン入れ替え
                viewBinding?.apply {
                    val drawable = if (exoPlayer.playWhenReady) {
                        getDrawable(R.drawable.ic_pause_black_24dp)
                    } else {
                        getDrawable(R.drawable.ic_play_arrow_24px)
                    }
                    overlayVideoControlInclude.playerControlPause.setImageDrawable(drawable)
                }
            }

        })
        // アスペクトひ
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                viewBinding?.apply {
                    // アスペクト比が4:3か16:9か
                    // 4:3 = 1.333... 16:9 = 1.777..
                    val calc = width.toFloat() / height.toFloat()
                    // 小数点第二位を捨てる
                    aspect = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                    popupLayoutParams = getParams(prefSetting.getInt("nicovideo_popup_width", if (aspect == 1.3) 480 else 640)) // なければ 640 (4:3動画なら480)
                    windowManager.updateViewLayout(root, popupLayoutParams)
                    applyCommentCanvas()
                    // 位置が保存されていれば適用
                    if (prefSetting.getInt("nicovideo_popup_x_pos", 0) != 0) {
                        popupLayoutParams.x = prefSetting.getInt("nicovideo_popup_x_pos", 0)
                        popupLayoutParams.y = prefSetting.getInt("nicovideo_popup_y_pos", 0)
                        windowManager.updateViewLayout(root, popupLayoutParams)
                    }
                }
            }
        })
    }

    /**
     * 次の動画へ
     * 連続再生([playlist]に値が入ってる)じゃないと動きません
     * */
    private fun nextPlaylistVideo() {
        if (playlist.isEmpty()) return
        // いったんハートビート切る
        nicoVideoHTML.destroy()
        // あとしまつ
        if (::exoPlayer.isInitialized) {
            // exoPlayer.release()
        }
        // 連続再生時のみ利用可能
        val nextVideoPos = if (currentPlaylistPos + 1 < playlist.size) {
            // 次の動画がある
            currentPlaylistPos + 1
        } else {
            // 最初の動画にする
            0
        }
        val videoData = playlist[nextVideoPos]
        // 次の動画に合わせる
        currentPlaylistPos = nextVideoPos
        if (videoData.isCache) {
            // キャッシュ再生
            cachePlay(videoData.videoId)
        } else {
            // データ取得など
            coroutine(videoData.videoId)
        }
    }

    /**
     * 前の動画へ
     * */
    private fun prevPlaylistVideo() {
        if (playlist.isEmpty()) return
        // いったんハートビート切る
        nicoVideoHTML.destroy()
        // あとしまつ
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        // 連続再生時のみ利用可能
        val prevVideoPos = if (currentPlaylistPos - 1 >= 0) {
            // 次の動画がある
            currentPlaylistPos - 1
        } else {
            // 最初の動画にする
            playlist.size - 1
        }
        val videoData = playlist[prevVideoPos]
        // 次の動画に合わせる
        currentPlaylistPos = prevVideoPos
        if (videoData.isCache) {
            // キャッシュ再生
            cachePlay(videoData.videoId)
        } else {
            // データ取得など
            coroutine(videoData.videoId)
        }
    }


    /**
     * インターネットから動画を取得して再生する
     * @param videoId 動画ID
     * */
    private fun coroutine(videoId: String) {
        isCurrentVideoCache = false
        currentVideoId = videoId

        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        GlobalScope.launch(errorHandler) {
            // 動画URL
            var contentUrl = ""

            // ログインしないならそもそもuserSessionの値を空にすれば！？
            val userSession = if (isLoginMode(this@NicoVideoPlayService)) {
                prefSetting.getString("user_session", "")!!
            } else {
                ""
            }
            val response = nicoVideoHTML.getHTML(videoId, userSession, "")
            val nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val responseString = response.body?.string()
            val jsonObject = nicoVideoHTML.parseJSON(responseString)

            // 公式アニメは暗号化されてて見れないので落とす
            if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                showToast(getString(R.string.encryption_video_not_play))
                Handler(Looper.getMainLooper()).post {
                    this@NicoVideoPlayService.stopSelf()
                }
                return@launch
            }
            // https://api.dmc.nico/api/sessions のレスポンス
            val sessionAPIResponse = if (startVideoQuality != null && startAudioQuality != null) {
                // 画質指定あり
                nicoVideoHTML.getSessionAPI(jsonObject, startVideoQuality!!, startAudioQuality!!)
            } else {
                // なし。おまかせ？
                nicoVideoHTML.getSessionAPI(jsonObject)
            }
            if (sessionAPIResponse != null) {
                // 動画URL
                contentUrl = nicoVideoHTML.parseContentURI(sessionAPIResponse)
                // ハートビート処理。これしないと切られる。
                nicoVideoHTML.startHeartBeat(sessionAPIResponse)
            }

            // コメント取得
            val commentJSON = nicoVideoHTML.getComment(userSession, jsonObject)
            if (commentJSON != null) {
                currentVideoCommentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!, videoId))
            }
            // タイトル
            currentVideoTitle = jsonObject.getJSONObject("video").getString("title")
            withContext(Dispatchers.Main) {
                // ExoPlayer
                playExoPlayer(false, contentUrl, nicoHistory)
                if (isPopupPlay()) {
                    // プレイヤーにセット
                    setPlayerUI(currentVideoId, currentVideoTitle)
                    // インターネットなので
                    showNetworkType(false)
                }
            }
        }
    }

    /**
     * キャッシュを利用して動画を再生
     * @param videoId 動画ID
     * */
    private fun cachePlay(videoId: String) {
        val nicoVideoCache = NicoVideoCache(this)
        isCurrentVideoCache = true
        currentVideoId = videoId

        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(this)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(this, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        } else {
            // タイトル
            currentVideoTitle = if (nicoVideoCache.hasCacheNewVideoInfoJSON(videoId)) {
                JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)).getJSONObject("video").getString("title")
            } else {
                // 動画ファイルの名前
                nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: videoId
            }
            GlobalScope.launch {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    val contentUrl = "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                    // コメント取得
                    val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                    currentVideoCommentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON, videoId))
                    withContext(Dispatchers.Main) {
                        // ExoPlayer
                        playExoPlayer(true, contentUrl, "")
                        if (isPopupPlay()) {
                            // プレイヤーにセット
                            setPlayerUI(currentVideoId, currentVideoTitle)
                            // キャッシュなので
                            showNetworkType(true)
                        }
                    }
                } else {
                    // 動画が見つからなかった。
                    showToast(getString(R.string.not_found_video))
                    stopSelf()
                    return@launch
                }
            }
        }
    }

    /** 動画再生 */
    private fun playExoPlayer(isCache: Boolean, contentUrl: String, nicoHistory: String) {
        // キャッシュ再生と分ける
        if (isCache) {
            // キャッシュ再生
            val dataSourceFactory = DefaultDataSourceFactory(this, "TatimiDroid;@takusan_23")
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(contentUrl.toUri()))
            exoPlayer.setMediaSource(videoSource)
        } else {
            // SmileサーバーはCookieつけないと見れないため
            val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
            dataSourceFactory.defaultRequestProperties.set("Cookie", nicoHistory)
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(contentUrl.toUri()))
            exoPlayer.setMediaSource(videoSource)
        }
        exoPlayer.prepare()
        // シーク
        exoPlayer.seekTo(seekMs)
        // 自動再生
        exoPlayer.playWhenReady = true
        // MediaSession。通知もう一階出せばなんか表示されるようになった。Androidむずかちい
        showNotification(currentVideoTitle)
        initMediaSession()
    }

    /** プレイヤーのUIを追加する */
    private fun initPlayerUI() {
        // 権限なければ落とす
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else {
                false
            }
        ) {
            showToast("権限がありませんでした。「他のアプリの上に重ねて表示」権限をください。")
            return
        }
        // 画面の半分を利用するように
        val width = DisplaySizeTool.getDisplayWidth(this) / 2
        // レイアウト読み込み
        val layoutInflater = LayoutInflater.from(this)
        popupLayoutParams = getParams(width)
        viewBinding = OverlayVideoPlayerLayoutBinding.inflate(layoutInflater)
        viewBinding?.apply {
            // 表示
            windowManager.addView(root, popupLayoutParams)
            commentCanvas = overlayVideoCommentCanvas
            commentCanvas?.isPopupView = true
        }
    }

    /**
     * キャッシュ再生かどうかをUIに反映させる。
     * @param isCache キャッシュ再生ならtrue
     * */
    private fun showNetworkType(isCache: Boolean) {
        // 再生方法
        val playingIconDrawable = when {
            isCache -> getDrawable(R.drawable.ic_folder_open_black_24dp)
            else -> InternetConnectionCheck.getConnectionTypeDrawable(this)
        }
        viewBinding?.overlayVideoControlInclude?.playerControlVideoNetwork?.setImageDrawable(playingIconDrawable)
    }

    /** プレイヤーの動画情報等を更新する */
    private fun setPlayerUI(videoId: String, videoTitle: String) {
        // UIないとか論外
        viewBinding?.apply {
            // SurfaceViewセット
            exoPlayer.setVideoSurfaceView(overlayVideoSurfaceview)

            // 使わないボタンを消す
            overlayVideoControlInclude.apply {
                playerControlPopup.isVisible = false
                playerControlBackground.isVisible = false
                playerControlScreenShot.isVisible = false
            }
            // 番組名、ID設定
            overlayVideoControlInclude.apply {
                playerControlTitle.text = videoTitle
                playerControlId.text = videoId
            }

            // 閉じる
            overlayVideoControlInclude.playerControlClose.isVisible = true
            overlayVideoControlInclude.playerControlClose.setOnClickListener {
                stopSelf()
            }

            // リピートするか
            if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
            }

            // ミュート・ミュート解除
            overlayVideoControlInclude.playerControlMute.isVisible = true
            overlayVideoControlInclude.playerControlMute.setOnClickListener {
                if (::exoPlayer.isInitialized) {
                    exoPlayer.apply {
                        //音が０のとき
                        if (volume == 0f) {
                            volume = 1f
                            overlayVideoControlInclude.playerControlMute.setImageDrawable(getDrawable(R.drawable.ic_volume_up_24px))
                        } else {
                            volume = 0f
                            overlayVideoControlInclude.playerControlMute.setImageDrawable(getDrawable(R.drawable.ic_volume_off_24px))
                        }
                    }
                }
            }

            // UI表示
            var job: Job? = null
            root.setOnClickListener {
                overlayVideoControlInclude.playerControlMain.isVisible = !overlayVideoControlInclude.playerControlMain.isVisible
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.Main) {
                    delay(3000)
                    overlayVideoControlInclude.playerControlMain.isVisible = false
                }
            }

            // ピンチイン、ピンチアウトでズームできるようにする
            val scaleGestureDetector = ScaleGestureDetector(this@NicoVideoPlayService, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
                    return true
                }

                override fun onScaleEnd(p0: ScaleGestureDetector?) {

                }

                override fun onScale(p0: ScaleGestureDetector?): Boolean {
                    // ピンチイン/アウト中。
                    if (p0 == null) return true
                    // なんかうまくいくコード
                    popupLayoutParams.width = (popupLayoutParams.width * p0.scaleFactor).toInt()
                    // 縦の大きさは計算で出す（widthの時と同じようにやるとアスペクト比が崩れる。）
                    popupLayoutParams.height = if (aspect == 1.7) {
                        (popupLayoutParams.width / 16) * 9 // 16:9
                    } else {
                        (popupLayoutParams.width / 4) * 3 // 4:3
                    }
                    // 更新
                    windowManager.updateViewLayout(root, popupLayoutParams)
                    // 大きさを保持しておく
                    prefSetting.edit {
                        putInt("nicovideo_popup_height", popupLayoutParams.height)
                        putInt("nicovideo_popup_width", popupLayoutParams.width)
                    }
                    return true
                }
            })

            // 移動
            root.setOnTouchListener { view, motionEvent ->
                // タップした位置を取得する
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()
                // ついに直感的なズームが！？
                scaleGestureDetector.onTouchEvent(motionEvent)
                // 移動できるように
                when (motionEvent.action) {
                    // Viewを移動させてるときに呼ばれる
                    MotionEvent.ACTION_MOVE -> {
                        // 中心からの座標を計算する
                        val centerX = x - (DisplaySizeTool.getDisplayWidth(this@NicoVideoPlayService) / 2)
                        val centerY = y - (DisplaySizeTool.getDisplayHeight(this@NicoVideoPlayService) / 2)

                        // オーバーレイ表示領域の座標を移動させる
                        popupLayoutParams.x = centerX
                        popupLayoutParams.y = centerY

                        // 移動した分を更新する
                        windowManager.updateViewLayout(view, popupLayoutParams)

                        // サイズを保存しておく
                        prefSetting.edit {
                            putInt("nicovideo_popup_x_pos", popupLayoutParams.x)
                            putInt("nicovideo_popup_y_pos", popupLayoutParams.y)
                        }
                        return@setOnTouchListener true // setOnclickListenerが呼ばれてしまうため true 入れる
                    }
                }
                return@setOnTouchListener false
            }

            //アプリ起動
            overlayVideoControlInclude.playerControlFullscreen.setOnClickListener {
                stopSelf()
                // アプリ起動
                val intent = Intent(this@NicoVideoPlayService, MainActivity::class.java)
                intent.putExtra("videoId", videoId)
                intent.putExtra("cache", isCurrentVideoCache)
                intent.putExtra("start_pos", (exoPlayer.currentPosition / 1000).toInt()) // 秒で渡す
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            // 連続再生とアイコン変える
            val nextIcon = if (isPlayList) getDrawable(R.drawable.ic_skip_next_black_24dp) else getDrawable(R.drawable.ic_redo_black_24dp)
            val prevIcon = if (isPlayList) getDrawable(R.drawable.ic_skip_previous_black_24dp) else getDrawable(R.drawable.ic_undo_black_24dp)
            overlayVideoControlInclude.playerControlNext.setImageDrawable(nextIcon)
            overlayVideoControlInclude.playerControlPrev.setImageDrawable(prevIcon)

            // スキップ秒数
            val skipValueMs = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000
            // コントローラー
            overlayVideoControlInclude.playerControlNext.setOnClickListener {
                if (isPlayList) {
                    // 次の動画
                    nextPlaylistVideo()
                } else {
                    // 進める
                    exoPlayer.seekTo(exoPlayer.currentPosition + skipValueMs)
                }
            }
            overlayVideoControlInclude.playerControlPrev.setOnClickListener {
                if (isPlayList) {
                    // 前の動画
                    prevPlaylistVideo()
                } else {
                    // 戻す
                    exoPlayer.seekTo(exoPlayer.currentPosition - skipValueMs)
                }
            }

            overlayVideoControlInclude.playerControlPause.setOnClickListener {
                // 一時停止
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                // コメント止める
                commentCanvas?.isPause = !exoPlayer.playWhenReady
            }

            // リピート再生
            overlayVideoControlInclude.playerControlRepeat.setOnClickListener {
                when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_OFF -> {
                        // リピート無効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                        overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
                    }
                    Player.REPEAT_MODE_ONE -> {
                        // リピート有効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_black_24dp))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
                    }
                }
            }

            // シーク用に毎秒動くタイマー
            seekTimer.cancel()
            seekTimer = Timer()
            seekTimer.schedule(timerTask {
                if (exoPlayer.isPlaying) {
                    setProgress()
                    initDrawComment()
                }
            }, 100, 100)
        }
    }

    // サイズ変更をCommentCanvasに反映させる
    private fun applyCommentCanvas() {
        commentCanvas?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 高さ更新
                if (commentCanvas != null) {
                    commentCanvas!!.finalHeight = commentCanvas!!.height
                    commentCanvas!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })
    }

    /**
     * コメントを流す関数。定期的に呼んでください。
     * */
    private fun initDrawComment() {
        if (commentCanvas != null) {
            val currentPosition = exoPlayer.contentPosition / 100L
            if (tmpPosition != exoPlayer.contentPosition / 1000) {
                drewedList.clear()
                tmpPosition = currentPosition
            }
            GlobalScope.launch {
                val drawList = currentVideoCommentList.filter { commentJSONParse ->
                    (commentJSONParse.vpos.toLong() / 10L) == (currentPosition)
                }
                drawList.forEach {
                    // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                    val isAddable = drewedList.none { id -> it.commentNo == id || it.vpos == id } // 条件に合わなければtrue
                    if (isAddable) {
                        val commentNo = if (it.commentNo == "-1" || it.commentNo.isEmpty()) {
                            // vposで代替
                            it.vpos
                        } else {
                            it.commentNo
                        }
                        drewedList.add(commentNo)
                        if (!it.comment.contains("\n")) {
                            // SingleLine
                            commentCanvas!!.post {
                                commentCanvas!!.postComment(it.comment, it)
                            }
                        } else {
                            // 複数行？
                            val asciiArtComment = if (it.mail.contains("shita")) {
                                it.comment.split("\n").reversed() // 下コメントだけ逆順にする
                            } else {
                                it.comment.split("\n")
                            }
                            for (line in asciiArtComment) {
                                commentCanvas!!.post {
                                    commentCanvas!!.postComment(line, it, true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // シークを動画再生時間に合わせる
    private fun setProgress() {
        Handler(Looper.getMainLooper()).post {
            if (!isTouchingSeekBar) {
                viewBinding?.overlayVideoControlInclude?.playerControlSeek?.progress = (exoPlayer.currentPosition / 1000L).toInt()
                val formattedTime = DateUtils.formatElapsedTime(exoPlayer.currentPosition / 1000L)
                viewBinding?.overlayVideoControlInclude?.playerControlCurrent?.text = formattedTime
            }
        }
    }

    private fun getParams(width: Int): WindowManager.LayoutParams {
        //アスペクト比16:9なので
        val height = when (aspect) {
            1.3 -> (width / 4) * 3 // 4:3
            1.7 -> (width / 16) * 9 // 16:9
            else -> (width / 16) * 9
        }
        // オーバーレイViewの設定をする
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        }
        return params
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ポップアップ再生かどうかを返す
     * @return ポップアップ再生ならtrue
     * */
    private fun isPopupPlay(): Boolean {
        return playMode == "popup"
    }

    /** MediaSession。音楽アプリの再生中のあれ */
    private fun initMediaSession() {
        val mode = if (isPopupPlay()) {
            getString(R.string.popup_video_player)
        } else {
            getString(R.string.background_video_player)
        }
        mediaSessionCompat = MediaSessionCompat(this, "tatimidroid_nicovideo_service")
        // メタデータ
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "$currentVideoTitle / $currentVideoId")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentVideoId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentVideoTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentVideoTitle)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mode)
        }.build()
        mediaSessionCompat.apply {
            setMetadata(mediaMetadataCompat) // メタデータ入れる
            isActive = true // これつけないとAlways On Displayで表示されない
            // 常に再生状態にしておく。これでAODで表示できる
            setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1F).build())
        }
    }

    /**
     * サービス実行中通知を送る関数。
     * まずこれを呼んでServiceを終了させないようにしないといけない。
     * */
    private fun showNotification(message: String) {
        // 停止Broadcast送信
        val stopPopupIntent = Intent("video_popup_close")
        // サイズをもとに戻すボタン用Broadcast
        val fixPopupSize = Intent("video_popup_fix_size")
        // 通知のタイトル設定
        val title = if (isPopupPlay()) {
            getString(R.string.popup_video_player)
        } else {
            getString(R.string.background_video_player)
        }
        val icon = if (isPopupPlay()) {
            R.drawable.ic_popup_icon
        } else {
            R.drawable.ic_background_icon
        }
        // 通知作成
        val programNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "video_popup"
            val notificationChannel = NotificationChannel(notificationChannelId, getString(R.string.video_popup_background_play_service), NotificationManager.IMPORTANCE_HIGH)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, notificationChannelId)
        } else {
            NotificationCompat.Builder(this)
        }
        programNotification.apply {
            setContentTitle(title)
            setContentText(message)
            setSmallIcon(icon)
            addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoVideoPlayService, 24, stopPopupIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)))
            if (isPopupPlay()) {
                addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.popup_fix_size), PendingIntent.getBroadcast(this@NicoVideoPlayService, 34, fixPopupSize, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)))
            }
        }
        startForeground(NOTIFICAION_ID, programNotification.build())
    }

    /** ブロードキャスト初期化 */
    private fun initBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("video_popup_close")
        intentFilter.addAction("video_popup_fix_size")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "video_popup_close" -> {
                        // 終了
                        this@NicoVideoPlayService.stopSelf()
                    }
                    "video_popup_fix_size" -> {
                        // ポップアップの大きさを治す
                        popupLayoutParams = getParams(800) // 大きさを初期化
                        windowManager.updateViewLayout(viewBinding?.root, popupLayoutParams)
                        // 大きさを保持しておく
                        prefSetting.edit {
                            putInt("nicovideo_popup_height", popupLayoutParams.height)
                            putInt("nicovideo_popup_width", popupLayoutParams.width)
                        }
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        if (::mediaSessionCompat.isInitialized) {
            mediaSessionCompat.apply {
                isActive = false
                setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0L, 1F).build())
                release()
            }
        }
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        if (viewBinding != null) {
            windowManager.removeView(viewBinding!!.root)
        }
        seekTimer.cancel()
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
 * ポップアップ再生、バッググラウンド再生サービス起動用関数。トップレベル関数 なのでどっからでも呼べると思う？
 * @param mode "popup"（ポップアップ再生）か"background"（バッググラウンド再生）
 * @param context Context
 * @param videoId 動画ID
 * @param isCache キャッシュ再生ならtrue
 * @param seek シークするなら値を入れてね。省略可能。
 * @param playlist 連続再生を利用する場合は[NicoVideoData]の配列を入れてください。
 * @param videoQuality 画質を指定する場合は入れてね。無くてもいいよ。キャッシュ再生の時は使わない。例：「archive_h264_4000kbps_1080p」
 * @param audioQuality 音質を指定する場合は入れてね。無くてもいいよ。キャッシュ再生の時は使わない。例：「archive_aac_192kbps」
 * */
fun startVideoPlayService(context: Context?, mode: String, videoId: String, isCache: Boolean, seek: Long = 0L, videoQuality: String? = null, audioQuality: String? = null, playlist: ArrayList<NicoVideoData>? = null) {
    // ポップアップ再生の権限あるか
    if (mode == "popup") {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(context)
            } else {
                false
            }
        ) {
            // 権限取得画面出す
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
            context?.startActivity(intent)
            return
        }
    }
    val intent = Intent(context, NicoVideoPlayService::class.java).apply {
        putExtra("mode", mode)
        putExtra("video_id", videoId)
        putExtra("is_cache", isCache)
        putExtra("seek", seek)
        // 連続再生？
        if (playlist != null) {
            val nicoVideoCache = NicoVideoCache(context)
            putExtra("playlist", playlist.filter { nicoVideoData ->
                if (nicoVideoData.isCache) {
                    // 動画ファイルが存在してるもののみ取り出す。コメントのみ流すモードは未実装
                    nicoVideoCache.hasCacheVideoFile(nicoVideoData.videoId)
                } else {
                    true
                }
            } as Serializable)
        }
        // 画質入れる。
        if (videoQuality != null && audioQuality != null) {
            putExtra("video_quality", videoQuality)
            putExtra("audio_quality", audioQuality)
        }
    }
    // サービス終了（起動させてないときは何もならないと思う）させてから起動させる。（
    // 起動してない状態でstopService呼ぶ分にはなんの問題もないっぽい？）
    context?.stopService(intent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context?.startForegroundService(intent)
    } else {
        context?.startService(intent)
    }
}
