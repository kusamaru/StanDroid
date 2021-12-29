package io.github.takusan23.tatimidroid.nicovideo.compose

import android.text.format.DateUtils
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.OrigamiLayout
import io.github.takusan23.tatimidroid.compose.TagButton
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoHTMLSeriesData
import io.github.takusan23.tatimidroid.nicoapi.user.UserData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.tool.*
import kotlinx.coroutines.launch

/**
 * Jetpack Compose 略してJC
 * */

/** [NicoVideoInfoCard]とかの親のCardに指定するModifier */
val parentCardModifier = Modifier.padding(5.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定する丸み */
val parentCardShape = RoundedCornerShape(3.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定するElevation */
val parentCardElevation = 3.dp

/**
 * 動画説明、タイトルCard
 * @param nicoVideoData ニコ動データクラス。
 * @param isLiked いいねしたかどうか。
 * @param isOffline キャッシュ再生用。trueにするといいねボタンを非表示にします。
 * @param description 動画説明文
 * @param onLikeClick いいね押したときに呼ばっる
 * @param scaffoldState Snackbar表示で使う。[_root_ide_package_.androidx.compose.material.Scaffold]を使おう
 * @param descriptionClick 動画説明文のリンクを押した時。[NicoVideoDescriptionText.DESCRIPTION_TYPE_MYLIST]等参照
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoInfoCard(
    nicoVideoData: NicoVideoData?,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    isOffline: Boolean,
    scaffoldState: ScaffoldState,
    description: String,
    descriptionClick: (id: String, type: String) -> Unit,
) {
    // 動画説明文表示状態
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = parentCardModifier,
        // border = BorderStroke(1.dp, Color.Black),
        shape = parentCardShape,
        // backgroundColor = Color.Transparent,
        elevation = parentCardElevation,
    ) {

        Column(
            modifier = Modifier.padding(10.dp),
        ) {

            // お祝いメッセージ機能。お誕生日
            val anniversary = calcAnniversary(nicoVideoData?.date ?: 0L) // AnniversaryDateクラス みて
            // たんおめ
            val isBirthday = !(anniversary == 0) && anniversary != -1
            if (isBirthday) {
                Text(
                    text = AnniversaryDate.makeAnniversaryMessage(anniversary),
                    color = Color.Red,
                )
            }

            // 投稿日時
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_event_available_24px),
                    contentDescription = null,
                )
                Text(
                    text = "${stringResource(id = R.string.post_date)}：${toFormatTime(nicoVideoData?.date ?: 0L)}",
                )
            }
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history_24px),
                    contentDescription = null,
                )
                Text(
                    text = "今日から ${calcDayCount(toFormatTime(nicoVideoData?.date ?: 0L))} 日前に投稿",
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
                        text = nicoVideoData?.title ?: "",
                        fontSize = 20.sp,
                        maxLines = 2,
                    )
                    // 動画ID表示
                    Text(
                        text = nicoVideoData?.videoId ?: "",
                        fontSize = 12.sp,
                    )
                }
                // いいねぼたん
                if (!isOffline) {
                    NicoVideoLikeButton(
                        isLiked = isLiked,
                        onLikeClick = onLikeClick,
                    )
                }
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    // アイコンコード一行で召喚できる。npmのmdiみたいだな！
                    Icon(
                        painter = if (expanded.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = stringResource(id = R.string.nicovideo_info)
                    )
                }
            }

            // マイリスト数とかコメント数とか
            Row {
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_arrow_24px),
                        contentDescription = stringResource(id = R.string.view_count)
                    )
                    Text(text = nicoVideoData?.viewCount ?: "0")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                        contentDescription = stringResource(id = R.string.comment_count)
                    )
                    Text(text = nicoVideoData?.commentCount ?: "0")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                        contentDescription = stringResource(id = R.string.mylist_count)
                    )
                    Text(text = nicoVideoData?.mylistCount ?: "0")
                }
            }

            // 詳細表示
            if (expanded.value) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                // リンク押せるように
                                NicoVideoDescriptionText.setLinkText(text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), this, descriptionClick)
                            }
                        }
                    )
                }
            }

        }
    }
}

/**
 * いいねボタン。長いので切り出した
 * @param isLiked いいね済みかどうか。
 * @param onLikeClick いいねを押したら呼ばれる
 * @param scaffoldState Snackbar表示の際に使う。
 * [_root_ide_package_.androidx.compose.material.rememberScaffoldState()]をインスタンス化して[_root_ide_package_.androidx.compose.material.Scaffold() {}]
 * で指定したのを利用してください。
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoLikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
) {
    // ピンク色
    val pinkColor = Color(android.graphics.Color.parseColor("#F69896"))

    // いいねボタン
    OutlinedButton(
        shape = RoundedCornerShape(20.dp), // 丸み
        onClick = { onLikeClick() },
    ) {
        Text(text = if (isLiked) stringResource(id = R.string.liked) else stringResource(id = R.string.like))
        Icon(
            painter = if (isLiked) painterResource(id = R.drawable.ic_favorite_black_24dp) else painterResource(id = R.drawable.ic_outline_favorite_border_24),
            tint = if (isLiked) pinkColor else LocalContentColor.current.copy(alpha = LocalContentColor.current.alpha),
            contentDescription = stringResource(id = R.string.like)
        )
    }
}


/**
 * 関連動画表示Card。従来のRecyclerViewを置いてる。使い回せるGJ
 *
 * @param nicoVideoDataList [NicoVideoData]の配列
 * */
@Composable
fun NicoVideoRecommendCard(nicoVideoDataList: ArrayList<NicoVideoData>) {
    Card(
        modifier = parentCardModifier.fillMaxWidth(),
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 関連動画
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_local_movies_24px),
                    contentDescription = null
                )
                Text(text = stringResource(id = R.string.recommend_video))
            }
            // 一覧表示。RecyclerViewを使い回す
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    RecyclerView(context).apply {
                        setHasFixedSize(true)
                        isNestedScrollingEnabled = false // これしないと関連動画スクロールした状態でミニプレイヤーに遷移できない
                        layoutManager = LinearLayoutManager(context)
                        adapter = NicoVideoListAdapter(nicoVideoDataList, isUseComposeAndroidView = true)
                    }
                }
            )
        }
    }
}

