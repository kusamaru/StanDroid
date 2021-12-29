package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * 動画のコメント表示用ボタンをComposeで作成する
 *
 * @param click ボタンを押した時
 * @param isComment コメントのアイコンを表示する場合はtrue
 * @param content ボタンの左側になにかComposeのUIを設置したい場合は使ってください。
 * @param topLeftRound 丸みを変えたい場合はどうぞ
 * */
@Composable
fun NicoVideoCommentButton(
    click: () -> Unit,
    isComment: Boolean,
) {
    // margin代わり
    Row(
        modifier = Modifier.background(colorResource(id = R.color.colorPrimary), RoundedCornerShape(20.dp, 0.dp, 0.dp, 0.dp)),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically, // 真ん中にする
        ) {
            // ボタン
            IconButton(
                onClick = { click() },
            ) {
                Icon(
                    painter = if (isComment) painterResource(id = R.drawable.ic_outline_comment_24px) else painterResource(id = R.drawable.ic_outline_info_24px),
                    tint = Color.White,
                    contentDescription = "コメント/動画情報 切り替え"
                )
            }
        }
    }
}