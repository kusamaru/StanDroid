package com.kusamaru.standroid.nicovideo.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel

/**
 * ニコニコ動画のいいねのメッセージ表示用UI
 *
 * @param nicoVideoViewModel ニコ動FragmentのViewModel
 * @param onCloseClick 閉じる押したとき呼ばれる
 * */
@Composable
fun NicoVideoLikeMessageScreen(nicoVideoViewModel: NicoVideoViewModel, onCloseClick: () -> Unit) {
    Column(modifier = Modifier.padding(10.dp)) {
        val thanksMessage = nicoVideoViewModel.likeThanksMessageLiveData.observeAsState()
        val userData = nicoVideoViewModel.userDataLiveData.observeAsState()
        // お礼メッセージ表示
        NicoVideoLikeThanksMessage(thanksMessage = thanksMessage.value!!)
        // 真ん中に表示する
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ユーザー情報
            NicoVideoLikeUser(
                iconUrl = userData.value!!.largeIcon,
                userName = userData.value!!.nickName,
                modifier = Modifier.weight(1f)
            )
            // 閉じるボタン
            NicoVideoLikeCloseButton(onClick = { onCloseClick() })
        }
    }
}