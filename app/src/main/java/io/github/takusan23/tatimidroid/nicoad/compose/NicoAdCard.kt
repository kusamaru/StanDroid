package io.github.takusan23.tatimidroid.nicoad.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.compose.TabPadding
import io.github.takusan23.tatimidroid.nicoapi.nicoad.NicoAdData
import io.github.takusan23.tatimidroid.nicoapi.nicoad.NicoAdHistoryUserData
import io.github.takusan23.tatimidroid.nicoapi.nicoad.NicoAdRankingUserData
import io.github.takusan23.tatimidroid.nicolive.adapter.NicoAdHistoryAdapter
import io.github.takusan23.tatimidroid.nicolive.adapter.NicoAdRankingAdapter
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.R

/**
 * ニコニ広告の累計ポイント、期間中ポイントを表示する
 *
 * @param nicoAdData ニコニ広告のデータ
 * @param onClickOpenBrowser ブラウザで開くボタンを押した時
 * */
@Composable
fun NicoAdTop(
    nicoAdData: NicoAdData,
    onClickOpenBrowser: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 画像
            val bitmap = getBitmapCompose(url = nicoAdData.thumbnailUrl)?.asImageBitmap()
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    modifier = Modifier
                        .width(160.dp)
                        .height(90.dp)
                        .padding(5.dp),
                    contentDescription = "サムネイル"
                )
            }
            // タイトル
            Column {
                Text(
                    text = nicoAdData.contentTitle,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(5.dp)
                )
                Text(
                    text = nicoAdData.contentId,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }
        // 区切り
        Divider(modifier = Modifier.padding(5.dp))
        // 広告ポイント
        Row(modifier = Modifier.padding(5.dp)) {
            // 累計ポイント
            Column(
                Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.nicoad_total),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${nicoAdData.totalPoint} pt",
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // 期間中ポイント
            Column(
                Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.nicoad_active),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${nicoAdData.activePoint} pt",
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            // ブラウザ起動ボタン
            IconButton(onClick = { onClickOpenBrowser() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                    contentDescription = stringResource(id = R.string.open_browser)
                )
            }
        }
    }
}

/**
 * ニコニ広告のランキング一覧表示
 *
 * @param nicoAdRankingUserList ニコニ広告のランキングユーザー配列
 * */
@Composable
fun NicoAdRankingList(nicoAdRankingUserList: ArrayList<NicoAdRankingUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = NicoAdRankingAdapter(nicoAdRankingUserList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * ニコニ広告の履歴一覧表示
 *
 * @param nicoAdHistoryUserList ニコニ広告のユーザー履歴配列
 * */
@Composable
fun NicoAdHistoryList(nicoAdHistoryUserList: ArrayList<NicoAdHistoryUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = NicoAdHistoryAdapter(nicoAdHistoryUserList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * ニコニ広告の貢献度ランキング、履歴選択用タブレイアウト
 *
 * @param selectTabIndex 現座選択中のタブのいち
 * @param onClickTabItem タブを押した時
 * */
@Composable
fun NicoAdTabMenu(
    selectTabIndex: Int,
    onClickTabItem: (Int) -> Unit,
) {
    TabRow(
        backgroundColor = Color.Transparent,
        selectedTabIndex = selectTabIndex
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.nico_ad_ranking),
            tabIcon = painterResource(id = R.drawable.ic_signal_cellular_alt_black_24dp),
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.nico_ad_history),
            tabIcon = painterResource(id = R.drawable.ic_history_24px),
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
    }
}