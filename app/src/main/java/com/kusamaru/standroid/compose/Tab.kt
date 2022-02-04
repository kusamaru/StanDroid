package com.kusamaru.standroid.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kusamaru.standroid.tool.isDarkMode

/**
 * タブレイアウトの一つ一つのTab。
 * @param index 何個目のTabかどうか
 * @param tabIcon アイコン
 * @param tabName 名前
 * @param selectedIndex 現在選択中のタブの位置
 * @param tabClick タブを押した時
 * */
@Composable
fun TabPadding(index: Int, tabName: String, tabIcon: Painter, selectedIndex: Int, tabClick: (Int) -> Unit) {
    val themeColor = if (isDarkMode(LocalContext.current)) Color.White else Color.Black
    Tab(
        modifier = Modifier.padding(5.dp),
        selected = selectedIndex == index,
        selectedContentColor = themeColor,
        unselectedContentColor = themeColor.copy(alpha = ContentAlpha.medium),
        onClick = { tabClick(index) }
    ) {
        Icon(painter = tabIcon, contentDescription = tabName)
        Text(text = tabName)
    }
}