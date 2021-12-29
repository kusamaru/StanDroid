package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

/**
 * [MaterialTheme]に渡すテーマ。コードでテーマの設定ができるってマジ？
 * */

/** ダークモード。OLED特化 */
val DarkColors = darkColors(
    primary = Color.White,
    secondary = Color.Black,
)

/** ライトテーマ */
val LightColors = lightColors(
    primary = Color(android.graphics.Color.parseColor("#757575")),
    primaryVariant = Color(android.graphics.Color.parseColor("#494949")),
    secondary = Color(android.graphics.Color.parseColor("#a4a4a4")),
)