package com.kusamaru.standroid.nicolive.compose

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.button.MaterialButton
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.IncludeNicoliveEnquateBinding
import com.kusamaru.standroid.databinding.IncludeNicolivePlayerBinding
import com.kusamaru.standroid.fragment.PlayerBaseFragment
import com.kusamaru.standroid.nicolive.CommentRoomFragment
import com.kusamaru.standroid.nicolive.CommentViewFragment
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.nicolive.viewmodel.factory.NicoLiveViewModelFactory
import com.kusamaru.standroid.nicovideo.compose.DarkColors
import com.kusamaru.standroid.nicovideo.compose.LightColors
import com.kusamaru.standroid.service.startLivePlayService
import com.kusamaru.standroid.tool.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ニコ生再生Fragment。一部Jetpack Composeで作る。新UI
 *
 * 一部の関数は[PlayerBaseFragment]に実装しています
 *
 * 入れてほしいもの
 *
 * liveId       | String | 番組ID
 * watch_mode   | String | 現状 comment_post のみ
 * */
class JCNicoLiveFragment : PlayerBaseFragment() {

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** プレイヤー部分のUI */
    private val nicolivePlayerUIBinding by lazy { IncludeNicolivePlayerBinding.inflate(layoutInflater) }

    /** そうですね、やっぱり僕は、王道を征く、ExoPlayerですか */
    private val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    /** 共有 */
    private val contentShare by lazy { ContentShareTool(requireContext()) }

