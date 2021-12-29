package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.CommentColorList


/** まっしろなテキストフィールド */
@Composable
private fun getWhiteColorOutlinedTextField() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White,
    unfocusedLabelColor = Color.White,
    focusedLabelColor = Color.White,
    disabledLabelColor = Color.White,
    errorLabelColor = Color.White,
    cursorColor = Color.White,
    errorCursorColor = Color.White,
)

/**
 * 生放送のコメント表示用、投稿UIをComposeで作成する
 * @param onClick ボタンを押した時
 * @param isComment コメントのアイコンを表示する場合はtrue
 * @param comment コメント本文
 * @param isShowCommentInfoChangeButton 番組情報 <-> コメント一覧 切り替えボタンを表示する場合はtrue。コメントのみ表示機能のために用意した
 * @param isPremium プレミアム会員かどうか。trueにするとプレ垢限定色を開放します。
 * @param onCommentChange コメントInputに変更が入ったときに呼ばれる
 * @param is184 匿名で投稿する場合はtrue。もしfalseになった場合はテキストボックスのヒントにに生IDで投稿されるという旨が表示されます。
 * @param onPostClick 投稿ボタン押した時
 * @param isMultiLine 複数行コメントを送信する場合はtrue。falseの場合はEnterキーを送信キーに変更します。
 * @param isHideCommentInputLayout コメント入力テキストボックス非表示にするかどうか
 * @param isTimeShiftMode タイムシフト再生中はtrue。コメント、番組情報切り替えボタンのみを表示させます。
 * @param onHideCommentInputLayoutChange コメント入力テキストボックスの表示が切り替わったら呼ばれる
 * @param onPosValueChange 固定位置が変わったら呼ばれる
 * @param onSizeValueChange 大きさが変わったら呼ばれる
 * @param onColorValueChange 色が変わったら呼ばれる
 * @param onTokumeiChange いやよ、生IDが切り替わったら呼ばれる。trueで匿名
 * @param position コメントの位置
 * @param size コメントの大きさ
 * @param color コメントの色
 * */
@Composable
fun NicoLiveCommentInputButton(
    onClick: () -> Unit,
    isComment: Boolean,
    is184: Boolean = true,
    isPremium: Boolean = true,
    comment: String,
    onCommentChange: (String) -> Unit,
    onPostClick: () -> Unit,
    isShowCommentInfoChangeButton: Boolean = true,
    isMultiLine: Boolean,
    isHideCommentInputLayout: Boolean = false,
    isTimeShiftMode: Boolean = false,
    position: String,
    size: String,
    color: String,
    onHideCommentInputLayoutChange: (Boolean) -> Unit = {},
    onPosValueChange: (String) -> Unit,
    onSizeValueChange: (String) -> Unit,
    onColorValueChange: (String) -> Unit,
    onTokumeiChange: (Boolean) -> Unit
) {
    // コメント入力テキストボックスを格納するかどうか
    val isHideCommentLayout = remember { mutableStateOf(isHideCommentInputLayout) }
    // コメント入力テキストボックスの表示、非表示変更時に呼ばれるやつ
    onHideCommentInputLayoutChange(isHideCommentLayout.value)
    // コマンドパネル表示するか
    val isShowCommandPanel = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .background(
                colorResource(id = R.color.colorPrimary),
                RoundedCornerShape(
                    // コメント入力テキストボックス表示中は角を丸くしない
                    topStart = if (!isHideCommentLayout.value && !isTimeShiftMode) 0.dp else 20.dp,
                    topEnd = 0.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            ),
    ) {
        // コマンドパネル
        if (isShowCommandPanel.value && !isHideCommentLayout.value) {
            NicoLiveCommentCommandPanel(
                isPremium = isPremium,
                is184 = is184,
                position = position,
                size = size,
                color = color,
                onPosValueChange = onPosValueChange,
                onSizeValueChange = onSizeValueChange,
                onColorValueChange = onColorValueChange,
                onTokumeiChange = onTokumeiChange
            )
        }
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically, // 真ん中にする
        ) {
            // コメント投稿欄。タイムシフト再生時はそもそも表示しない
            if (!isTimeShiftMode) {
                // コメント投稿エリア収納
                IconButton(onClick = { isHideCommentLayout.value = !isHideCommentLayout.value }) {
                    Icon(
                        painter = if (isHideCommentLayout.value) painterResource(id = R.drawable.ic_outline_create_24px) else painterResource(id = R.drawable.ic_outline_keyboard_arrow_right_24),
                        tint = Color.White,
                        contentDescription = "コメント投稿UI表示"
                    )
                }
                // コメント入力展開するか
                if (!isHideCommentLayout.value) {
                    // コマンドパネル
                    IconButton(onClick = { isShowCommandPanel.value = !isShowCommandPanel.value }) {
                        Column {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_settings_24px),
                                tint = Color.White,
                                contentDescription = "設定",
                            )
                        }
                    }
                    // コメント入力
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
                        value = comment,
                        onValueChange = { onCommentChange(it) },
                        label = {
                            Text(
                                text = if (is184) {
                                    stringResource(id = R.string.comment)
                                } else {
                                    // 生IDで投稿する旨を表示
                                    "${stringResource(id = R.string.comment)} ${stringResource(id = R.string.disabled_tokumei_comment)}"
                                }
                            )
                        },
                        textStyle = TextStyle(color = Color.White),
                        colors = getWhiteColorOutlinedTextField(),
                        // 複数行投稿が無効な場合はEnterキーを送信、そうじゃない場合は改行へ
                        keyboardOptions = if (!isMultiLine) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
                        keyboardActions = KeyboardActions(onSend = {
                            // 送信！
                            onPostClick()
                        })
                    )
                    // 投稿ボタン
                    IconButton(onClick = { onPostClick() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send_black),
                            tint = Color.White,
                            contentDescription = stringResource(id = R.string.comment_post)
                        )
                    }
                }
            }
            // ボタン
            if (isShowCommentInfoChangeButton) {
                IconButton(onClick = { onClick() }) {
                    Icon(
                        painter = if (isComment) painterResource(id = R.drawable.ic_outline_comment_24px) else painterResource(id = R.drawable.ic_outline_info_24px),
                        tint = Color.White,
                        contentDescription = "コメント表示/番組情報 切り替え"
                    )
                }
            }
        }
    }

}

