package com.kusamaru.standroid.nicoapi.nicofeed

/**
 * フォロー新着APIからデータを引っ張ってくるとkindもついてくる
 * いろいろ種類あるっぽいけど必要になり次第追加していく
 */
enum class NicoFeedKinds(val kind: String) {
    Video("nicovideo.user.video.upload"),
    Live("nicolive.user.program.onairs"),
    Seiga("nicoseiga.user.illust.upload");

}