package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.CommentServerData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectIndexed
import okhttp3.internal.toLongOrDefault
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.net.URI
import java.util.*

/**
 * タイムシフト特化コメント取得クラス
 *
 * タイムシフトの番組のコメントを取得する方法は、いつもどおりHTML内のJSONを解析し、視聴セッションWebSocketを接続し、コメントサーバーの情報を取得します。
 *
 * その後、コメント鯖と接続し、接続が確立したあとに投げるJSONの中身を少し変えます。
 *
 * res_from 取得したいコメント番号。最後のコメント番号+1でいいと思う（初回時は1）
 * when res_fromからこの時間までのコメントを返すので時間を指定。UnixTimeだと思う。（番組開始時間ではなく、現実時間）
 *
 * たちみどろいどでは、最後のコメントの時間になったら次の一分間のコメントを取得するようになっています。
 *
 * ・問題のシーク
 *
 * シーク時はコメント番号がまだわからんので、まずシーク時の時間をwhenへ、res_fromをマイナスの値にしてシーク前のコメントを取得します。
 *
 * そのあと、取得したコメントの中から一番新しいコメントの番号を使ってあとは上記の方法で取得。
 *
 * */
class NicoLiveTimeShiftComment {

    private val nicoLiveComment = NicoLiveComment()

    private var webSocketClient: WebSocketClient? = null

    /** 再生時間を定期的に入れてください。 */
    var currentPositionSec = 0L

    /** 再生状態。trueで再生中 */
    var isPlaying = true

    /** コメントを一時的に保持しておく */
    private val commentJSONList = arrayListOf<CommentJSONParse>()

    /** 定期実行用 */
    private var commentTimer: Job? = null

    /** 一分感覚でJSONをWebSocketに投げて次の一分間のコメントを取得するのに使う */
    private var jsonPostTimer: Job? = null

    /** コメント取得時に指定した時間が入ってる。 */
    private var lastPostTimeSec = 0L

    /** コメント鯖情報 */
    private var commentServerData: CommentServerData? = null

    /** 番組開始時間 */
    private var programBeginTime: Long? = null

    /** コメントをほかでも受け取りたいのでFlow。直近の一件を保持しておく */
    private val commentFlow = MutableSharedFlow<CommentJSONParse>(replay = 1)

    /** シークでコメントをFlowで受け取るのでそのコルーチンのJob */
    private var seekCommentReceiveJob: Job? = null

    /** 接続を維持するために定期的になんか投げないといけないのでそのための */
    private var heartBeatJob: Job? = null

    /**
     * WebSocketにコメントをリクエストした際に返ってくる最後のコメント番号を控える
     * [CommentJSONParse]に[CommentJSONParse.commentNo]あるやんけｗって思うけど公式番組にはないので。
     * */
    private var lastCommentNo = 0

