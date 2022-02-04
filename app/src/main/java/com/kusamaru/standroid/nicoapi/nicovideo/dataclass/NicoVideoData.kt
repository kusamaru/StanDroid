package com.kusamaru.standroid.nicoapi.nicovideo.dataclass

import java.io.Serializable

/**
 * 動画タイトル、動画ID、サムネとか
 * */
data class NicoVideoData(
    val isCache: Boolean, // キャッシュならtrue
    val isMylist: Boolean, // （自分の）マイリストならtrue
    val title: String,
    val videoId: String,
    val thum: String,
    val date: Long,
    val viewCount: String,
    val commentCount: String,
    val mylistCount: String,
    val mylistItemId: String = "",// マイリストのitem_idの値。マイリスト以外は空文字。
    val mylistAddedDate: Long? = null,// マイリストの追加日時。マイリスト以外はnull可能
    val duration: Long?,// 再生時間（秒）。
    val cacheAddedDate: Long? = null,// キャッシュ取得日時。キャッシュ以外ではnullいいよ
    val uploaderName: String? = null, // キャッシュ再生で使うからキャッシュ以外はnull
    val videoTag: ArrayList<String>? = arrayListOf(), // キャッシュ再生で使うからキャッシュ以外は省略していいよ
    val mylistId: String? = null, // マイリストのID。削除の時に使う。
    val isToriaezuMylist: Boolean = false, // とりあえずマイリストのときはtrue。他のマイリストorそもそもマイリストじゃないときはfalse
    val likeCount: Int = -1, // いいね数があれば入ってる
) : Serializable
