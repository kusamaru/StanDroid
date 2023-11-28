package com.kusamaru.standroid

import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * nvComment仕様のコメントjsonに対応するやつ
 */
@kotlinx.serialization.Serializable
data class NvCommentJSONParse(
    val id: String,
    val no: Int,
    val vposMs: Int,
    val body: String,
    val commands: ArrayList<String>?,
    val userId: String,
    val isPremium: Boolean,
    val score: Int,
    val postedAt: String,
    val deleted: Int? = null,
    val nicoruCount: Int,
    val nicoruId: String?, // とりあえず決め打ちStringにしたけどこれ何？
    val source: String,
    val isMyPost: Boolean,
)

/**
 * TODO: めちゃめちゃ遅い。
 * NvCommentJSONParse(新仕様)からCommentJSONParse(旧仕様)にマッピングする。
 * 対応が追いつき次第使わないようにしたいけどね
 */
fun NvCommentJSONParse.toCommentJSONParse(videoId: String, forkType: NicoVideoHTML.CommentForkType): CommentJSONParse? {
    if (this.deleted != null) {
        // deletedっぽいんで触るのやめる、これでええんか
        return null
    }

    val self = this
    val jsonComment = JSONObject().apply {
        put("chat", JSONObject().apply {
            val dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            val postedAtEpoch = OffsetDateTime.parse(self.postedAt).toEpochSecond().toString()
            val premium = when (self.isPremium) {
                true -> "1"
                false -> null
            }
            put("content", self.body)
            put("date_usec", postedAtEpoch)
            put("commentNo", self.no)
            put("user_id", self.userId)
            put("date", postedAtEpoch)
            premium?.let {
                put("premium", it)
            }
            put("mail", self.commands?.joinToString(" ")) // とりあえず結合してみる
            put("vpos", Math.round((self.vposMs.toDouble() / 10)))
            put("origin", "") // origin <-> sourceって対応でええんか？
            if (self.score > 0) {
                put("score", self.score)
            }
            put("nicoru", self.nicoruCount)
            put("fork", forkType.typeId)
            put("yourpost", self.isMyPost)
        })
    }

    return CommentJSONParse(jsonComment.toString(), "ニコ動", videoId)
}