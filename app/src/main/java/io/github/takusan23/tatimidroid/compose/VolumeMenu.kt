package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 音量調整
 *
 * @param volume 音量の値。1f
 * @param volumeChange シークバーいじったら呼ばれる
 * */
@Composable
fun VolumeMenu(
    volume: Float,
    volumeChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // 音量調整スライダー
        Slider(
            value = volume,
            onValueChange = {
                volumeChange(it)
            }
        )
    }
}