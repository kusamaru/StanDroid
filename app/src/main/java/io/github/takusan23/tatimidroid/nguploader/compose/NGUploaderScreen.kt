package io.github.takusan23.tatimidroid.nguploader.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.nguploader.viewmodel.NGUploaderViewModel
import kotlinx.coroutines.launch

/**
 * NG投稿者の画面
 *
 * @param viewModel ViewModel
 * */
@Composable
fun NGUploaderScreen(viewModel: NGUploaderViewModel) {
    // NG投稿者一覧
    val ngUploaderUserIdList = viewModel.ngUploaderUserIdListLiveData.observeAsState()
    // NG投稿者機能が有効かどうか
    val isEnableNGUploader = remember { mutableStateOf(viewModel.ngUploaderTool.getEnableNGUploader()) }

    // SnackBar表示用
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    Scaffold(
        scaffoldState = scaffoldState,
    ) {
        Surface {
            Column {
                // タイトル、説明文
                NGUploaderTitle()
                NGUploaderDescription()
                Divider(modifier = Modifier.padding(5.dp))
                // 有効、無効スイッチ
                NGUploaderEnableSwitch(
                    _isEnable = isEnableNGUploader.value,
                    onChecked = {
                        // 有効、無効化
                        viewModel.ngUploaderTool.setNGUploaderEnable(it)
                        isEnableNGUploader.value = !isEnableNGUploader.value
                    }
                )
                Divider(modifier = Modifier.padding(5.dp))
                // 有効時のみこれ以降先を表示させる
                if (isEnableNGUploader.value) {
                    // 一覧
                    if (ngUploaderUserIdList.value != null) {
                        NGUploaderUserList(
                            list = ngUploaderUserIdList.value!!,
                            onClickDelete = { userId ->
                                // 削除
                                scope.launch {
                                    val result = scaffoldState.snackbarHostState.showSnackbar("削除してもいい？", "削除")
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.deleteNGUploaderId(userId)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}