    /**
     * タイムシフト視聴特化コメント鯖接続関数
     *
     * @param commentServerData コメント鯖データ
     * @param onMessageFunc コメント来たときに呼ばれる高階関数
     * @param programBeginTime 番組開始時間
     * */
    fun connectCommentServerTimeShift(
        commentServerData: CommentServerData,
        programBeginTime: Long,
        onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit,
    ) {
        this@NicoLiveTimeShiftComment.programBeginTime = programBeginTime
        this@NicoLiveTimeShiftComment.commentServerData = commentServerData

        // ユーザーエージェントとプロトコル
        val protocol = Draft_6455(Collections.emptyList(), Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?)
        val headerMap = mutableMapOf<String, String>()
        headerMap["User-Agent"] = "TatimiDroid;@takusan_23"
        webSocketClient = object : WebSocketClient(URI(commentServerData.webSocketUri), protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                /**
                 * JSON作成して送信。
                 * 多分指定時間からコメント数を指定して取得する方法と、コメント番号から指定した時間まで取得する方法がある。今回は後者（番組開始直後はこの方法のみ？）
                 * */
                val jsonString = nicoLiveComment.createSendJson(commentServerData, 1, programBeginTime + 60, true)
                send(jsonString)

                // WebSocketとの接続を維持するためになんか投げておく
                heartBeatJob = GlobalScope.launch {
                    while (isActive) {
                        webSocketClient?.sendPing()
                        delay(60 * 1000)
                    }
                }

            }

            override fun onMessage(message: String?) {
                // 受け取る
                if (message != null) {

                    // thread オブジェクトを取得
                    val jsonObject = JSONObject(message)
                    if (jsonObject.has("thread")) {
                        // コメントをリクエストすると、最後のコメントの番号が最初に流れてくるJSONに入ってくるので
                        lastCommentNo = jsonObject.getJSONObject("thread").getInt("last_res")
                    }

                    // 配列に一旦収納。このあとのcommentTimerで高階関数をいい感じに呼ぶ
                    val commentJSONParse = CommentJSONParse(message, commentServerData.roomName, "")
                    commentJSONList.add(commentJSONParse)
                    // flowにも送る
                    commentFlow.tryEmit(commentJSONParse)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onError(ex: Exception?) {

            }

        }
        // 接続
        webSocketClient?.connect()

        /**
         * 定期実行する。
         * ここでは指定した時間のコメントを高階関数経由で返す処理と、コメントを追加で取得する処理が書いてある。
         * */
        commentTimer = GlobalScope.launch {

            while (isActive) {
                if (isPlaying) {
                    // 秒単位で流す。投稿日時から開始時間を引けば何秒のときに流せばいいのか出るので
                    val drawCommentList = commentJSONList.toList().filter { comment -> comment.date.toLongOrDefault(0) - programBeginTime == currentPositionSec }
                    if (drawCommentList.isNotEmpty()) {
                        // 公開関数を呼ぶ
                        drawCommentList.forEach { comment ->
                            onMessageFunc(comment.commentJson, commentServerData.roomName, false)
                        }
                    }
                    // 最後のコメントの時間になったらJSONをコメント鯖に投げて追加リクエスト
                    commentJSONList.lastOrNull()?.let { data ->
                        val date = data.date.toLongOrNull() ?: return@let
                        if (date - programBeginTime == currentPositionSec) {
                            // 最後のコメント番号から一分後までのコメントを取得
                            val jsonString = nicoLiveComment.createSendJson(commentServerData, lastCommentNo + 1, data.date.toLong() + 60, true)
                            if (webSocketClient?.isOpen == true) {
                                webSocketClient?.send(jsonString)
                            }
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    /**
     * シークする。
     *
     * @param seekPos 再生時間。秒。こっちは番組開始からの時間でいいよ
     * */
    fun seek(seekPos: Long) {
        if (commentServerData == null && programBeginTime == null) return
        // 時間移動
        lastPostTimeSec = programBeginTime!! + seekPos
        // コメントを受け取る
        seekCommentReceiveJob?.cancel()
        seekCommentReceiveJob = GlobalScope.launch {
            commentFlow.collectIndexed { index, commentData ->
                if (index == 2) {
                    // -1番目のコメントが来たので、次のコメント番号から一分間のコメントを貰いに行く
                    val commentNo = lastCommentNo + 1
                    val commentDate = commentData.date.toLong()
                    // WebSocketへ送信
                    val jsonString = nicoLiveComment.createSendJson(commentServerData!!, commentNo, commentDate + 60, true)
                    if (webSocketClient?.isOpen == true) {
                        webSocketClient?.send(jsonString)
                    }
                } else {
                    // 失敗する時がある
                }
            }
        }
        // シーク時間から-1番目のコメントをリクエスト
        val jsonString = nicoLiveComment.createSendJson(commentServerData!!, -1, lastPostTimeSec, true)
        if (webSocketClient?.isOpen == true) {
            webSocketClient?.send(jsonString)
        }
    }

    /** 終了時に呼んでください */
    fun destroy() {
        webSocketClient?.close()
        commentTimer?.cancel()
        jsonPostTimer?.cancel()
        seekCommentReceiveJob?.cancel()
        heartBeatJob?.cancel()
    }

}