    /** レイアウト変更コールバック */
    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    /** ViewModel初期化。ネットワークとかUI関係ないやつはこっちに書いていきます。 */
    val viewModel by lazy {
        val liveId = arguments?.getString("liveId")!!
        ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, true)).get(NicoLiveViewModel::class.java)
    }

    @ExperimentalAnimationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // プレイヤー追加など
        setPlayerUI()

        // コメント描画設定。フォント設定など
        setCommentCanvas()

        // LiveData監視
        setLiveData()

        // アスペクト比調整
        setOnLayoutChangeAspectRatioFix()

        // Fragment設置
        setFragment()

        // コメント送信用UI（Jetpack Compose）設定
        setCommentPostUI()

        // 累計来場者、コメント投稿数、アクテイブ人数表示UI設定
        setStatisticsUI()

        // プレイヤーUI
        setPlayerUICompose()

        // スリープにしない
        caffeine()

    }

    private fun setPlayerUICompose() {
        nicolivePlayerUIBinding.includeNicolivePlayerComposeView.setContent {
            MaterialTheme(
                // ダークモード。動的にテーマ変更できるようになるんか？
                colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
            ) {
                // 番組情報
                val programData = viewModel.nicoLiveProgramData.observeAsState()
                // 経過時間
                val currentPosSec = viewModel.programCurrentPositionSecLiveData.observeAsState(initial = 0)
                // 番組の期間（放送時間）
                val duration = viewModel.programDurationTimeLiveData.observeAsState(initial = 0)
                // ミニプレイヤーかどうか
                val isMiniPlayerMode = viewModel.isMiniPlayerMode.observeAsState(false)
                // コメント描画
                val isShowDrawComment = remember { mutableStateOf(nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.isVisible) }
                // TS再生中？
                val isWatchingTS = viewModel.isWatchingTimeShiftLiveData.observeAsState(initial = false)

                if (programData.value != null) {
                    NicoLivePlayerUI(
                        liveTitle = programData.value!!.title,
                        liveId = programData.value!!.programId,
                        isMiniPlayer = isMiniPlayerMode.value,
                        isDisableMiniPlayerMode = isDisableMiniPlayerMode,
                        isFullScreen = viewModel.isFullScreenMode,
                        isConnectedWiFi = isConnectionWiFiInternet(requireContext()),
                        isShowCommentCanvas = isShowDrawComment.value,
                        isAudioOnlyMode = viewModel.currentQuality == "audio_high",
                        isTimeShiftMode = isWatchingTS.value,
                        onClickMiniPlayer = {
                            when {
                                isDisableMiniPlayerMode -> finishFragment()
                                isMiniPlayerMode.value -> toDefaultPlayer()
                                else -> toMiniPlayer()
                            }
                        },
                        onClickFullScreen = { if (viewModel.isFullScreenMode) setDefaultScreen() else setFullScreen() },
                        onClickNetwork = { showNetworkTypeMessage() },
                        onClickCommentDraw = {
                            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.isVisible = !nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.isVisible
                            isShowDrawComment.value = nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.isVisible
                        },
                        onClickPopUpPlayer = {
                            startLivePlayService(
                                context = requireContext(),
                                mode = "popup",
                                liveId = programData.value!!.programId,
                                isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                                startQuality = viewModel.currentQuality
                            )
                            finishFragment()
                        },
                        onClickBackgroundPlayer = {
                            startLivePlayService(
                                context = requireContext(),
                                mode = "background",
                                liveId = programData.value!!.programId,
                                isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                                startQuality = viewModel.currentQuality
                            )
                            finishFragment()
                        },
                        onClickCommentPost = { comment -> viewModel.sendComment(comment) },
                        currentPosition = currentPosSec.value,
                        duration = duration.value,
                        onTsSeek = { viewModel.tsSeekPosition(it) } // TS再生時のシークバー
                    )
                }
            }
        }
    }

    /** Jetpack Composeで作成したコメント投稿UIを追加する */
    private fun setCommentPostUI() {
        // コメント一覧展開ボタンを設置する
        bottomComposeView.setContent {
            // コメント展開するかどうか
            val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
            // コメント本文
            val commentPostText = remember { mutableStateOf("") }
            // 匿名で投稿するか
            val isTokumeiPost = remember { mutableStateOf(viewModel.nicoLiveHTML.isPostTokumeiComment) }
            // 文字の大きさ
            val commentSize = remember { mutableStateOf("medium") }
            // 文字の位置
            val commentPos = remember { mutableStateOf("naka") }
            // 文字の色
            val commentColor = remember { mutableStateOf("white") }
            // 複数行コメントを許可している場合はtrue。falseならEnterキーでコメント送信
            val isAcceptMultiLineComment = !prefSetting.getBoolean("setting_enter_post", true)
            // タイムシフト視聴中はテキストボックス出さない
            val isTimeShiftWatching = viewModel.isWatchingTimeShiftLiveData.observeAsState(initial = false)

            NicoLiveCommentInputButton(
                onClick = { viewModel.commentListShowLiveData.postValue(!isComment.value) },
                isComment = isComment.value,
                comment = commentPostText.value,
                onCommentChange = { commentPostText.value = it },
                onPostClick = {
                    // コメント投稿
                    viewModel.sendComment(commentPostText.value, commentColor.value, commentSize.value, commentPos.value)
                    commentPostText.value = "" // クリアに
                },
                position = commentPos.value,
                size = commentSize.value,
                color = commentColor.value,
                isTimeShiftMode = isTimeShiftWatching.value,
                onPosValueChange = { commentPos.value = it },
                onSizeValueChange = { commentSize.value = it },
                onColorValueChange = { commentColor.value = it },
                is184 = isTokumeiPost.value,
                onTokumeiChange = {
                    // 匿名、生ID切り替わった時
                    isTokumeiPost.value = !isTokumeiPost.value
                    prefSetting.edit { putBoolean("nicolive_post_tokumei", it) }
                    viewModel.nicoLiveHTML.isPostTokumeiComment = it
                },
                isMultiLine = isAcceptMultiLineComment,
            )
        }

    }

    private fun setStatisticsUI() {
        // 累計情報。来場者、コメント数などを表示するCompose
        fragmentCommentHostTopComposeView.apply {
            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                ) {
                    Surface {
                        // 統計情報LiveData
                        val statisticsLiveData = viewModel.statisticsLiveData.observeAsState()
                        // アクティブ人数
                        val activeCountLiveData = viewModel.activeCommentPostUserLiveData.observeAsState()

                        NicoLiveStatisticsUI(
                            allViewer = statisticsLiveData.value?.viewers ?: 0,
                            allCommentCount = statisticsLiveData.value?.comments ?: 0,
                            activeCountText = activeCountLiveData.value ?: "計測中",
                            onClickRoomChange = {
                                // Fragment切り替え
                                val toFragment = when (childFragmentManager.findFragmentById(fragmentCommentHostFrameLayout.id)) {
                                    is CommentRoomFragment -> CommentViewFragment()
                                    else -> CommentRoomFragment()
                                }
                                childFragmentManager.beginTransaction().replace(fragmentCommentHostFrameLayout.id, toFragment).commit()
                            },
                            onClickActiveCalc = {
                                // アクティブ人数計算
                                viewModel.calcToukei(true)
                            }
                        )
                    }
                }
            }
        }
    }

    /** Fragment設置 */
    private fun setFragment() {
        // 動画情報Fragment、コメントFragment設置
        childFragmentManager.beginTransaction().replace(fragmentHostFrameLayout.id, JCNicoLiveInfoFragment()).commit()
        childFragmentManager.beginTransaction().replace(fragmentCommentHostFrameLayout.id, CommentViewFragment()).commit()
        // ダークモード
        fragmentCommentLinearLayout.background = ColorDrawable(getThemeColor(requireContext()))
        // コメント一覧Fragmentを表示するかどうかのやつ
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

    /** LiveData監視 */
    private fun setLiveData() {
        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                playerFrameLayout.post {
                    toMiniPlayer() // これ直したい
                }
            }
        }
        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                "finish" -> finishFragment()
            }
        }
        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            showSnackBar(it, null, null)
        }
        // 新ニコニコ実況の番組と発覚した場合
        viewModel.isNicoJKLiveData.observe(viewLifecycleOwner) { nicoJKId ->
            // 映像を受信しないモードをtrueへ
            viewModel.isNotReceiveLive.postValue(true)
            // 通常画面へ
            playerFrameLayout.post { toDefaultPlayer() }
            // コメント一覧も表示
            lifecycleScope.launch {
                delay(1000)
                showSnackBar(getString(R.string.nicolive_jk_not_live_receive), null, null)
                if (!viewModel.isFullScreenMode && !viewModel.isAutoCommentListShowOff) {
                    // フルスクリーン時 もしくは 自動で展開しない場合 は操作しない
                    viewModel.commentListShowLiveData.postValue(true)
                }
            }
        }
        // 映像を受信しないモード。映像なしだと3分で620KBぐらい？
        viewModel.isNotReceiveLive.observe(viewLifecycleOwner) { isNotReceiveLive ->
            if (isNotReceiveLive) {
                // 背景真っ暗へ
                nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
                exoPlayer.stop()
            } else {
                // 生放送再生
                viewModel.hlsAddressLiveData.value?.let { playExoPlayer(it) }
            }
        }
        // うんこめ
        viewModel.unneiCommentLiveData.observe(viewLifecycleOwner) { unnkome ->
            showInfoOrUNEIComment(CommentJSONParse(unnkome, getString(R.string.room_integration), viewModel.liveIdOrCommunityId).comment)
        }
        // あんけーと
        viewModel.startEnquateLiveData.observe(viewLifecycleOwner) { enquateList ->
            setStartEnquateLayout(enquateList)
        }
        viewModel.openEnquateLiveData.observe(viewLifecycleOwner) { perList ->
            setResultEnquateLayout(perList)
        }
        viewModel.stopEnquateLiveData.observe(viewLifecycleOwner) { stop ->
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
        }
        // HLSアドレス取得
        viewModel.hlsAddressLiveData.observe(viewLifecycleOwner) { address ->
            // ニコ生版ニコニコ実況の場合 と 映像を受信しないモード 以外なら映像を流す
            if (viewModel.isNicoJKLiveData.value == null && viewModel.isNotReceiveLive.value == false) {
                playExoPlayer(address)
            }
        }
        // 音量調整LiveData
        viewModel.exoplayerVolumeLiveData.observe(viewLifecycleOwner) { volume ->
            exoPlayer.volume = volume
        }
        viewModel.isUseNicoNamaWebView.observe(viewLifecycleOwner) {
            if (it) {
                setNicoNamaWebView()
            } else {
                removeNicoNamaWebView()
            }
        }
        // 画質変更
        viewModel.changeQualityLiveData.observe(viewLifecycleOwner) { quality ->
            showSnackBar("${getString(R.string.successful_quality)}\n→${quality}", null, null)
        }
        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { commentJSONParse ->
            // 豆先輩とか
            if (!commentJSONParse.comment.contains("\n")) {
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
            } else {
                // https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
                // 豆先輩！！！！！！！！！！！！！！！！！！
                // 下固定コメントで複数行だとAA（アスキーアートの略 / CA(コメントアート)とも言う）がうまく動かない。配列の中身を逆にする必要がある
                // Kotlinのこの書き方ほんと好き
                val asciiArtComment = if (commentJSONParse.mail.contains("shita")) {
                    commentJSONParse.comment.split("\n").reversed() // 下コメントだけ逆順にする
                } else {
                    commentJSONParse.comment.split("\n")
                }
                // 複数行対応Var
                nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
            }
        }
    }

    /** ニコ生WebViewを削除 */
    private fun removeNicoNamaWebView() {
        nicolivePlayerUIBinding.includeNicolivePlayerWebviewFrameLayout.removeAllViews()
    }

    /** ニコ生WebViewを用意 */
    private fun setNicoNamaWebView() {
        if (viewModel.nicoLiveProgramData.value != null) {
            val webView = WebView(requireContext())
            NicoNamaGameWebViewTool.init(webView, viewModel.nicoLiveProgramData.value!!.programId, false)
            nicolivePlayerUIBinding.includeNicolivePlayerWebviewFrameLayout.addView(webView)
        }
    }

    /** アンケート開票のUIをセットする */
    private fun setResultEnquateLayout(perList: List<String>) {
        // 選択肢：パーセンテージのPairを作成する
        if (viewModel.startEnquateLiveData.value != null) {
            // 選択肢
            val enquateList = viewModel.startEnquateLiveData.value!!.drop(1)
            // Pair作成 + Button作成
            val buttonList = perList.mapIndexed { index, percent ->
                MaterialButton(requireContext()).apply {
                    // テキスト
                    text = "${enquateList[index]}\n$percent"
                    // Paddingとか
                    val linearLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    linearLayoutParams.weight = 1F
                    linearLayoutParams.setMargins(10, 10, 10, 10)
                    layoutParams = linearLayoutParams
                }
            }
            // アンケ用レイアウト読み込み
            val nicoliveEnquateLayout = IncludeNicoliveEnquateBinding.inflate(layoutInflater)
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
            nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.addView(nicoliveEnquateLayout.root)
            nicoliveEnquateLayout.enquateTitle.text = enquateList[0] // 0番目はアンケタイトル
            // ボタン配置
            buttonList.forEachIndexed { index, materialButton ->
                when {
                    index in 0..2 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                    index in 3..5 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                    index in 6..8 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                }
            }
            // アンケ結果共有Snackbar
            val shareText = perList.mapIndexed { index, percent -> "${enquateList[index]}\n$percent" }.joinToString(separator = "\n")
            showSnackBar(getString(R.string.enquate_result), getString(R.string.share)) {
                ShareCompat.IntentBuilder.from(requireActivity()).apply {
                    setChooserTitle(viewModel.startEnquateLiveData.value!![0])
                    setSubject(shareText)
                    setText(shareText)
                    setType("text/plain")
                }.startChooser()
            }
        }
    }

    /** アンケート開始のUIをセットする。引数の配列は0番目がタイトル、それ以降がアンケートの選択肢 */
    private fun setStartEnquateLayout(enquateList: List<String>) {
        // 何回目か教えてくれるmapIndexed
        val buttonList = enquateList
            .drop(1)
            .mapIndexed { i, enquate ->
                MaterialButton(requireContext()).apply {
                    // テキスト
                    text = enquate
                    // Paddingとか
                    val linearLayoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    linearLayoutParams.weight = 1F
                    linearLayoutParams.setMargins(10, 10, 10, 10)
                    layoutParams = linearLayoutParams
                    setOnClickListener {
                        // 投票
                        viewModel.enquatePOST(i - 1)
                        // アンケ画面消す
                        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
                        // Snackbar
                        showSnackBar("${getString(R.string.enquate)}：$enquate", null, null)
                    }
                }
            }
        // アンケ用レイアウト読み込み
        val nicoliveEnquateLayout = IncludeNicoliveEnquateBinding.inflate(layoutInflater)
        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.removeAllViews()
        nicolivePlayerUIBinding.includeNicolivePlayerEnquateFrameLayout.addView(nicoliveEnquateLayout.root)
        nicoliveEnquateLayout.enquateTitle.text = enquateList[0] // 0番目はアンケタイトル
        // ボタン配置
        buttonList.forEachIndexed { index, materialButton ->
            when {
                index in 0..2 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                index in 3..5 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
                index in 6..8 -> nicoliveEnquateLayout.enquateLinearlayout1.addView(materialButton)
            }
        }
    }


    /**
     * Info（ニコニ広告、ランクイン等）と、運営コメントを表示する関数
     * */
    private fun showInfoOrUNEIComment(comment: String) {
        val isNicoad = comment.contains("/nicoad")
        val isInfo = comment.contains("/info")
        val isUadPoint = comment.contains("/uadpoint")
        val isSpi = comment.contains("/spi")
        val isGift = comment.contains("/gift")
        // エモーション。いらない
        val isHideEmotion = prefSetting.getBoolean("setting_nicolive_hide_emotion", false)
        val isEmotion = comment.contains("/emotion")
        // アニメーション
        val infoAnim = nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.toDropPopAlert() // 自作ライブラリ
        val uneiAnim = nicolivePlayerUIBinding.includeNicolivePlayerUneiCommentTextView.toDropPopAlert()
        when {
            isInfo || isUadPoint -> {
                // info
                val message = comment.replace("/info \\d+ ".toRegex(), "")
                nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                infoAnim.alert(DropPopAlert.ALERT_UP)
            }
            isNicoad -> {
                // 広告
                val json = JSONObject(comment.replace("/nicoad ", ""))
                val message = json.getString("message")
                nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                infoAnim.alert(DropPopAlert.ALERT_UP)
            }
            isSpi -> {
                // ニコニコ新市場
                val message = comment.replace("/spi ", "")
                nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                infoAnim.alert(DropPopAlert.ALERT_UP)
            }
            isGift -> {
                // 投げ銭。スペース区切り配列
                val list = comment.replace("/gift ", "").split(" ")
                val userName = list[2]
                val giftPoint = list[3]
                val giftName = list[5]
                val message = "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                infoAnim.alert(DropPopAlert.ALERT_UP)
            }
            isEmotion && !isHideEmotion -> {
                // エモーション
                val message = comment.replace("/emotion ", "エモーション：")
                nicolivePlayerUIBinding.includeNicolivePlayerInfoCommentTextView.text = message
                infoAnim.alert(DropPopAlert.ALERT_UP)
            }
            else -> {
                // 生主コメント表示
                nicolivePlayerUIBinding.includeNicolivePlayerUneiCommentTextView.text = HtmlCompat.fromHtml(comment, HtmlCompat.FROM_HTML_MODE_COMPACT)
                uneiAnim.alert(DropPopAlert.ALERT_DROP)
            }
        }
    }

    /** ExoPlayerで生放送を再生する */
    private fun playExoPlayer(address: String) {
        // ExoPlayer
        // 音声のみの再生はSurfaceViewを暗黒へ。わーわー言うとりますが、お時間でーす
        if (viewModel.currentQuality == "audio_high") {
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = ColorDrawable(Color.BLACK)
        } else {
            nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.background = null
        }
        // HLS受け取り
        val mediaItem = MediaItem.fromUri(address.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        // SurfaceView
        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
        // 再生
        exoPlayer.playWhenReady = true
        // ミニプレイヤーから通常画面へ遷移
        var isFirst = true
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                // タイムシフト再生時は再生時間をViewModelでカウントするので再生状態をViewModelにわたす
                val isPlaying = (state == Player.STATE_READY || state == Player.STATE_ENDED)
                viewModel.isTimeShiftPlaying.postValue((viewModel.isWatchingTimeShiftLiveData.value == true) && isPlaying)

                // 一度だけ
                if (isFirst) {
                    isFirst = false
                    // 通常画面へ。なおこいつのせいで画面回転前がミニプレイヤーでもミニプレイヤーにならない
                    // toDefaultPlayer()
                    // コメント一覧も表示
                    lifecycleScope.launch {
                        delay(1000)
                        if (!viewModel.isFullScreenMode && !viewModel.isAutoCommentListShowOff) {
                            // フルスクリーン時 もしくは 自動で展開しない場合 は操作しない
                            viewModel.commentListShowLiveData.postValue(true)
                        }
                    }
                } else {
                    exoPlayer.removeListener(this)
                }
            }
        })

        // もしエラー出たら
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                error.printStackTrace()
                println("生放送の再生が止まりました。")
                //再接続する？
                //それからニコ生視聴セッションWebSocketが切断されてなければ
                if (!viewModel.nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                    println("再度再生準備を行います")
                    activity?.runOnUiThread {
                        //再生準備
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        //SurfaceViewセット
                        exoPlayer.setVideoSurfaceView(nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView)
                        //再生
                        exoPlayer.playWhenReady = true
                        // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                        if (viewModel.nicoLiveHTML.isLowLatency) {
                            showSnackBar(getString(R.string.error_player), getString(R.string.low_latency_off)) {
                                // 低遅延OFFを送信
                                viewModel.nicoLiveHTML.sendLowLatency(!viewModel.nicoLiveHTML.isLowLatency)
                            }
                        } else {
                            showSnackBar(getString(R.string.error_player), null, null)
                        }
                    }
                }
            }
        })
    }

    /**
     * アスペクト比を治す。一度だけ呼べばいいです（レイアウト変更を検知して自動で変更するため）
     * */
    private fun setOnLayoutChangeAspectRatioFix() {
        if (!isAdded) return
        // 既存のコールバックは消す
        if (onGlobalLayoutListener != null) {
            fragmentPlayerFrameLayout.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        var prevHeight = 0
        onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val playerHeight = fragmentPlayerFrameLayout.height
            val playerWidth = fragmentPlayerFrameLayout.width
            if (prevHeight != playerHeight) {
                prevHeight = playerHeight
                val calcWidth = (playerHeight / 9) * 16
                if (calcWidth > fragmentPlayerFrameLayout.width) {
                    // 画面外にプレイヤーが行く
                    nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.updateLayoutParams {
                        width = playerWidth
                        height = (playerWidth / 16) * 9
                    }
                } else {
                    nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView.updateLayoutParams {
                        width = calcWidth
                        height = playerHeight
                    }
                }
            }
        }
        fragmentPlayerFrameLayout.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    /** コメント描画設定を適用 */
    private fun setCommentCanvas() {
        val font = CustomFont(requireContext())
        if (font.isApplyFontFileToCommentCanvas) {
            // フォント設定
            nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas.typeFace = font.typeface
        }
    }

    /** プレイヤーのUIをFragmentに追加する */
    private fun setPlayerUI() {
        // ここは動画と一緒
        addPlayerFrameLayout(nicolivePlayerUIBinding.root)
        // 全画面モードなら
        if (viewModel.isFullScreenMode) {
            setFullScreen()
        }
        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
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
            toDefaultScreen()
        }
    }

    override fun onBottomSheetStateChane(state: Int, isMiniPlayer: Boolean) {
        super.onBottomSheetStateChane(state, isMiniPlayer)
        // ViewModelへ状態通知
        viewModel.isMiniPlayerMode.value = isMiniPlayerMode()
    }

    /** 画像つき共有を行う */
    fun showShareSheetMediaAttach() {
        viewModel.nicoLiveProgramData.value?.apply {
            lifecycleScope.launch {
                contentShare.showShareContentAttachPicture(
                    playerView = nicolivePlayerUIBinding.includeNicolivePlayerSurfaceView,
                    commentCanvas = nicolivePlayerUIBinding.includeNicolivePlayerCommentCanvas,
                    contentId = programId,
                    contentTitle = title,
                    fromTimeSecond = null,
                )
            }
        }
    }

    /** テキストのみ共有を行う */
    fun showShareSheet() {
        viewModel.nicoLiveProgramData.value?.apply {
            contentShare.showShareContent(
                programId = programId,
                programName = title,
                fromTimeSecond = null
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        caffeineUnlock()
    }
}