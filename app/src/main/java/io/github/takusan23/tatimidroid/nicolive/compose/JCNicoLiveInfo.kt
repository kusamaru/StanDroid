package io.github.takusan23.tatimidroid.nicolive.compose

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import io.github.takusan23.tatimidroid.compose.OrigamiLayout
import io.github.takusan23.tatimidroid.compose.TagButton
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.CommunityOrChannelData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardElevation
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardModifier
import io.github.takusan23.tatimidroid.nicovideo.compose.parentCardShape
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveKonomiTagData
import io.github.takusan23.tatimidroid.tool.NicoVideoDescriptionText
import io.github.takusan23.tatimidroid.tool.toFormatTime

/**
 * 番組情報を表示するCard
 * @param nicoLiveProgramData 番組情報データクラス
 * @param programDescription 番組説明文
 * @param isRegisteredTimeShift タイムシフト予約済みかどうか
 * @param isAllowTSRegister タイムシフト予約が利用可能かどうか。falseの場合はTS予約ボタンを非表示にします
 * @param onClickTimeShift タイムシフト予約ボタンを押した時
 * */
@Composable
fun NicoLiveInfoCard(
    nicoLiveProgramData: NicoLiveProgramData,
    programDescription: String,
    isRegisteredTimeShift: Boolean,
    isAllowTSRegister: Boolean,
    onClickTimeShift: () -> Unit,
    descriptionClick: (id: String, type: String) -> Unit,
) {
    // 動画説明文表示状態
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            // 番組開始、終了時刻
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_meeting_room_24px),
                    contentDescription = null,
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_begin_time)}：${toFormatTime(nicoLiveProgramData.beginAt.toLong() * 1000)}",
                )
            }
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_no_meeting_room_24),
                    contentDescription = null,
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_end_time)}：${toFormatTime(nicoLiveProgramData.endAt.toLong() * 1000)}",
                )
            }
            // 区切り線
            Divider(modifier = Modifier.padding(5.dp))
            // 真ん中にする
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    Text(
                        text = nicoLiveProgramData.title,
                        style = TextStyle(fontSize = 18.sp),
                        maxLines = 2,
                    )
                    // 生放送ID
                    Text(
                        text = nicoLiveProgramData.programId,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                // タイムシフト予約ボタン
                if (isAllowTSRegister) {
                    TimeShiftRegisterButton(
                        isRegisteredTimeShift = isRegisteredTimeShift,
                        onClickTimeShift = onClickTimeShift
                    )
                }
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded = !expanded }) {
                    // アイコンコード一行で召喚できる。Node.jsのnpmのmdiみたいだな！
                    Icon(
                        painter = if (expanded) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = stringResource(id = R.string.program_info),
                    )
                }
            }
            // 詳細表示
            if (expanded) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(factory = { context ->
                        TextView(context).apply {
                            // リンク押せるように
                            NicoVideoDescriptionText.setLinkText(text = HtmlCompat.fromHtml(programDescription, HtmlCompat.FROM_HTML_MODE_COMPACT), this, descriptionClick)
                        }
                    })
                }
            }
        }
    }
}

/**
 * タイムシフト予約ボタン
 *
 * @param isRegisteredTimeShift タイムシフト予約済みかどうか
 * @param onClickTimeShift タイムシフト予約ボタン押した時
 * */
@Composable
fun TimeShiftRegisterButton(
    isRegisteredTimeShift: Boolean,
    onClickTimeShift: () -> Unit
) {
    OutlinedButton(
        shape = RoundedCornerShape(20.dp), // 丸み
        onClick = { onClickTimeShift() },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_history_24px),
            contentDescription = stringResource(id = R.string.timeshift)
        )
        Text(text = if (isRegisteredTimeShift) stringResource(id = R.string.nicolive_time_shift_un_register_short) else stringResource(id = R.string.nicolive_time_shift_register_short))
    }
}

/**
 * コミュニティー情報表示Card
 *
 * @param communityOrChannelData コミュ、番組情報
 * @param onCommunityOpenClick コミュ情報押した時に呼ばれる
 * @param isFollow フォロー中かどうか
 * @param onFollowClick フォロー押した時
 * */
@Composable
fun NicoLiveCommunityCard(
    communityOrChannelData: CommunityOrChannelData,
    onCommunityOpenClick: () -> Unit,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_people_outline_24px),
                    contentDescription = null,
                )
                Text(text = stringResource(id = R.string.community_name))
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bitmap = getBitmapCompose(url = communityOrChannelData.icon)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                        contentDescription = null
                    )
                }
                Text(
                    text = communityOrChannelData.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                )
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // コミュだけフォローボタンを出す
                if (!communityOrChannelData.isChannel) {
                    TextButton(modifier = Modifier.padding(3.dp), onClick = { onFollowClick() }) {
                        if (isFollow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.person_remove_alt_black_24dp),
                                    contentDescription = stringResource(id = R.string.nicovideo_account_remove_follow)
                                )
                                Text(text = stringResource(id = R.string.nicovideo_account_remove_follow))
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_outline_star_border_24),
                                    contentDescription = stringResource(id = R.string.nicovideo_account_remove_follow)
                                )
                                Text(text = stringResource(id = R.string.community_follow))
                            }
                        }
                    }
                }
                TextButton(modifier = Modifier.padding(3.dp), onClick = { onCommunityOpenClick() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                        contentDescription = stringResource(id = R.string.open_browser)
                    )
                }
            }
        }
    }
}

