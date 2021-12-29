package io.github.takusan23.tatimidroid.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * 折り返せるViewGroupみたいなやつ。FlexBoxLayoutのCompose版（超簡易的）
 *
 * @param content 置きたいComposeの部品（並びは保証しません）
 * @param isAcceptSort 小さい順にソートしてもいいならtrue
 * @param modifier Padding掛けたければどうぞ
 * @param isExpended 格納する場合はtrue
 * @param minHeight 格納時の高さ
 * */
@Composable
fun OrigamiLayout(
    modifier: Modifier = Modifier,
    isAcceptSort: Boolean = false,
    isExpended: Boolean = false,
    minHeight: Int = 100,
    content: @Composable () -> Unit,
) {
    // Viewで言うところのViewGroupの自作
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        // この中に入るCompose（子供Compose）の幅とかの情報の配列にする
        // なんかConstraints()のmaxのところはMAX_VALUE入れといてminには0を入れてあげれば大きさが取れるようになる
        val placeableList = measurables.map { it.measure(Constraints(0, constraints.maxWidth, 0, constraints.maxHeight)) }.toMutableList()
        // 最終的に入れるときに使うやつ
        val childrenDataList = arrayListOf<Triple<Int, Int, Placeable>>()
        // このComposeの幅
        val origamiWidth = constraints.maxWidth
        // 高さ計算
        var origamiHeight = 0
        // 列に入ってるComposeの合計の幅
        var lineWidth = 0
        // できる限り有効活用したいので小さい順に並び替える（許可されていれば）
        if (isAcceptSort) {
            // sortByは可変長配列（ArrayListとか）じゃないとない
            placeableList.sortBy { placeable -> placeable.width }
        }
        // 子供Composeがの位置を決定する
        placeableList.forEach { placeable ->
            if (lineWidth + placeable.width < origamiWidth) {
                // 今の行の幅が足りている場合
                // width / height / placeable
                childrenDataList.add(Triple(lineWidth, origamiHeight, placeable))
                lineWidth += placeable.width
            } else {
                // 足りてない
                // 次はもう入らないので次の行へ
                lineWidth = 0
                origamiHeight += placeable.height
                // width / height / placeable
                childrenDataList.add(Triple(lineWidth, origamiHeight, placeable))
                // 次の行に移動して幅を足す
                lineWidth = placeable.width
            }
        }
        // 展開時は必要な高さを
        val origamiLayoutHeight = if (isExpended) {
            origamiHeight + placeableList.last().height
        } else {
            minHeight
        }
        layout(width = constraints.maxWidth, height = origamiLayoutHeight) {
            // 部品を置いていく
            childrenDataList.forEach { triple ->
                val xPos = triple.first
                val yPos = triple.second
                val placeable = triple.third
                // 設置
                placeable.place(xPos, yPos)
            }
        }
    }
}