/**
 * コマンドを選ぶやつ。色とか位置とか。
 *
 * @param isPremium プレミアム会員かどうか。trueにするとプレ垢限定色を使うことができます
 * （というかカラーコードがそのまま使えるようになる方が有能だったりする）
 * @param is184 匿名で投稿するか。
 * @param position コメントの位置
 * @param size コメントの大きさ
 * @param color コメントの色
 * @param onPosValueChange 固定位置が変わったら呼ばれる
 * @param onSizeValueChange 大きさが変わったら呼ばれる
 * @param onColorValueChange 色が変わったら呼ばれる
 * @param onTokumeiChange いやよ、生IDが切り替わったら呼ばれる。trueで匿名
 * */
@Composable
fun NicoLiveCommentCommandPanel(
    isPremium: Boolean = true,
    is184: Boolean = true,
    position: String,
    size: String,
    color: String,
    onPosValueChange: (String) -> Unit,
    onSizeValueChange: (String) -> Unit,
    onColorValueChange: (String) -> Unit,
    onTokumeiChange: (Boolean) -> Unit,
) {
    // 一般
    val colorList = CommentColorList.COLOR_LIST
    // プレ垢のみ
    val premiumColorList = CommentColorList.PREMIUM_COLOR_LIST
    // ボタンの色
    val buttonColor = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
    // ボタンのアウトラインの色
    val buttonOutlineColor = BorderStroke(1.dp, Color.White)
    // どの位置にしたか
    val selectPos = remember { mutableStateOf(position) }
    // どの大きさにしたか
    val selectSize = remember { mutableStateOf(size) }
    // どの色押したかどうか
    val selectColor = remember { mutableStateOf(color) }
    // 引数の関数たちをよぶ
    onPosValueChange(selectPos.value)
    onSizeValueChange(selectSize.value)
    onColorValueChange(selectColor.value)
    Column(modifier = Modifier.padding(5.dp)) {
        // サイズ
        Row {
            OutlinedButton(
                onClick = { selectSize.value = "big" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "大", fontSize = 15.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = { selectSize.value = "medium" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "中", fontSize = 13.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = { selectSize.value = "small" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "小", fontSize = 10.sp, color = Color.White)
            }
        }
        // 位置
        Row {
            OutlinedButton(
                onClick = { selectPos.value = "ue" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "↑", color = Color.White)
            }
            OutlinedButton(
                onClick = { selectPos.value = "naka" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "←", color = Color.White)
            }
            OutlinedButton(
                onClick = { selectPos.value = "shita" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "↓", color = Color.White)
            }
        }
        // 一般でも使える
        LazyRow {
            items(colorList) { color ->
                Button(
                    onClick = { selectColor.value = color.name },
                    modifier = Modifier.padding(5.dp),
                    colors = ButtonDefaults.textButtonColors(backgroundColor = Color(android.graphics.Color.parseColor(color.colorCode)))
                ) {

                }
            }
        }
        // プレミアム限定
        if (isPremium) {
            LazyRow {
                items(premiumColorList) { color ->
                    Button(
                        onClick = { selectColor.value = color.name },
                        modifier = Modifier.padding(5.dp),
                        colors = ButtonDefaults.textButtonColors(backgroundColor = Color(android.graphics.Color.parseColor(color.colorCode)))
                    ) {
                    }
                }
            }
        }
        // それぞれのテキストボックス
        Row {
            OutlinedTextField(
                value = selectPos.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.position)) },
                onValueChange = { selectPos.value = it },
                colors = getWhiteColorOutlinedTextField()
            )
            OutlinedTextField(
                value = selectSize.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.size)) },
                onValueChange = { selectSize.value = it },
                colors = getWhiteColorOutlinedTextField()
            )
            OutlinedTextField(
                value = selectColor.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.color)) },
                onValueChange = { selectColor.value = it },
                colors = getWhiteColorOutlinedTextField()
            )
            // リセットボタン
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(2.dp),
                onClick = {
                    // 初期値に戻す
                    selectSize.value = "medium"
                    selectPos.value = "naka"
                    selectColor.value = "white"
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace_24px),
                    tint = Color.White,
                    contentDescription = stringResource(id = R.string.reset)
                )
            }
        }
        // 匿名切り替えスイッチ
        Row(
            modifier = Modifier
                .padding(5.dp)
                .clickable(onClick = { onTokumeiChange(!is184) }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iyayo_comment), modifier = Modifier.weight(1f),
                color = Color.White
            )
            Switch(checked = is184, onCheckedChange = { onTokumeiChange(!is184) })
        }
    }
}