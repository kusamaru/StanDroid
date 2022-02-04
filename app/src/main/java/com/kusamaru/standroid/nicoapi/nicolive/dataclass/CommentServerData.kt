package com.kusamaru.standroid.nicoapi.nicolive.dataclass

import java.io.Serializable

/**
 * コメントサーバーのデータクラス
 * @param webSocketUri WebSocketアドレス
 * @param threadId スレッドID
 * @param roomName 部屋の名前
 * @param userId ユーザーID。なんで必要なのかは知らん。nullでも多分いい
 * @param threadKey 視聴セッションのWebSocketのときは「yourPostKey」ってのがJSONで流れてくるので指定して欲しい。nullても動く。ただ自分が投稿できたかの結果「yourpost」が取れない
 * */
data class CommentServerData(
    val webSocketUri: String,
    val threadId: String,
    val roomName: String,
    val threadKey: String? = null,
    val userId: String? = null,
) : Serializable
