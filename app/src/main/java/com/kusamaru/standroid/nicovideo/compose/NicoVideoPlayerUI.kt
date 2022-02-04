package com.kusamaru.standroid.nicovideo.compose

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kusamaru.standroid.R
import com.kusamaru.standroid.compose.NumberSlider
import com.kusamaru.standroid.tool.TimeFormatTool
import kotlinx.coroutines.delay

/**
 * 動画のプレイヤーUI。将来的にComposeに移行するんで準備
 *
 * @param videoTitle 動画タイトル
 * @param videoId 動画ID
 * @param isMiniPlayer ミニプレイヤー時はtrue
 * @param isDisableMiniPlayerMode ミニプレイヤー無効時はtrue
 * @param isFullScreen 全画面時はtrue
 * @param isConnectedWiFi Wi-Fi接続時はtrue
 * @param isShowCommentCanvas コメント描画してるならtrue
 * @param isRepeat リピート再生時はtrue
 * @param isCachePlay キャッシュ再生時はtrue
 * @param isLoading 読込中はtrue
 * @param isPlaylistMode 連続再生中はtrue
 * @param isPlaying 再生中はtrue
 * @param currentPosition 動画の現在の位置
 * @param duration 動画の時間
 * @param onClickMiniPlayer ミニプレイヤーボタンを押したときに呼ばれる
 * @param onClickFullScreen 全画面ボタン押したら呼ばれる
 * @param onClickNetwork ネットワーク状態ボタンを押したときに呼ばれる
 * @param onClickRepeat リピートモード押したときに呼ばれる
 * @param onClickPlayType 再生の種類を押したときに呼ばれる
 * @param onClickCommentDraw コメント描画ON、OFF押したときに呼ばれる
 * @param onClickPopUpPlayer ポップアップ再生ボタンを押したときに呼ばれる
 * @param onClickBackgroundPlayer バックグラウンド再生ボタンを押したときに呼ばれる
 * @param onClickPicture 画像として保存を押したときに呼ばれる
 * @param onClickPauseOrPlay 一時停止ボタンを押したときに呼ばれる
 * @param onClickPrev 連続再生時のみ、前の動画に戻るボタンを押したら呼ばれる
 * @param onClickNext 連続再生時のみ、次の動画に進むボタンを押したら呼ばれる
 * @param onDoubleClickSeek ダブルクリックでできるシークを行ったときに呼ばれる。trueで戻るシーク
 * @param onSeek シークバーいじったら呼ばれる
 * @param onTouchingSeek シークバーを操作中ならtrue。離したらfalse
 * */
