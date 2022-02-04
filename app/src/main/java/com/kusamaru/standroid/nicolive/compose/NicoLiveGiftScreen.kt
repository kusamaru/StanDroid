package com.kusamaru.standroid.nicolive.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveGiftViewModel

/**
 * [com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveGiftBottomFragment]で使う
 * */
@Composable
fun NicoLiveGiftScreen(viewModel: NicoLiveGiftViewModel) {

    /** 投げ銭履歴LiveData */
    val historyDataLiveData = viewModel.nicoLiveGiftHistoryUserListLiveData.observeAsState()

    /** 投げ銭ランキングLiveData */
    val rankingDataLiveData = viewModel.nicoLiveGiftRankingUserListLiveData.observeAsState()

    /** 投げ銭トータルポイントLiveData */
    val totalPointLiveData = viewModel.nicoLiveGiftTotalPointLiveData.observeAsState()

    /** 投げられたアイテム一覧LiveData */
    val giftItemListLiveData = viewModel.nicoLiveGiftItemListLiveData.observeAsState()

    Column {
        if (totalPointLiveData.value != null && giftItemListLiveData.value != null) {
            Card(
                modifier = Modifier.padding(5.dp),
                elevation = 5.dp,
                shape = RoundedCornerShape(3.dp)
            ) {
                // トータルポイントと投げられたアイテムを横に並べるやつ
                NicoLiveGiftTop(
                    totalPoint = totalPointLiveData.value!!,
                    giftItemDataList = giftItemListLiveData.value!!
                )
            }
        }

        // タブとランキング、履歴表示
        Card(
            modifier = Modifier.padding(5.dp),
            elevation = 5.dp,
            shape = RoundedCornerShape(3.dp)
        ) {
            Column {
                // 現在選択中ダブ
                val selectTab = remember { mutableStateOf(0) }
                // タブ
                NicoLiveGiftTab(
                    selectTabIndex = selectTab.value,
                    onClickTabItem = { selectTab.value = it }
                )
                // どっちを表示するか
                if (historyDataLiveData.value != null && rankingDataLiveData.value != null) {
                    when (selectTab.value) {
                        0 -> NicoLiveGiftHistoryList(giftHistoryUserDataList = historyDataLiveData.value!!)
                        1 -> NicoLiveGiftRankingList(giftRankingUserDataList = rankingDataLiveData.value!!)
                    }
                }
            }
        }
    }
}