/**
 * ユーザー情報Card
 * @param userData ユーザー情報データクラス
 * @param onUserOpenClick ユーザー情報詳細ボタン押した時に呼ばれる
 * */
@Composable
fun NicoVideoUserCard(userData: UserData, onUserOpenClick: () -> Unit) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bitmap = getBitmapCompose(url = userData.largeIcon)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                    contentDescription = null,
                )
            }
            Text(
                text = userData.nickName,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            )
            OutlinedButton(onClick = { onUserOpenClick() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                    contentDescription = stringResource(id = R.string.open_browser)
                )
            }
        }
    }
}

/**
 * タグ一覧表示Card
 * @param tagItemDataList [NicoTagItemData]配列
 * @param onClickTag 押したときに呼ばれる。
 * @param onClickNicoPedia ニコニコ大百科ボタンを押したときに呼ばれる
 * */
@Composable
fun NicoVideoTagCard(
    tagItemDataList: ArrayList<NicoTagItemData>,
    onClickTag: (NicoTagItemData) -> Unit,
    onClickNicoPedia: (String) -> Unit
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
                        onClickNicoPedia = { onClickNicoPedia(it) }
                    )
                }
            }
        }
    }
}

/**
 * シリーズが設定されてる場合は表示する
 *
 * @param nicoVideoHTMLSeriesData シリーズ情報データクラス。次の動画とかを表示するため
 * @param onClickStartSeriesPlay 連続再生押した時
 * @param onClickFirstVideoPlay シリーズの最初の動画を再生するボタンを押した時
 * @param onClickNextVideoPlay 次の動画を再生するボタンを押した時
 * @param onClickPrevVideoPlay 前の動画を再生するボタンを押した時
 * */
