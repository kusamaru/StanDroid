package com.kusamaru.standroid.nicovideo.compose

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
 * メニューのタブの部分だけ
 *
 * マイリスト登録とかメニューとか？
 *
 * @param selectedIndex 選択中の位置。
 * @param tabClick Tabを押した時。引数はタブの位置
 * */
@Composable
fun NicoVideoMenuTab(
    selectedIndex: Int,
    tabClick: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        backgroundColor = Color.Transparent,
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.mylist),
            tabIcon = painterResource(id = R.drawable.ic_folder_open_black_24dp),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(0) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.nicovideo_menu_comment_hide),
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
            tabName = stringResource(id = R.string.cache),
            tabIcon = painterResource(id = R.drawable.ic_folder_open_black_24dp),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(4) }
        )
        TabPadding(
            index = 5,
            tabName = stringResource(id = R.string.volume),
            tabIcon = painterResource(id = R.drawable.ic_volume_up_24px),
            selectedIndex = selectedIndex,
            tabClick = { tabClick(5) }
        )
    }
}


/**
 * 3DS、かんたんこめんと排除スイッチをおいてる。
 *
 * @param is3DSHide 3DSを排除するか
 * @param isSwitchHide Switchを排除するか
 * @param isKandanCommentHide かんたんコメントを排除するか
 * @param onDsSwitchChange 3DS排除スイッチを切り替えたときに呼ばれる
 * @Param onSwitchSwitchChange Switchのスイッチを切り替えたときに呼ばれる。なんちて
 * @param onKantanCommentSwitchChange かんたんコメント排除スイッチを切り替えたときに呼ばれる
 * */

@Composable
fun NicoVideoCommentHideMenu(
    is3DSHide: Boolean,
    isSwitchHide: Boolean,
    isKandanCommentHide: Boolean,
    onDsSwitchChange: (Boolean) -> Unit,
    onSwitchSwitchChange: (Boolean) -> Unit,
    onKantanCommentSwitchChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        // 3ds排除
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_device_3ds),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = is3DSHide,
                onCheckedChange = {
                    onDsSwitchChange(it)
                },
            )
        }
        // Switch排除
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_device_switch),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isSwitchHide,
                onCheckedChange = {
                    onSwitchSwitchChange(it)
                },
            )
        }
        // かんたんコメント排除
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_kantan_comment),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isKandanCommentHide,
                onCheckedChange = {
                    onKantanCommentSwitchChange(it)
                },
            )
        }
    }
}

/**
 * マイリスト追加ボタン
 *
 * @param onClickAddMylist マイリスト追加ボタンを押した時
 * @param onClickAddAtodemiru あとでみる追加ボタンを押した時
 * */
@Composable
fun NicoVideoMylistsMenu(onClickAddMylist: () -> Unit, onClickAddAtodemiru: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // マイリスト追加
        TextButton(
            onClick = { onClickAddMylist() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                    contentDescription = stringResource(id = R.string.add_mylist)
                )
                Text(
                    text = stringResource(id = R.string.add_mylist),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // あとでみる（旧：とりあえずマイリスト）
        TextButton(
            onClick = { onClickAddAtodemiru() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                    contentDescription = stringResource(id = R.string.add_atodemiru)
                )
                Text(
                    text = stringResource(id = R.string.add_atodemiru),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * その他のメニュー。画質変更とかスキップ秒数変更とか画面回転とか
 *
 * @param onClickQualityChange 画質変更ボタン押した時
 * @param onClickScreenRotation 画面回転ボタン押した時
 * @param onClickOpenBrowser ブラウザで開くボタンを押した時
 * @param onClickNgList NG一覧ボタンを押した時
 * @param onClickKotehanList コテハン一覧ボタンを押した時
 * @param onClickSkipSetting スキップ秒数変更ボタン押した時
 * @param
 * */
@Composable
fun NicoVideoOtherButtonMenu(
    onClickQualityChange: () -> Unit,
    onClickCopyVideoId: () -> Unit,
    onClickScreenRotation: () -> Unit,
    onClickOpenBrowser: () -> Unit,
    onClickNgList: () -> Unit,
    onClickKotehanList: () -> Unit,
    onClickSkipSetting: () -> Unit,
    onClickShowNicoAd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // 画質変更
        TextButton(onClick = { onClickQualityChange() })
        {
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
        // ニコニ広告
        TextButton(onClick = { onClickShowNicoAd() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_money_24px),
                    contentDescription = stringResource(id = R.string.nicoads)
                )
                Text(
                    text = stringResource(id = R.string.nicoads),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // 画面回転
        TextButton(onClick = { onClickScreenRotation() })
        {
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
        // 動画IDコピー
        TextButton(onClick = { onClickCopyVideoId() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_content_paste_black_24dp),
                    contentDescription = stringResource(id = R.string.video_id_copy)
                )
                Text(
                    text = stringResource(id = R.string.video_id_copy),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // ブラウザで開く
        TextButton(onClick = { onClickOpenBrowser() })
        {
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
        TextButton(onClick = { onClickNgList() })
        {
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
        TextButton(onClick = { onClickKotehanList() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_account_box_24),
                    contentDescription = stringResource(id = R.string.kotehan)
                )
                Text(
                    text = stringResource(id = R.string.kotehan_list),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // スキップ秒数
        TextButton(onClick = { onClickSkipSetting() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_redo_black_24dp),
                    contentDescription = stringResource(id = R.string.skip_setting)
                )
                Text(
                    text = stringResource(id = R.string.skip_setting),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * キャッシュ取得ボタン
 *
 * @param onClickCacheGet キャッシュ取得ボタン押した時
 * @param onClickCacheGetEco キャッシュ取得ボタン（エコノミー）押した時
 * @param isCachePlay キャッシュ再生の場合はtrueにすることで動画情報を更新するボタンを表示します。ですがtrueの場合は前述のボタンを表示しません。
 * @param onClickCacheUpdate キャッシュの動画情報更新ボタンを押した時
 * */
@Composable
fun NicoVideoCacheMenu(
    onClickCacheGet: () -> Unit,
    onClickCacheGetEco: () -> Unit,
    isCachePlay: Boolean,
    onClickCacheUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // キャッシュ再生 か それ以外
        if (isCachePlay) {
            // 情報更新ボタン表示
            TextButton(onClick = { onClickCacheUpdate() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh_black_24dp),
                        contentDescription = stringResource(id = R.string.get_cache_re_get)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache_re_get),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
        } else {
            // 取得ボタン
            TextButton(onClick = { onClickCacheGet() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                        contentDescription = stringResource(id = R.string.get_cache)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
            // 取得ボタン（エコノミー）
            TextButton(onClick = { onClickCacheGetEco() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                        contentDescription = stringResource(id = R.string.get_cache_eco)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache_eco),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
        }
    }
}