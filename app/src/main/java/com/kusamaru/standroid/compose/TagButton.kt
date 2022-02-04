package com.kusamaru.standroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoTagItemData
import com.kusamaru.standroid.R

/**
 * タグのボタン。ボタンが半分に区切られていて、検索と大百科に飛べるやつ
 *
 * @param data タグのデータ
 * @param onClickNicoPedia 大百科押したとき
 * @param onClickTag タグ押したとき
 * */
@Composable
fun TagButton(
    data: NicoTagItemData,
    onClickTag: (NicoTagItemData) -> Unit,
    onClickNicoPedia: (String) -> Unit,
) {
    // 角丸ボタン
    Surface(
        border = ButtonDefaults.outlinedBorder,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(2.dp)
            .wrapContentWidth(align = Alignment.Start)
            .wrapContentHeight(align = Alignment.Top)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.requiredHeight(IntrinsicSize.Min)) {
            // タグ検索
            Surface(modifier = Modifier.clickable { onClickTag(data) }) {
                Row(modifier = Modifier.padding(10.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_local_offer_24px),
                        contentDescription = stringResource(id = R.string.serch),
                    )
                    Text(text = data.tagName)
                }
            }
            // 大百科。あるときのみ
            if (data.hasNicoPedia) {
                // 区切り線
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .requiredWidth(1.dp)
                        .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                )
                Surface(modifier = Modifier.clickable { onClickNicoPedia(data.nicoPediaUrl) }) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                            contentDescription = stringResource(id = R.string.nico_pedia),
                        )
                        Text(text = stringResource(id = R.string.nico_pedia))
                    }
                }
            }
        }
    }
}