package com.kusamaru.standroid.nicovideo

import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.activity.KotehanListActivity
import com.kusamaru.standroid.activity.NGListActivity
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSPMyListAPI
import com.kusamaru.standroid.nicoapi.NicoVideoCache
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoAddMylistBottomFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoQualityBottomFragment
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoSkipCustomizeBottomFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.R
import com.kusamaru.standroid.service.startCacheService
import com.kusamaru.standroid.service.startVideoPlayService
import com.kusamaru.standroid.tool.ContentShareTool
import com.kusamaru.standroid.tool.MotionLayoutTool
import com.kusamaru.standroid.tool.isConnectionInternet
import com.kusamaru.standroid.tool.isNotLoginMode
import com.kusamaru.standroid.databinding.FragmentNicovideoMenuBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * めにゅー
 * 3DSコメント非表示オプションなど
 * */
class NicoVideoMenuFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    var userSession = ""
    var videoId = ""

    /** キャッシュ再生ならtrue */
    var isCache = false

    // JSON
    lateinit var jsonObject: JSONObject

    /** ニコ動Fragment取得*/
    private fun requireNicoVideoFragment() = requireParentFragment() as NicoVideoFragment

    // NicoVideoFragmentのViewModelを取得する
    val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

    /** 共有 */
    private val contentShare by lazy { ContentShareTool(requireContext()) }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoMenuBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // キャッシュ
        isCache = arguments?.getBoolean("cache") ?: false

        // 動画ID
        videoId = arguments?.getString("id", "") ?: ""

        // そもそもキャッシュ取得できない（アニメ公式はhls形式でAES-128で暗号化されてるので取れない）動画はキャッシュボタン非表示
        if (::jsonObject.isInitialized) {
            // TODO: キャッシュ関係は段階的に廃止する
            // if (NicoVideoHTML().isEncryption(jsonObject.toString())) {
                viewBinding.fragmentNicovideoMenuGetCacheButton.visibility = View.GONE
                viewBinding.fragmentNicovideoMenuGetCacheEcoButton.visibility = View.GONE
            // }
        }

        // ログインしないモード用
        if (isNotLoginMode(context)) {
            viewBinding.fragmentNicovideoMenuAddMylistButton.visibility = View.GONE
        }

        // マイリスト追加ボタン
        initMylistButton()

        // キャッシュ取得ボタン
        initCacheButton()

        // 再取得ボタン
        initReGetButton()

        // 画質変更
        initQualityButton()

        // 共有できるようにする
        initShare()

        // 動画再生
        initPlayButton()

        // コピーボタン
        initCopyButton()

        // 強制画面回転ボタン
        initRotationButton()

        // ポップアップ再生、バッググラウンド再生ボタン
        initVideoPlayServiceButton()

        // 音量コントロール
        initVolumeControl()

        // 他のアプリで開くボタン
        initBrowserLaunchButton()

        // NG一覧Activity呼び出す
        initNGActivityButton()

        // スキップ秒数
        initSkipSetting()

        // 3DS消す
        init3DSHideSwitch()

        // Switch消す
        initSwitchHideSwitch()

        // かんたんコメント消す
        initKantanCommentHideSwitch()

        // 再生速度コントロール
        initPlaybackSpeedSlider()

        // コメント流さないモード
        initCommentCanvasVisibleSwitch()

    }

    private fun initCommentCanvasVisibleSwitch() {
        viewBinding.fragmentNicovideoMenuHideCommentCanvasSwitch.setOnCheckedChangeListener { compoundButton, b ->
            // 設定保存
            prefSetting.edit { putBoolean("nicovideo_comment_canvas_hide", b) }
            // 消す
            val visibility = if (b) View.GONE else View.VISIBLE
            MotionLayoutTool.setMotionLayoutViewVisible(requireNicoVideoFragment().viewBinding.fragmentNicovideoMotionLayout, requireNicoVideoFragment().viewBinding.fragmentNicovideoCommentCanvas.id, visibility)
        }
        // 設定読み出し
        viewBinding.fragmentNicovideoMenuHideCommentCanvasSwitch.isChecked = prefSetting.getBoolean("nicovideo_comment_canvas_hide", false)
    }

    private fun initKotehanButton() {
        viewBinding.fragmentNicovideoMenuKotehanActivityButton.setOnClickListener {
            // コテハン一覧
            val intent = Intent(context, KotehanListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initSwitchHideSwitch() {
        viewBinding.fragmentNicovideoMenuSwitchSwitch.isChecked = prefSetting.getBoolean("nicovideo_comment_switch_hidden", false)
        viewBinding.fragmentNicovideoMenuSwitchSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 変更
            prefSetting.edit { putBoolean("nicovideo_comment_switch_hidden", isChecked) }
            // コメント再適用
            lifecycleScope.launch {
                viewModel.commentFilter()
            }
        }
    }

    private fun init3DSHideSwitch() {
        viewBinding.fragmentNicovideoMenu3dsSwitch.isChecked = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        viewBinding.fragmentNicovideoMenu3dsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 変更
            prefSetting.edit { putBoolean("nicovideo_comment_3ds_hidden", isChecked) }
            // コメント再適用
            lifecycleScope.launch {
                viewModel.commentFilter()
            }
        }
    }

    private fun initKantanCommentHideSwitch() {
        viewBinding.fragmentNicovideoMenuHideKantanCommentSwitch.isChecked = prefSetting.getBoolean("nicovideo_comment_kantan_comment_hidden", false)
        viewBinding.fragmentNicovideoMenuHideKantanCommentSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 変更
            prefSetting.edit { putBoolean("nicovideo_comment_kantan_comment_hidden", isChecked) }
            // コメント再適用
            lifecycleScope.launch {
                viewModel.commentFilter()
            }
        }
    }

    private fun initSkipSetting() {
        // スキップ秒数変更画面表示
        viewBinding.fragmentNicovideoMenuSkipSettingButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("video_id", videoId)
            val skipCustomizeBottomFragment = NicoVideoSkipCustomizeBottomFragment()
            skipCustomizeBottomFragment.arguments = bundle
            skipCustomizeBottomFragment.show(parentFragmentManager, "skip")
        }
    }

    private fun initNGActivityButton() {
        viewBinding.fragmentNicovideoMenuNgActivityButton.setOnClickListener {
            val intent = Intent(context, NGListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initBrowserLaunchButton() {
        //ブラウザで再生。
        viewBinding.fragmentNicovideoMenuBrowserLaunchButton.setOnClickListener {
            openBrowser("https://nico.ms/$videoId")
        }
    }

    private fun openBrowser(addr: String) {
        val intent = Intent(Intent.ACTION_VIEW, addr.toUri())
        startActivity(intent)
    }


    // ポップアップ再生、バッググラウンド再生ボタン。startVideoPlayService()はNicoVideoPlayServiceに書いてあります。（internal funなのでどこでも呼べる）
    private fun initVideoPlayServiceButton() {
        viewModel.isNotPlayVideoMode.observe(viewLifecycleOwner) { isCommentDrawOnlyMode ->
            if (isCommentDrawOnlyMode) {
                // 映像流さずコメントだけ流す場合はポップアップ再生ボタンをふさぐ
                viewBinding.fragmentNicovideoMenuPopupButton.isEnabled = false
                viewBinding.fragmentNicovideoMenuBackgroundButton.isEnabled = false
            } else {
                viewBinding.fragmentNicovideoMenuPopupButton.setOnClickListener {
                    // ポップアップ再生
                    startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
                    // Activity落とす
                    activity?.finish()
                }
                viewBinding.fragmentNicovideoMenuBackgroundButton.setOnClickListener {
                    // バッググラウンド再生
                    startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
                    // Activity落とす
                    activity?.finish()
                }
            }
        }
    }

    private fun initRotationButton() {
        viewBinding.fragmentNicovideoMenuRotationButton.setOnClickListener {
            val conf = resources.configuration
            //live_video_view.stopPlayback()
            when (conf.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    //縦画面
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    viewModel.forcedRotationState = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                Configuration.ORIENTATION_LANDSCAPE -> {
                    //横画面
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    viewModel.forcedRotationState = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    private fun initCopyButton() {
        viewBinding.fragmentNicovideoMenuCopyButton.setOnClickListener {
            val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", videoId))
            Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${videoId}", Toast.LENGTH_SHORT).show()
        }
    }

    // 動画再生ボタン
    private fun initPlayButton() {
        viewBinding.fragmentNicovideoMenuVideoPlayButton.setOnClickListener {
            requireNicoVideoFragment().setCommentOnlyMode(!viewModel.isCommentOnlyMode)
        }
    }

    // マイリスト追加ボタン初期化
    private fun initMylistButton() {
        // マイリスト追加ボタン。インターネット接続時で動画IDでなければ消す
        if (!isConnectionInternet(context) && (videoId.contains("sm") || videoId.contains("so"))) {
            viewBinding.fragmentNicovideoMenuAddMylistButton.isVisible = false
            viewBinding.fragmentNicovideoMenuAtodemiruButton.isVisible = false
        }
        viewBinding.fragmentNicovideoMenuAddMylistButton.setOnClickListener {
            val addMylistBottomFragment = NicoVideoAddMylistBottomFragment()
            val bundle = Bundle()
            bundle.putString("id", videoId)
            addMylistBottomFragment.arguments = bundle
            addMylistBottomFragment.show(parentFragmentManager, "mylist")
        }
        viewBinding.fragmentNicovideoMenuAtodemiruButton.setOnClickListener {
            // あとで見るに追加する
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                // あとで見る追加APIを叩く
                val spMyListAPI = NicoVideoSPMyListAPI()
                val atodemiruResponse = spMyListAPI.addAtodemiruListVideo(userSession, videoId)
                if (!atodemiruResponse.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${atodemiruResponse.code}")
                    return@launch
                }
                // 成功したか
                when (atodemiruResponse.code) {
                    201 -> {
                        // 成功時
                        showToast(getString(R.string.atodemiru_ok))
                    }
                    200 -> {
                        // すでに追加済み
                        showToast(getString(R.string.already_added))
                    }
                    else -> {
                        // えらー
                        showToast(getString(R.string.error))
                    }
                }
            }
        }
    }

    // キャッシュボタン初期化
    private fun initCacheButton() {
        // キャッシュ
        if (isCache) {
            // キャッシュ取得ボタン塞ぐ
            viewBinding.fragmentNicovideoMenuGetCacheButton.visibility = View.GONE
            viewBinding.fragmentNicovideoMenuGetCacheEcoButton.visibility = View.GONE
            // キャッシュ（動画情報、コメント）再取得ボタン表示
            viewBinding.fragmentNicovideoMenuReGetCacheButton.visibility = View.VISIBLE
        } else {
            viewBinding.fragmentNicovideoMenuReGetCacheButton.visibility = View.GONE
        }
        // 取得
        viewBinding.fragmentNicovideoMenuGetCacheButton.setOnClickListener {
            if (!isCache) {
                // キャッシュ取得サービス起動
                startCacheService(context, videoId)
            }
        }
        // ログインするかはService側に書いてあるので。。。
        viewBinding.fragmentNicovideoMenuGetCacheEcoButton.setOnClickListener {
            if (!isCache) {
                // キャッシュ取得サービス起動
                startCacheService(context, videoId)
            }
        }
    }

    // 再取得ボタン初期化
    private fun initReGetButton() {
        val nicoVideoCache =
            NicoVideoCache(context)
        // インターネットに繋がってなければ非表示
        if (!isConnectionInternet(context)) {
            viewBinding.fragmentNicovideoMenuReGetCacheButton.isVisible = false
        }
        // 動画IDじゃない場合も非表示
        if (!nicoVideoCache.checkVideoId(videoId)) {
            viewBinding.fragmentNicovideoMenuReGetCacheButton.isVisible = false
        }
        viewBinding.fragmentNicovideoMenuReGetCacheButton.setOnClickListener {
            lifecycleScope.launch {
                nicoVideoCache.getReGetVideoInfoComment(videoId, userSession, context)
            }
        }
    }


    // 画質変更ボタン初期化
    private fun initQualityButton() {
        // キャッシュ再生時またはキャッシュ優先再生時は非表示
        if (isCache) {
            viewBinding.fragmentNicovideoMenuQualityButton.visibility = View.GONE
        } else {
            viewBinding.fragmentNicovideoMenuQualityButton.setOnClickListener {
                // 画質変更BottomSheet
                val qualityBottomFragment = NicoVideoQualityBottomFragment()
                qualityBottomFragment.show(parentFragmentManager, "quality")
            }
        }
    }


    // 共有
    private fun initShare() {
        // 写真付き共有
        viewBinding.fragmentNicovideoMenuShareMediaAttachButton.setOnClickListener {
            lifecycleScope.launch {
                requireNicoVideoFragment().apply {
                    // 再生時間も載せる
                    val currentPos = exoPlayer.currentPosition
                    val currentTime = DateUtils.formatElapsedTime(currentPos)
                    // 共有
                    contentShare.showShareContentAttachPicture(
                        playerView = viewBinding.fragmentNicovideoSurfaceView,
                        commentCanvas = viewBinding.fragmentNicovideoCommentCanvas,
                        contentId = videoId,
                        contentTitle = this.viewModel.nicoVideoData.value?.title,
                        fromTimeSecond = null,
                        message = currentTime
                    )
                }
            }
        }
        // 共有
        viewBinding.fragmentNicovideoMenuShareButton.setOnClickListener {
            requireNicoVideoFragment().apply {
                // 再生時間も載せる
                val currentPos = exoPlayer.currentPosition
                val currentTime = DateUtils.formatElapsedTime(currentPos)
                // 共有
                contentShare.showShareContent(
                    programId = videoId,
                    programName = this.viewModel.nicoVideoData.value?.title,
                    fromTimeSecond = null,
                    uri = null,
                    message = currentTime
                )
            }
        }
    }

    // 音量コントロール
    private fun initVolumeControl() {
        // 音量
        viewBinding.fragmentNicovideoMenuVolumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                requireNicoVideoFragment().exoPlayer.volume = (progress.toFloat() / 10)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        viewBinding.fragmentNicovideoMenuVolumeSeek.progress = (requireNicoVideoFragment().exoPlayer.volume * 10).toInt()
    }

    // 再生速度コントロール
    private fun initPlaybackSpeedSlider() {
        viewBinding.fragmentNicovideoMenuPlaybackSpeedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val progressValue = if (progress >= 7) {
                     2.0f // progress.toFloat() * 0.25f + 0.25f = 2.0f
                } else {
                    progress.toFloat() * 0.25f // 0.25刻みで0.25~1.5まで
                }
                // requireNicoVideoFragment().exoPlayer.setPlaybackSpeed(progressValue)
                viewModel.playbackSpeedControlLiveData.postValue(progressValue)
                viewBinding.fragmentNicovideoMenuPlaybackSpeedText.text = progressValue.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        val playbackSpeed = requireNicoVideoFragment().exoPlayer.playbackParameters.speed
        viewBinding.fragmentNicovideoMenuPlaybackSpeedSeek.progress = (playbackSpeed * 4).toInt()
        viewBinding.fragmentNicovideoMenuPlaybackSpeedText.text = playbackSpeed.toString()
    }


    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}