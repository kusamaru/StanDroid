package io.github.takusan23.tatimidroid.nguploader.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.NGUploaderUserIdEntity
import java.text.SimpleDateFormat

/**
 * NG投稿者のタイトル部分のUI
 * */
@Composable
fun NGUploaderTitle() {
    Column {
        Text(
            text = "NG投稿者機能（実験中）",
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * NG投稿者機能を有効、無効にする
 * */
@Composable
fun NGUploaderEnableSwitch(_isEnable: Boolean, onChecked: (Boolean) -> Unit) {
    // 有効かどうか
    val isEnable = remember { mutableStateOf(_isEnable) }

    // スイッチ変更関数
    fun change() {
        isEnable.value = !isEnable.value
        onChecked.invoke(isEnable.value)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .padding(10.dp)
            .clickable(onClick = { change() }) // 全体押せるように
    ) {
        Text(
            text = "NG投稿者機能を利用する",
            modifier = Modifier
                .weight(1f)
        )
        Switch(checked = isEnable.value, onCheckedChange = { change() })
    }
}

/**
 * NG投稿者機能の説明文
 * */
@Composable
fun NGUploaderDescription() {
    Text(
        text = """
            NG投稿者として登録すると、その投稿者が投稿した動画のIDを定期的に取得します。
            取得した動画IDが検索結果、ランキングに入っていれば除外して表示します。
            動画一覧のメニューを押してNG投稿者登録が可能です。
        """.trimIndent(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        textAlign = TextAlign.Start
    )
}

/**
 * NGユーザーの一覧表示で使う一個一個のレイアウト
 *
 * @param ngUploaderUserIdEntity NG投稿者のデータクラス
 * @param onClickDelete 削除ボタン押したら呼ばれる。
 * */
@Composable
fun NGUploaderUserListItem(
    ngUploaderUserIdEntity: NGUploaderUserIdEntity,
    onClickDelete: (String) -> Unit
) {
    // 時間フォーマット
    val simpleDateFormat = SimpleDateFormat("yyyy/MM/ss HH:mm:ss")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(5.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ngUploaderUserIdEntity.userId,
                fontSize = 18.sp
            )
            Text(text = "NG動画数：${ngUploaderUserIdEntity.videoCount}")
            Text(text = "追加日時：${simpleDateFormat.format(ngUploaderUserIdEntity.addDateTime)}")
            Text(text = "最終更新：${simpleDateFormat.format(ngUploaderUserIdEntity.latestUpdateTime)}")
        }
        // 削除ボタン
        IconButton(onClick = { onClickDelete(ngUploaderUserIdEntity.userId) }) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_delete_24px),
                contentDescription = "削除"
            )
        }
    }
}

/**
 * NG投稿者一覧
 *
 * @param list NG投稿者の配列
 * @param onClickDelete 削除ボタン押したら呼ばれる
 * */
@Composable
fun NGUploaderUserList(list: List<NGUploaderUserIdEntity>, onClickDelete: (String) -> Unit) {
    LazyColumn {
        items(list) { userData ->
            NGUploaderUserListItem(
                ngUploaderUserIdEntity = userData,
                onClickDelete = { onClickDelete(it) }
            )
            Divider()
        }
    }
}

/**
 * NG投稿者追加用UI
 *
 * @param onClickAddButton 追加ボタンを押したときに呼ばれる
 * */
@Composable
fun NGUploaderAddUserIdTextField(onClickAddButton: (String) -> Unit) {
    val textFieldValue = remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(5.dp)
    ) {
        OutlinedTextField(
            value = textFieldValue.value,
            onValueChange = { textFieldValue.value = it },
            modifier = Modifier.weight(1f),
            label = { Text(text = "ユーザーID") }
        )
        TextButton(onClick = { onClickAddButton(textFieldValue.value) }) {
            Icon(painter = painterResource(id = R.drawable.ic_baseline_add_24), contentDescription = "追加")
            Text(text = "NG投稿者追加")
        }
    }
}

@Preview
@Composable
fun NGUploaderPreview() {
    val demoNGUserData = listOf(
        NGUploaderUserIdEntity(0, "40210583", System.currentTimeMillis(), System.currentTimeMillis(), videoCount = 0, ""),
        NGUploaderUserIdEntity(0, "40210583", System.currentTimeMillis(), System.currentTimeMillis(), videoCount = 0, ""),
        NGUploaderUserIdEntity(0, "40210583", System.currentTimeMillis(), System.currentTimeMillis(), videoCount = 0, ""),
        NGUploaderUserIdEntity(0, "40210583", System.currentTimeMillis(), System.currentTimeMillis(), videoCount = 0, ""),
        NGUploaderUserIdEntity(0, "40210583", System.currentTimeMillis(), System.currentTimeMillis(), videoCount = 0, ""),
    )
    Column {
        NGUploaderTitle()
        NGUploaderDescription()
        Divider(modifier = Modifier.padding(5.dp))
        NGUploaderEnableSwitch(true, {})
        Divider(modifier = Modifier.padding(5.dp))
        NGUploaderUserList(list = demoNGUserData, onClickDelete = { })
    }
}