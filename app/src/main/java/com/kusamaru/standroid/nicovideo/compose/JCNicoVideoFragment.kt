package com.kusamaru.standroid.nicovideo.compose

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.common.MimeTypes
import androidx.media3.common.VideoSize
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import com.kusamaru.standroid.PlayerParentFrameLayout
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.IncludeNicovideoPlayerBinding
import com.kusamaru.standroid.fragment.PlayerBaseFragment
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.NicoVideoCommentFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.ComememoBottomFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoCacheJSONUpdateRequestBottomFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoVideoViewModelFactory
import com.kusamaru.standroid.service.startVideoPlayService
import com.kusamaru.standroid.tool.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 開発中のニコ動クライアント（？）
 *
 * 一部の関数は[PlayerBaseFragment]の方に書いてあるのでなかったらそっちも見に行ってね
 *
 * id           |   動画ID。必須
 * --- 任意 ---
 * cache        |   キャッシュ再生ならtrue。なければfalse
 * eco          |   エコノミー再生するなら（?eco=1）true
 * internet     |   キャッシュ有っても強制的にインターネットを利用する場合はtrue
 * fullscreen   |   最初から全画面で再生する場合は true。
 * video_list   |   連続再生する場合は[NicoVideoData]の配列を[Bundle.putSerializable]使って入れてね
 * start_pos    |   開始位置。秒で
 * */
class JCNicoVideoFragment : PlayerBaseFragment() {

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** プレイヤー部分のUI */
    private val nicovideoPlayerUIBinding by lazy { IncludeNicovideoPlayerBinding.inflate(layoutInflater) }

    /** そうですね、やっぱり僕は、王道を征く、ExoPlayerですか */
    private val exoPlayer by lazy { ExoPlayer.Builder(requireContext()).build() }

    /** シーク操作中かどうか */
    private var isTouchSeekBar = false

    /** 共有 */
    private val contentShare by lazy { ContentShareTool(requireContext()) }

    /** レイアウト変更コールバック */
    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    /** ViewModel。データ取得など */
    val viewModel by lazy {
        // 動画ID
        val videoId = arguments?.getString("id")
        // キャッシュ再生
        val isCache = arguments?.getBoolean("cache")
        // エコノミー再生
        val isEconomy = arguments?.getBoolean("eco") ?: false
        // 強制的にインターネットを利用して取得
        val useInternet = arguments?.getBoolean("internet") ?: false
        // 全画面で開始
        val isStartFullScreen = arguments?.getBoolean("fullscreen") ?: false
        // 連続再生
        val videoList = arguments?.getSerializable("video_list") as? ArrayList<NicoVideoData>
        // 開始位置
        val startPos = arguments?.getInt("start_pos")
        // ViewModel用意
        ViewModelProvider(this, NicoVideoViewModelFactory(requireActivity().application, videoId, isCache, isEconomy, useInternet, isStartFullScreen, videoList, startPos)).get(NicoVideoViewModel::class.java)
    }

    @ExperimentalComposeUiApi
    @ExperimentalFoundationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // プレイヤー追加など
        setPlayerUI()

        // ExoPlayer初期化
        initExoPlayer()

        // コメント描画設定。フォント設定など
        setCommentCanvas()

        // LiveData監視
        setLiveData()

        // コメント動かす
        setTimer()

        // 動画情報Fragment設置
        setFragment()

        // スリープにしない
        caffeine()

