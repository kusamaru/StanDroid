package com.kusamaru.standroid.nicoapi

/**
 * コメントのコマンドで使える色
 * */
object CommentColorList {

    /** 一般/プレ垢共通で利用できる色 */
    val COLOR_LIST = arrayListOf(
        ColorClass("white", "#ffffff"),
        ColorClass("red", "#FF0000"),
        ColorClass("pink", "#FF8080"),
        ColorClass("orange", "#FFC000"),
        ColorClass("yellow", "#FFFF00"),
        ColorClass("green", "#00FF00"),
        ColorClass("cyan", "#00FFFF"),
        ColorClass("blue", "#0000FF"),
        ColorClass("purple", "#C000FF"),
        ColorClass("black", "#000000"),
    )

    /** プレ垢のみが利用できる色 */
    val PREMIUM_COLOR_LIST = arrayListOf(
        ColorClass("white2", "#CCCC99"),
        ColorClass("red2", "#CC0033"),
        ColorClass("pink2", "#FF33CC"),
        ColorClass("orange2", "#FF6600"),
        ColorClass("yellow2", "#999900"),
        ColorClass("green2", "#00CC66"),
        ColorClass("cyan2", "#00CCCC"),
        ColorClass("blue2", "#3399FF"),
        ColorClass("purple2", "#6633CC"),
        ColorClass("black2", "#666666"),
    )

}

/**
 * ニコ動で使う色のデータクラス。
 * @param name 名前。white2とか
 * @param colorCode カラーコード。16進数
 * */
data class ColorClass(
    val name: String,
    val colorCode: String,
)