@Composable
fun NicoVideoSeriesCard(
    nicoVideoHTMLSeriesData: NicoVideoHTMLSeriesData,
    onClickStartSeriesPlay: () -> Unit,
    onClickFirstVideoPlay: (NicoVideoData) -> Unit,
    onClickNextVideoPlay: (NicoVideoData) -> Unit,
    onClickPrevVideoPlay: (NicoVideoData) -> Unit,
) {

    /**
     * シリーズメニュー表示状態
     *
     * 本当は引数に出すべきなんだけど、なんか引数にすると全部のUIに更新が行ってしまう
     * */
    val expanded = remember { mutableStateOf(false) }

    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row {
                Icon(
                    painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                    contentDescription = stringResource(id = R.string.series)
                )
                Text(text = stringResource(id = R.string.series))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // さむね
                val bitmap = getBitmapCompose(url = nicoVideoHTMLSeriesData.seriesData.thumbUrl)
                if (bitmap != null) {
                    // ちいさめ
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentDescription = null,
                    )
                }
                // タイトル
                Text(
                    text = nicoVideoHTMLSeriesData.seriesData.title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                )
                // 一覧表示
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        painter = if (expanded.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = "シリーズメニュー",
                    )
                }
            }
            if (expanded.value) {
                Column {
                    // 区切り
                    Divider()
                    // 連続再生開始などのメニュー
                    TextButton(onClick = { onClickStartSeriesPlay() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow_24px),
                            contentDescription = stringResource(id = R.string.nicovideo_playlist_start)
                        )
                        Text(text = stringResource(id = R.string.nicovideo_playlist_start), modifier = Modifier.weight(1f))
                    }
                    // 最初から再生
                    if (nicoVideoHTMLSeriesData.firstVideoData != null) {
                        TextButton(onClick = { onClickFirstVideoPlay(nicoVideoHTMLSeriesData.firstVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_filter_1_24),
                                contentDescription = stringResource(id = R.string.nicovideo_series_first_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_first_video)}\n${nicoVideoHTMLSeriesData.firstVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                    // 前の動画
                    if (nicoVideoHTMLSeriesData.prevVideoData != null) {
                        TextButton(onClick = { onClickPrevVideoPlay(nicoVideoHTMLSeriesData.prevVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_black_24dp),
                                contentDescription = stringResource(id = R.string.nicovideo_series_prev_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_prev_video)}\n${nicoVideoHTMLSeriesData.prevVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                    // 次の動画
                    if (nicoVideoHTMLSeriesData.nextVideoData != null) {
                        TextButton(onClick = { onClickNextVideoPlay(nicoVideoHTMLSeriesData.nextVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_arrow_forward_24),
                                contentDescription = stringResource(id = R.string.nicovideo_series_next_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_next_video)}\n${nicoVideoHTMLSeriesData.nextVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 連続再生一覧。BottomFragmentでも良かった？
 * */
@Composable
fun NicoVideoPlayList(
    isShowList: Boolean,
    showButtonClick: () -> Unit,
    videoList: ArrayList<NicoVideoData>,
    playingVideoId: String,
    videoClick: (String) -> Unit,
    isReverse: Boolean,
    reverseClick: () -> Unit,
    isShuffle: Boolean,
    shuffleClick: () -> Unit,
) {
    // 表示中かどうか
    val isPlaylistShow = remember { mutableStateOf(false) }

    // 選択中
    val playingColor = colorResource(id = R.color.colorAccent)
    // 影をつけるため？
    Surface(
        elevation = 10.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = LocalContext.current.getDrawable(R.drawable.ic_tatimidroid_list_icon_black)?.toBitmap()?.asImageBitmap()
                if (icon != null) {
                    Icon(
                        bitmap = icon,
                        modifier = Modifier.padding(5.dp),
                        contentDescription = stringResource(id = R.string.playlist_button)
                    )
                }
                Text(
                    text = stringResource(id = R.string.playlist_button),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { isPlaylistShow.value = !isPlaylistShow.value }) {
                    Icon(
                        painter = if (isPlaylistShow.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = "動画一覧"
                    )
                }
            }
            if (isPlaylistShow.value) {
                // シャッフルとか
                LazyRow {
                    item {
                        // 動画時間
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { }
                        ) {
                            // 何分か
                            val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_timer_24),
                                contentDescription = stringResource(id = R.string.playlist_total_time),
                            )
                            Text(text = "${stringResource(id = R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}")
                        }
                        // 作品数
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_view_list_black),
                                contentDescription = stringResource(id = R.string.video_count),
                            )
                            Text(text = "${stringResource(id = R.string.video_count)}：${videoList.size}")
                        }
                        // 逆順
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { reverseClick() }
                        ) {
                            Icon(
                                painter = if (isReverse) painterResource(id = R.drawable.ic_outline_check_box_24) else painterResource(id = R.drawable.ic_outline_check_box_outline_blank_24),
                                contentDescription = stringResource(id = R.string.reverse),
                            )
                            Text(text = stringResource(id = R.string.reverse))
                        }
                        // シャッフル
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { shuffleClick() }
                        ) {
                            Icon(
                                painter = if (isShuffle) painterResource(id = R.drawable.ic_outline_check_box_24) else painterResource(id = R.drawable.ic_outline_check_box_outline_blank_24),
                                contentDescription = stringResource(id = R.string.shuffle),
                            )
                            Text(text = stringResource(id = R.string.shuffle))
                        }
                    }
                }
                // 一覧表示
                val scope = rememberCoroutineScope()
                val state = rememberLazyListState()
                // スクロール実行
                scope.launch {
                    // 位置を特定
                    val index = videoList.indexOfFirst { it.videoId == playingVideoId }
                    if (index != -1) {
                        state.scrollToItem(index)
                    }
                }
                // RecyclerViewみたいに画面外は描画しないやつ
                LazyColumn(
                    state = state,
                    content = {
                        this.items(videoList) { data ->
                            Row(
                                modifier = Modifier
                                    .background(color = if (playingVideoId == data.videoId) playingColor else Color.Transparent)
                                    .clickable(onClick = {
                                        scope.launch {
                                            // 位置を特定
                                            val index = videoList.indexOfFirst { it.videoId == data.videoId }
                                            // スクロール実行
                                            state.scrollToItem(index)
                                        }
                                        videoClick(data.videoId)
                                    }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bitmap = getBitmapCompose(url = data.thum)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(110.dp)
                                            .padding(5.dp),
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    text = data.title,
                                    maxLines = 2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(5.dp),
                                )
                            }
                            // 区切り線
                            Divider()
                        }
                    }
                )
            }
            // 区切り線
            Divider()
        }
    }
}

/**
 * コメント一覧表示用Fabです
 *
 * @param isShowCommentList コメント一覧表示中かどうか。trueで表示中
 * @param click Fab押した時
 * */
@Composable
fun NicoVideoCommentListFab(
    isShowCommentList: Boolean,
    click: () -> Unit,
) {
    // コメント表示Fabを出す
    FloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 20.dp),
        onClick = {
            // 押した時
            click()
        })
    {
        Icon(
            painter = if (isShowCommentList) painterResource(id = R.drawable.ic_outline_info_24px) else painterResource(id = R.drawable.ic_outline_comment_24px),
            contentDescription = null
        )
    }
}

