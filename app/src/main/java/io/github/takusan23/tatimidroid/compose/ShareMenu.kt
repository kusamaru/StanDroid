package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.nicovideo.compose.ComememoFilePathText
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.PlayerCommentPictureTool

/**
 * 共有メニュー
 *
 * 画像つきとかそのままとか
 * */
@Composable
fun ShareMenuCard(
    onClickShare: () -> Unit,
    onClickShareAttachImg: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // 共有
        Row {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { onClickShare() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = stringResource(id = R.string.share)
                )
                Text(
                    text = stringResource(id = R.string.share),
                    modifier = Modifier.padding(5.dp),
                )
            }
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { onClickShareAttachImg() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = stringResource(id = R.string.share_attach_image)
                )
                Text(
                    text = stringResource(id = R.string.share_attach_image),
                    modifier = Modifier.padding(5.dp),
                )
            }
        }
        ComememoFilePathText(filePath = PlayerCommentPictureTool.getSaveFolder())
    }
}
