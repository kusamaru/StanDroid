package com.kusamaru.standroid.nicovideo

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.MainActivityPlayerFragmentInterface
import com.kusamaru.standroid.R
import com.kusamaru.standroid.adapter.parcelable.TabLayoutData
import com.kusamaru.standroid.databinding.FragmentNicovideoBinding
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoRecyclerPagerAdapter
import com.kusamaru.standroid.nicovideo.bottomfragment.ComememoBottomFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoCacheJSONUpdateRequestBottomFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoPlayListBottomFragment
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoSeriesFragment
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoUploadVideoFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoVideoViewModelFactory
import com.kusamaru.standroid.service.startVideoPlayService
import com.kusamaru.standroid.tool.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt

/**
 * 開発中のニコ動クライアント（？）
 *
 * id           |   動画ID。必須
 * --- 任意 ---
 * cache        |   キャッシュ再生ならtrue。なければfalse
 * eco          |   エコノミー再生するなら（?eco=1）true
 * internet     |   キャッシュ有っても強制的にインターネットを利用する場合はtrue
 * fullscreen   |   最初から全画面で再生する場合は true。
 * start_pos    |   再生開始時間。秒で渡して
 * */
class NicoVideoFragment : Fragment(), MainActivityPlayerFragmentInterface {

    lateinit var prefSetting: SharedPreferences
    lateinit var darkModeSupport: DarkModeSupport

    // ViewPager
    lateinit var viewPagerAdapter: NicoVideoRecyclerPagerAdapter

    // フォント
    lateinit var font: CustomFont

    // シーク操作中かどうか
    var isTouchSeekBar = false

    /**
     * MVVM ｲｸｿﾞｵｵｵｵｵ！
     * データを置くためのクラスです。これは画面回転しても値を失うことはないです。
     * */
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