        // 画面回転もしていない、最初の一回のみ実行する
        if (savedInstanceState == null) {
            showLatestSeekSnackBar()
            showCommentList()
        }

    }

    /** コメント一覧を展開する */
    private fun showCommentList() {
        // コメント一覧も表示
        lifecycleScope.launch {
            delay(1000)
            if (!viewModel.isFullScreenMode && !viewModel.isAutoCommentListShowOff) {
                // フルスクリーン時 もしくは 自動で展開しない場合 は操作しない
                viewModel.commentListShowLiveData.postValue(true)
            }
        }
    }

    /** キャッシュ再生時のみ。最後見たところから再生を表示させる */
    private fun showLatestSeekSnackBar() {
        val progress = prefSetting.getLong("progress_${viewModel.playingVideoId.value}", 0)
        if (progress != 0L && viewModel.isOfflinePlay.value == true) {
            lifecycleScope.launch {
                delay(500)
                // 継承元に実装あり
                showSnackBar("${getString(R.string.last_time_position_message)}(${DateUtils.formatElapsedTime(progress / 1000L)})", getString(R.string.play)) {
                    viewModel.playerSetSeekMs.postValue(progress)
                }
            }
        }
    }

    /** [JCNicoVideoInfoFragment] / [NicoVideoCommentFragment] を設置する */
    private fun setFragment() {
        // 動画情報Fragment、コメントFragment設置
        childFragmentManager.beginTransaction().replace(fragmentHostFrameLayout.id, JCNicoVideoInfoFragment()).commit()
        childFragmentManager.beginTransaction().replace(fragmentCommentHostFrameLayout.id, NicoVideoCommentFragment()).commit()
        // ダークモード
        fragmentCommentLinearLayout.background = ColorDrawable(getThemeColor(requireContext()))
        // コメント一覧展開ボタンを設置する
        bottomComposeView.apply {
            setContent {
                val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
                NicoVideoCommentButton(
                    click = {
                        viewModel.commentListShowLiveData.postValue(!isComment.value)
                    },
                    isComment = isComment.value
                )
            }
        }
        // コメント一覧展開など
        viewModel.commentListShowLiveData.observe(viewLifecycleOwner) { isShow ->
            // アニメーション？自作ライブラリ
            val dropPopAlert = fragmentCommentLinearLayout.toDropPopAlert()
            if (isShow) {
                dropPopAlert.showAlert(DropPopAlert.ALERT_UP)
            } else {
                dropPopAlert.hideAlert(DropPopAlert.ALERT_UP)
            }
        }
    }

    /** コメントと経過時間を定期的に更新していく */
    private fun setTimer() {
        // 勝手に終了してくれるコルーチンコンテキスト
        lifecycleScope.launch {
            while (isActive) {
                delay(100)
                // 再生時間をコメント描画Canvasへ入れ続ける
                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.currentPos = viewModel.playerCurrentPositionMs
                // 再生中かどうか
                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isPlaying = if (viewModel.isNotPlayVideoMode.value == false) {
                    // 動画バッファー中かも？
                    exoPlayer.isPlaying
                } else {
                    viewModel.playerIsPlaying.value!!
                }
                // 再生中のみ
                if (viewModel.playerIsPlaying.value == true) {
                    // ExoPlayerが利用できる場合は再生時間をViewModelへ渡す
                    if (viewModel.isNotPlayVideoMode.value == false) {
                        viewModel.playerCurrentPositionMs = exoPlayer.currentPosition
                    }
                    // シークバー操作中でなければ
                    if (!isTouchSeekBar) {
                        // ViewModelの再生時間更新
                        viewModel.currentPosition = viewModel.playerCurrentPositionMs
                        viewModel.playerCurrentPositionMsLiveData.value = viewModel.currentPosition
                    }
                }
            }
        }
    }

    /** LiveDataを監視する。ViewModelの結果を受け取る */
    private fun setLiveData() {
        // キャッシュ再生時に、JSONファイルの再取得を求めるやつ
        viewModel.cacheVideoJSONUpdateLiveData.observe(viewLifecycleOwner) { isNeedUpdate ->
            NicoVideoCacheJSONUpdateRequestBottomFragment().show(childFragmentManager, "update")
        }
        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                toMiniPlayer()
            }
        }
        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                getString(R.string.encryption_video_not_play) -> finishFragment()
            }
        }
        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            showSnackBar(it, null, null)
        }
        // コメント
        viewModel.commentList.observe(viewLifecycleOwner) { commentList ->
            // ついでに動画の再生時間を取得する。非同期
            viewModel.playerDurationMs.observe(viewLifecycleOwner, object : Observer<Long> {
                override fun onChanged(t: Long?) {
                    if (t != null && t > 0) {
                        nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.initCommentList(commentList, t)
                        // 一回取得したらコールバック無効化。SAM変換をするとthisの指すものが変わってしまう
                        viewModel.playerDurationMs.removeObserver(this)
                    }
                }
            })
        }
        // 動画再生 or 動画なしモード
        if (viewModel.isCommentOnlyMode) {
            /** コメントのみは [JCNicoVideoCommentOnlyFragment] にあります。 */
        } else {
            // 動画再生
            viewModel.contentUrl.observe(viewLifecycleOwner) { contentUrl ->
                val oldPosition = exoPlayer.currentPosition
                playExoPlayer(contentUrl)
                // 画質変更時は途中から再生。動画IDが一致してないとだめ
                if (oldPosition > 0 && exoPlayer.currentMediaItem?.mediaId == viewModel.playingVideoId.value) {
                    exoPlayer.seekTo(oldPosition)
                }
                exoPlayer.setVideoSurfaceView(nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView)
            }
        }
        // 一時停止、再生になったとき
        viewModel.playerIsPlaying.observe(viewLifecycleOwner) { isPlaying ->
            exoPlayer.playWhenReady = isPlaying
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isPlaying = isPlaying
        }
        // シークしたとき
        viewModel.playerSetSeekMs.observe(viewLifecycleOwner) { seekPos ->
            if (0 <= seekPos) {
                viewModel.playerCurrentPositionMs = seekPos
                exoPlayer.seekTo(seekPos)
            } else {
                // 負の値に突入するので０
                viewModel.playerCurrentPositionMs = 0
            }
            // シークさせる
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.currentPos = seekPos
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.seekComment()
        }
        // リピートモードが変わったとき
        viewModel.playerIsRepeatMode.observe(viewLifecycleOwner) { isRepeatMode ->
            exoPlayer.repeatMode = if (isRepeatMode) {
                // リピート有効時
                Player.REPEAT_MODE_ONE
            } else {
                // リピート無効時
                Player.REPEAT_MODE_OFF
            }
            prefSetting.edit { putBoolean("nicovideo_repeat_on", isRepeatMode) }
        }
        // 音量調整
        viewModel.volumeControlLiveData.observe(viewLifecycleOwner) { volume ->
            exoPlayer.volume = volume
        }
        // 再生速度変更
        viewModel.playbackSpeedControlLiveData.observe(viewLifecycleOwner) { speed ->
            exoPlayer.setPlaybackSpeed(speed)
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.changePlaybackSpeed(speed)
        }
    }

    /** ExoPlayerで動画を再生する */
    private fun playExoPlayer(contentUrl: String) {
        // nullチェック挟む
        if (viewModel.playingVideoId.value == null) {
            return
        }
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "Stan-Droid;@kusamaru_jp")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value!!).build())
                exoPlayer.setMediaSource(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                val dataSourceFactory = DefaultHttpDataSource.Factory().setUserAgent("Stan-Droid;@kusamaru_jp")
                // domandに対応
                viewModel.domandCookie?.let {
                    dataSourceFactory.setDefaultRequestProperties(mapOf("Cookie" to it))
                }
                val videoSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value!!).setMimeType(MimeTypes.APPLICATION_M3U8).build())
                exoPlayer.setMediaSource(videoSource)
            }
        }
        // 準備と再生
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        viewModel.playerIsPlaying.postValue(true)
        // プログレスバー動かす
        viewModel.playerIsLoading.postValue(true)
    }

    /** ExoPlayerを初期化する */
    private fun initExoPlayer() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 動画時間をセットする
                viewModel.playerDurationMs.postValue(exoPlayer.duration)
                // くるくる
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    viewModel.playerIsLoading.postValue(false)
                } else {
                    viewModel.playerIsLoading.postValue(true)
                }
                // 動画おわった。連続再生時なら次の曲へ
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    viewModel.nextVideo()
                }
            }
        })
        // 縦、横取得
        exoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                // DMCのJSONからも幅とかは取れるけどキャッシュ再生でJSONがない場合をサポートしたいため
                if (isAdded) { // コールバックなのでこの時点でもう無いかもしれない
                    viewModel.apply {
                        videoHeight = videoSize.height
                        videoWidth = videoSize.width
                    }
                    // アスペクト比調整
                    setOnLayoutChangeAspectRatioFix(videoSize.width, videoSize.height)
                }
            }
        })
    }

    /**
     * 画面サイズが変わったらアスペクト比を直す。一回だけ呼べばいいです。
     * @param videoHeight 動画の高さ
     * @param videoWidth 動画の幅
     * */
    private fun setOnLayoutChangeAspectRatioFix(videoWidth: Int, videoHeight: Int) {
        if (!isAdded) return
        // 既存のコールバックは消す
        if (onGlobalLayoutListener != null) {
            fragmentPlayerFrameLayout.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        // 前のViewの高さ
        var prevHeight = 0

        /** アスペクト比調整 */
        fun aspectRateFix() {
            val playerHeight = fragmentPlayerFrameLayout.height
            val playerWidth = fragmentPlayerFrameLayout.width
            // サイズが違うときのみ
            // ゼロチェックを挟まないとゼロ除算で落ちるときがある
            if (prevHeight != playerHeight && playerWidth != 0 && playerHeight != 0 && videoHeight != 0) {
                prevHeight = playerHeight
                val calcWidth = viewModel.nicoVideoHTML.calcVideoWidthDisplaySize(videoWidth, videoHeight, playerHeight).roundToInt()
                if (calcWidth > fragmentPlayerFrameLayout.width) {
                    // 画面外にプレイヤーが行く
                    nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView.updateLayoutParams {
                        width = playerWidth
                        height = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(videoWidth, videoHeight, playerWidth).roundToInt()
                    }
                } else {
                    nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView.updateLayoutParams {
                        width = calcWidth
                        height = playerHeight
                    }
                }
            }
        }

        // とりあえず呼ぶ
        aspectRateFix()

        onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            // サイズ変更時も呼ぶ
            aspectRateFix()
        }
        // コールバック追加
        fragmentPlayerFrameLayout.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    /** コメント描画設定。フォント設定など */
    private fun setCommentCanvas() {
        val font = CustomFont(requireContext())
        if (font.isApplyFontFileToCommentCanvas) {
            // 適用する設定の場合
            nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.typeFace = font.typeface
        }
    }

    /** プレイヤーFrameLayoutにUIを追加する */
    @ExperimentalComposeUiApi
    @ExperimentalFoundationApi
    @SuppressLint("ClickableViewAccessibility")
    private fun setPlayerUI() {
        addPlayerFrameLayout(nicovideoPlayerUIBinding.root)
        // Composeで作ったプレイヤーのUIを用意
        nicovideoPlayerUIBinding.includeNicovideoPlayerComposeView.apply {
            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                ) {

                    // 動画情報
                    val videoData = viewModel.nicoVideoData.observeAsState()
                    // ミニプレイヤーかどうか
                    val isMiniPlayerMode = viewModel.isMiniPlayerMode.observeAsState(false)
                    // コメント描画
                    val isShowDrawComment = remember { mutableStateOf(nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isVisible) }
                    // 連続再生かどうか
                    val isPlaylistMode = viewModel.isPlayListMode.observeAsState(initial = false)
                    // 再生中かどうか
                    val isPlaying = viewModel.playerIsPlaying.observeAsState(initial = false)
                    // 再生時間
                    val currentPosition = viewModel.playerCurrentPositionMsLiveData.observeAsState(initial = 0)
                    // リピート再生？
                    val isRepeat = viewModel.playerIsRepeatMode.observeAsState(initial = true)
                    // 動画の長さ
                    val duration = viewModel.playerDurationMs.observeAsState(initial = 0)
                    // ローディング
                    val isLoading = viewModel.playerIsLoading.observeAsState(initial = false)

                    // 時間
                    if (videoData.value != null) {
                        NicoVideoPlayerUI(
                            videoTitle = videoData.value!!.title,
                            videoId = videoData.value!!.videoId,
                            isMiniPlayer = isMiniPlayerMode.value,
                            isDisableMiniPlayerMode = isDisableMiniPlayerMode,
                            isFullScreen = viewModel.isFullScreenMode,
                            isConnectedWiFi = isConnectionWiFiInternet(requireContext()),
                            isShowCommentCanvas = isShowDrawComment.value,
                            isRepeat = isRepeat.value,
                            isCachePlay = videoData.value!!.isCache,
                            isLoading = isLoading.value,
                            isPlaylistMode = isPlaylistMode.value,
                            isPlaying = isPlaying.value,
                            currentPosition = currentPosition.value.toLong() / 1000,
                            duration = duration.value.toLong() / 1000,
                            onClickMiniPlayer = {
                                if (isMiniPlayerMode()) {
                                    toDefaultPlayer()
                                } else {
                                    toMiniPlayer()
                                }
                            },
                            onClickFullScreen = {
                                if (viewModel.isFullScreenMode) {
                                    setDefaultScreen()
                                } else {
                                    setFullScreen()
                                }
                            },
                            onClickNetwork = { showNetworkTypeMessage() },
                            onClickRepeat = { viewModel.playerIsRepeatMode.postValue(!viewModel.playerIsRepeatMode.value!!) },
                            onClickCommentDraw = {
                                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isVisible = !nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isVisible;
                                isShowDrawComment.value = nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.isVisible
                            },
                            onClickPopUpPlayer = {
                                startVideoPlayService(
                                    context = requireContext(),
                                    mode = "popup",
                                    videoId = videoData.value!!.videoId,
                                    isCache = videoData.value!!.isCache,
                                    seek = viewModel.currentPosition,
                                    videoQuality = viewModel.currentVideoQuality,
                                    audioQuality = viewModel.currentAudioQuality,
                                    playlist = viewModel.playlistLiveData.value
                                )
                                // Fragment閉じる
                                finishFragment()
                            },
                            onClickBackgroundPlayer = {
                                startVideoPlayService(
                                    context = requireContext(),
                                    mode = "background",
                                    videoId = videoData.value!!.videoId,
                                    isCache = videoData.value!!.isCache,
                                    seek = viewModel.currentPosition,
                                    videoQuality = viewModel.currentVideoQuality,
                                    audioQuality = viewModel.currentAudioQuality,
                                    playlist = viewModel.playlistLiveData.value
                                )
                                // Fragment閉じる
                                finishFragment()
                            },
                            onClickPicture = { showComememoBottomFragment() },
                            onClickPauseOrPlay = { viewModel.playerIsPlaying.postValue(!viewModel.playerIsPlaying.value!!) },
                            onClickPrev = { viewModel.prevVideo() },
                            onClickNext = { viewModel.nextVideo() },
                            onDoubleClickSeek = { isPrev ->
                                val seekValue = prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5
                                val seekMs = if (isPrev) {
                                    viewModel.currentPosition - (seekValue * 1000)
                                } else {
                                    viewModel.currentPosition + (seekValue * 1000)
                                }
                                viewModel.playerCurrentPositionMsLiveData.value = seekMs
                                viewModel.playerSetSeekMs.value = seekMs
                            },
                            onSeek = { progress ->
                                // コメントシークに対応させる
                                nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas.seekComment()
                                // ExoPlayer再開
                                viewModel.playerCurrentPositionMsLiveData.value = progress * 1000
                                viewModel.playerSetSeekMs.value = progress * 1000
                            },
                            onTouchingSeek = { isTouchSeekBar = it },
                        )
                    }
                }
            }
        }
        // 全画面モードなら
        if (viewModel.isFullScreenMode) {
            setFullScreen()
        }
        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
        }
    }

    /**  コメメモ（動画スクショ機能）BottomFragmentを表示する */
    private fun showComememoBottomFragment() {
        val data = viewModel.nicoVideoData.value ?: return
        Toast.makeText(context, getString(R.string.comememo_generating), Toast.LENGTH_SHORT).show()
        // 動画は一時停止
        viewModel.playerIsPlaying.postValue(false)
        lifecycleScope.launch(Dispatchers.Default) {
            // 表示する
            ComememoBottomFragment.show(
                fragmentManager = childFragmentManager,
                surfaceView = nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView,
                commentCanvas = nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas,
                title = data.title,
                contentId = data.videoId,
                position = viewModel.currentPosition
            )
        }
    }

    /** 全画面UIへ切り替える。非同期です */
    private fun setFullScreen() {
        lifecycleScope.launch {
            viewModel.isFullScreenMode = true
            // コメント / 動画情報Fragmentを非表示にする
            toFullScreen()
        }
    }

    /** 全画面UIを戻す。非同期です */
    private fun setDefaultScreen() {
        lifecycleScope.launch {
            viewModel.isFullScreenMode = false
            // コメント / 動画情報Fragmentを表示にする
            toDefaultScreen(viewModel.forcedRotationState)
        }
    }

    /** 画像つき共有をする */
    fun showShareSheetMediaAttach() {
        lifecycleScope.launch {
            // 親のFragment取得
            contentShare.showShareContentAttachPicture(
                playerView = nicovideoPlayerUIBinding.includeNicovideoPlayerSurfaceView,
                commentCanvas = nicovideoPlayerUIBinding.includeNicovideoPlayerCommentCanvas,
                contentId = viewModel.playingVideoId.value,
                contentTitle = viewModel.nicoVideoData.value?.title,
                fromTimeSecond = (exoPlayer.currentPosition / 1000L).toInt()
            )
        }
    }

    /** 共有する */
    fun showShareSheet() {
        // 親のFragment取得
        contentShare.showShareContent(
            programId = viewModel.playingVideoId.value,
            programName = viewModel.nicoVideoData.value?.title,
            fromTimeSecond = (exoPlayer.currentPosition / 1000L).toInt()
        )
    }

    override fun onPause() {
        super.onPause()
        // 画面回転しても一時停止しない
        if (!prefSetting.getBoolean("setting_nicovideo_screen_rotation_not_pause", false)) {
            viewModel.playerIsPlaying.value = false
        }
        // キャッシュ再生の場合は位置を保存する
        if (viewModel.isOfflinePlay.value == true) {
            prefSetting.edit {
                putLong("progress_${viewModel.playingVideoId.value}", viewModel.playerCurrentPositionMs)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 再生位置を保管。画面回転後LiveDataで受け取る
        viewModel.playerSetSeekMs.value = exoPlayer.currentPosition
        exoPlayer.release()
        caffeineUnlock()
    }

    override fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {
        super.onBottomSheetStateChane(state, isMiniPlayer)
        // 展開 or ミニプレイヤー のみ
        if (state == PlayerParentFrameLayout.PLAYER_STATE_DEFAULT || state == PlayerParentFrameLayout.PLAYER_STATE_MINI) {
            // 一応UI表示
            nicovideoPlayerUIBinding.root.performClick()
            // ViewModelへ状態通知
            viewModel.isMiniPlayerMode.value = isMiniPlayerMode()
        }
    }

}