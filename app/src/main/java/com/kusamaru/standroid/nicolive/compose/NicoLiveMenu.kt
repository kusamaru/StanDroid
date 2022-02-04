package com.kusamaru.standroid.nicolive.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.compose.TabPadding
import com.kusamaru.standroid.R

/**
 * ニコ生のメニューCardのTab
 *
 * @param selectedIndex 選択中のタブの位置
 * @param tabClick タブを押したとき
 * */
@Composable
fun NicoLiveMenuTab(
    selectedIndex: Int,
    tabClick: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        backgroundColor = Color.Transparent,
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.menu),
            tabIcon = painterResource(id = R.drawable.ic_outline_toggle_on_24),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(0) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.comment_viewer_setting),
            tabIcon = painterResource(id = R.drawable.ic_outline_comment_24px),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(1) }
        )
        TabPadding(
            index = 2,
            tabName = stringResource(id = R.string.menu),
            tabIcon = painterResource(id = R.drawable.ic_outline_menu_24),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(2) }
        )
        TabPadding(
            index = 3,
            tabName = stringResource(id = R.string.share),
            tabIcon = painterResource(id = R.drawable.ic_share),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(3) }
        )
        TabPadding(
            index = 4,
            tabName = stringResource(id = R.string.volume),
            tabIcon = painterResource(id = R.drawable.ic_volume_up_24px),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(4) }
        )
        TabPadding(
            index = 5,
            tabName = stringResource(id = R.string.nico_nama_game),
            tabIcon = painterResource(id = R.drawable.ic_videogame_asset_black_24dp),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(5) }
        )
    }
}

/**
 * スイッチ系の設定。低遅延OFFとか
 *
 * @param isHideUNEIComment 運営コメントを非表示にする場合はtrue
 * @param onSwitchHideUNEIComment 運営コメントを非表示にするスイッチを切り替えたら呼ばれる
 * @param isHideEmotion エモーション非表示
 * @param onSwitchHideEmotion エモーション非表示を切り替えたら呼ばれる
 * @param isHideTokumeiComment 匿名コメントを非表示にするか
 * @param onSwitchHideTokumeiComment 匿名コメントを非表示に設定するスイッチを切り替えたら呼ばれる
 * @param isLowLatency 低遅延モードかどうか
 * @param onSwitchLowLatency 低遅延モードを切り替えたときに呼ばれる
 * @param isNotReceiveLive 映像の受信をやめるかどうか
 * @param onSwitchNotReceiveLive 映像の受信をやめる設定が切り替わったら呼ばれる
 * */
@Composable
fun NicoLiveSwitchMenu(
    isHideUNEIComment: Boolean,
    onSwitchHideUNEIComment: (Boolean) -> Unit,
    isHideEmotion: Boolean,
    onSwitchHideEmotion: (Boolean) -> Unit,
    isHideTokumeiComment: Boolean,
    onSwitchHideTokumeiComment: (Boolean) -> Unit,
    isLowLatency: Boolean,
    onSwitchLowLatency: (Boolean) -> Unit,
    isNotReceiveLive: Boolean,
    onSwitchNotReceiveLive: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        // 運コメ非表示
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.hide_info_perm),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isHideUNEIComment,
                onCheckedChange = { onSwitchHideUNEIComment(it) },
            )
        }
        // エモーション非表示
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.hide_emotion),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isHideEmotion,
                onCheckedChange = { onSwitchHideEmotion(it) },
            )
        }
        // エモーション非表示
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.iyayo_hidden),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isHideTokumeiComment,
                onCheckedChange = { onSwitchHideTokumeiComment(it) },
            )
        }
        // 低遅延モード
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.low_latency),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isLowLatency,
                onCheckedChange = { onSwitchLowLatency(it) },
            )
        }
        // 映像の受信をやめる
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.not_receive_live),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isNotReceiveLive,
                onCheckedChange = { onSwitchNotReceiveLive(it) },
            )
        }
    }
}

/**
 * コメビュ設定のメニュー。一行表示とか
 *
 * @param isHideUserId ユーザーIDを表示しないかどうか
 * @param onSwitchCommentOneLine ユーザーIDを非表示にしないスイッチを切り替えたとき
 * @param isCommentOneLine コメント本文を一行で表示するか
 * @param onSwitchHideUserId コメント本文を一行で表示するスイッチを切り替えたとき
 * */