    val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    /** findViewById駆逐。さよなら Kotlin Android Extensions */
    val viewBinding by lazy { FragmentNicovideoBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // ふぉんと
        font = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (font.isApplyFontFileToCommentCanvas) {
            viewBinding.fragmentNicovideoCommentCanvas.typeFace = font.typeface
        }

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // くーるくるー
        showSwipeToRefresh()

        // ダークモード
        initDarkmode()

        // ExoPlayer初期化
        initExoPlayer()

        if (savedInstanceState == null) {
            // 初回時のみ
            showSeekLatestPosition()
        }

        // 全画面モードなら
        if (viewModel.isFullScreenMode) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            setFullScreen()
        }

        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // MainActivityのBottomNavを表示させるか
            (requireActivity() as MainActivity).setVisibilityBottomNav()
            setMiniPlayer(isMiniPlayerMode)
            // アイコン直す
            val icon = when (viewBinding.fragmentNicovideoMotionLayout.currentState) {
                R.id.fragment_nicovideo_transition_end -> requireContext().getDrawable(R.drawable.ic_expand_less_black_24dp)
                else -> requireContext().getDrawable(R.drawable.ic_expand_more_24px)
            }
            viewBinding.fragmentNicovideoControlInclude.playerControlBackButton.setImageDrawable(icon)
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                viewBinding.fragmentNicovideoMotionLayout.transitionToState(R.id.fragment_nicovideo_transition_end)
            }
        }

        // キャッシュ再生かどうか。
        viewModel.isOfflinePlay.observe(viewLifecycleOwner) {
            // 動画変更時にやることリスト
            initViewPager(viewModel.dynamicAddFragmentList)
            viewBinding.fragmentNicovideoCommentCanvas.clearCommentList()
        }


        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                getString(R.string.encryption_video_not_play) -> finishFragment()
            }
        }

        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            Snackbar.make(viewBinding.fragmentNicovideoSurfaceView, it, Snackbar.LENGTH_SHORT).show()
        }

        if (viewModel.isCommentOnlyMode) {
            setCommentOnlyMode(true)
        } else {
            // 動画再生
            viewModel.contentUrl.observe(viewLifecycleOwner) { contentUrl ->
                val oldPosition = exoPlayer.currentPosition
                playExoPlayer(contentUrl)
                // 画質変更時は途中から再生。動画IDが一致してないとだめ
                if (oldPosition > 0 && exoPlayer.currentMediaItem?.mediaId == viewModel.playingVideoId.value) {
                    exoPlayer.seekTo(oldPosition)
                }
                exoPlayer.setVideoSurfaceView(viewBinding.fragmentNicovideoSurfaceView)
            }
        }

        // コメント
        viewModel.commentList.observe(viewLifecycleOwner) { commentList ->
            // ついでに動画の再生時間を取得する。非同期
            viewModel.playerDurationMs.observe(viewLifecycleOwner, object : Observer<Long> {
                override fun onChanged(t: Long?) {
                    if (t != null && t > 0) {
                        viewBinding.fragmentNicovideoCommentCanvas.initCommentList(commentList, t)
                        // 一回取得したらコールバック無効化。SAM変換をするとthisの指すものが変わってしまう
                        viewModel.playerDurationMs.removeObserver(this)
                    }
                }
            })
        }

        // 動画情報
        viewModel.nicoVideoData.observe(viewLifecycleOwner) { nicoVideoData ->
            initUI(nicoVideoData)
        }

        // ViewPager追加など
        viewModel.nicoVideoJSON.observe(viewLifecycleOwner) { json ->
            if (viewModel.isOfflinePlay.value == false) {
                viewPagerAddAccountFragment(json)
            }
        }

        // 動画を再生しないコメントを流すのみのモード
        viewModel.isNotPlayVideoMode.observe(viewLifecycleOwner) { isCommentDrawOnly ->
            // コメントのみ流す
            if (isCommentDrawOnly) {
                // ExoPlayer終了（使わないので）
                exoPlayer.stop()
                hideSwipeToRefresh()

                // ポップアップ再生も使わないため
                viewBinding.fragmentNicovideoControlInclude.playerControlPopup.isVisible = false
                viewBinding.fragmentNicovideoControlInclude.playerControlBackground.isVisible = false

                // プログレスバー用意
                initVideoProgressBar()

            }
        }

        // 連続再生
        viewModel.isPlayListMode.observe(viewLifecycleOwner) { isPlaylist ->
            // 連続再生とそれ以外でアイコン変更
            val playListModePrevIcon = if (isPlaylist) requireContext().getDrawable(R.drawable.ic_skip_previous_black_24dp) else requireContext().getDrawable(R.drawable.ic_undo_black_24dp)
            val playListModeNextIcon = if (isPlaylist) requireContext().getDrawable(R.drawable.ic_skip_next_black_24dp) else requireContext().getDrawable(R.drawable.ic_redo_black_24dp)
            viewBinding.fragmentNicovideoControlInclude.playerControlPrev.setImageDrawable(playListModePrevIcon)
            viewBinding.fragmentNicovideoControlInclude.playerControlNext.setImageDrawable(playListModeNextIcon)
            viewBinding.fragmentNicovideoControlInclude.playerControlPlaylist.isVisible = isPlaylist
        }

        // キャッシュ再生時に、JSONファイルの再取得を求めるやつ
        viewModel.cacheVideoJSONUpdateLiveData.observe(viewLifecycleOwner) { isNeedUpdate ->
            NicoVideoCacheJSONUpdateRequestBottomFragment().show(childFragmentManager, "update")
        }

        // LiveDataの通知でExoPlayerを操作するようにしたので
        initExoPlayerControlLiveData()

        // コントローラー用意
        initController()

        // アスペクト比直す。とりあえず16:9で
        aspectRatioFix(16, 9)

    }

    /**
     * LiveDataを経由してExoPlayerを操作するので、コールバックをセットする関数
     * と思ったけどくそ使いにくいなこれ。どうにかしたい
     * */
    private fun initExoPlayerControlLiveData() {
        // 一時停止、再生になったとき
        viewModel.playerIsPlaying.observe(viewLifecycleOwner) { isPlaying ->
            exoPlayer.playWhenReady = isPlaying
            val drawable = if (isPlaying) {
                context?.getDrawable(R.drawable.ic_pause_black_24dp)
            } else {
                context?.getDrawable(R.drawable.ic_play_arrow_24px)
            }
            viewBinding.fragmentNicovideoControlInclude.playerControlPause.setImageDrawable(drawable)
            viewBinding.fragmentNicovideoCommentCanvas.isPlaying = false
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
            viewBinding.fragmentNicovideoCommentCanvas.currentPos = seekPos
            viewBinding.fragmentNicovideoCommentCanvas.seekComment()
        }
        // 動画の再生時間
        viewModel.playerDurationMs.observe(viewLifecycleOwner) { duration ->
            if (duration > 0) {
                viewBinding.fragmentNicovideoControlInclude.playerControlSeek.max = (duration / 1000).toInt()
                viewBinding.fragmentNicovideoControlInclude.playerControlDuration.text = DateUtils.formatElapsedTime(duration / 1000)
            }
        }
        // リピートモードが変わったとき
        viewModel.playerIsRepeatMode.observe(viewLifecycleOwner) { isRepeatMode ->
            if (isRepeatMode) {
                // リピート有効時
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                viewBinding.fragmentNicovideoControlInclude.playerControlRepeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
                prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
            } else {
                // リピート無効時
                exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                viewBinding.fragmentNicovideoControlInclude.playerControlRepeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_black_24dp))
                prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
            }
        }
    }

    /** UIに反映させる */
    private fun initUI(nicoVideoData: NicoVideoData) {
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = when {
            nicoVideoData.isCache -> requireContext().getDrawable(R.drawable.ic_folder_open_black_24dp)
            else -> InternetConnectionCheck.getConnectionTypeDrawable(requireContext())
        }
        viewBinding.fragmentNicovideoControlInclude.playerControlVideoNetwork.setImageDrawable(playingTypeDrawable)
        viewBinding.fragmentNicovideoControlInclude.playerControlVideoNetwork.setOnClickListener {
            // なんの方法（キャッシュ・モバイルデータ・Wi-Fi）で再生してるかを表示する
            val message = if (nicoVideoData.isCache) {
                getString(R.string.use_cache)
            } else {
                InternetConnectionCheck.createNetworkMessage(requireContext())
            }
            showToast(message)
        }

        viewBinding.fragmentNicovideoControlInclude.playerControlTitle.text = nicoVideoData.title
        // Marqueeを有効にするにはフォーカスをあてないといけない？。<marquee>とかWeb黎明期感ある（その時代の人じゃないけど）
        viewBinding.fragmentNicovideoControlInclude.playerControlTitle.isSelected = true
        viewBinding.fragmentNicovideoControlInclude.playerControlId.text = nicoVideoData.videoId
        // リピートボタン押したとき
        viewBinding.fragmentNicovideoControlInclude.playerControlRepeat.setOnClickListener {
            // リピートモード変更LiveData送信
            viewModel.playerIsRepeatMode.postValue(!viewModel.playerIsRepeatMode.value!!)
        }
    }

    private fun playExoPlayer(contentUrl: String) {
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "TatimiDroid;@takusan_23")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                // SmileサーバーはCookieつけないと見れないため
                val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", viewModel.nicoHistory)
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
        }
        // 準備と再生
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        viewModel.playerIsPlaying.postValue(true)
    }

    /** 最後に見たところから再生SnackBarを表示。キャッシュ再生時のみ */
    private fun showSeekLatestPosition() {
        val videoId = viewModel.nicoVideoData.value?.videoId ?: viewModel.playingVideoId.value
        val progress = prefSetting.getLong("progress_$videoId", 0)
        if (progress != 0L && viewModel.isOfflinePlay.value == true) {
            Snackbar.make(viewBinding.fragmentNicovideoSurfaceView, "${getString(R.string.last_time_position_message)}(${DateUtils.formatElapsedTime(progress / 1000L)})", Snackbar.LENGTH_LONG).apply {
                setAction(R.string.play) {
                    viewModel.playerSetSeekMs.postValue(progress)
                }
                show()
            }
        }
    }

    /** ExoPlayerを初期化する */
    private fun initExoPlayer() {
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 再生
                // viewModel.playerIsPlaying.postValue(exoPlayer.playWhenReady)
                // 動画時間をセットする
                viewModel.playerDurationMs.postValue(exoPlayer.duration)
                if (state == Player.STATE_BUFFERING) {
                    // STATE_BUFFERING はシークした位置からすぐに再生できないとき。読込み中のこと。
                    showSwipeToRefresh()
                } else {
                    hideSwipeToRefresh()
                }
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    // 動画おわった。連続再生時なら次の曲へ
                    viewModel.nextVideo()
                }
            }

        })
        // 縦、横取得
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // DMCのJSONからも幅とかは取れるけどキャッシュ再生でJSONがない場合をサポートしたいため
                if (isAdded) { // コールバックなのでこの時点でもう無いかもしれない
                    aspectRatioFix(width, height)
                }
            }
        })

        // プログレスバー動かす
        initVideoProgressBar()
    }

    /** 動画のプログレスバーを動かす */
    private fun initVideoProgressBar() {
        var tempTime = 0L
        // コルーチンでらくらく定期実行
        lifecycleScope.launch {
            while (isActive) {
                delay(100)

                // 再生時間をコメント描画Canvasへ入れ続ける
                viewBinding.fragmentNicovideoCommentCanvas.currentPos = viewModel.playerCurrentPositionMs
                viewBinding.fragmentNicovideoCommentCanvas.isPlaying = if (viewModel.isNotPlayVideoMode.value == false) {
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
                    // シークバー動かす+ViewModelの再生時間更新
                    setProgress()
                }
            }
        }
    }

    /** アスペクト比を直す関数 */
    private fun aspectRatioFix(width: Int, height: Int) {
        viewBinding.root.doOnLayout {
            val displayWidth = requireActivity().window.decorView.width
            val displayHeight = requireActivity().window.decorView.height
            if (isLandscape()) {
                // 横画面
                var playerWidth = displayWidth / 2
                var playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
                // 縦動画の場合は調整する
                if (playerHeight > displayHeight) {
                    playerWidth /= 2
                    playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
                }
                // レイアウト調整。プレイヤーのFrameLayoutのサイズを変える
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_start).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, playerHeight)
                    constrainWidth(R.id.fragment_nicovideo_surface_view, playerWidth)
                }
                // ミニプレイヤーも
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_end).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, (playerHeight / 1.5).roundToInt())
                    constrainWidth(R.id.fragment_nicovideo_surface_view, (playerWidth / 1.5).roundToInt())
                }
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_finish).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, (playerHeight / 1.5).roundToInt())
                    constrainWidth(R.id.fragment_nicovideo_surface_view, (playerWidth / 1.5).roundToInt())
                }
                // 全画面UI
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_fullscreen).apply {
                    val fullScreenHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, displayWidth).toInt()
                    if (fullScreenHeight > displayHeight) {
                        // 画面外に行く
                        val fullScreenWidth = viewModel.nicoVideoHTML.calcVideoWidthDisplaySize(width, height, displayHeight).toInt()
                        constrainHeight(R.id.fragment_nicovideo_surface_view, displayHeight)
                        constrainWidth(R.id.fragment_nicovideo_surface_view, fullScreenWidth)
                    } else {
                        // おさまる
                        constrainHeight(R.id.fragment_nicovideo_surface_view, fullScreenHeight)
                        constrainWidth(R.id.fragment_nicovideo_surface_view, displayWidth)
                    }
                }
            } else {
                // 縦画面
                var playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, displayWidth).roundToInt()
                var playerWidth = displayWidth
                // 縦動画の場合は調整する
                if (playerHeight > displayWidth) {
                    playerWidth /= 2
                    playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
                }
                // レイアウト調整。プレイヤーのFrameLayoutのみ。背景はプレイヤーFrameLayoutの高さに合うように制約が設定してある。幅は最大
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_start).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, playerHeight)
                    constrainWidth(R.id.fragment_nicovideo_surface_view, playerWidth)
                }
                // ミニプレイヤーも
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_end).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, playerHeight / 2)
                    constrainWidth(R.id.fragment_nicovideo_surface_view, playerWidth / 2)
                }
                viewBinding.fragmentNicovideoMotionLayout.getConstraintSet(R.id.fragment_nicovideo_transition_finish).apply {
                    constrainHeight(R.id.fragment_nicovideo_surface_view, playerHeight / 2)
                    constrainWidth(R.id.fragment_nicovideo_surface_view, playerWidth / 2)
                }
            }
        }
    }

    /**
     * フルスクリーンへ移行。
     * 先日のことなんですけども、ついに（待望）（念願の）（満を持して）、わたくしの動画が、無断転載されてました（ﾄﾞｩﾋﾟﾝ）
     * ↑これ sm29392869 全画面で見れるようになる
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        viewModel.isFullScreenMode = true
        viewBinding.fragmentNicovideoControlInclude.playerControlFullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // コメント一覧非表示
        viewBinding.fragmentNicovideoMotionLayout.transitionToState(R.id.fragment_nicovideo_transition_fullscreen)
        setSystemBarVisibility(false)
        // 背景を黒く
        viewBinding.fragmentNicovideoBackground.background = ColorDrawable(Color.BLACK)
    }

    /** フルスクリーン解除 */
    private fun setCloseFullScreen() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        viewModel.isFullScreenMode = false
        viewBinding.fragmentNicovideoControlInclude.playerControlFullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // コメント一覧表示
        viewBinding.fragmentNicovideoMotionLayout.transitionToState(R.id.fragment_nicovideo_transition_start)
        // システムバー表示
        setSystemBarVisibility(true)
        // 背景戻す
        viewBinding.fragmentNicovideoBackground.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /**
     * システムバーを非表示にする関数
     * システムバーはステータスバーとナビゲーションバーのこと。多分
     * @param isShow 表示する際はtrue。非表示の際はfalse
     * */
    private fun setSystemBarVisibility(isShow: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 systemUiVisibilityが非推奨になり、WindowInsetsControllerを使うように
            activity?.window?.insetsController?.apply {
                if (isShow) {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH
                    show(WindowInsets.Type.systemBars())
                } else {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY の WindowInset版。ステータスバー表示等でスワイプしても、操作しない場合はすぐに戻るやつです。
                    hide(WindowInsets.Type.systemBars()) // Type#systemBars を使うと Type#statusBars() Type#captionBar() Type#navigationBars() 一斉に消せる
                }
            }
        } else {
            // Android 10 以前
            if (isShow) {
                activity?.window?.decorView?.systemUiVisibility = 0
            } else {
                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
    }

    /**
     * ノッチ領域に侵略する関数。
     * この関数はAndroid 9以降で利用可能なので各自条件分岐してね。
     * @param isShow 侵略する際はtrue。そうじゃないならfalse
     * */
    @RequiresApi(Build.VERSION_CODES.P)
    fun setNotchVisibility(isShow: Boolean) {
        val attribute = activity?.window?.attributes
        attribute?.layoutInDisplayCutoutMode = if (isShow) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /** コントローラー初期化。ミニプレイヤーをスワイプして終了はこの関数を呼ばないと使えない */
    fun initController() {
        // コントローラーを消すためのコルーチン
        val job = Job()
        // 戻るボタン
        viewBinding.fragmentNicovideoControlInclude.playerControlBackButton.isVisible = true
        viewBinding.fragmentNicovideoControlInclude.playerControlBackButton.setOnClickListener {
            // 最小化するとかしないとか
            viewBinding.fragmentNicovideoMotionLayout.apply {
                when {
                    viewModel.isFullScreenMode -> {
                        setCloseFullScreen()
                    }
                    currentState == R.id.fragment_nicovideo_transition_start -> {
                        transitionToState(R.id.fragment_nicovideo_transition_end)
                    }
                    else -> {
                        transitionToState(R.id.fragment_nicovideo_transition_start)
                    }
                }
            }
        }
        // MotionLayoutでスワイプできない対策に独自FrameLayoutを作った
        viewBinding.fragmentNicovideoMotionlayoutParentFramelayout.apply {
            allowIdList.add(R.id.fragment_nicovideo_transition_start) // 通常状態（コメント表示など）は無条件でタッチを渡す。それ以外はプレイヤー部分のみタッチ可能
            allowIdList.add(R.id.fragment_nicovideo_transition_fullscreen) // フルスクリーン時もクリックが行かないように
            swipeTargetView = viewBinding.fragmentNicovideoControlInclude.root
            motionLayout = viewBinding.fragmentNicovideoMotionLayout
            // プレイヤーを押した時。普通にsetOnClickListenerとか使うと競合して動かなくなる
            onSwipeTargetViewClickFunc = {
                viewBinding.fragmentNicovideoControlInclude.playerControlMain.isVisible = !viewBinding.fragmentNicovideoControlInclude.playerControlMain.isVisible
                // ３秒待ってもViewが表示されてる場合は消せるように。
                updateHideController(job)
            }
            onSwipeTargetViewDoubleClickFunc = { ev ->
                // player_control_parentでコントローラーのUIが表示されてなくてもスキップできるように
                if (ev != null) {
                    val skipMs = prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5
                    val isLeft = ev.x < viewBinding.fragmentNicovideoControlInclude.playerControlCenterParent.width / 2
                    val seekMs = if (isLeft) {
                        // 半分より左
                        viewModel.playerCurrentPositionMs - skipMs * 1000
                    } else {
                        // 半分より右
                        viewModel.playerCurrentPositionMs + skipMs * 1000
                    }
                    // LivaData経由でシークしろと通知飛ばす
                    viewModel.playerSetSeekMs.postValue(seekMs)
                    updateHideController(job)
                }
            }
            // swipeTargetViewの上にあるViewをここに書く。ここに書いたViewを押した際はonSwipeTargetViewClickFuncが呼ばれなくなる(View#setOnClickListenerは呼ばれる)
            addAllIsClickableViewFromParentView(viewBinding.fragmentNicovideoControlInclude.playerControlMain)
            blockViewList.add(viewBinding.fragmentNicovideoControlInclude.playerControlSeek)
            // blockViewListに追加したViewが押さてたときに共通で行いたい処理などを書く
            onBlockViewClickFunc = { view, event ->
                viewBinding.fragmentNicovideoControlInclude.playerControlMain.apply {
                    // UI非表示なら表示
                    if (!isVisible) {
                        onSwipeTargetViewClickFunc?.invoke(null)
                    } else {
                        //view?.performClick()
                    }
                }
            }
        }
        // 再生/一時停止
        viewBinding.fragmentNicovideoControlInclude.playerControlPause.setOnClickListener {
            // 再生ボタン押したとき
            viewModel.playerIsPlaying.postValue(!viewModel.playerIsPlaying.value!!)
        }
        viewBinding.fragmentNicovideoControlInclude.playerControlPrev.setOnClickListener {
            // 前の動画へ
            viewModel.prevVideo()
        }
        viewBinding.fragmentNicovideoControlInclude.playerControlNext.setOnClickListener {
            // 次の動画へ
            viewModel.nextVideo()
        }
        // 全画面ボタン
        viewBinding.fragmentNicovideoControlInclude.playerControlFullscreen.setOnClickListener {
            if (viewModel.isFullScreenMode) {
                // 全画面終了ボタン
                setCloseFullScreen()
            } else {
                // なお全画面は横のみサポート。SCREEN_ORIENTATION_USER_LANDSCAPEを使うと逆向き横画面を回避できる（横画面でも二通りあるけど自動で解決してくれる）
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                setFullScreen()
            }
        }
        // 連続再生ならプレイリスト
        viewBinding.fragmentNicovideoControlInclude.playerControlPlaylist.setOnClickListener {
            NicoVideoPlayListBottomFragment().show(childFragmentManager, "list")
        }
        // ポップアップ/バッググラウンドなど
        viewBinding.fragmentNicovideoControlInclude.playerControlPopup.setOnClickListener {
            // ポップアップ再生
            viewModel.nicoVideoData.value?.let { data ->
                startVideoPlayService(
                    context = context,
                    mode = "popup",
                    videoId = data.videoId,
                    isCache = data.isCache,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value,
                    seek = viewModel.currentPosition
                )
                // Fragment閉じる
                finishFragment()
            }
        }
        viewBinding.fragmentNicovideoControlInclude.playerControlBackground.setOnClickListener {
            // バッググラウンド再生
            viewModel.nicoVideoData.value?.let { data ->
                startVideoPlayService(
                    context = context,
                    mode = "background",
                    videoId = data.videoId,
                    isCache = data.isCache,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value,
                    seek = viewModel.currentPosition
                )
                // Fragment閉じる
                finishFragment()
            }
        }
        // コメメモ機能
        viewBinding.fragmentNicovideoControlInclude.playerControlScreenShot.setOnClickListener {
            showKokosukiBottomFragment()
        }
        // 戻るキー押した時
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            // コメントのみの表示の際はFragment終了
            if (viewModel.isCommentOnlyMode) {
                finishFragment()
            } else {
                viewBinding.fragmentNicovideoMotionLayout.apply {
                    if (currentState == R.id.fragment_nicovideo_transition_end) {
                        this@addCallback.isEnabled = false
                    } else {
                        transitionToState(R.id.fragment_nicovideo_transition_end)
                    }
                }
            }
        }

        // MotioLayoutのコールバック
        viewBinding.fragmentNicovideoMotionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {

            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                // 戻るキー監視は通常時とフルスクリーン時のみ
                callback.isEnabled = p1 == R.id.fragment_nicovideo_transition_start || p1 == R.id.fragment_nicovideo_transition_fullscreen
                if (p1 == R.id.fragment_nicovideo_transition_finish) {
                    // 終了時。左へスワイプした時
                    parentFragmentManager.beginTransaction().remove(this@NicoVideoFragment).commit()
                } else {
                    // ここどうする？
                    val isMiniPlayerMode = isMiniPlayerMode()
                    viewModel.isMiniPlayerMode.value = isMiniPlayerMode
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

            }
        })

        // シークバー用意
        viewBinding.fragmentNicovideoControlInclude.playerControlSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !viewBinding.fragmentNicovideoControlInclude.playerControlMain.isVisible) {
                    // ユーザー操作だけど、プレイヤーが非表示の時は変更しない。なんかMotionLayoutのせいなのかVisibility=GONEでも操作できるっぽい？
                    seekBar?.progress = (viewModel.playerCurrentPositionMs / 1000L).toInt()
                    // シークいじったら時間反映されるように
                    val formattedTime = DateUtils.formatElapsedTime((seekBar?.progress ?: 0).toLong())
                    val videoLengthFormattedTime = DateUtils.formatElapsedTime((viewModel.playerDurationMs.value ?: 0) / 1000L)
                    viewBinding.fragmentNicovideoControlInclude.playerControlCurrent.text = formattedTime
                    viewBinding.fragmentNicovideoControlInclude.playerControlDuration.text = videoLengthFormattedTime
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (viewBinding.fragmentNicovideoControlInclude.playerControlMain.isVisible) {
                    isTouchSeekBar = true
                    job.cancelChildren()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // コメントシークに対応させる
                viewBinding.fragmentNicovideoCommentCanvas.seekComment()
                // ExoPlayer再開
                viewModel.playerSetSeekMs.postValue((seekBar?.progress ?: 0) * 1000L)
                // コントローラー非表示カウントダウン開始
                updateHideController(job)
                isTouchSeekBar = false
            }
        })
        // Viewを数秒後に非表示するとか
        updateHideController(job)
    }

    /**  コメメモ（動画スクショ機能）BottomFragmentを表示する */
    private fun showKokosukiBottomFragment() {
        val data = viewModel.nicoVideoData.value ?: return
        Toast.makeText(context, getString(R.string.comememo_generating), Toast.LENGTH_SHORT).show()
        // 動画は一時停止
        viewModel.playerIsPlaying.postValue(false)
        lifecycleScope.launch(Dispatchers.Default) {
            // 表示する
            ComememoBottomFragment.show(
                fragmentManager = childFragmentManager,
                surfaceView = viewBinding.fragmentNicovideoSurfaceView,
                commentCanvas = viewBinding.fragmentNicovideoCommentCanvas,
                title = data.title,
                contentId = data.videoId,
                position = viewModel.currentPosition
            )
        }
    }

    /**
     * コントローラーを消すためのコルーチン。
     * */
    private fun updateHideController(job: Job) {
        // Viewを数秒後に非表示するとか
        job.cancelChildren()
        lifecycleScope.launch(job) {
            // Viewを数秒後に消す
            delay(3000)
            if (viewBinding.fragmentNicovideoControlInclude.playerControlMain.isVisible == true) {
                viewBinding.fragmentNicovideoControlInclude.playerControlMain?.isVisible = false
            }
        }
    }

    /**
     * ミニプレイヤー用UIを有効/無効にする関数
     * @param isMiniPlayerMode 有効にする場合はtrue。通常に戻す場合はfalse
     * */
    private fun setMiniPlayer(isMiniPlayerMode: Boolean) {
        listOf(
            viewBinding.fragmentNicovideoControlInclude.playerControlVideoNetwork,
            viewBinding.fragmentNicovideoControlInclude.playerControlPopup,
            viewBinding.fragmentNicovideoControlInclude.playerControlBackground,
            viewBinding.fragmentNicovideoControlInclude.playerControlRepeat,
            viewBinding.fragmentNicovideoControlInclude.playerControlFullscreen,
            viewBinding.fragmentNicovideoControlInclude.playerControlPrev,
            viewBinding.fragmentNicovideoControlInclude.playerControlNext,
            viewBinding.fragmentNicovideoControlInclude.playerControlCurrent,
            viewBinding.fragmentNicovideoControlInclude.playerControlSeek,
            viewBinding.fragmentNicovideoControlInclude.playerControlDuration,
        ).forEach { view ->
            // 一応3つ書いとく
            view.isVisible = !isMiniPlayerMode
            view.isClickable = !isMiniPlayerMode
            view.isEnabled = !isMiniPlayerMode
        }
        viewBinding.fragmentNicovideoControlInclude.playerControlPlaylist.isVisible = !isMiniPlayerMode && viewModel.isPlayListMode.value!!
        viewBinding.fragmentNicovideoControlInclude.playerControlScreenShot.isVisible = !isMiniPlayerMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    // Progress表示
    private fun showSwipeToRefresh() {
        viewBinding.fragmentNicovideoSwipe.apply {
            isRefreshing = true
            isEnabled = true
        }
    }

    // Progress非表示
    private fun hideSwipeToRefresh() {
        viewBinding.fragmentNicovideoSwipe.apply {
            isRefreshing = false
            isEnabled = false
        }
    }

    // ダークモード
    private fun initDarkmode() {
        darkModeSupport = DarkModeSupport(requireContext())
        viewBinding.fragmentNicovideoTablayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(requireContext()))
        viewBinding.fragmentNicovideoViewpagerParent.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.fragmentNicovideoBackground.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /**
     * Snackbarを表示させる関数
     * 第一引数はnull禁止。第二、三引数はnullにするとSnackBarのボタンが表示されません
     * */
    fun showSnackbar(message: String, clickMessage: String?, click: (() -> Unit)?) {
        Snackbar.make(viewBinding.fragmentNicovideoSurfaceView, message, Snackbar.LENGTH_SHORT).apply {
            if (clickMessage != null && click != null) {
                setAction(clickMessage) { click() }
            }
            show()
        }
    }

    /**
     * 動画再生View（SurfaceView）を非表示にしてコメントのみの表示にする関数
     * @param enable コメントのみにする場合はtrue
     * */
    fun setCommentOnlyMode(enable: Boolean) {
        viewModel.isCommentOnlyMode = enable
        exoPlayer.stop()
        if (enable) {
            // MotionLayoutを無効
            MotionLayoutTool.allTransitionEnable(viewBinding.fragmentNicovideoMotionLayout, false)
            MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.fragmentNicovideoMotionLayout, R.id.fragment_nicovideo_surface_view, View.GONE)
            hideSwipeToRefresh()
            viewModel.playerIsPlaying.value = false
        } else {
            // MotionLayoutを有効
            MotionLayoutTool.allTransitionEnable(viewBinding.fragmentNicovideoMotionLayout, true)
            MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.fragmentNicovideoMotionLayout, R.id.fragment_nicovideo_surface_view, View.VISIBLE)
            if (viewModel.isNotPlayVideoMode.value == true) {
                viewModel.playerIsPlaying.value = true
            } else {
                viewModel.contentUrl.value?.let { playExoPlayer(it) }
                showSwipeToRefresh()
            }
        }
    }

    /** コメント一覧Fragmentを取得する。無い可能性も有る？ */
    private fun requireCommentFragment() = (viewPagerAdapter.fragmentList[1] as? NicoVideoCommentFragment)

    /**
     * 進捗進める
     *
     * 定期的に呼んでね
     * */
    private fun setProgress() {
        // 動画なしコメントのみを再生するモード
        if (viewBinding.fragmentNicovideoControlInclude.playerControlSeek != null && !isTouchSeekBar) {
            // シークバー操作中でなければ
            viewBinding.fragmentNicovideoControlInclude.playerControlSeek.progress = (viewModel.playerCurrentPositionMs / 1000L).toInt()
            viewModel.currentPosition = viewModel.playerCurrentPositionMs
            // 再生時間TextView
            val formattedTime = DateUtils.formatElapsedTime(viewModel.playerCurrentPositionMs / 1000L)
            viewBinding.fragmentNicovideoControlInclude.playerControlCurrent.text = formattedTime
        }
    }


    /**
     * ViewPager初期化
     * 注意：[NicoVideoViewModel.isOfflinePlay]、[NicoVideoViewModel.playingVideoId]が必要
     * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。なければ省略していいです。
     * */
    private fun initViewPager(dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) {
        // このFragmentを置いたときに付けたTag
        viewPagerAdapter = NicoVideoRecyclerPagerAdapter(this, viewModel.playingVideoId.value!!, viewModel.isOfflinePlay.value ?: false, dynamicAddFragmentList)
        viewBinding.fragmentNicovideoViewpager.adapter = viewPagerAdapter
        TabLayoutMediator(viewBinding.fragmentNicovideoTablayout, viewBinding.fragmentNicovideoViewpager) { tab, position ->
            tab.text = viewPagerAdapter.fragmentTabName[position]
        }.attach()
        // コメントを指定しておく。View#post{}で確実にcurrentItemが仕事するようになった。ViewPager2頼むよ～
        viewBinding.fragmentNicovideoViewpager.post {
            viewBinding.fragmentNicovideoViewpager.setCurrentItem(1, false)
        }
        // もしTabLayoutを常時表示する場合は
        if (prefSetting.getBoolean("setting_scroll_tab_hide", false)) {
            viewBinding.fragmentNicovideoTablayout.updateLayoutParams<AppBarLayout.LayoutParams> {
                // KTX有能
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
        }
    }

    /**
     * ViewPagerにアカウントを追加する
     * */
    private fun viewPagerAddAccountFragment(jsonObject: JSONObject) {
        if (!jsonObject.isNull("owner")) {
            val ownerObject = jsonObject.getJSONObject("owner")
            val userId = ownerObject.getInt("id").toString()
            val nickname = ownerObject.getString("nickname")
            // DevNicoVideoFragment
            val postFragment = NicoVideoUploadVideoFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
            // すでにあれば追加しない
            if (!viewPagerAdapter.fragmentTabName.contains(nickname)) {
                viewPagerAdapter.addFragment(postFragment, nickname) // Fragment追加関数
            }
            // シリーズ一覧Fragment追加
            val seriesId = NicoVideoHTML().getSeriesId(jsonObject)
            val seriesTitle = NicoVideoHTML().getSeriesTitle(jsonObject)
            if (seriesId != null && seriesTitle != null) {
                // シリーズ設定してある。ViewPager2にFragment追加
                val seriesFragment = NicoVideoSeriesFragment().apply {
                    arguments = Bundle().apply {
                        putString("series_id", seriesId)
                        putString("series_title", seriesTitle)
                    }
                }
                // 登録済みなら追加しない
                if (!viewPagerAdapter.fragmentTabName.contains(seriesTitle)) {
                    viewPagerAdapter.addFragment(seriesFragment, seriesTitle) // Fragment追加関数
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 画面回転しても一時停止しない
        if (!prefSetting.getBoolean("setting_nicovideo_screen_rotation_not_pause", false)) {
            viewModel.playerIsPlaying.value = false
        }
        // コントローラー表示
        viewBinding.fragmentNicovideoMotionlayoutParentFramelayout.onSwipeTargetViewClickFunc?.invoke(null)
        // キャッシュ再生の場合は位置を保存する
        if (viewModel.isOfflinePlay.value == true) {
            prefSetting.edit {
                val videoId = viewModel.nicoVideoData.value?.videoId ?: viewModel.playingVideoId.value
                putLong("progress_$videoId", viewModel.playerCurrentPositionMs)
            }
        }
        (requireActivity() as MainActivity).setVisibilityBottomNav()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 再生位置を保管。画面回転後LiveDataで受け取る
        viewModel.playerSetSeekMs.value = exoPlayer.currentPosition
        exoPlayer.release()
        // BottomNav表示
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
        // 牛乳を飲んで状態異常を解除
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Fragment終了関数 */
    private fun finishFragment() = parentFragmentManager.beginTransaction().remove(this).commit()

    /** 画面が横かどうかを返す。横ならtrue */
    fun isLandscape() = requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** 戻るキー押した時 */
    override fun onBackButtonPress() {
        // コメントのみの表示の際は何もしない
    }

    /** ミニプレイヤーかどうか。なんかnullの時がある？ */
    override fun isMiniPlayerMode(): Boolean {
        // なんかたまにnullになる
        return viewBinding.fragmentNicovideoMotionLayout.let {
            // この行がreturnの値になる。apply{ }に返り値機能がついた
            it.currentState == R.id.fragment_nicovideo_transition_end || it.currentState == R.id.fragment_nicovideo_transition_finish
        } ?: false
    }

}
