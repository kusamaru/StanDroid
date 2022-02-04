package com.kusamaru.standroid.nicolive.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveKonomiTagEditViewModel

/**
 * ニコ生の好みタグの編集の画面
 * */
@Composable
fun NicoLiveKonomiTagEditScreen(viewModel: NicoLiveKonomiTagEditViewModel) {
    // 自分がフォローしている好みタグ
    val myFollowingKonomiTagList = viewModel.myFollowingKonomiTagLiveData.observeAsState()
    // おすすめの好みタグ
    val recommendKonomiTagList = viewModel.recommendKonomiTagLiveData.observeAsState()
    // 放送者がフォローしている好みタグ
    val broadCasterFollowingKonomiTagList = viewModel.broadCasterFollowingKonomiTagLiveData.observeAsState()
    // 放送者がフォローしている好みタグを取得できるか
    val hasBroadCasterUserId = viewModel.hasBroadCasterUserId
    // 選択中タブ
    val selectedIndex = remember { mutableStateOf(0) }

    Column {
        NicoLiveKonomiTagTitle()
        Divider()
        NicoLiveKonomiTagDescription()
        NicoLiveKonomiTagMenuTab(
            selectIndex = selectedIndex.value,
            isVisibleBroadCasterTab = hasBroadCasterUserId,
            onSelect = { selectedIndex.value = it }
        )
        if (!hasBroadCasterUserId) {
            // おすすめ。フォロー中のみver
            if (myFollowingKonomiTagList.value != null && recommendKonomiTagList.value != null) {
                when (selectedIndex.value) {
                    0 -> NicoLiveKonomiTagList(konomiTagList = myFollowingKonomiTagList.value!!, onClickKonomiTagFollow = { if (it.isFollowing) viewModel.removeFollowKonomiTag(it.tagId) else viewModel.followKonomiTag(it.tagId) })
                    1 -> NicoLiveKonomiTagList(konomiTagList = recommendKonomiTagList.value!!, onClickKonomiTagFollow = { if (it.isFollowing) viewModel.removeFollowKonomiTag(it.tagId) else viewModel.followKonomiTag(it.tagId) })
                }
            }
        } else {
            // 放送者がフォローしているタブありver
            if (myFollowingKonomiTagList.value != null && recommendKonomiTagList.value != null && broadCasterFollowingKonomiTagList.value != null) {
                when (selectedIndex.value) {
                    0 -> NicoLiveKonomiTagList(konomiTagList = myFollowingKonomiTagList.value!!, onClickKonomiTagFollow = { if (it.isFollowing) viewModel.removeFollowKonomiTag(it.tagId) else viewModel.followKonomiTag(it.tagId) })
                    1 -> NicoLiveKonomiTagList(konomiTagList = recommendKonomiTagList.value!!, onClickKonomiTagFollow = { if (it.isFollowing) viewModel.removeFollowKonomiTag(it.tagId) else viewModel.followKonomiTag(it.tagId) })
                    2 -> NicoLiveKonomiTagList(konomiTagList = broadCasterFollowingKonomiTagList.value!!, onClickKonomiTagFollow = { if (it.isFollowing) viewModel.removeFollowKonomiTag(it.tagId) else viewModel.followKonomiTag(it.tagId) })
                }
            }
        }
    }
}