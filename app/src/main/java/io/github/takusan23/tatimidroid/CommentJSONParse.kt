package io.github.takusan23.tatimidroid

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class CommentJSONParse(val commentJson: String, var roomName: String, val videoOrLiveId: String) : Serializable {

    var comment = ""
    var dateUsec = ""
    var commentNo = ""
    var userId = ""
    var date = ""
    var premium = ""
    var mail = ""
    var vpos = "0"
    var origin = ""
    var score = ""
    var uneiComment = "" // ニコニ広告、ランクインなどをきれいにする
    var nicoru = 0 // ニコ動のみ。ニコる

    /**
     * ニコ動のみ。forkが1なら投稿者コメント、2ならかんたんコメントです。0なら通常
     * でもコメントしない層は何してもコメントしないと思いました。
     * */
    var fork = 0

    /** 自分が投稿したコメントの場合 true */
    var yourPost = false

    init {
        val jsonObject = JSONObject(commentJson)
        if (jsonObject.has("chat")) {
            val chatObject = jsonObject.getJSONObject("chat")
            comment = chatObject.optString("content", "")
            commentNo = chatObject.optString("no", "")
            userId = chatObject.optString("user_id", "")
            date = chatObject.getString("date")
            dateUsec = chatObject.optString("date_usec", "")
            vpos = chatObject.optString("vpos", "")
            //プレミアムかどうかはJSONにpremiumがあればいい（一般にはないので存在チェックいる）
            if (chatObject.has("premium")) {
                when (chatObject.getString("premium")) {
                    "1" -> premium = "\uD83C\uDD7F"
                    "2" -> premium = "運営"
                    "3" -> premium = "生主"
                }
            }
            //NGスコア？
            if (chatObject.has("score")) {
                score = chatObject.getString("score").toString()
            }
            //コメントが服従表示される問題
            if (chatObject.has("origin")) {
                origin = chatObject.getString("origin")
            }
            //mailの中に色コメントの色の情報があったりする
            if (chatObject.has("mail")) {
                mail = chatObject.getString("mail")
            }
            // ニコる
            if (chatObject.has("nicoru")) {
                nicoru = chatObject.getInt("nicoru")
            }
            // ニコ動限定。投稿者、かんたんコメントの判断に使える
            fork = chatObject.optInt("fork", 0)
            // yourpost（自分が投稿したコメントかどうか。ニコ生のみ？）取得←originのフィルターのせいで動いてない
            yourPost = chatObject.optInt("yourpost", 0) == 1
            // /nicoad、/info
            if (premium == "生主" || premium == "運営") {
                if (comment.contains("/info")) {
                    // /info {数字}　を消す
                    val regex = "/info \\d+ ".toRegex()
                    uneiComment = comment.replace(regex, "")
                }
                if (comment.contains("/nicoad")) {
                    // ニコニ広告
                    try {
                        val jsonObject = JSONObject(comment.replace("/nicoad ", "")) // HTML貼られると落ちてしまうので
                        uneiComment = jsonObject.getString("message")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                if (comment.contains("/spi")) {
                    // 新市場に貼られたとき
                    uneiComment = comment.replace("/spi ", "")
                }
                // 投げ銭
                if (comment.contains("/gift")) {
                    // スペース区切り配列
                    val list = comment.replace("/gift ", "")
                        .split(" ")
                    val userName = list[2]
                    val giftPoint = list[3]
                    val giftName = list[5]
                    uneiComment = "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                }
            }
        }
    }

    /**
     * コメントデータをコピーする。DeepCopy。参照渡しじゃない。値渡し
     *
     * このクラス、データクラスじゃないのでcopy()がない
     *
     * 深海少女　まだまだ沈む　
     *
     * @param argCommentJSONParse コピーするコメントクラス
     * @return コピーした。共有されることのない
     * */
    fun deepCopy(argCommentJSONParse: CommentJSONParse): CommentJSONParse {
        return CommentJSONParse(argCommentJSONParse.commentJson, argCommentJSONParse.roomName, argCommentJSONParse.videoOrLiveId)
    }

}