package io.github.takusan23.tatimidroid.nicovideo.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R

/**
 * コメメモBottomFragmentのタイトル
 * */
@Composable
fun ComememoTitle() {
    // タイトル
    Text(
        text = stringResource(id = R.string.comememo_title),
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
    )
}

/**
 * コメメモBottomFragmentのImage。16：9
 *
 * @param bitmap nullのときは変わりの画像を表示させます
 * */
@Composable
fun ComememoPreviewImage(bitmap: Bitmap?) {
    val imageModifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .aspectRatio(16f / 9f) // 16:9
    if (bitmap == null) {
        // 読込中
        Image(
            painter = painterResource(id = R.drawable.screen_shot_icon),
            contentDescription = "preview",
            modifier = imageModifier,
        )
    } else {
        // プレビュー
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "preview",
            modifier = imageModifier,
        )
    }
}

/**
 * コメメモBottomFragmentの設定。
 * */
@Composable
fun ComememoSettingSwitch(isWriteVideoInfoAndDate: Boolean, onChange: (Boolean) -> Unit) {
    // 設定など
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.comememo_is_write_info_date),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isWriteVideoInfoAndDate,
            onCheckedChange = { onChange(it) }
        )
    }
}

/**
 * ファイルパスの表示
 * @param filePath ファイルパス
 * */
@Composable
fun ComememoFilePathText(filePath: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
            contentDescription = "save"
        )
        Text(
            text = "${stringResource(id = R.string.save_path)}：${filePath}",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * コメメモBottomFragmentの保存ボタン
 * */
@Composable
fun ComememoSaveButton(onClickSaveButton: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 保存
        Button(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterHorizontally),
            onClick = { onClickSaveButton() }
        ) {
            Icon(painter = painterResource(R.drawable.ic_folder_open_black_24dp), contentDescription = "save")
            Text(text = stringResource(id = R.string.comememo_save))
        }
    }
}