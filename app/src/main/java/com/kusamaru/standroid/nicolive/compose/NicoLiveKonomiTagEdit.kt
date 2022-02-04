package com.kusamaru.standroid.nicolive.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveKonomiTagData

/**
 * ニコ生の好みタグの編集画面のタイトル
 * */
@Composable
fun NicoLiveKonomiTagTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_outline_favorite_border_24),
            contentDescription = null
        )
        Text(
            text = stringResource(id = R.string.nicolive_konomi_tag_edit),
            fontSize = 20.sp,
        )
    }
}

/**
 * ニコ生の好みタグの説明。
 *
 * フォローしたタグは他の人にバレるなど
 * */
@Composable
fun NicoLiveKonomiTagDescription() {
    Text(
        modifier = Modifier.padding(10.dp),
        text = """
        ・好みタグは、番組につけられるタグとは違い、ユーザーに紐づきます。
        ・フォローした好みタグは、他の人から見ることができる（このアプリでは未実装）ので、気をつけてください。
        ・新参は使うと人が来るらしい。
        ・新しい番組を探すときに使ってください。
        ・私もいまいち使い所がわからない。
    """.trimIndent()
    )
}

/**
 * ニコ生の好みタグのおすすめ、放送者フォロー中、フォロー中のタブ
 *
 * @param selectIndex 選択中のタブ
 * @param onSelect タブ押したとき
 * @param isVisibleBroadCasterTab 放送者フォロー中タブを表示する場合true
 * */
@Composable
fun NicoLiveKonomiTagMenuTab(
    selectIndex: Int,
    isVisibleBroadCasterTab: Boolean = false,
    onSelect: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectIndex,
        backgroundColor = Color.Transparent,
    ) {
        Tab(
            modifier = Modifier.padding(10.dp),
            selected = selectIndex == 0,
            onClick = { onSelect(0) }
        ) {
            Text(text = stringResource(id = R.string.nicolive_konomitag_following_tag))
        }
        Tab(
            modifier = Modifier.padding(10.dp),
            selected = selectIndex == 1,
            onClick = { onSelect(1) }
        ) {
            Text(text = stringResource(id = R.string.nicolive_konomitag_recomment_tag))
        }
        // 番組視聴画面から選択した場合はこれを有効に
        if (isVisibleBroadCasterTab) {
            Tab(
                modifier = Modifier.padding(10.dp),
                selected = selectIndex == 2,
                onClick = { onSelect(2) }
            ) {
                Text(
                    text = stringResource(id = R.string.nicolive_konomitag_broadcaster_following_tag),
                    maxLines = 1
                )

            }
        }
    }
}

@Composable
fun NicoLiveKonomiTagList(
    konomiTagList: List<NicoLiveKonomiTagData>,
    onClickKonomiTagFollow: (NicoLiveKonomiTagData) -> Unit
) {
    LazyColumn(Modifier.fillMaxHeight()) {
        items(konomiTagList) { konomiData ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = konomiData.name,
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1f)
                )
                TextButton(onClick = { onClickKonomiTagFollow(konomiData) }) {
                    if (konomiData.isFollowing) {
                        Icon(painter = painterResource(id = R.drawable.ic_baseline_done_24), contentDescription = "follow")
                        Text(text = stringResource(id = R.string.nicolive_konomitag_remove_follow))
                    } else {
                        Icon(painter = painterResource(id = R.drawable.ic_outline_favorite_border_24), contentDescription = "follow")
                        Text(text = stringResource(id = R.string.nicolive_konomitag_follow))
                    }
                }
            }
            Divider()
        }
    }
}


@Preview
@Composable
fun NicoLiveKonomiTagPreview() {
    Column(Modifier.fillMaxWidth()) {
        NicoLiveKonomiTagTitle()
        Divider()
        NicoLiveKonomiTagDescription()
        NicoLiveKonomiTagMenuTab(selectIndex = 0, isVisibleBroadCasterTab = true, onSelect = {})
    }
}