@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
fun NicoVideoPlayerUI(
    videoTitle: String,
    videoId: String,
    isMiniPlayer: Boolean,
    isDisableMiniPlayerMode: Boolean,
    isFullScreen: Boolean = false,
    isConnectedWiFi: Boolean = false,
    isShowCommentCanvas: Boolean = true,
    isRepeat: Boolean = false,
    isCachePlay: Boolean = false,
    isLoading: Boolean = false,
    isPlaylistMode: Boolean = false,
    isPlaying: Boolean = false,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    onClickMiniPlayer: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickNetwork: () -> Unit,
    onClickRepeat: () -> Unit,
    onClickCommentDraw: () -> Unit,
    onClickPopUpPlayer: () -> Unit,
    onClickBackgroundPlayer: () -> Unit,
    onClickPicture: () -> Unit,
    onClickPauseOrPlay: () -> Unit,
    onClickPrev: () -> Unit,
    onClickNext: () -> Unit,
    onDoubleClickSeek: (isPrev: Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onTouchingSeek: (Boolean) -> Unit,
) {

    // プレイヤー押したらプレイヤーUIを非表示にしたいので
    val isShowPlayerUI = remember { mutableStateOf(true) }
    // シーク中かどうか
    val isTouchingSlider = remember { mutableStateOf(false) }
    onTouchingSeek(isTouchingSlider.value)
    // 再生位置
    val seekBarValue = remember { mutableStateOf(currentPosition) }
    // シーク操作中は引数の値が更新されても無視
    if (!isTouchingSlider.value) {
        seekBarValue.value = currentPosition
    }
    // クリック位置。X座標
    val clickXPos = remember { mutableStateOf(0f) }

    // 一定時間後にfalseにする
    LaunchedEffect(key1 = isShowPlayerUI.value, block = {
        delay(3 * 1000L)
        // シーク中ではない場合
        if (isShowPlayerUI.value && !isTouchingSlider.value) {
            isShowPlayerUI.value = false
        }
    })
    // dp -> px変換用にdensity取得
    val context = LocalContext.current
    val density = remember { context.resources.displayMetrics.density }

    // プレイヤーのUIの大きさがほしいのでBoxWithなんたらをつかう
    BoxWithConstraints {

        // まとめて色を変えられる
        Surface(
            contentColor = Color.White, // アイコンとかテキストの色をまとめて指定
            color = Color.Transparent,
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { isShowPlayerUI.value = !isShowPlayerUI.value },
                        // ピクセル単位なのでdpをpxにする必要あり
                        onDoubleTap = { onDoubleClickSeek(clickXPos.value < ((maxWidth.value * density) / 2)) }
                    )
                }
                .pointerInteropFilter { motionEvent ->
                    clickXPos.value = motionEvent.x
                    false
                }
        ) {
            // FrameLayout的な
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {

                // 読込中
                if (isLoading) {
                    CircularProgressIndicator()
                }

                // UI表示。読込中以外
                if (isShowPlayerUI.value) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(color = Color.Black.copy(alpha = 0.8f))
                    ) {
                        // タイトル、閉じるボタン
                        Row {
                            IconButton(onClick = { onClickMiniPlayer() }) {
                                Icon(
                                    painter = when {
                                        isDisableMiniPlayerMode -> painterResource(id = R.drawable.ic_arrow_back_black_24dp)
                                        isMiniPlayer -> painterResource(id = R.drawable.ic_expand_less_black_24dp)
                                        else -> painterResource(id = R.drawable.ic_expand_more_24px)
                                    },
                                    contentDescription = "ミニプレイヤーへ"
                                )
                            }
                            Column {
                                Text(
                                    text = videoTitle,
                                    maxLines = 1
                                )
                                Text(
                                    text = videoId,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        // ミニプレイヤー時はボタンを描画しない
                        if (!isMiniPlayer) {
                            // アイコン
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    IconButton(onClick = { onClickPicture() }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.screen_shot_icon),
                                            contentDescription = "スクショ"
                                        )
                                    }
                                }
                                IconButton(onClick = { onClickBackgroundPlayer() }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_background_icon_black),
                                        contentDescription = "バッググラウンド"
                                    )
                                }
                                IconButton(onClick = { onClickPopUpPlayer() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_popup_icon_black),
                                        contentDescription = "ポップアップ"
                                    )
                                }
                                IconButton(onClick = { onClickCommentDraw() }) {
                                    Icon(
                                        painter = if (isShowCommentCanvas) painterResource(id = R.drawable.ic_comment_on) else painterResource(id = R.drawable.ic_comment_off),
                                        contentDescription = "コメント描画ON"
                                    )
                                }
                                IconButton(onClick = { onClickNetwork() }) {
                                    Icon(
                                        painter = when {
                                            isCachePlay -> painterResource(id = R.drawable.ic_cache_icon_list)
                                            isConnectedWiFi -> painterResource(id = R.drawable.ic_wifi_black_24dp)
                                            else -> painterResource(id = R.drawable.ic_signal_cellular_alt_black_24dp)
                                        },
                                        contentDescription = "海鮮"
                                    )
                                }
                                IconButton(onClick = { onClickRepeat() }) {
                                    Icon(
                                        painter = if (isRepeat) painterResource(id = R.drawable.ic_repeat_one_24px) else painterResource(id = R.drawable.ic_repeat_black_24dp),
                                        contentDescription = "リピートモード"
                                    )
                                }
                                IconButton(onClick = { onClickFullScreen() }) {
                                    Icon(
                                        painter = if (isFullScreen) painterResource(id = R.drawable.ic_fullscreen_exit_black_24dp) else painterResource(id = R.drawable.ic_fullscreen_black_24dp),
                                        contentDescription = "全画面"
                                    )
                                }
                            }
                        }
                        // 真ん中。一時停止ボタンなど
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {

                            // 前の動画、一時停止、次の動画ボタン
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                // 連続再生時でミニプレイヤーじゃないときのみ前、次ボタン表示
                                if (isPlaylistMode && !isMiniPlayer) {
                                    Icon(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = rememberRipple(bounded = false, radius = 30.dp),
                                                onClick = { onClickPrev() }
                                            ),
                                        painter = painterResource(id = R.drawable.ic_skip_previous_black_24dp),
                                        contentDescription = "前の動画"
                                    )
                                    Spacer(modifier = Modifier.size(50.dp))
                                }
                                Icon(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = rememberRipple(bounded = false, radius = 50.dp),
                                            onClick = { onClickPauseOrPlay() }
                                        ),
                                    painter = painterResource(id = if (isPlaying) R.drawable.ic_pause_black_24dp else R.drawable.ic_play_arrow_24px),
                                    contentDescription = "一時停止、再生"
                                )
                                if (isPlaylistMode && !isMiniPlayer) {
                                    Spacer(modifier = Modifier.size(50.dp))
                                    Icon(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = rememberRipple(bounded = false, radius = 30.dp),
                                                onClick = { onClickNext() }
                                            ),
                                        painter = painterResource(id = R.drawable.ic_skip_next_black_24dp),
                                        contentDescription = "次の動画"
                                    )
                                }
                            }
                        }
                        // 再生時間など
                        Row(modifier = Modifier.padding(10.dp)) {
                            Text(text = TimeFormatTool.timeFormat(currentPosition), modifier = Modifier.align(alignment = Alignment.CenterVertically))
                            if (!isMiniPlayer) {
                                // 整数用にSliderを作った
                                NumberSlider(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 5.dp, end = 5.dp),
                                    maxValue = duration,
                                    currentValue = seekBarValue.value,
                                    onValueChangeFinished = {
                                        isTouchingSlider.value = false
                                        onSeek(seekBarValue.value)
                                    },
                                    onValueChange = {
                                        isTouchingSlider.value = true
                                        seekBarValue.value = it
                                    },
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            Text(text = TimeFormatTool.timeFormat(duration), modifier = Modifier.align(alignment = Alignment.CenterVertically))
                        }
                    }
                }
            }
        }
    }
}
