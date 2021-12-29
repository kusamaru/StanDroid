package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.ComememoViewModel

/**
 * コメメモ機能（動画スクショ機能）
 *
 * @param viewModel コメメモViewModel
 * @param onClickSaveButton 保存ボタン押したとき
 * */
@Composable
fun ComememoScreen(viewModel: ComememoViewModel, onClickSaveButton: () -> Unit) {
    val bitmap = viewModel.makeBitmapLiveData.observeAsState()
    val isWriteVideoInfoAndDate = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // タイトル
        ComememoTitle()
        Divider()
        // プレビュー
        ComememoPreviewImage(bitmap = bitmap.value)
        // 設定など
        ComememoSettingSwitch(isWriteVideoInfoAndDate = isWriteVideoInfoAndDate.value) {
            isWriteVideoInfoAndDate.value = it
            viewModel.makeBitmap(it)
        }
        Divider()
        ComememoFilePathText(filePath = viewModel.saveFolderPath)
        // 保存ボタン
        ComememoSaveButton {
            viewModel.saveBitmapToMediaStore()
            onClickSaveButton()
        }
    }
}