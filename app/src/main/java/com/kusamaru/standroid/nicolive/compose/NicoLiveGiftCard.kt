package com.kusamaru.standroid.nicolive.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.compose.TabPadding
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftHistoryUserData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftItemData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftRankingUserData
import com.kusamaru.standroid.nicolive.adapter.GiftHistoryRecyclerViewAdapter
import com.kusamaru.standroid.nicolive.adapter.GiftRankingRecyclerViewAdapter
import com.kusamaru.standroid.nicovideo.compose.getBitmapCompose
import com.kusamaru.standroid.R

/**
 * トータルギフトポイントと、投げられたアイテムの一覧を横に並べるやつ
 *
 * @param totalPoint トータルポイント
 * @param giftItemDataList 投げられたアイテムのデータクラスの配列
 * */
@Composable
fun NicoLiveGiftTop(
    totalPoint: Int,
    giftItemDataList: ArrayList<NicoLiveGiftItemData>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // トータルポイント
        Text(
            text = "${stringResource(id = R.string.total)}：$totalPoint pt",
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterHorizontally),
            fontSize = 20.sp
        )
        Divider(modifier = Modifier.padding(10.dp))
        // 投げられたアイテムを横に並べていく
        LazyRow(content = {
            items(giftItemDataList) { data ->
                NicoLiveGiftIcon(giftItemData = data)
            }
        })
    }
}

/**
 * [NicoLiveGiftTop]のアイテムの横スクロールのひとつひとつの部品
 *
 * @param giftItemData 投げ銭アイテムのデータクラス
 * */
@Composable
fun NicoLiveGiftIcon(giftItemData: NicoLiveGiftItemData) {
    Column(modifier = Modifier.padding(10.dp)) {
        // 画像取得
        val giftIcon = getBitmapCompose(url = giftItemData.thumbnailUrl)?.asImageBitmap()
        if (giftIcon != null) {
            Image(
                bitmap = giftIcon,
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp)
                    .align(Alignment.CenterHorizontally),
                contentDescription = giftItemData.itemName
            )
            // アイテムの名前
            Text(
                text = giftItemData.itemName,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            // どんだけ売れたか。まいどありー
            Text(
                text = "${giftItemData.totalSoldCount} 個",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * 投げ銭履歴を表示する
 *
 * @param giftHistoryUserDataList 投げ銭履歴ユーザー配列
 * */
@Composable
fun NicoLiveGiftRankingList(giftRankingUserDataList: ArrayList<NicoLiveGiftRankingUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            return@AndroidView RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = GiftRankingRecyclerViewAdapter(giftRankingUserDataList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * 投げ銭履歴を表示する
 *
 * @param giftHistoryUserDataList 投げ銭履歴ユーザー配列
 * */
@Composable
fun NicoLiveGiftHistoryList(giftHistoryUserDataList: ArrayList<NicoLiveGiftHistoryUserData>) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            return@AndroidView RecyclerView(context).apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = GiftHistoryRecyclerViewAdapter(giftHistoryUserDataList)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }
    )
}

/**
 * 投げ銭履歴、ランキング切り替えTabLayout
 *
 * @param selectTabIndex 現在選択中のタブの位置
 * @param onClickTabItem タブを押した時
 * */
@Composable
fun NicoLiveGiftTab(
    selectTabIndex: Int,
    onClickTabItem: (Int) -> Unit,
) {
    TabRow(
        backgroundColor = Color.Transparent,
        selectedTabIndex = selectTabIndex
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.gift_history),
            tabIcon = painterResource(id = R.drawable.ic_history_24px),
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.gift_ranking),
            tabIcon = painterResource(id = R.drawable.ic_signal_cellular_alt_black_24dp),
            selectedIndex = selectTabIndex,
            tabClick = { onClickTabItem(it) }
        )
    }
}