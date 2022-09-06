package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.CommentServerData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.*
import okhttp3.Request
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.net.URI
import java.util.*

/**
 * 全部屋コメントサーバーに接続する関数。
 * 全部屋の中にstoreってのが混じる様になったけどこれ部屋の統合（全部アリーナ）で入り切らなかったコメントが流れてくる場所らしい。
 * 公式番組では今の部屋のみ接続している。
 * */
class NicoLiveComment {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    // 接続済みWebSocketアドレスが入る
    private val connectedWebSocketAddressList = arrayListOf<String>()

    /** 接続中の[CommentServerData]が入る配列。なお重複は消してます */
    private val connectionCommentServerDataList = arrayListOf<CommentServerData>()

    // 接続済みWebSocketClientが入る
    private val connectionWebSocketClientList = arrayListOf<WebSocketClient>()

    // タイムシフトコメント取得関数用。定期的にコメントを取りに行ってるのでキャンセル用に
    private var tsCommentTimerJob: Job? = null

    /**
     * 公式番組は視聴セッションWebSocketから流れてきたmessageServerUriとかを使ってこれ「connectionWebSocket()」使って
     * */

    /**
     * 公式以外の番組。
     * 全部屋WebSocketアドレスがあるAPIがあるので使わせてもらう
     * @return OkhttpのResponse
     * */
    suspend fun getProgramInfo(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/watch/$liveId/programinfo")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * データクラスの配列にパースする
     * @param responseString getProgramInfo()のレスポンス
     * @param arena アリーナの文字列をローカライズする場合は
     * @return CommentServerDataの配列
     * */
    suspend fun parseCommentServerDataList(responseString: String?, allRoomName: String, storeRoomName: String) = withContext(Dispatchers.Default) {
        val list = arrayListOf<CommentServerData>()
        val jsonObject = JSONObject(responseString)
        val data = jsonObject.getJSONObject("data")
        val room = data.getJSONArray("rooms")
        // アリーナ、立ち見のコメントサーバーへ接続
        for (index in 0 until room.length()) {
            val roomObject = room.getJSONObject(index)
            val webSocketUri = roomObject.getString("webSocketUri")
            val name = roomObject.getString("name")
            val roomName = name
            val threadId = roomObject.getString("threadId")
            val data = CommentServerData(webSocketUri, threadId, roomName)
            list.add(data)
        }
        list
    }

    /**
     * 流量制限にかかったコメントが流れてくるコメント鯖に接続するのに必要な値をJSONから取り出す関数。
     * 流量制限のコメントサーバーは他のコメントビューアーでは「store」って表記されていると思いますが、わからんので流量制限って書いてます。覚えとくと良いかも
     * 注意：公式番組（アニメ一挙放送など。ハナヤマタよかった）では利用できません。
     * @param responseString [getProgramInfo]のレスポンスボディー
     * @param storeRoomName 流量制限って文字列を入れて
     * @return [connectCommentServerWebSocket]で使う値の入ったデータクラス
     * */
    suspend fun parseStoreRoomServerData(responseString: String?, storeRoomName: String) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val data = jsonObject.getJSONObject("data")
        val room = data.getJSONArray("rooms")
        // 部屋の中から部屋名がstoreなものを探す
        for (index in 0 until room.length()) {
            val roomObject = room.getJSONObject(index)
            val webSocketUri = roomObject.getString("webSocketUri")
            val name = roomObject.getString("name")
            if (name == "store") {
                // ここが流量制限で入り切らないコメントが流れてくるコメントサーバー
                val threadId = roomObject.getString("threadId")
                return@withContext CommentServerData(webSocketUri, threadId, storeRoomName)
            }
        }
        return@withContext null
    }

    /**
     * コメント鯖へ接続する関数。
     * なおこの関数ではすでに接続済みかどうかの判定はしてません。というか多分いらないはず（部屋割り時代は定期的に部屋があるか確認してたので必要だった）
     * @param commentServerData コメントサーバーの情報が入ったデータクラス。threadId,部屋の名前,threadKeyがあれば作成可能です
     * @param requestHistoryCommentCount コメントの取得する量。負の値で
     * @param onMessageFunc コメントが来た時に呼ばれる高階関数
     * @param onOpen WebSocketが接続できたら呼ばれる
     * */
    fun connectCommentServerWebSocket(commentServerData: CommentServerData, requestHistoryCommentCount: Int = -100, onOpen: (() -> Unit)? = null, onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit) {
        // 過去コメントか流れてきたコメントか
        var historyComment = requestHistoryCommentCount
        // 過去コメントだとtrue
        var isHistoryComment = true
        val uri = URI(commentServerData.webSocketUri)
        // ユーザーエージェントとプロトコル
        val protocol = Draft_6455(Collections.emptyList(), Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?)
        val headerMap = mutableMapOf<String, String>()
        headerMap["User-Agent"] = "Stan-Droid;@kusamaru_jp"
        val webSocketClient = object : WebSocketClient(uri, protocol, headerMap) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //スレッド番号、過去コメントなど必要なものを最初に送る
                onOpen?.invoke()
                this.send(createSendJson(commentServerData, historyComment))
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    //過去コメントかな？
                    if (historyComment < 0) {
                        historyComment++
                    } else {
                        isHistoryComment = false
                    }
                    // 高階関数呼ぶ
                    onMessageFunc(message, commentServerData.roomName, isHistoryComment)
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        // 忘れるな
        webSocketClient.connect()
        connectionWebSocketClientList.add(webSocketClient)
        connectionCommentServerDataList.add(commentServerData)
        // 重複は消す
        connectionCommentServerDataList.distinctBy { commentServerData -> commentServerData.webSocketUri }
    }

