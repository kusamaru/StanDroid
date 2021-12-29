package io.github.takusan23.tatimidroid.nicolive

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.activity.KotehanListActivity
import io.github.takusan23.tatimidroid.activity.NGListActivity
import io.github.takusan23.tatimidroid.nicolive.bottomfragment.NicoLiveQualitySelectBottomSheet
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.tool.*
import io.github.takusan23.tatimidroid.databinding.FragmentCommentMenuBinding
import kotlinx.coroutines.launch


/**
 * ここをメンテしにきた私へ
 * CommentMenuBottomFragment と このクラスは違うよ。命名雑だった。ごめん
 * CommentFragmentのメニューはここ。
 * コメントを押した時にできる（ロックオン、コテハン登録）なんかは [CommentLockonBottomFragment] へどうぞ
 * */
class CommentMenuFragment : Fragment() {

    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting: SharedPreferences

    val liveId by lazy { viewModel.nicoLiveHTML.liveId }

    // CommentFragmentとそれのViewModel
    val commentFragment by lazy { requireParentFragment() as CommentFragment }
    val viewModel by viewModels<NicoLiveViewModel>({ commentFragment })

    // Activity Result API !!!。で権限を取得する
    private val resultAPIPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGrented ->
        if (isGrented) {
            // 付与された
            commentFragment.startPopupPlay()
        }
    }

    /** 共有用 */
    private val contentShare by lazy { ContentShareTool(requireContext()) }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentCommentMenuBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        darkModeSupport = DarkModeSupport(requireContext())
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //値設定
        setValue()

        //CommentFragmentへ値を渡す
        setCommentFragmentValue()

        //クリックイベント
        initSwitch()

        //OutlinedButtonのテキストの色
        darkmode()

        // Android 5の場合はWebViewが落ちてしまうので塞ぐ
        // こればっかりはandroidx.appcompat:appcompatのせいなので私悪くない
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP_MR1) {
            viewBinding.fragmentCommentFragmentNicoNamaGameSwitch.isEnabled = false
        }

    }

    fun darkmode() {
        if (isDarkMode(context)) {
            //ダークモード時ボタンのテキストの色が変わらないので
            val color = ColorStateList.valueOf(Color.parseColor("#ffffff"))
            viewBinding.fragmentCommentFragmentMenuRotationButton.setTextColor(color)
            viewBinding.fragmentCommentFragmentMenuCopyLiveIdButton.setTextColor(color)
            viewBinding.fragmentCommentFragmentMenuCopyCommunityIdButton.setTextColor(color)
            viewBinding.fragmentCommentFragmentMenuOpenBrowserButton.setTextColor(color)
            viewBinding.fragmentCommentFragmentMenuNgListButton.setTextColor(color)
            viewBinding.fragmentCommentFragmentMenuShareButton.setTextColor(color)
        }
    }

    //クリックイベント
    private fun initSwitch() {
        //キャスト
        if (commentFragment.isInitGoogleCast()) {
            val googleCast = commentFragment.googleCast
            googleCast.setUpCastButton(viewBinding.fragmentCommentFragmentMenuCastButton)
        }

        // 画質変更
        viewBinding.fragmentCommentFragmentMenuQualityButton.setOnClickListener {
            NicoLiveQualitySelectBottomSheet().show(commentFragment.childFragmentManager, "quality_change")
        }
        //強制画面回転
        viewBinding.fragmentCommentFragmentMenuRotationButton.setOnClickListener {
            commentFragment.setLandscapePortrait()
        }
        //番組IDコピー
        viewBinding.fragmentCommentFragmentMenuCopyLiveIdButton.setOnClickListener {
            commentFragment.copyProgramId()
        }
        //コミュニティIDコピー
        viewBinding.fragmentCommentFragmentMenuCopyCommunityIdButton.setOnClickListener {
            commentFragment.copyCommunityId()
        }
        //ブラウザで開く
        viewBinding.fragmentCommentFragmentMenuOpenBrowserButton.setOnClickListener {
            val uri = "https://live2.nicovideo.jp/watch/$liveId".toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
        //NGリスト
        viewBinding.fragmentCommentFragmentMenuNgListButton.setOnClickListener {
            val intent = Intent(context, NGListActivity::class.java)
            startActivity(intent)
        }
        //（画像添付しない）共有
        viewBinding.fragmentCommentFragmentMenuShareButton.setOnClickListener {
            contentShare.showShareContent(
                programId = liveId,
                programName = viewModel.programTitle,
                fromTimeSecond = null,
                uri = null
            )
        }
        //画像つき共有
        viewBinding.fragmentCommentFragmentMenuShareImageAttachButton.setOnClickListener {
            // 今いる部屋の名前入れる
            lifecycleScope.launch {
                contentShare.showShareContentAttachPicture(
                    playerView = commentFragment.viewBinding.commentFragmentSurfaceView,
                    commentCanvas = commentFragment.viewBinding.commentFragmentCommentCanvas,
                    contentId = viewModel.programTitle,
                    contentTitle = liveId,
                    fromTimeSecond = null,
                )
            }
        }
        //生放送を再生ボタン
        viewBinding.fragmentCommentFragmentMenuViewLiveButton.setOnClickListener {
            (requireParentFragment() as CommentFragment).apply {
                setCommentOnlyMode(!viewModel.isCommentOnlyMode)
            }
        }
        // バッググラウンド再生。
        viewBinding.fragmentCommentFragmentMenuBackgroundButton.setOnClickListener {
            commentFragment.apply {
                startBackgroundPlay()
                viewModel.isNotReceiveLive.value = true
                setCommentOnlyMode(!viewModel.isCommentOnlyMode)
            }
        }

        //ポップアップ再生。いつか怒られそう（プレ垢限定要素だし）
        viewBinding.fragmentCommentFragmentMenuPopupButton.setOnClickListener {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    !Settings.canDrawOverlays(context)
                } else {
                    false
                }
            ) {
                // 上に重ねる権限無いとき。取りに行く
                resultAPIPermission.launch(Manifest.permission.SYSTEM_ALERT_WINDOW)
            } else {
                commentFragment.apply {
                    //ポップアップ再生。コメント付き
                    startPopupPlay()
                    viewModel.isNotReceiveLive.value = true
                    setCommentOnlyMode(!viewModel.isCommentOnlyMode)
                }
            }
        }

        //フローティングコメビュ
        viewBinding.fragmentCommentFragmentMenuFloatingButton.setOnClickListener {
            //Activity移動
            commentFragment.showBubbles()
        }

        //フローティングコメビュはAndroid10以降で利用可能
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewBinding.fragmentCommentFragmentMenuFloatingButton.isEnabled = false
        }

        // （ホーム画面に追加のやつ）
        viewBinding.fragmentCommentFragmentMenuDymanicShortcutButton.setOnClickListener {
            createHomeScreenShortcut()
        }

        //匿名非表示
        viewBinding.fragmentCommentFragmentMenuIyayoHiddenSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.isHideTokumei.value = isChecked
        }

        // 低遅延
        viewBinding.fragmentCommentFragmentMenuLowLatencySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean("nicolive_low_latency", isChecked) }
            viewModel.nicoLiveHTML.sendLowLatency(isChecked)
        }

        // 映像を受信しない（将来のニコニコ実況のため？）
        viewBinding.fragmentCommentFragmentMenuNotLiveReceiveSwitch.isChecked = viewModel.isNotReceiveLive.value ?: false
        viewBinding.fragmentCommentFragmentMenuNotLiveReceiveSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.isNotReceiveLive.postValue(!viewModel.isNotReceiveLive.value!!)
        }

        // ユーザーID非表示モード
        viewBinding.fragmentCommentFragmentMenuCommentSettingHiddenIdSwtich.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_id_hidden", isChecked)
                apply()
            }
        }

        // コメント一行モード on/off
        viewBinding.fragmentCommentFragmentMenuSettingOneLineSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_one_line", isChecked)
                apply()
            }
        }

        // コマンド保持モード
        viewBinding.fragmentCommentFragmentMenuCommandSaveSwitch.setOnCheckedChangeListener { compoundButton, b ->
            prefSetting.edit {
                putBoolean("setting_command_save", b)
            }
        }

        // ノッチ領域に侵略する
        viewBinding.fragmentCommentFragmentMenuDisplayCutoutInfoSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean("setting_display_cutout", isChecked)
            }
            activity?.runOnUiThread {
                commentFragment.hideStatusBarAndSetFullScreen()
            }
        }

        // ニコ生ゲーム
        viewBinding.fragmentCommentFragmentNicoNamaGameSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            commentFragment.apply {
                if (isChecked) {
                    setNicoNamaGame()
                } else {
                    removeNicoNamaGame()
                }
            }
        }

        // ニコ生ゲーム（生放送・コメントもWebViewで利用する）
        viewBinding.fragmentCommentFragmentNicoNamaGameWebviewPlayerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            commentFragment.apply {
                if (isChecked) {
                    exoPlayer.volume = 0F
                    setNicoNamaGame(isChecked)
                } else {
                    exoPlayer.volume = 1F
                    removeNicoNamaGame()
                }
            }
        }

        viewBinding.fragmentCommentFragmentVolumeSeek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                commentFragment.apply {
                    exoPlayer.volume = progress / 10F
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

        // コテハン一覧画面
        viewBinding.fragmentCommentFragmentMenuKotehanButton.setOnClickListener {
            val intent = Intent(context, KotehanListActivity::class.java)
            startActivity(intent)
        }

        // エモーションをコメント一覧に表示しない
        viewBinding.fragmentCommentFragmentMenuHideEmotionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean("setting_nicolive_hide_emotion", isChecked) }
        }

    }

    /**「ホーム画面に追加」のやつ */
    private fun createHomeScreenShortcut() {
        viewModel.nicoLiveCommunityOrChannelDataLiveData.value?.let { communityOrChannelData ->
            lifecycleScope.launch {
                // ショートカット作成関数
                CreateShortcutTool.createHomeScreenShortcut(
                    context = requireContext(),
                    contentId = communityOrChannelData.id,
                    contentTitle = communityOrChannelData.name,
                    thumbUrl = communityOrChannelData.icon
                )
            }
        }
    }

    private fun setValue() {
        //コメント非表示
        viewBinding.fragmentCommentFragmentMenuCommentHiddenSwitch.isChecked = prefSetting.getBoolean("nicolive_comment_canvas_hide", false)
        //Infoコメント非表示
        viewBinding.fragmentCommentFragmentMenuHideInfoPermSwitch.isChecked = commentFragment.hideInfoUnnkome
        //匿名で投稿するか
        viewBinding.fragmentCommentFragmentMenuIyayoCommentSwitch.isChecked = viewModel.nicoLiveHTML.isPostTokumeiComment
        //匿名コメントを非表示にするか
        viewBinding.fragmentCommentFragmentMenuIyayoHiddenSwitch.isChecked = viewModel.isHideTokumei.value ?: false
        //低遅延モードの有効無効
        viewBinding.fragmentCommentFragmentMenuLowLatencySwitch.isChecked = prefSetting.getBoolean("nicolive_low_latency", true)
        // コメント一行もーど
        viewBinding.fragmentCommentFragmentMenuCommentSettingHiddenIdSwtich.isChecked = prefSetting.getBoolean("setting_id_hidden", false)
        // コマンド保持モード
        viewBinding.fragmentCommentFragmentMenuCommandSaveSwitch.isChecked = prefSetting.getBoolean("setting_command_save", false)
        // ユーザーID非表示モード
        viewBinding.fragmentCommentFragmentMenuSettingOneLineSwitch.isChecked = prefSetting.getBoolean("setting_one_line", false)
        //音量
        viewBinding.fragmentCommentFragmentVolumeSeek.progress = (commentFragment.exoPlayer.volume * 10).toInt()
        //ニコ生ゲーム有効時
        viewBinding.fragmentCommentFragmentNicoNamaGameSwitch.isChecked = commentFragment.isAddedNicoNamaGame
        // ノッチ領域に侵略
        viewBinding.fragmentCommentFragmentMenuDisplayCutoutInfoSwitch.isChecked = prefSetting.getBoolean("setting_display_cutout", false)
    }

    //CommentFragmentへ値を渡す
    private fun setCommentFragmentValue() {
        //押したらすぐ反映できるように
        viewBinding.fragmentCommentFragmentMenuCommentHiddenSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            //コメント非表示
            prefSetting.edit { putBoolean("nicolive_comment_canvas_hide", isChecked) }
            val visibility = if (isChecked) View.GONE else View.VISIBLE
            MotionLayoutTool.setMotionLayoutViewVisible(commentFragment.viewBinding.commentFragmentMotionLayout, commentFragment.viewBinding.commentFragmentCommentCanvas.id, visibility)
        }
        viewBinding.fragmentCommentFragmentMenuHideInfoPermSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            //Infoコメント非表示
            commentFragment.hideInfoUnnkome = isChecked
        }
        viewBinding.fragmentCommentFragmentMenuIyayoCommentSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 匿名で投稿するかどうか。
            viewModel.nicoLiveHTML.isPostTokumeiComment = isChecked
            prefSetting.edit { putBoolean("nicolive_post_tokumei", isChecked) }
        }
    }

}