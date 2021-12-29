package io.github.takusan23.tatimidroid.setting.dbclear.compose

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.setting.dbclear.DatabaseClearData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * データベース削除設定画面
 * @param onBackClick 戻るボタン押したときに呼ばれる
 * */
@Composable
fun DatabaseClearSettingScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    // データベース一覧
    val dbList = remember { mutableStateListOf<DatabaseClearData>().apply { addAll(DatabaseClearData.getDatabaseList(context)) } }

    Scaffold(
        modifier = Modifier.padding(),
        scaffoldState = scaffoldState,
    ) {
        Column {
            DatabaseClearSettingTitle(onBackClick)
            DatabaseClearSettingDatabaseList(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
                dbList = dbList,
                onDeleteCheckChange = { index ->
                    dbList[index] = dbList[index].copy(isDelete = !dbList[index].isDelete)
                }
            )
            DatabaseClearSettingButtonGroup(
                onBackClick = onBackClick,
                onDeleteClick = {
                    // UIスレッド無理なので
                    scope.launch {
                        // 確認。この書き方Win32のMessageBoxみたい
                        val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(context.getString(R.string.setting_database_clear_message), context.getString(R.string.delete))
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            dbList
                                .filter { it.isDelete }
                                .map { dbClearItem -> async(Dispatchers.IO) { dbClearItem.database.clearAllTables() } }
                                .forEach { it.await() }
                            // 通知して戻る
                            Toast.makeText(context, R.string.delete_successful, Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }
                    }
                }
            )
        }
    }
}

/**
 * キャンセルボタン、さくじょ実行ボタンが有る
 * */
@Composable
private fun DatabaseClearSettingButtonGroup(onBackClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(text = stringResource(id = R.string.setting_database_clear_cancel_button))
        }
        Button(
            onClick = onDeleteClick,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(text = stringResource(id = R.string.setting_database_clear_delete_button))
        }
    }
}

/**
 * データベース一覧を表示する
 * @param modifier Modifier.weight(1f)を指定してね
 * @param dbList データベース一覧
 * @param onDeleteCheckChange チェックを入れたら呼ばれる。引数は位置
 * */
@Composable
private fun DatabaseClearSettingDatabaseList(modifier: Modifier, dbList: List<DatabaseClearData>, onDeleteCheckChange: (index: Int) -> Unit) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(dbList) { index, db ->
            Row(
                modifier = Modifier.clickable { onDeleteCheckChange(index) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = db.databaseDescription,
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1f)
                )
                Checkbox(
                    modifier = Modifier.padding(10.dp),
                    checked = db.isDelete,
                    onCheckedChange = { onDeleteCheckChange(index) }
                )
            }
            Divider()
        }
    }
}

/**
 * タイトル部分
 * @param onBackClick 戻るボタン押したときに呼ばれる
 * */
@Composable
private fun DatabaseClearSettingTitle(onBackClick: () -> Unit) {
    Column {
        val modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 10.dp, bottom = 5.dp)

        IconButton(
            modifier = modifier,
            onClick = onBackClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back_black_24dp),
                contentDescription = null
            )
        }
        Icon(
            modifier = modifier.size(30.dp),
            painter = painterResource(id = R.drawable.ic_outline_delete_24px),
            contentDescription = null
        )
        Text(
            modifier = modifier,
            text = stringResource(id = R.string.setting_database_clear_title),
            fontSize = 30.sp
        )
        Text(
            modifier = modifier,
            text = stringResource(id = R.string.setting_database_clear_select)
        )
    }
}