    /**
     * [CommentServerData]からコメントサーバーへ投げるJSONを作成して返す
     * @param commentServerData サーバー情報
     * @param historyComment 取得するコメント数
     * @param isTimeShiftMode タイムシフト再生時はtrue
     * @param whenValue 現実時間。番組開始時間から見ての時間ではない。タイムシフトのときのみ使う。それ以外はnull
     * @return WebSocketに投げるJSON
     * */
    fun createSendJson(commentServerData: CommentServerData, historyComment: Int = -100, whenValue: Long? = null, isTimeShiftMode: Boolean = false): String {
        val sendJSONObject = JSONObject()
        val jsonObject = JSONObject().apply {
            put("version", "20061206")
            // put("service", "LIVE")
            put("thread", commentServerData.threadId)
            put("scores", 1)
            put("res_from", historyComment)
            put("nicoru", 0)
            put("with_global", 1)
            put("user_id", commentServerData.userId)
            // タイムシフト視聴時は threadkey つけると resultcode:9 が返ってくる
            if (!isTimeShiftMode) {
                put("threadkey", commentServerData.threadKey)
            }
            put("waybackkey", "")
            if (whenValue != null) {
                // 過去コメント
                put("when", whenValue)
            }
        }
        sendJSONObject.put("thread", jsonObject)
        return sendJSONObject.toString()
    }

    /**
     * タイムシフト再生用の[connectCommentServerWebSocket]
     *
     * タイムシフト再生時の挙動は、定期的にJSONを投げているっぽい。JSONを投げるとそこから一分間のコメントが帰ってくる？
     *
     * whenに時間を入れる。番組開始時刻+5分を足した時間を秒にして送信すると、5分までのコメントが帰ってくる
     *
     * @param startTime 番組開始時間。
     * @param commentServerData サーバー情報
     * @param onMessageFunc コメントが来たら呼ばれる関数
     * @param requestHistoryCommentCount 過去コメ取得数
     * */
    @Deprecated("別クラスが担当")
    fun connectCommentServerWebSocketTimeShiftVersion(commentServerData: CommentServerData, startTime: Long, requestHistoryCommentCount: Int = -100, onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit) {
        // 今残ってるWebSocket接続もキャンセル
        destroy()
        // とりあえず既存のタイマーはキャンセル
        tsCommentTimerJob?.cancel()
        // 最後にコメントくれ～って送信した時間。
        var lastCommentRequestTime = startTime

        /** タイムシフトコメントをリクエストする関数 */
        fun getComment(lastTime: Long) {
            // 時間を指定してコメントを取得する。
            val postCommentServerData = commentServerData.copy()
            // 多分配列の0番目が上でつないだWebSocketClient
            val client = connectionWebSocketClientList.first()
            // コメントサーバーにリクエストする
            client.send(createSendJson(postCommentServerData, requestHistoryCommentCount))
        }

        // WebSocketにとりあえず接続する。この段階ではコメントが流れてこない。WebSocketClientが欲しいのでそれだけ。来たコメントはここに来る
        connectCommentServerWebSocket(
            commentServerData,
            0,
            onOpen = {
                // 定期実行する
                tsCommentTimerJob = GlobalScope.launch {
                    while (isActive) {
                        lastCommentRequestTime += 60
                        // タイムシフトコメントリクエスト
                        getComment(lastCommentRequestTime)
                        // 1分間隔で
                        delay(60 * 1000)
                    }
                }
            },
            onMessageFunc = { commentText, roomMane, isHistory -> onMessageFunc(commentText, roomMane, isHistory) }
        )
    }

    /**
     * 終了時に呼んでね
     * */
    fun destroy() {
        connectionWebSocketClientList.forEach { it.close() }
        connectedWebSocketAddressList.clear()
        tsCommentTimerJob?.cancel()
    }

}