/**
 * タグ表示Card。動画とは互換性がない（データクラスが違うの）
 * @param tagItemDataList [NicoTagItemData]の配列
 * @param onClickTag タグを押した時
 * @param isEditable 編集可能かどうか。falseで編集ボタンを非表示にします。
 * @param onClickEditButton 編集ボタンを押した時
 * @param onClickNicoPediaButton 二コ百押したとき
 * */
@Composable
fun NicoLiveTagCard(
    tagItemDataList: ArrayList<NicoTagItemData>,
    onClickTag: (NicoTagItemData) -> Unit,
    isEditable: Boolean,
    onClickEditButton: () -> Unit,
    onClickNicoPediaButton: (String) -> Unit,
) {
    // 展開状態かどうか
    val isShowAll = remember { mutableStateOf(false) }
    Card(
        modifier = parentCardModifier
            .fillMaxWidth(),
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            // 関連動画
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 5.dp, end = 5.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_local_offer_24px),
                    contentDescription = null,
                )
                Text(text = stringResource(id = R.string.tag), modifier = Modifier.weight(1f))
                // 編集
                // タグ編集ボタン
                if (isEditable) {
                    TextButton(onClick = { onClickEditButton() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_create_24px),
                            contentDescription = stringResource(id = R.string.tag_edit)
                        )
                        Text(text = stringResource(id = R.string.tag_edit))
                    }
                }
                // 展開ボタン
                IconButton(onClick = { isShowAll.value = !isShowAll.value }) {
                    if (isShowAll.value) {
                        Icon(painter = painterResource(id = R.drawable.ic_expand_less_black_24dp), contentDescription = "格納")
                    } else {
                        Icon(painter = painterResource(id = R.drawable.ic_expand_more_24px), contentDescription = "展開")
                    }
                }
            }
            // --- キリトリセン ---
            Divider(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(start = 5.dp, end = 5.dp)
            )
            // 折り返すレイアウト
            OrigamiLayout(
                modifier = Modifier
                    .padding(start = 5.dp, end = 5.dp)
                    .wrapContentHeight(),
                isExpended = isShowAll.value,
                minHeight = 200
            ) {
                tagItemDataList.forEach { data ->
                    // タグのボタン設置
                    TagButton(
                        data = data,
                        onClickTag = { onClickTag(it) },
                        onClickNicoPedia = { onClickNicoPediaButton(it) }
                    )
                }
            }
        }
    }
}

/**
 * 好みタグ表示Card。いまいちよくわからん機能
 *
 * @param konomiTagList 好みタグの文字列配列。いまんところ文字列の配列でいいや（そもそもこの機能いる？）
 * @param onClickEditButton 編集ボタンを押したとき
 * */
@Composable
fun NicoLiveKonomiCard(
    konomiTagList: List<NicoLiveKonomiTagData>,
    onClickEditButton: () -> Unit
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_favorite_border_24),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(id = R.string.konomi_tag),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onClickEditButton() }) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_create_24px), contentDescription = null)
                    Text(text = stringResource(id = R.string.nicolive_konomi_tag_edit))
                }
            }
            Divider(modifier = Modifier.padding(5.dp))
            // 0件の場合
            if (konomiTagList.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.konomi_tag_empty),
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                konomiTagList.forEach { konomiTag ->
                    Text(text = konomiTag.name, modifier = Modifier.padding(10.dp))
                    Divider()
                }
            }
        }
    }
}

/**
 * ニコニ広告 / 投げ銭のポイントを表示するCard
 *
 * @param totalNicoAdPoint 広告ポイント
 * @param totalGiftPoint 投げ銭ポイント
 * @param onClickNicoAdOpen ニコニ広告画面に遷移するボタンを押した時
 * @param onClickGiftOpen 投げ銭画面に遷移するボタンを押した時
 * */
@Composable
fun NicoLivePointCard(
    totalNicoAdPoint: Int,
    totalGiftPoint: Int,
    onClickNicoAdOpen: () -> Unit,
    onClickGiftOpen: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            // ニコニ広告、投げ銭を表示
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
            ) {
                // ニコニ広告
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_money_24px),
                            contentDescription = stringResource(id = R.string.nicoads),
                        )
                        Text(text = stringResource(id = R.string.nicoads))
                    }
                    Divider()
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(5.dp),
                        text = "$totalNicoAdPoint pt",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                    )
                    TextButton(
                        onClick = { onClickNicoAdOpen() },
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_arrow_forward_24),
                            contentDescription = stringResource(id = R.string.show_nicoad),
                        )
                        Text(text = stringResource(id = R.string.show_nicoad))
                    }
                }
                // 投げ銭
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_card_giftcard_24px),
                            contentDescription = stringResource(id = R.string.gift),
                        )
                        Text(text = stringResource(id = R.string.gift))
                    }
                    Divider()
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(5.dp),
                        text = "$totalGiftPoint pt",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                    )
                    TextButton(
                        onClick = { onClickGiftOpen() },
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_outline_arrow_forward_24),
                            contentDescription = stringResource(id = R.string.show_gift)
                        )
                        Text(text = stringResource(id = R.string.show_gift))
                    }
                }
            }
        }
    }
}

