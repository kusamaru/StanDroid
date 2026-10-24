package com.kusamaru.standroid.nicoapi.nicovideo

import org.json.JSONArray
import org.json.JSONObject

/**
 * watchの応答を旧形式（data.response）と同じ形にする。保存済み・オフラインのJSONには使わない。
 *
 * 旧形式と同じく、再生に必要な項目だけを必須にする。投稿者・シリーズなどは取れなければnullにして再生を続ける。
 * */
internal object NicoVideoWatchResponse {
    fun unwrap(body: String?): JSONObject {
        require(!body.isNullOrBlank()) { "動画情報の応答が空です" }
        val root = JSONObject(body)
        val status = root.optJSONObject("meta")?.optInt("status", 200) ?: 200
        check(status in 200..299) { errorMessage(status, body) }
        return root.getJSONObject("data").getJSONObject("response")
    }

    /** 失敗時のメッセージ。本文に data.response.reason があれば添える。読み取れなければステータスのみ。 */
    fun errorMessage(status: Int, body: String?): String {
        val base = "動画情報の取得に失敗しました ($status)"
        val reason = try {
            JSONObject(body ?: return base).optJSONObject("data")?.optJSONObject("response")?.optString("reason")
        } catch (e: Exception) {
            null
        }
        return if (reason.isNullOrBlank()) base else "$base: $reason"
    }

    fun v4Data(response: JSONObject): JSONObject? = response.optJSONObject("\$watchV4")?.optJSONObject("data")

    /**
     * @param lazy v4/watch/lazy の data。取得できなかった場合はnull（投稿者・シリーズなしで続ける）
     * */
    fun normalize(response: JSONObject, lazy: JSONObject? = null): JSONObject {
        // 旧形式の応答はそのまま
        val v4 = v4Data(response) ?: return response
        val result = JSONObject(v4.toString())
        result.remove("lazy")
        result.remove("metadata")

        val video = result.getJSONObject("video")
        video.optJSONObject("thumbnail")?.let { thumbnail ->
            if (!thumbnail.has("url")) thumbnail.put("url", thumbnail.optString("normal", ""))
        }
        if (!result.isNull("viewer")) {
            video.put("viewer", JSONObject().put("like", JSONObject().put("isLiked", video.optBoolean("isLikedByViewer", false))))
        }
        result.put("tag", result.optJSONObject("tags") ?: JSONObject().put("items", JSONArray()))
        result.remove("tags")

        // 有料・会員限定などはmediaがnullになる。billingTypeで既存のisEncryption()に「再生できません」を出させる
        val media = result.optJSONObject("media")
        if (media != null) {
            val contents = media.optJSONObject("contents")
            media.put("domand", JSONObject()
                .put("accessRightKey", media.opt("accessRightKey"))
                .put("videos", contents?.opt("videos"))
                .put("audios", contents?.opt("audios")))
            media.put("delivery", JSONObject.NULL)
        } else {
            result.put("media", JSONObject().put("delivery", JSONObject.NULL).put("domand", JSONObject.NULL))
        }
        val payment = result.optJSONObject("payment") ?: JSONObject().also { result.put("payment", it) }
        payment.put("video", JSONObject().put("billingType", payment.optString("billingType", "free")))

        if (lazy == null) println("NicoVideoWatchResponse: lazy data is missing; owner and series are treated as absent")
        putOwner(result, lazy?.optJSONObject("owner"))
        result.put("series", lazy?.optJSONObject("series") ?: JSONObject.NULL)
        return result
    }

    /** 旧形式の owner（ユーザー）/ channel（チャンネル）に振り分ける。それ以外は退会済みと同じ扱い（両方null） */
    private fun putOwner(result: JSONObject, owner: JSONObject?) {
        result.put("owner", JSONObject.NULL).put("channel", JSONObject.NULL)
        if (owner == null || owner.optString("visibility") != "visible") return
        when (owner.optString("type")) {
            "user" -> result.put("owner", JSONObject(owner.toString())
                .put("iconUrl", owner.optJSONObject("icon")?.optString("url", "") ?: ""))
            "channel" -> result.put("channel", JSONObject()
                .put("id", owner.optString("id"))
                .put("name", owner.optString("name"))
                .put("thumbnail", JSONObject().put("url", owner.optJSONObject("thumbnail")?.optString("url", "") ?: "")))
            else -> println("NicoVideoWatchResponse: unknown owner type: ${owner.optString("type")}")
        }
    }
}
