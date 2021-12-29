package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.NumberSlider
import io.github.takusan23.tatimidroid.tool.TimeFormatTool
import kotlinx.coroutines.delay

/**
 * 生放送のプレイヤーに重ねるUI
 * @param liveTitle 番組タイトル
 * @param liveId 番組ID
 * @param isFullScreen 全画面再生時ならtrue
 * @param isDisableMiniPlayerMode ミニプレイヤーモードを無効にしている場合はtrue。代わりに戻るアイコンを出します（クリックイベントは同じ）
 * @param isMiniPlayer ミニプレイヤー時ならtrue
 * @param isConnectedWiFi Wi-Fi接続時はtrue。右上のネットワーク状態アイコンで使う
 * @param isShowCommentCanvas コメントを描画している場合はtrue。アイコンで使う
 * @param programCurrentTime 現在の時間
 * @param programEndTime 番組終了時刻
 * @param onClickBackgroundPlayer バックグラウンド再生を押したときに呼ばれる
 * @param onClickCommentDraw コメントを描画する、しないボタンを押したときに呼ばれる
 * @param onClickCommentPost コメント投稿ボタン（全画面UIのみ）を押したときに呼ばれる
 * @param onClickFullScreen 全画面ボタンを押したときに呼ばれる
 * @param onClickMiniPlayer ミニプレイヤー遷移ボタン、[isDisableMiniPlayerMode]のときは矢印ボタンを押したときに呼ばれる
 * @param onClickNetwork ネットワーク状態ボタンを押したときに呼ばれる
 * @param onClickPopUpPlayer ポップアップ再生ボタンを押したときに呼ばれる
 * @param isAudioOnlyMode 音声のみの再生時はtrueにしてね
 * @param isTimeShiftMode タイムシフト再生時はtrueにしてね。シークバーを出します
 * @param currentPosition 番組経過時間。番組開始時間から数えて
 * @param onTsSeek タイムシフト再生中のみ。シークバーいじったら呼ばれる。0からの整数
 * @param duration タイムシフト再生時のみ。番組の時間。秒で
 * */
@Composable
fun NicoLivePlayerUI(
    liveTitle: String,
    liveId: String,
    isMiniPlayer: Boolean,
    isDisableMiniPlayerMode: Boolean,
    isFullScreen: Boolean = false,
    isConnectedWiFi: Boolean = false,
    isShowCommentCanvas: Boolean = true,
    isAudioOnlyMode: Boolean = false,
    isTimeShiftMode: Boolean = false,
    currentPosition: Long = 0L,
    duration: Long = 0L,
    onClickMiniPlayer: () -> Unit,
    onClickFullScreen: () -> Unit,
    onClickNetwork: () -> Unit,
    onClickCommentDraw: () -> Unit,
    onClickPopUpPlayer: () -> Unit,
    onClickBackgroundPlayer: () -> Unit,
    onClickCommentPost: (String) -> Unit,
    onTsSeek: (Long) -> Unit = { },
) {

    // プレイヤー押したらプレイヤーUIを非表示にしたいので
    val isShowPlayerUI = remember { mutableStateOf(true) }
    // コメント本文
    val commentPostText = remember { mutableStateOf("") }
    // コメント入力中かどうか
    val isInputingComment = remember { mutableStateOf(false) }
    // シーク中かどうか
    val isTouchingSlider = remember { mutableStateOf(false) }
    // 再生位置
    val seekBarValue = remember { mutableStateOf(currentPosition) }
    // シーク操作中は引数の値が更新されても無視
    if (!isTouchingSlider.value) {
        seekBarValue.value = currentPosition
    }

    // 一定時間後にfalseにする
    LaunchedEffect(key1 = isShowPlayerUI.value, block = {
        delay(3 * 1000)
        // コメント入力してないとき
        if (isShowPlayerUI.value && !isInputingComment.value) {
            isShowPlayerUI.value = false
        }
    })

    /** まとめて色を変えられる */
    Surface(
        contentColor = Color.White, // アイコンとかテキストの色をまとめて指定
        color = Color.Transparent,
        modifier = Modifier
            .clickable(
                indication = null, // Rippleいらんわ
                interactionSource = remember { MutableInteractionSource() },
                onClick = { isShowPlayerUI.value = !isShowPlayerUI.value },
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {

            // 音声のみだよ
            if (isAudioOnlyMode) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.ic_tatimidroid_playlist_play_black),
                        contentDescription = "音声のみ",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = stringResource(id = R.string.audio_only_play))
                }
            }

            // UI表示
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
                                text = liveTitle,
                                maxLines = 1
                            )
                            Text(
                                text = liveId,
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
                                    painter = if (isConnectedWiFi) painterResource(id = R.drawable.ic_wifi_black_24dp) else painterResource(id = R.drawable.ic_signal_cellular_alt_black_24dp),
                                    contentDescription = "ミニプレイヤーへ"
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
                    // 真ん中。アンケートとか
                    Box(
                        modifier = if (!isInputingComment.value) {
                            Modifier.weight(1f)
                        } else {
                            Modifier
                        },
                        contentAlignment = Alignment.Center,
                    ) { }
                    // 再生時間など
                    Row(modifier = Modifier.padding(10.dp)) {
                        Text(text = TimeFormatTool.timeFormat(currentPosition), modifier = Modifier.align(alignment = Alignment.CenterVertically))
                        if (isTimeShiftMode && !isMiniPlayer) {
                            // TS再生用にシークバーを出す。整数用にSliderを作った
                            NumberSlider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 5.dp, end = 5.dp),
                                maxValue = duration,
                                currentValue = seekBarValue.value,
                                onValueChangeFinished = {
                                    isTouchingSlider.value = false
                                    onTsSeek(seekBarValue.value)
                                },
                                onValueChange = {
                                    isTouchingSlider.value = true
                                    seekBarValue.value = it
                                },
                            )
                        } else {
                            // ダイナモ感覚　ダイナモ感覚 YO YO YO YEAR!
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text(text = TimeFormatTool.timeFormat(duration), modifier = Modifier.align(alignment = Alignment.CenterVertically))
                    }
                    // コメント投稿エリア。全画面再生時のみ
                    if (isFullScreen && !isMiniPlayer && !isTimeShiftMode) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isInputingComment.value = it.isFocused } // フォーカスが当たっているか
                                .padding(5.dp),
                            value = commentPostText.value,
                            label = { Text(text = stringResource(id = R.string.comment)) },
                            onValueChange = { commentPostText.value = it },
                            leadingIcon = {
                                IconButton(onClick = { commentPostText.value = "" }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_backspace_24px), contentDescription = "クリア")
                                }
                            },
                            maxLines = 1,
                            // Enterキーを送信にする
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                // 送信！
                                onClickCommentPost(commentPostText.value)
                                commentPostText.value = ""
                            }),
                            trailingIcon = {
                                // 送信ボタン
                                IconButton(onClick = {
                                    onClickCommentPost(commentPostText.value)
                                    commentPostText.value = ""
                                }) {
                                    Icon(painter = painterResource(id = R.drawable.ic_send_black), contentDescription = "送信")
                                }
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                unfocusedLabelColor = Color.White,
                                focusedLabelColor = Color.White,
                                disabledLabelColor = Color.White,
                                errorLabelColor = Color.White,
                                cursorColor = Color.White,
                                errorCursorColor = Color.White,
                                backgroundColor = Color.White,
                                leadingIconColor = Color.White,
                                trailingIconColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}