@Composable
fun NicoLiveCommentViewerMenu(
    isHideUserId: Boolean,
    onSwitchHideUserId: (Boolean) -> Unit,
    isCommentOneLine: Boolean,
    onSwitchCommentOneLine: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        // IDを非表示にする
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.setting_hidden_id),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isHideUserId,
                onCheckedChange = { onSwitchHideUserId(it) },
            )
        }
        // コメントの最大行を一行に制限
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.setting_one_line),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isCommentOneLine,
                onCheckedChange = { onSwitchCommentOneLine(it) },
            )
        }
    }
}

/**
 * ニコ生のボタン系の設定。画質変更とか
 *
 * @param onClickQualityChange 画質変更ボタンをおしたとき
 * @param onClickScreenRotation 画面回転ボタンを押したとき
 * @param onClickCopyProgramId 番組IDコピーボタンを押したとき
 * @param onClickCopyCommunityId コミュIDコピーボタンを押したとき
 * @param onClickNGList NG一覧表示ボタンを押したとき
 * @param onClickKotehanList コテハン一覧表示ボタンを押したとき
 * @param onClickHomeScreenPin ホーム画面に追加ボタンを押したとき
 * @param onClickOpenBrowser ブラウザで開くボタンを押したとき
 * */
@Composable
fun NicoLiveButtonMenu(
    onClickQualityChange: () -> Unit,
    onClickScreenRotation: () -> Unit,
    onClickCopyProgramId: () -> Unit,
    onClickCopyCommunityId: () -> Unit,
    onClickOpenBrowser: () -> Unit,
    onClickNGList: () -> Unit,
    onClickKotehanList: () -> Unit,
    onClickHomeScreenPin: () -> Unit,
    onClickLaunchFloatingCommentViewer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
    ) {
        // 画質変更
        TextButton(onClick = { onClickQualityChange() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_photo_filter_24px),
                    contentDescription = stringResource(id = R.string.quality)
                )
                Text(
                    text = stringResource(id = R.string.quality),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // ホーム画面に追加
        TextButton(onClick = { onClickHomeScreenPin() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_add_to_home_screen_24),
                    contentDescription = stringResource(id = R.string.add_homescreen)
                )
                Text(
                    text = stringResource(id = R.string.add_homescreen),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // 画面回転
        TextButton(onClick = { onClickScreenRotation() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_screen_rotation_24px),
                    contentDescription = stringResource(id = R.string.landscape_portrait)
                )
                Text(
                    text = stringResource(id = R.string.landscape_portrait),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // フローティングコメビュ起動。なおAndroid 10以前は利用できないように
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            TextButton(onClick = { onClickLaunchFloatingCommentViewer() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_library_books_24px),
                        contentDescription = stringResource(id = R.string.floating_comment_viewer)
                    )
                    Text(
                        text = stringResource(id = R.string.floating_comment_viewer),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
        }
        // 番組IDコピー
        TextButton(onClick = { onClickCopyProgramId() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_paste_black_24dp),
                    contentDescription = stringResource(id = R.string.copy_program_id)
                )
                Text(
                    text = stringResource(id = R.string.copy_program_id),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // コミュIDコピー
        TextButton(onClick = { onClickCopyCommunityId() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_paste_black_24dp),
                    contentDescription = stringResource(id = R.string.copy_communityid)
                )
                Text(
                    text = stringResource(id = R.string.copy_communityid),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // ブラウザで開く
        TextButton(onClick = { onClickOpenBrowser() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                    contentDescription = stringResource(id = R.string.open_browser)
                )
                Text(
                    text = stringResource(id = R.string.open_browser),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // NG一覧
        TextButton(onClick = { onClickNGList() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_block_24px),
                    contentDescription = stringResource(id = R.string.ng_list)
                )
                Text(
                    text = stringResource(id = R.string.ng_list),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // コテハン
        TextButton(onClick = { onClickKotehanList() }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_account_box_24),
                    contentDescription = stringResource(id = R.string.kotehan_list)
                )
                Text(
                    text = stringResource(id = R.string.kotehan_list),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * ニコ生ゲームを遊ぶために再生画面の上にWebViewを重ねるか
 *
 * @param isNicoNamaGame ニコ生ゲームが有効かどうか
 * @param onSwitchNicoNamaGame ニコ生ゲーム有効スイッチ切り替えた時
 * */
@Composable
fun NicoLiveNicoNamaGameCard(
    isNicoNamaGame: Boolean,
    onSwitchNicoNamaGame: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_info_24px),
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(id = R.string.nico_nama_game_description))
                Text(text = stringResource(id = R.string.nico_nama_game_engineers))
            }
        }
        Divider()
        Row(modifier = Modifier.padding(10.dp)) {
            Text(
                text = stringResource(id = R.string.play_nico_nama_game),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isNicoNamaGame,
                onCheckedChange = { onSwitchNicoNamaGame(it) }
            )
        }
    }
}
