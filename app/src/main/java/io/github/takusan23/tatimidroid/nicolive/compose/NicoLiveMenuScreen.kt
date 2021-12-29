package io.github.takusan23.tatimidroid.nicolive.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.activity.KotehanListActivity
import io.github.takusan23.tatimidroid.activity.NGListActivity
import io.github.takusan23.tatimidroid.compose.ShareMenuCard
import io.github.takusan23.tatimidroid.compose.VolumeMenu
import io.github.takusan23.tatimidroid.nicolive.activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.nicolive.bottomfragment.NicoLiveQualitySelectBottomSheet
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardElevation
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardModifier
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardShape
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.CreateShortcutTool
import kotlinx.coroutines.launch

/**
 * 生放送メニューCard。長いのでまとめた
 *
 * @param parentFragment [JCNicoLiveFragment]を指すように（ViewModelで使う）
 * */
@Composable
fun NicoLiveMenuScreen(parentFragment: Fragment) {

    /** ViewModel取得 */
    val viewModel by parentFragment.viewModels<NicoLiveViewModel>({ parentFragment })

    /** FragmentManager */
    val fragmentManager = parentFragment.childFragmentManager

    /** Context */
    val context = LocalContext.current

    /** Coroutine */
    val scope = rememberCoroutineScope()

    // Preference
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * 関数たち
     * ---------------------
     * */

    /** 画質変更BottomFragment表示 */
    fun openQualityChangeBottomFragment() {
        NicoLiveQualitySelectBottomSheet().show(parentFragment.childFragmentManager, "quality")
    }

    /** 画面回転する */
    fun rotateScreen() {
        when (parentFragment.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                parentFragment.requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }

    /** 番組IDコピー */
    fun copyProgramId() {
        viewModel.nicoLiveProgramData.value?.apply {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", programId))
            //コピーしました！
            Toast.makeText(context, "${context.getString(R.string.copy_program_id)} : $programId", Toast.LENGTH_SHORT).show()
        }
    }

    /** コミュIDコピー */
    fun copyCommunityId() {
        viewModel.nicoLiveCommunityOrChannelDataLiveData.value?.apply {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", id))
            //コピーしました！
            Toast.makeText(context, "${context.getString(R.string.copy_communityid)} : $id", Toast.LENGTH_SHORT).show()
        }
    }

    /** ブラウザを起動する */
    fun openBrowser() {
        val uri = "https://live2.nicovideo.jp/watch/${viewModel.nicoLiveProgramData.value?.programId}".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    /** NG一覧表示など */
    fun openNGListActivity() {
        val intent = Intent(context, NGListActivity::class.java)
        context.startActivity(intent)
    }

    /** コテハン一覧 */
    fun openKotehanListActivity() {
        val intent = Intent(context, KotehanListActivity::class.java)
        context.startActivity(intent)
    }

    /** ホーム画面にショートカット（ピン留め）をする */
    fun createHomeScreenShortcut() {
        viewModel.nicoLiveCommunityOrChannelDataLiveData.value?.let { communityOrChannelData ->
            scope.launch {
                // ショートカット作成関数
                CreateShortcutTool.createHomeScreenShortcut(
                    context = context,
                    contentId = communityOrChannelData.id,
                    contentTitle = communityOrChannelData.name,
                    thumbUrl = communityOrChannelData.icon
                )
            }
        }
    }


    /** 画像つき共有をする */
    fun showShereSheetMediaAttach() {
        // 親のFragment取得
        (parentFragment as? JCNicoLiveFragment)?.showShareSheetMediaAttach()
    }

    /** 共有する */
    fun showShareSheet() {
        // 親のFragment取得
        (parentFragment as? JCNicoLiveFragment)?.showShareSheet()
    }

    /** フローティングコメビュ */
    fun openFloatingCommentViewer() {
        viewModel.nicoLiveProgramData.value?.apply {
            FloatingCommentViewer.showBubbles(context, this.programId, this.title, this.thum)
        }
    }

    /**
     * こっからレイアウト
     * ----------------------------
     * */
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            // コルーチン
            val scope = rememberCoroutineScope()
            // タブの選択位置
            val selectIndex = remember { mutableStateOf(0) }

            // メニューのタブ部分
            NicoLiveMenuTab(
                selectedIndex = selectIndex.value,
                tabClick = { index -> selectIndex.value = index }
            )
            // メニューの本命
            when (selectIndex.value) {
                0 -> {
                    // スイッチ系設定
                    val isNotReceiveLiveLiveData = viewModel.isNotReceiveLive.observeAsState(initial = false)
                    val isHideUNEICommentLivaData = viewModel.isHideInfoUnnkome.observeAsState(initial = false)
                    val isHideTokumeiCommentLiveData = viewModel.isHideTokumei.observeAsState(initial = false)
                    val isHideEmotionLiveData = viewModel.isHideEmotion.observeAsState(initial = false)

                    NicoLiveSwitchMenu(
                        isHideUNEIComment = isHideUNEICommentLivaData.value,
                        onSwitchHideUNEIComment = { viewModel.isHideInfoUnnkome.postValue(it) },
                        isHideEmotion = isHideEmotionLiveData.value,
                        onSwitchHideEmotion = { viewModel.isHideEmotion.postValue(it) },
                        isHideTokumeiComment = isHideTokumeiCommentLiveData.value,
                        onSwitchHideTokumeiComment = { viewModel.isHideTokumei.postValue(it) },
                        isLowLatency = viewModel.nicoLiveHTML.isLowLatency,
                        onSwitchLowLatency = { viewModel.nicoLiveHTML.sendLowLatency(it) },
                        isNotReceiveLive = isNotReceiveLiveLiveData.value,
                        onSwitchNotReceiveLive = { viewModel.isNotReceiveLive.postValue(it) }
                    )
                }
                1 -> {
                    // コメントビューワー設定
                    val isHideUserId = remember { mutableStateOf(prefSetting.getBoolean("setting_id_hidden", false)) }
                    val isCommentSingleLine = remember { mutableStateOf(prefSetting.getBoolean("setting_one_line", false)) }
                    NicoLiveCommentViewerMenu(
                        isHideUserId = isHideUserId.value,
                        onSwitchHideUserId = {
                            isHideUserId.value = it
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("setting_id_hidden", it) }
                        },
                        isCommentOneLine = isCommentSingleLine.value,
                        onSwitchCommentOneLine = {
                            isCommentSingleLine.value = it
                            // Preferenceに反映
                            prefSetting.edit { putBoolean("setting_one_line", it) }
                        }
                    )
                }
                2 -> {
                    // コメビュメニュー
                    NicoLiveButtonMenu(
                        onClickQualityChange = { openQualityChangeBottomFragment() },
                        onClickScreenRotation = { rotateScreen() },
                        onClickCopyProgramId = { copyProgramId() },
                        onClickCopyCommunityId = { copyCommunityId() },
                        onClickOpenBrowser = { openBrowser() },
                        onClickNGList = { openNGListActivity() },
                        onClickKotehanList = { openKotehanListActivity() },
                        onClickHomeScreenPin = { createHomeScreenShortcut() },
                        onClickLaunchFloatingCommentViewer = { openFloatingCommentViewer() }
                    )
                }
                3 -> {
                    // 共有メニュー
                    ShareMenuCard(
                        onClickShare = { showShareSheet() },
                        onClickShareAttachImg = { showShereSheetMediaAttach() }
                    )
                }
                4 -> {
                    // 音量調整。ちなみにAndroidの音量調整ではなく動画再生ライブラリ側で音量調整している。
                    val volumeLiveData = viewModel.exoplayerVolumeLiveData.observeAsState(initial = 1f)
                    VolumeMenu(
                        volume = volumeLiveData.value,
                        volumeChange = { volume -> viewModel.exoplayerVolumeLiveData.postValue(volume) }
                    )
                }
                5 -> {
                    // ニコ生ゲーム用WebView。
                    val isUseNicoNamaWebView = viewModel.isUseNicoNamaWebView.observeAsState(initial = false)
                    NicoLiveNicoNamaGameCard(
                        isNicoNamaGame = isUseNicoNamaWebView.value,
                        onSwitchNicoNamaGame = { isUse -> viewModel.isUseNicoNamaWebView.postValue(isUse) }
                    )
                }
            }
        }
    }
}

