package com.kusamaru.standroid.nicolive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import android.text.InputType
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.kusamaru.standroid.*
import com.kusamaru.standroid.databinding.FragmentNicoliveBinding
import com.kusamaru.standroid.databinding.IncludeNicoliveEnquateBinding
import com.kusamaru.standroid.googlecast.GoogleCast
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.nicolive.activity.FloatingCommentViewer
import com.kusamaru.standroid.nicolive.adapter.NicoLivePagerAdapter
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.nicolive.viewmodel.factory.NicoLiveViewModelFactory
import com.kusamaru.standroid.service.startLivePlayService
import com.kusamaru.standroid.tool.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.timerTask

/**
 * 生放送再生Fragment。
 * NicoLiveFragmentじゃなくてCommentFragmentになってるのはもともと全部屋見れるコメビュを作りたかったから
 * このFragmentを探す際はTagじゃなくてIDで頼む。
 *
 * BottomFragmentとかViewPager2の中のFragmentは[requireParentFragment]が使えるかもしれないから伝えておく。
 * BottomFragmentで[requireParentFragment]を使うときは、このFragmentの[getChildFragmentManager]を使って開く必要がある
 *
 * ひつようなもの
 * liveId       | String    | 番組ID か コミュID か チャンネルID。生放送ID以外が来る可能性もある
 * isOfficial   | Boolean   | 公式番組ならtrue。無くてもいいかも？
 *
 * */
class CommentFragment : Fragment(), MainActivityPlayerFragmentInterface {

    lateinit var commentActivity: AppCompatActivity
    lateinit var prefSetting: SharedPreferences
    lateinit var darkModeSupport: DarkModeSupport

    /** ユーザーセッション */
    var usersession = ""

    /** 番組ID */
    var liveId = ""

    /** コメント表示をOFFにする場合はtrue */
    var isCommentHide = false

    // アンケート内容いれとく
    var enquateJSONArray = ""

    // アンケートView
    lateinit var enquateView: View

    // 運コメ・infoコメント非表示
    var hideInfoUnnkome = false

    // 二窓モードになっている場合
    var isNimadoMode = false

    // ExoPlayer
    lateinit var exoPlayer: SimpleExoPlayer

    // GoogleCast使うか？
    lateinit var googleCast: GoogleCast

    // 公式かどうか
    var isOfficial = false

    // フォント変更機能
    lateinit var customFont: CustomFont

    // ニコ生ゲームが有効になっているか
    var isAddedNicoNamaGame = false

    // スワイプで画面切り替えるやつ
    lateinit var nicoLivePagerAdapter: NicoLivePagerAdapter

    /**
     * このFragmentからUI関係以外（インターネット接続とかデータベース追加とか）
     * はこっちに書いてある。
     * */
    lateinit var viewModel: NicoLiveViewModel

    /** findViewById駆逐。Kotlin Android Extensions 代替 */
    val viewBinding by lazy { FragmentNicoliveBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isNimadoMode = activity is NimadoActivity

        commentActivity = activity as AppCompatActivity

        darkModeSupport = DarkModeSupport(requireContext())
        darkModeSupport.setActivityTheme(activity as AppCompatActivity)

        //ダークモード対応
        applyViewThemeColor()

        // GoogleCast？
        googleCast = GoogleCast(requireContext())
        // GooglePlay開発者サービスがない可能性あり、Gapps焼いてない、ガラホ　など
        if (googleCast.isGooglePlayServicesAvailable()) {
            googleCast.init()
        }

        // 公式番組の場合はAPIが使えないため部屋別表示を無効にする。
        isOfficial = arguments?.getBoolean("isOfficial") ?: false

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        liveId = arguments?.getString("liveId") ?: ""

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()

        // アスペクト比なおす
        aspectRatioFix()

        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(commentActivity, lifecycle)
        }

        // ユーザーの設定したフォント読み込み
        customFont = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (customFont.isApplyFontFileToCommentCanvas) {
            viewBinding.commentFragmentCommentCanvas.typeFace = customFont.typeface
        }

