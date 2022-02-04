package com.kusamaru.standroid.nicovideo.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.R


/**
 * お礼メッセージ表示用
 *
 * @param thanksMessage お礼メッセージ
 * */
@Composable
fun NicoVideoLikeThanksMessage(thanksMessage: String) {
    // 角丸の線の背景
    Surface(
        border = ButtonDefaults.outlinedBorder,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(5.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = thanksMessage,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

/**
 * 投稿者名表示用。
 *
 * @param iconUrl 画像URL
 * @param userName ユーザー名
 * @param modifier weight(1)を設定するときに使って
 * */
@Composable
fun NicoVideoLikeUser(iconUrl: String, userName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // アイコン
        val bitmap = getBitmapCompose(url = iconUrl)?.asImageBitmap()
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "アイコン",
                modifier = Modifier.clip(RoundedCornerShape(5.dp))
            )
        }
        // 名前
        Text(
            text = userName,
            modifier = Modifier.padding(5.dp)
        )
    }
}

/**
 * 閉じるボタン
 * @param onClick 閉じる押したとき
 * */
@Composable
fun NicoVideoLikeCloseButton(onClick: () -> Unit) {
    Button(
        onClick = { onClick() },
        modifier = Modifier.clip(RoundedCornerShape(50)) // 50パーセント
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clear_black),
            contentDescription = stringResource(id = R.string.close)
        )
        Text(text = stringResource(id = R.string.close))
    }
}