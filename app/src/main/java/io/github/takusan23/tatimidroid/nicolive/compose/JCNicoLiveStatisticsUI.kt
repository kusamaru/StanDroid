package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * 統計情報（来場者、コメント投稿数、アクティブ人数）を表示するためのUI
 *
 * @param isAllRoomCommentMode 全部屋表示中ならtrue
 * @param allViewer 累計来場者
 * @param allCommentCount コメント数
 * @param activeCountText 一分間のコメント数
 * @param onClickRoomChange 部屋別表示に切り替え押した時
 * @param onClickActiveCalc アクティブ人数計算ボタン押した時
 * */
@Composable
fun NicoLiveStatisticsUI(
    allViewer: Int,
    allCommentCount: Int,
    activeCountText: String,
    onClickRoomChange: () -> Unit,
    onClickActiveCalc: () -> Unit,
) {
    // 共有Modifier
    val paddingModifier = Modifier.padding(start = 5.dp, end = 5.dp)
    Row {
        // 来場者
        Row(modifier = paddingModifier.align(Alignment.CenterVertically)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_people_outline_24px),
                modifier = paddingModifier,
                contentDescription = "累計来場者",
            )
            Text(
                text = allViewer.toString(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        // コメント数
        Row(modifier = paddingModifier.align(Alignment.CenterVertically)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                modifier = paddingModifier,
                contentDescription = stringResource(id = R.string.comment_count),
            )
            Text(
                text = allCommentCount.toString(),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        // アクティブ人数
        Row(modifier = paddingModifier.align(Alignment.CenterVertically)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_active_icon),
                modifier = paddingModifier,
                contentDescription = stringResource(id = R.string.active),
            )
            Text(
                text = activeCountText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // アクティブ人数計算ボタン
        IconButton(onClick = { onClickActiveCalc() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_timeline_black_24dp),
                contentDescription = "アクティブ人数計算"
            )
        }
        // 部屋切り替えボタン
        IconButton(onClick = { onClickRoomChange() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_meeting_room_24px),
                stringResource(id = R.string.room_comment)
            )
        }
    }
}