        if (isNimadoMode) {
            MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, false)
        }

        viewBinding.fragmentNicoLiveFab.setOnClickListener {
            //表示アニメーションに挑戦した。
            val showAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_show_animation)
            //表示
            viewBinding.include.root.startAnimation(showAnimation)
            viewBinding.include.root.isVisible = true
            viewBinding.fragmentNicoLiveFab.hide()
            //コメント投稿など
            commentCardView()
        }

        // ステータスバー透明化＋タイトルバー非表示＋ノッチ領域にも侵略。関数名にAndがつくことはあんまりない
        hideStatusBarAndSetFullScreen()
        // なんかxmlが効かないので
        MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.include_nicolive_player_audio_only_text_view, View.GONE)

        // ログイン情報がなければ終了
        if (prefSetting.getString("mail", "")?.contains("") != false) {
            usersession = prefSetting.getString("user_session", "") ?: ""
            // ViewModel初期化
            viewModel = ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, true)).get(NicoLiveViewModel::class.java)
        } else {
            showToast(getString(R.string.mail_pass_error))
            finishFragment()
        }

        // 全画面再生時なら
        if (viewModel.isFullScreenMode) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            setFullScreen()
        }

        // MainActivityのBottomNavを表示させるか。二窓Activityのせいでas?にしている
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav()

        // アスペクト比直す。再生直前でもやってるけど
        aspectRatioFix()

        // ミニプレイヤー時なら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // MainActivityのBottomNavを表示させるか
            (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
            setMiniPlayer(isMiniPlayerMode)
            // アイコン直す
            val icon = when (viewBinding.commentFragmentMotionLayout.currentState) {
                R.id.comment_fragment_transition_end -> requireContext().getDrawable(R.drawable.ic_expand_less_black_24dp)
                else -> requireContext().getDrawable(R.drawable.ic_expand_more_24px)
            }
            viewBinding.commentFragmentControl.playerNicoliveControlBackButton.setImageDrawable(icon)
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                viewBinding.commentFragmentMotionLayout.transitionToState(R.id.comment_fragment_transition_end)
            }
        }

        // 統計情報は押したときに計算するようにした
        viewBinding.playerNicoliveControlStatistics.setOnClickListener {
            viewModel.calcToukei(true)
        }

        // SnackBar表示
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) { message ->
            multiLineSnackbar(viewBinding.playerNicoliveControlActiveText, message)
        }

        // Activity終了
        viewModel.messageLiveData.observe(viewLifecycleOwner) { message ->
            when (message) {
                "finish" -> finishFragment()
            }
        }

        // 番組情報
        viewModel.nicoLiveProgramData.observe(viewLifecycleOwner) { data ->
            // ViewPager初期化
            isOfficial = data.isOfficial
            initViewPager()
            // コントローラも
            initController(data)
            googleCast.apply {
                programTitle = data.title
                programSubTitle = data.programId
                programThumbnail = data.thum
            }
        }

        // 新ニコニコ実況の番組と発覚した場合
        viewModel.isNicoJKLiveData.observe(viewLifecycleOwner) { nicoJKId ->
            // バックグラウンド再生無いので非表示
            viewBinding.commentFragmentControl.playerNicoliveControlBackground.isVisible = false
            // 映像を受信しない。
            multiLineSnackbar(viewBinding.playerNicoliveControlActiveText, getString(R.string.nicolive_jk_not_live_receive))
            // 映像を受信しないモードをtrueへ
            viewModel.isNotReceiveLive.postValue(true)
        }

        // 映像を受信しないモード。映像なしだと3分で620KBぐらい？
        viewModel.isNotReceiveLive.observe(viewLifecycleOwner) { isNotReceiveLive ->
            if (isNotReceiveLive) {
                // 背景真っ暗へ
                viewBinding.commentFragmentSurfaceView.background = ColorDrawable(Color.BLACK)
                exoPlayer.release()
            } else {
                setPlayVideoView()
            }
        }

        // うんこめ
        viewModel.unneiCommentLiveData.observe(viewLifecycleOwner) { unnkome ->
            showInfoOrUNEIComment(CommentJSONParse(unnkome, getString(R.string.room_integration), liveId).comment)
        }

        // あんけーと
        viewModel.enquateLiveData.observe(viewLifecycleOwner) { enquateMessage ->
            showEnquate(enquateMessage)
        }

        // 統計情報
        viewModel.statisticsLiveData.observe(viewLifecycleOwner) { statistics ->
            viewBinding.playerNicoliveControlWatchCount.text = statistics.viewers.toString()
            viewBinding.playerNicoliveControlCommentCount.text = statistics.comments.toString()
        }

        // アクティブユーザー？
        viewModel.activeCommentPostUserLiveData.observe(viewLifecycleOwner) { active ->
            viewBinding.playerNicoliveControlActiveText.text = active
            viewBinding.playerNicoliveControlStatistics.isVisible = true
        }

        // 経過時間
        viewModel.programCurrentPositionSecLiveData.observe(viewLifecycleOwner) { currentPosSec ->
            viewBinding.playerNicoliveControlTime.text = TimeFormatTool.timeFormat(currentPosSec)
        }

        // 場組の期間
        viewModel.programDurationTimeLiveData.observe(viewLifecycleOwner) { duration ->
            viewBinding.playerNicoliveControlEndTime.text = TimeFormatTool.timeFormat(duration)
        }

        // HLSアドレス取得
        viewModel.hlsAddressLiveData.observe(viewLifecycleOwner) { address ->
            if (viewModel.isCommentOnlyMode) {
                setCommentOnlyMode(true)
            } else {
                setPlayVideoView()
                googleCast.apply {
                    hlsAddress = address
                    resume()
                }
            }
        }

        // 画質変更
        viewModel.changeQualityLiveData.observe(viewLifecycleOwner) { quality ->
            showQualityChangeSnackBar(viewModel.currentQuality)
        }

        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { commentJSONParse ->
            // コメント非表示モードの場合はなさがない
            if (!isCommentHide) {
                // 豆先輩とか
                if (!commentJSONParse.comment.contains("\n")) {
                    viewBinding.commentFragmentCommentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
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
                    viewBinding.commentFragmentCommentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // コメントのみの表示の際はFragment終了
            if (viewModel.isCommentOnlyMode) {
                finishFragment()
            } else {
                viewBinding.commentFragmentMotionLayout.apply {
                    if (currentState == R.id.comment_fragment_transition_end) {
                        // 終了へ
                        transitionToState(R.id.comment_fragment_transition_finish)
                    } else {
                        transitionToState(R.id.comment_fragment_transition_end)
                    }
                }
            }
        }

    }

    /**
     * コメントのみの表示に切り替える関数
     * @param isCommentOnly コメントのみの表示にする場合
     * */
    fun setCommentOnlyMode(isCommentOnly: Boolean) {
        viewModel.isCommentOnlyMode = isCommentOnly
        exoPlayer.stop()
        if (isCommentOnly) {
            MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, false)
            MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.comment_fragment_surface_view, View.GONE)
        } else {
            MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, true)
            MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.comment_fragment_surface_view, View.VISIBLE)
            setPlayVideoView()
        }
    }

    /** ダークモード等テーマに合わせた色を設定する */
    private fun applyViewThemeColor() {
        viewBinding.activityCommentTabLayout.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.commentFragmentAppBar.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.commentViewpager.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.commentFragmentBackground.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.playerNicoliveControlInfoMain.background = ColorDrawable(getThemeColor(requireContext()))
        viewBinding.commentFragmentBackground.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /** コントローラーを初期化する。HTML取得後にやると良さそう */
    private fun initController(data: NicoLiveProgramData) {
        // クリックイベントを通過させない
        val job = Job()
        // 最小化するとかしないとか
        viewBinding.commentFragmentControl.playerNicoliveControlBackButton.isVisible = true
        viewBinding.commentFragmentControl.playerNicoliveControlBackButton.setOnClickListener {
            when {
                viewModel.isFullScreenMode -> {
                    setCloseFullScreen()
                }
                viewBinding.commentFragmentMotionLayout.currentState == R.id.comment_fragment_transition_start -> {
                    viewBinding.commentFragmentMotionLayout.transitionToState(R.id.comment_fragment_transition_end)
                }
                else -> {
                    viewBinding.commentFragmentMotionLayout.transitionToState(R.id.comment_fragment_transition_start)
                }
            }
        }
        // 番組情報
        viewBinding.commentFragmentControl.playerNicoliveControlTitle.text = data.title
        viewBinding.commentFragmentControl.playerNicoliveControlId.text = data.programId
        // Marqueeを有効にするにはフォーカスをあてないといけない？。<marquee>とかWeb黎明期感ある（その時代の人じゃないけど）
        viewBinding.commentFragmentControl.playerNicoliveControlTitle.isSelected = true
        // 全画面/ポップアップ/バッググラウンド
        viewBinding.commentFragmentControl.playerNicoliveControlPopup.setOnClickListener { startPlayService("popup") }
        viewBinding.commentFragmentControl.playerNicoliveControlBackground.setOnClickListener { startPlayService("background") }
        viewBinding.commentFragmentControl.playerNicoliveControlFullscreen.setOnClickListener {
            if (viewModel.isFullScreenMode) {
                // 全画面終了
                setCloseFullScreen()
            } else {
                // 全画面移行
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                setFullScreen()
            }
        }
        // 統計情報表示
        viewBinding.commentFragmentStatisticsShow?.setOnClickListener {
            viewBinding.playerNicoliveControlInfoMain.isVisible = !viewBinding.playerNicoliveControlInfoMain.isVisible
        }
        // 横画面なら常に表示
        if (!viewModel.isFullScreenMode && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible = true
        }
        // 接続方法
        viewBinding.commentFragmentControl.playerNicoliveControlVideoNetwork.apply {
            setImageDrawable(InternetConnectionCheck.getConnectionTypeDrawable(requireContext()))
            setOnClickListener {
                showToast(InternetConnectionCheck.createNetworkMessage(requireContext()))
            }
        }

        // MotionLayout関係
        viewBinding.commentFragmentMotionlayoutParentFramelayout.apply {
            allowIdList.add(R.id.comment_fragment_transition_start) // 通常状態（コメント表示など）は無条件でタッチを渡す。それ以外はプレイヤー部分のみタッチ可能
            allowIdList.add(R.id.comment_fragment_transition_fullscreen) // フルスクリーン時もクリックが行かないように
            swipeTargetView = viewBinding.commentFragmentControl.root
            motionLayout = viewBinding.commentFragmentMotionLayout
            // プレイヤーを押した時。普通にsetOnClickListenerとか使うと競合して動かなくなる
            onSwipeTargetViewClickFunc = { event ->
                viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible = !viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible
                // フルスクリーン時はFabも消す
                if (viewModel.isFullScreenMode) {
                    if (viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible) viewBinding.fragmentNicoLiveFab.show() else viewBinding.fragmentNicoLiveFab.hide()
                }
                updateHideController(job)
            }
            // swipeTargetViewの上にあるViewをここに書く。ここに書いたViewを押した際はonSwipeTargetViewClickFuncが呼ばれなくなる(View#setOnClickListenerは呼ばれる)
            addAllIsClickableViewFromParentView(viewBinding.commentFragmentControl.playerNicoliveControlMain)
            // blockViewListに追加したViewが押さてたときに共通で行いたい処理などを書く
            onBlockViewClickFunc = { view, event ->
                // UI非表示なら表示。なんかnullになる
                if (!viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible) {
                    onSwipeTargetViewClickFunc?.invoke(null)
                } else {
                    //
                }
            }
        }
        viewBinding.commentFragmentMotionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {

            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                // 終了アニメーション
                if (p1 == R.id.comment_fragment_transition_finish) {
                    // Fragment終了へ
                    parentFragmentManager.beginTransaction().remove(this@CommentFragment).commit()
                } else {
                    // ここどうする？
                    val isMiniPlayerMode = isMiniPlayerMode()
                    viewModel.isMiniPlayerMode.value = isMiniPlayerMode
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

            }
        })
        updateHideController(job)
    }

    /**
     * ミニプレイヤー用UIを有効/無効にする関数
     * @param isMiniPlayerMode 有効にする場合はtrue。通常に戻す場合はfalse
     * */
    private fun setMiniPlayer(isMiniPlayerMode: Boolean) {
        listOf(
            viewBinding.commentFragmentControl.playerNicoliveControlVideoNetwork,
            viewBinding.commentFragmentControl.playerNicoliveControlPopup,
            viewBinding.commentFragmentControl.playerNicoliveControlBackground,
            viewBinding.commentFragmentControl.playerNicoliveControlFullscreen,
        ).forEach { view ->
            // 三種の神器。これするとMotionLayoutがうまく動く？
            view.isEnabled = !isMiniPlayerMode
            view.isClickable = !isMiniPlayerMode
            view.isVisible = !isMiniPlayerMode
        }
    }

    /** コントローラーを消すためのコルーチン。 */
    private fun updateHideController(job: Job) {
        job.cancelChildren()
        // Viewを数秒後に非表示するとか
        lifecycleScope.launch(job) {
            // Viewを数秒後に消す
            delay(3000)
            if (viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible) {
                viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible = false
                // フルスクリーン時はFabも消す
                if (viewModel.isFullScreenMode) {
                    if (viewBinding.fragmentNicoLiveFab.isShown) viewBinding.fragmentNicoLiveFab.hide() else viewBinding.fragmentNicoLiveFab.show()
                }
            }
        }
    }

    /**
     * フルスクリーン再生。
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        // 全画面だよ
        viewModel.isFullScreenMode = true
        // コメビュ非表示
        viewBinding.commentFragmentMotionLayout.transitionToState(R.id.comment_fragment_transition_fullscreen)
        // 経過時間消す
        viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible = false
        // システムバー非表示
        setSystemBarVisibility(false)
        // 背景色
        viewBinding.commentFragmentBackground.background = ColorDrawable(Color.BLACK)
        // アイコン変更
        viewBinding.commentFragmentControl.playerNicoliveControlFullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // 高さ更新
        viewBinding.commentFragmentCommentCanvas.finalHeight = viewBinding.commentFragmentCommentCanvas.height
    }

    /**
     * 全画面解除
     * */
    private fun setCloseFullScreen() {
        // 全画面ではない
        viewModel.isFullScreenMode = false
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // コメビュ表示
        viewBinding.commentFragmentMotionLayout.transitionToState(R.id.comment_fragment_transition_start)
        // 経過時間出す
        viewBinding.commentFragmentControl.playerNicoliveControlMain.isVisible = true
        // システムバー表示
        setSystemBarVisibility(true)
        // アイコン変更
        viewBinding.commentFragmentControl.playerNicoliveControlFullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // 背景色もどす
        viewBinding.commentFragmentBackground.background = ColorDrawable(getThemeColor(context))
        // 高さ更新
        viewBinding.commentFragmentCommentCanvas.finalHeight = viewBinding.commentFragmentCommentCanvas.height
    }

    /**
     * Info（ニコニ広告、ランクイン等）と、運営コメントを表示する関数
     * */
    private fun showInfoOrUNEIComment(comment: String) {
        requireActivity().runOnUiThread {
            val isNicoad = comment.contains("/nicoad")
            val isInfo = comment.contains("/info")
            val isUadPoint = comment.contains("/uadpoint")
            val isSpi = comment.contains("/spi")
            val isGift = comment.contains("/gift")
            // エモーション。いらない
            val isHideEmotion = prefSetting.getBoolean("setting_nicolive_hide_emotion", false)
            val isEmotion = comment.contains("/emotion")
            // アニメーション
            val infoAnim = viewBinding.commentFragmentInfoCommentTextview.toDropPopAlertMotionLayoutFix().also { anim ->
//                anim.motionLayout = viewBinding.commentFragmentMotionLayout
            }
            val uneiAnim = viewBinding.commentFragmentUneiCommentTextview.toDropPopAlertMotionLayoutFix().also { anim ->
//                anim.motionLayout = viewBinding.commentFragmentMotionLayout
            }
            when {
                isInfo || isUadPoint -> {
                    // info
                    val message = comment.replace("/info \\d+ ".toRegex(), "")
                    viewBinding.commentFragmentInfoCommentTextview.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isNicoad -> {
                    // 広告
                    val json = JSONObject(comment.replace("/nicoad ", ""))
                    val message = json.getString("message")
                    viewBinding.commentFragmentInfoCommentTextview.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isSpi -> {
                    // ニコニコ新市場
                    val message = comment.replace("/spi ", "")
                    viewBinding.commentFragmentInfoCommentTextview.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isGift -> {
                    // 投げ銭。スペース区切り配列
                    val list = comment.replace("/gift ", "").split(" ")
                    val userName = list[2]
                    val giftPoint = list[3]
                    val giftName = list[5]
                    val message = "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                    viewBinding.commentFragmentInfoCommentTextview.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                isEmotion && !isHideEmotion -> {
                    // エモーション
                    val message = comment.replace("/emotion ", "エモーション：")
                    viewBinding.commentFragmentInfoCommentTextview.text = message
                    infoAnim.alert(DropPopAlertMotionLayoutFix.ALERT_UP)
                }
                else -> {
                    // 生主コメント表示
                    viewBinding.commentFragmentUneiCommentTextview.text = HtmlCompat.fromHtml(comment, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    uneiAnim.alert(DropPopAlertMotionLayoutFix.ALERT_DROP)
                }
            }
        }
    }

    /**
     * disconnectのSnackBarを表示
     * */
    fun showProgramEndMessageSnackBar(message: String, commentJSONParse: CommentJSONParse) {
        if (commentJSONParse.premium.contains("運営")) {
            //終了メッセージ
            Snackbar.make(viewBinding.fragmentNicoLiveFab, context?.getString(R.string.program_disconnect) ?: "", Snackbar.LENGTH_SHORT).setAction(context?.getString(R.string.end)) {
                //終了
                if (activity !is NimadoActivity) {
                    //二窓Activity以外では終了できるようにする。
                    activity?.finish()
                }
            }.setAnchorView(getSnackbarAnchorView()).show()
        }
    }

    /**
     * アンケート表示
     * @param message /voteが文字列に含まれていることが必須
     * */
    fun showEnquate(message: String) {
        // コメント取得
        val jsonObject = JSONObject(message)
        val chatObject = jsonObject.getJSONObject("chat")
        val content = chatObject.getString("content")
        val premium = chatObject.getInt("premium")
        if (premium == 3) {
            // 運営コメントに表示
            // アンケ開始
            if (content.contains("/vote start")) {
                commentActivity.runOnUiThread {
                    setEnquetePOSTLayout(content, "start")
                }
            }
            // アンケ結果
            if (content.contains("/vote showresult")) {
                commentActivity.runOnUiThread {
                    setEnquetePOSTLayout(content, "showresult")
                }
            }
            // アンケ終了
            if (content.contains("/vote stop")) {
                commentActivity.runOnUiThread {
                    viewBinding.commentFragmentEnquateFramelayout.removeAllViews()
                }
            }
        }
    }

    /**
     * 画質変更SnackBar表示
     * @param selectQuality 選択した画質
     * */
    private fun showQualityChangeSnackBar(selectQuality: String?) {
        // 画質変更した。SnackBarでユーザーに教える
        multiLineSnackbar(viewBinding.commentFragmentSurfaceView, "${getString(R.string.successful_quality)}\n→${selectQuality}")
    }

    /** ViewPager2初期化 */
    private fun initViewPager() {
        viewBinding.commentViewpager.id = View.generateViewId()
        nicoLivePagerAdapter = NicoLivePagerAdapter(this, liveId, isOfficial)
        viewBinding.commentViewpager.adapter = nicoLivePagerAdapter
        // Tabに入れる名前
        TabLayoutMediator(viewBinding.activityCommentTabLayout, viewBinding.commentViewpager) { tab, position ->
            tab.text = nicoLivePagerAdapter.fragmentTabNameList[position]
        }.attach()
        // コメントを指定しておく。View#post{}で確実にcurrentItemが仕事するようになった。ViewPager2頼むよ～
        viewBinding.commentViewpager.post {
            viewBinding.commentViewpager?.setCurrentItem(1, false)
        }
        // もしTabLayoutを常時表示する場合は
        if (prefSetting.getBoolean("setting_scroll_tab_hide", false)) {
            // 多分AppBarLayoutは一人っ子(AppBarLayoutの子のViewは一個)
            viewBinding.commentFragmentAppBar.getChildAt(0).updateLayoutParams<AppBarLayout.LayoutParams> {
                // KTX有能
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
        }
    }

    /**
     * ステータスバーとノッチに侵略するやつ
     * */
    fun hideStatusBarAndSetFullScreen() {
        if (prefSetting.getBoolean("setting_display_cutout", false)) {
            // 非表示にする
            setSystemBarVisibility(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setNotchVisibility(false)
            }
        } else {
            // 表示する
            setSystemBarVisibility(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setNotchVisibility(true)
            }
        }
    }

    /**
     * システムバーを非表示にする関数
     * システムバーはステータスバーとナビゲーションバーのこと。多分
     * @param isShow 表示する際はtrue。非表示の際はfalse
     * */
    fun setSystemBarVisibility(isShow: Boolean) {
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

    /**
     * 予め置いておいたWebViewでニコ生を再生してゲームを遊ぶ。
     * @param isWebViewPlayer 生放送再生もWebViewでやる場合はtrue
     * */
    fun setNicoNamaGame(isWebViewPlayer: Boolean = false) {
        MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.comment_fragment_webview, View.VISIBLE)
        MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, false)
        NicoNamaGameWebViewTool.init(viewBinding.commentFragmentWebview, liveId, isWebViewPlayer)
        isAddedNicoNamaGame = true
    }

    /**
     * ニコ生ゲームをやめる。
     * */
    fun removeNicoNamaGame() {
        MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.comment_fragment_webview, View.GONE)
        MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, true)
        viewBinding.commentFragmentWebview.loadUrl("about:blank")
        isAddedNicoNamaGame = false
    }

    private fun testEnquate() {
        setEnquetePOSTLayout(
            "/vote start コロナ患者近くにいる？ はい いいえ 僕がコロナです",
            "start"
        )
        Timer().schedule(timerTask {
            commentActivity.runOnUiThread {
                setEnquetePOSTLayout(
                    "/vote showresult per 176 353 471",
                    "result"
                )
            }
        }, 5000)
    }

    /**
     * MultilineなSnackbar
     *
     * https://stackoverflow.com/questions/30705607/android-multiline-snackbar
     * */
    private fun multiLineSnackbar(view: View, message: String): Snackbar {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById(R.id.snackbar_text) as TextView
        textView.maxLines = 5 // show multiple line
        snackbar.anchorView = getSnackbarAnchorView() // 何のViewの上に表示するか指定
        snackbar.show()
        return snackbar
    }

    //視聴モード
    private fun setPlayVideoView() {

        // ExoPlayer作り直す
        val hlsAddress = viewModel.hlsAddressLiveData.value ?: return
        exoPlayer.release()
        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()

        // ニコ生版ニコニコ実況の場合 と 映像を受信しないモードのとき は接続しないので即return
        if (viewModel.nicoLiveHTML.getNicoJKIdFromChannelId(viewModel.communityId) != null || viewModel.isNotReceiveLive.value == true) {
            return
        }

        //設定で読み込むかどうか
        Handler(Looper.getMainLooper()).post {

            // 音声のみの再生はその旨（むね）を表示して、SurfaceViewを暗黒へ。わーわー言うとりますが、お時間でーす
            if (viewModel.currentQuality == "audio_high") {
                MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.include_nicolive_player_audio_only_text_view, View.VISIBLE)
                MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.comment_fragment_surface_view, View.VISIBLE)
                viewBinding.commentFragmentSurfaceView.background = ColorDrawable(Color.BLACK)
            } else {
                MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.include_nicolive_player_audio_only_text_view, View.GONE)
                MotionLayoutTool.setMotionLayoutViewVisible(viewBinding.commentFragmentMotionLayout, R.id.include_nicolive_player_audio_only_text_view, View.GONE)
                viewBinding.commentFragmentSurfaceView.background = null
            }

            aspectRatioFix()

            // 高さ更新
            viewBinding.commentFragmentCommentCanvas.finalHeight = viewBinding.commentFragmentCommentCanvas.height
            val sourceFactory = DefaultDataSourceFactory(requireContext(), "Stan-Droid;@kusamaru_jp", object : TransferListener {
                override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }

                override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }

                override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {

                }

                override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }
            })
            val hlsMediaSource = HlsMediaSource.Factory(sourceFactory).createMediaSource(MediaItem.fromUri(hlsAddress.toUri()))
            //再生準備
            exoPlayer.setMediaSource(hlsMediaSource)
            exoPlayer.prepare()
            //SurfaceViewセット
            exoPlayer.setVideoSurfaceView(viewBinding.commentFragmentSurfaceView)
            //再生
            exoPlayer.playWhenReady = true

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
                            exoPlayer.setMediaSource(hlsMediaSource)
                            exoPlayer.prepare()
                            //SurfaceViewセット
                            exoPlayer.setVideoSurfaceView(viewBinding.commentFragmentSurfaceView)
                            //再生
                            exoPlayer.playWhenReady = true
                            Snackbar.make(viewBinding.fragmentNicoLiveFab, getString(R.string.error_player), Snackbar.LENGTH_SHORT).apply {
                                anchorView = getSnackbarAnchorView()
                                // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                                if (viewModel.nicoLiveHTML.isLowLatency) {
                                    setAction(getString(R.string.low_latency_off)) {
                                        viewModel.nicoLiveHTML.sendLowLatency(!viewModel.nicoLiveHTML.isLowLatency)
                                    }
                                }
                                show()
                            }
                        }
                    }
                }
            })
        }
    }

    /** アスペクト比を直す。ミニプレイヤーと全画面UIのアスペクト比も直す */
    private fun aspectRatioFix() {
        viewBinding.root.doOnLayout {
            val displayWidth = requireActivity().window.decorView.width
            val displayHeight = requireActivity().window.decorView.height
            val isLandScape = commentActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandScape) {
                // 横画面時
                val playerWidth = if (isNimadoMode) displayWidth / 4 else displayWidth / 2
                val playerHeight = (9F / 16 * playerWidth)
                // MotionLayoutのConstraintSetの高さを変えることになるので少しめんどい
                viewBinding.commentFragmentMotionLayout?.apply {
                    getConstraintSet(R.id.comment_fragment_transition_start)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt())
                        constrainWidth(R.id.comment_fragment_surface_view, playerWidth)
                    }
                    // ミニプレイヤー時
                    getConstraintSet(R.id.comment_fragment_transition_end)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, (playerHeight / 1.5).toInt())
                        constrainWidth(R.id.comment_fragment_surface_view, (playerWidth / 1.5).toInt())
                    }
                    getConstraintSet(R.id.comment_fragment_transition_finish)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, (playerHeight / 1.5).toInt())
                        constrainWidth(R.id.comment_fragment_surface_view, (playerWidth / 1.5).toInt())
                    }
                    // 全画面
                    getConstraintSet(R.id.comment_fragment_transition_fullscreen)?.apply {
                        val fullScreenHeight = (displayWidth / 16) * 9
                        if (fullScreenHeight > displayHeight) {
                            // 画面外に行く
                            val fullScreenWidth = (displayHeight / 9) * 16
                            constrainHeight(R.id.comment_fragment_surface_view, displayHeight)
                            constrainWidth(R.id.comment_fragment_surface_view, fullScreenWidth)
                        } else {
                            // おさまる
                            constrainHeight(R.id.comment_fragment_surface_view, fullScreenHeight)
                            constrainWidth(R.id.comment_fragment_surface_view, displayWidth)
                        }
                    }
                }
            } else {
                // 縦画面時
                val playerWidth = if (isNimadoMode) displayWidth / 2 else displayWidth
                val playerHeight = (9F / 16 * playerWidth)
                // MotionLayoutのConstraintSetの高さを変えることになるので少しめんどい
                viewBinding.commentFragmentMotionLayout?.apply {
                    getConstraintSet(R.id.comment_fragment_transition_start)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt())
                        constrainWidth(R.id.comment_fragment_surface_view, playerWidth)
                    }
                    // ミニプレイヤー時
                    getConstraintSet(R.id.comment_fragment_transition_end)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt() / 2)
                        constrainWidth(R.id.comment_fragment_surface_view, playerWidth / 2)
                    }
                    getConstraintSet(R.id.comment_fragment_transition_finish)?.apply {
                        constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt() / 2)
                        constrainWidth(R.id.comment_fragment_surface_view, playerWidth / 2)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
    }

    override fun onPause() {
        super.onPause()
        googleCast.pause()
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
    }

    fun showToast(message: String) {
        commentActivity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** フローティングコメントビューワー起動関数 */
    fun showBubbles() {
        // FloatingCommentViewer#showBubble()に移動させました
        FloatingCommentViewer.showBubbles(
            context = requireContext(),
            liveId = liveId,
            title = viewModel.programTitle,
            thumbUrl = viewModel.thumbnailURL
        )
    }


    fun setLandscapePortrait() {
        val conf = resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }

    fun copyProgramId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
        //コピーしました！
        Toast.makeText(context, "${getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT).show()
    }

    fun copyCommunityId() {
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", viewModel.communityId))
        //コピーしました！
        Toast.makeText(context, "${getString(R.string.copy_communityid)} : ${viewModel.communityId}", Toast.LENGTH_SHORT).show()
    }

    private fun destroyCode() {
        // 止める
        exoPlayer.release()
        // 牛乳を飲んで状態異常を解除
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyCode()
    }

    /** ポップアップ再生 */
    fun startPopupPlay() {
        startPlayService("popup")
    }

    /** バックグラウンド再生 */
    fun startBackgroundPlay() {
        startPlayService("background")
    }

    /**
     * ポップアップ再生、バッググラウンド再生サービス起動用関数
     * @param mode "popup"（ポップアップ再生）か"background"（バッググラウンド再生）
     * */
    private fun startPlayService(mode: String) {
        // サービス起動
        startLivePlayService(
            context = context,
            mode = mode,
            liveId = liveId,
            isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
            startQuality = viewModel.currentQuality
        )
        // Activity落とす
        finishFragment()
    }


    //Activity復帰した時に呼ばれる
    override fun onStart() {
        super.onStart()
        //再生部分を作り直す
        if (viewModel.hlsAddressLiveData.value?.isNotEmpty() == true && !viewModel.isCommentOnlyMode) {
            setPlayVideoView()
        }
    }

    private fun setEnquetePOSTLayout(message: String, type: String) {
        val bottomFragmentEnquateLayoutBinding = IncludeNicoliveEnquateBinding.inflate(layoutInflater)
        enquateView = bottomFragmentEnquateLayoutBinding.root
        if (type.contains("start")) {
            // アンケ中はMotionLayoutのTransitionを一時的に無効化
            MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, false)
            //アンケ開始
            viewBinding.commentFragmentEnquateFramelayout.removeAllViews()
            viewBinding.commentFragmentEnquateFramelayout.addView(enquateView)
            // /vote start ～なんとか　を配列にする
            val voteString = message.replace("/vote start ", "")
            val voteList = voteString.split(" ") // 空白の部分で分けて配列にする
            val jsonArray = JSONArray(voteList)
            //println(enquateStartMessageToJSONArray(message))
            //アンケ内容保存
            enquateJSONArray = jsonArray.toString()

            //０個目はタイトル
            val title = jsonArray[0]
            bottomFragmentEnquateLayoutBinding.enquateTitle.text = title.toString()

            //１個めから質問
            for (i in 0 until jsonArray.length()) {
                //println(i)
                val button = MaterialButton(requireContext())
                button.text = jsonArray.getString(i)
                button.setOnClickListener {
                    // 投票
                    viewModel.enquatePOST(i - 1)
                    // アンケ画面消す
                    viewBinding.commentFragmentEnquateFramelayout.removeAllViews()
                    // SnackBar
                    Snackbar.make(viewBinding.commentFragmentSurfaceView, "${getString(R.string.enquate)}：${jsonArray[i]}", Snackbar.LENGTH_SHORT).apply {
                        anchorView = getSnackbarAnchorView()
                        show()
                    }
                }
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 1..3) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout1.addView(button)
                }
                //4～6は一段目
                if (i in 4..6) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout2.addView(button)
                }
                //7～9は一段目
                if (i in 7..9) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout3.addView(button)
                }
            }
        } else if (enquateJSONArray.isNotEmpty()) {
            // 結果出たらMotionLayoutのTransitionを一時的に有効に戻す
            MotionLayoutTool.allTransitionEnable(viewBinding.commentFragmentMotionLayout, true)
            //アンケ結果
            viewBinding.commentFragmentEnquateFramelayout?.removeAllViews()
            viewBinding.commentFragmentEnquateFramelayout?.addView(enquateView)
            // /vote showresult ~なんとか を　配列にする
            val voteString = message.replace("/vote showresult per ", "")
            val voteList = voteString.split(" ")
            val jsonArray = JSONArray(voteList)
            val questionJsonArray = JSONArray(enquateJSONArray)
            //０個目はタイトル
            val title = questionJsonArray.getString(0)
            bottomFragmentEnquateLayoutBinding.enquateTitle.text = title
            //共有で使う文字
            var shareText = ""
            //結果は０個めから
            for (i in 0 until jsonArray.length()) {
                val result = jsonArray.getString(i)
                val question = questionJsonArray.getString(i + 1)
                val text = question + "\n" + enquatePerText(result)
                val button = MaterialButton(requireContext())
                button.text = text
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 0..2) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout1.addView(button)
                }
                //4～6は一段目
                if (i in 3..5) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout2.addView(button)
                }
                //7～9は一段目
                if (i in 6..8) {
                    bottomFragmentEnquateLayoutBinding.enquateLinearlayout3.addView(button)
                }
                //共有の文字
                shareText += "$question : ${enquatePerText(result)}\n"
            }
            //アンケ結果を共有
            Snackbar.make(viewBinding.commentFragmentSurfaceView, getString(R.string.enquate_result), Snackbar.LENGTH_SHORT).apply {
                anchorView = getSnackbarAnchorView()
                setAction(getString(R.string.share)) {
                    //共有する
                    share(shareText, "$title(${viewModel.programTitle}-$liveId)")
                }
                show()
            }
        }
    }

    //アンケートの結果を％表示
    private fun enquatePerText(per: String): String {
        // 176 を 17.6% って表記するためのコード。１桁増やして（9%以下とき対応できないため）２桁消す
        val percentToFloat = per.toFloat() * 10
        val result = "${(percentToFloat / 100)}%"
        return result
    }


    fun share(shareText: String, shareTitle: String) {
        val builder = ShareCompat.IntentBuilder.from(commentActivity)
        builder.setChooserTitle(shareTitle)
        builder.setSubject(shareTitle)
        builder.setText(shareText)
        builder.setType("text/plain")
        builder.startChooser()
    }

    //新しいコメント投稿画面
    fun commentCardView() {
        // プレ垢ならプレ垢用コメント色パレットを表示させる
        viewBinding.include.commentCardviewCommandEditColorPremiumLinearlayout.isVisible = viewModel.nicoLiveHTML.isPremium

        // 184が有効になっているときはコメントInputEditTextのHintに追記する
        if (viewModel.nicoLiveHTML.isPostTokumeiComment) {
            viewBinding.include.commentCardviewCommandColorTextinputlayout.hint = getString(R.string.comment)
        } else {
            viewBinding.include.commentCardviewCommandColorTextinputlayout.hint = "${getString(R.string.comment)}（${getString(R.string.disabled_tokumei_comment)}）"
        }
        //投稿ボタンを押したら投稿
        viewBinding.include.commentCardviewCommentSendButton.setOnClickListener {
            val comment = viewBinding.include.commentCardviewCommentTextinputEdittext.text.toString()
            // 7/27からコマンド（色や位置の指定）は全部つなげて送るのではなく、それぞれ指定する必要があるので
            val color = viewBinding.include.commentCardviewCommandColorTextinputlayout.text.toString()
            viewBinding.include.commentCardviewCommandSizeTextinputlayout
            val position = viewBinding.include.commentCardviewCommandPositionTextinputlayout.text.toString()
            val size = viewBinding.include.commentCardviewCommandSizeTextinputlayout.text.toString()
            // コメ送信
            lifecycleScope.launch(Dispatchers.IO) { viewModel.sendComment(comment, color, size, position) }
            viewBinding.include.commentCardviewCommentTextinputEdittext.setText("")
            if (!prefSetting.getBoolean("setting_command_save", false)) {
                // コマンドを保持しない設定ならクリアボタンを押す
                viewBinding.include.commentCardviewCommentCommandEditResetButton.callOnClick()
            }
        }
        // Enterキー(紙飛行機ボタン)を押したら投稿する
        if (prefSetting.getBoolean("setting_enter_post", true)) {
            viewBinding.include.commentCardviewCommentTextinputEdittext.imeOptions = EditorInfo.IME_ACTION_SEND
            viewBinding.include.commentCardviewCommentTextinputEdittext.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val text = viewBinding.include.commentCardviewCommentTextinputEdittext.text.toString()
                    if (text.isNotEmpty()) {
                        // 空じゃなければ、コメント投稿
                        // 7/27からコマンド（色や位置の指定）は全部つなげて送るのではなく、それぞれ指定する必要があるので
                        val color = viewBinding.include.commentCardviewCommandColorTextinputlayout.text.toString()
                        val position = viewBinding.include.commentCardviewCommandPositionTextinputlayout.text.toString()
                        val size = viewBinding.include.commentCardviewCommandSizeTextinputlayout.text.toString()
                        // コメ送信
                        lifecycleScope.launch(Dispatchers.IO) { viewModel.sendComment(text, color, size, position) }
                        // コメントリセットとコマンドリセットボタンを押す
                        viewBinding.include.commentCardviewCommentTextinputEdittext.setText("")
                        if (!prefSetting.getBoolean("setting_command_save", false)) {
                            // コマンドを保持しない設定ならクリアボタンを押す
                            viewBinding.include.commentCardviewCommentCommandEditResetButton.callOnClick()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } else {
            // 複数行？一筋縄では行かない
            // https://stackoverflow.com/questions/51391747/multiline-does-not-work-inside-textinputlayout
            viewBinding.include.commentCardviewCommentTextinputEdittext.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            viewBinding.include.commentCardviewCommentTextinputEdittext.maxLines = Int.MAX_VALUE
        }
        //閉じるボタン
        viewBinding.include.commentCardviewCloseButton.setOnClickListener {
            // 非表示アニメーションに挑戦した。
            val hideAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_hide_animation)
            // 表示
            viewBinding.include.root.startAnimation(hideAnimation)
            viewBinding.include.root.isVisible = false
            viewBinding.fragmentNicoLiveFab.show()
            // IMEも消す（Android 11 以降）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.hide(WindowInsets.Type.ime())
            }
        }
        viewBinding.include.commentCardviewCommentCommandEditButton.setOnClickListener {
            // コマンド入力画面展開
            val visibility = viewBinding.include.commentCardviewCommandEditLinearlayout.visibility
            if (visibility == View.GONE) {
                // 展開
                viewBinding.include.commentCardviewCommandEditLinearlayout.visibility = View.VISIBLE
                // アイコンを閉じるアイコンへ
                viewBinding.include.commentCardviewCommentCommandEditButton.setImageDrawable(context?.getDrawable(R.drawable.ic_expand_more_24px))
            } else {
                viewBinding.include.commentCardviewCommandEditLinearlayout.visibility = View.GONE
                viewBinding.include.commentCardviewCommentCommandEditButton.setImageDrawable(context?.getDrawable(R.drawable.ic_outline_format_color_fill_24px))
            }
        }

        // コマンドリセットボタン
        viewBinding.include.commentCardviewCommentCommandEditResetButton.setOnClickListener {
            // リセット
            viewBinding.include.commentCardviewCommandColorTextinputlayout.setText("")
            viewBinding.include.commentCardviewCommandPositionTextinputlayout.setText("")
            viewBinding.include.commentCardviewCommandSizeTextinputlayout.setText("")
            clearColorCommandSizeButton()
            clearColorCommandPosButton()
        }
        // 大きさ
        viewBinding.include.commentCardviewCommentCommandBigButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandSizeTextinputlayout.setText("big")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        viewBinding.include.commentCardviewCommentCommandMediumButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandSizeTextinputlayout.setText("medium")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        viewBinding.include.commentCardviewCommentCommandSmallButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandSizeTextinputlayout.setText("small")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの位置
        viewBinding.include.commentCardviewCommentCommandUeButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandPositionTextinputlayout.setText("ue")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        viewBinding.include.commentCardviewCommentCommandNakaButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandPositionTextinputlayout.setText("naka")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        viewBinding.include.commentCardviewCommentCommandShitaButton.setOnClickListener {
            viewBinding.include.commentCardviewCommandPositionTextinputlayout.setText("shita")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの色。流石にすべてのボタンにクリックリスナー書くと長くなるので、タグに色（文字列）を入れる方法で対処
        viewBinding.include.commentCardviewCommandEditColorLinearlayout.children.forEach {
            it.setOnClickListener {
                viewBinding.include.commentCardviewCommandColorTextinputlayout.setText(it.tag as String)
            }
        }
        // ↑のプレ垢版
        viewBinding.include.commentCardviewCommandEditColorPremiumLinearlayout.children.forEach {
            it.setOnClickListener {
                viewBinding.include.commentCardviewCommandColorTextinputlayout.setText(it.tag as String)
            }
        }
    }

    // ボタンの色を戻す サイズボタン
    fun clearColorCommandSizeButton() {
        viewBinding.include.commentCardviewCommentCommandSizeLayout.children.forEach {
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    // ボタンの色を戻す 位置ボタン
    fun clearColorCommandPosButton() {
        viewBinding.include.commentCardviewCommentCommandPosLayout.children.forEach {
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    fun getSnackbarAnchorView(): View? {
        if (viewBinding.fragmentNicoLiveFab.isShown) {
            return viewBinding.fragmentNicoLiveFab
        } else {
            return viewBinding.include.root
        }
    }

    /** Fragment終了関数 */
    private fun finishFragment() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    fun isInitGoogleCast(): Boolean = ::googleCast.isInitialized

    /** 戻るキー押しったとき */
    override fun onBackButtonPress() {

    }

    /** ミニプレイヤーで再生していればtrueを返す */
    override fun isMiniPlayerMode(): Boolean {
        // なんかたまにnullになる
        return viewBinding.commentFragmentMotionLayout?.let {
            // この行がreturnの値になる。apply{ }に返り値機能がついた
            it.currentState == R.id.comment_fragment_transition_end || it.currentState == R.id.comment_fragment_transition_finish
        } ?: false
    }

}