/**
 * Adapterにわたす引数が足りてない。から使う時気をつけて
 *
 * コメント一覧表示BottomSheet。Jetpack Compose結構揃ってる。なお現状めっちゃ落ちるので使ってない。バージョン上がったら使いたい。
 *
 * 注意 このレイアウトを最上位にしてその他は[content]の中に書いてください。
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoCommentBottomSheet(commentList: ArrayList<CommentJSONParse>, commentClick: (CommentJSONParse) -> Unit, content: @Composable () -> Unit) {
    // BottomSheetを表示させるかどうか
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded)

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetElevation = 10.dp,
        sheetShape = RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp),
        sheetContent = {
            LazyColumn(content = {
                this.item {
                    Column {
                        // コメントBottomSheetだよー
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                                contentDescription = null
                            )
                            Text(text = stringResource(id = R.string.comment))
                        }
                        // コメント一覧。AndroidViewで既存のRecyclerViewを使い回す。
                        AndroidView(factory = { context ->
                            RecyclerView(context).apply {
                                setHasFixedSize(true)
                                layoutManager = LinearLayoutManager(context)
                                // adapter = NicoVideoAdapter(commentList)
                                val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                                addItemDecoration(itemDecoration)
                            }
                        })
                    }
                }
            })
        },
        content = {
            // モーダル以外のレイアウト
            content()
        }
    )
}

/** BottomSheet表示用FAB */
@Composable
fun BottomSheetFab(fabClick: () -> Unit) {
    // BottomSheetを表示するためのFab
    // 右下にするために
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
    ) {
        // コメント表示Fabを出す
        FloatingActionButton(modifier = Modifier.padding(16.dp),
            onClick = {
                // 押した時
                fabClick()
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                contentDescription = null
            )
        }
    }
}

/*
@Preview
@Composable
fun PreviewVideoInfoCard() {
    VideoInfoCard()
}

*/
