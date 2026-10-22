package com.kusamaru.standroid.nicoapi.nicovideo

import org.json.JSONException
import org.json.JSONObject

/** Converts watch responses at the acquisition boundary, never saved/offline JSON. */
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

    fun v4Data(response: JSONObject): JSONObject? = response.optJSONObject("\$watchV4")?.getJSONObject("data")

    fun normalize(response: JSONObject, lazy: JSONObject? = null): JSONObject {
        // Preserve the old response as-is, including DMC/offline-compatible data.
        val v4 = v4Data(response) ?: return response
        check(v4.getString("responseType").isNotBlank() && !v4.has("error")) { "動画を視聴できませんでした" }
        val extra = requireNotNull(lazy) { "動画の追加情報を取得できませんでした" }
        val result = JSONObject(v4.toString())
        result.remove("lazy")
        result.remove("metadata")

        val video = result.getJSONObject("video")
        normalizeThumbnail(video.getJSONObject("thumbnail"))
        if (!result.isNull("viewer")) {
            val viewer = result.getJSONObject("viewer")
            viewer.getString("id")
            viewer.getBoolean("isPremium")
            video.put("viewer", JSONObject().put("like", JSONObject().put("isLiked", video.getBoolean("isLikedByViewer"))))
        }
        result.put("tag", result.getJSONObject("tags"))
        result.remove("tags")

        val media = result.getJSONObject("media")
        check(media.isNull("encryption")) { "暗号化された動画には対応していません" }
        val contents = media.getJSONObject("contents")
        media.put("domand", JSONObject()
            .put("accessRightKey", media.getString("accessRightKey"))
            .put("videos", contents.getJSONArray("videos"))
            .put("audios", contents.getJSONArray("audios")))
        media.put("delivery", JSONObject.NULL)
        val payment = result.getJSONObject("payment")
        payment.put("video", JSONObject().put("billingType", payment.getString("billingType")))

        // Do not turn a failed lazy request into deleted owner/absent series data.
        require(extra.has("owner") && extra.has("series")) { "動画の追加情報が不完全です" }
        result.put("owner", JSONObject.NULL).put("channel", JSONObject.NULL)
        extra.optJSONObject("owner")?.let { owner ->
            // Only the visible/user shape was observed; do not invent channel/deletion mappings.
            check(owner.getString("visibility") == "visible" && owner.getString("type") == "user") {
                "未対応の投稿者形式です"
            }
            result.put("owner", JSONObject(owner.toString())
                .put("iconUrl", owner.getJSONObject("icon").getString("url")))
        }
        // The observed V4 lazy series (user series) has the same shape the legacy parsers read.
        // A series that does not match fails before it overwrites a cache.
        val series = extra.optJSONObject("series")
        check(series != null || extra.isNull("series")) { "未対応のシリーズ形式です" }
        result.put("series", if (series == null) JSONObject.NULL else validateSeries(series))
        return validate(result)
    }

    private fun validateSeries(series: JSONObject): JSONObject {
        try {
            series.getInt("id")
            series.getString("title")
            series.getString("thumbnailUrl")
            val video = series.getJSONObject("video")
            // prev/next/first are absent or null at the ends of a series.
            for (key in listOf("prev", "next", "first")) {
                val item = video.optJSONObject(key) ?: continue
                item.getString("id")
                item.getString("title")
                item.getString("registeredAt")
                item.getJSONObject("thumbnail").getString("largeUrl")
                val counts = item.getJSONObject("count")
                for (name in listOf("view", "comment", "mylist")) counts.getInt(name)
                item.getInt("duration")
            }
        } catch (e: JSONException) {
            throw IllegalStateException("未対応のシリーズ形式です", e)
        }
        return JSONObject(series.toString())
    }

    private fun normalizeThumbnail(thumbnail: JSONObject) {
        if (!thumbnail.has("url")) thumbnail.put("url", thumbnail.getString("normal"))
    }

    private fun validate(json: JSONObject): JSONObject {
        val video = json.getJSONObject("video")
        video.getString("id")
        video.getString("title")
        video.getString("description")
        video.getString("registeredAt")
        video.getLong("duration")
        video.getJSONObject("thumbnail").getString("url")
        val counts = video.getJSONObject("count")
        for (key in listOf("view", "comment", "mylist", "like")) counts.getLong(key)
        val client = json.getJSONObject("client")
        client.getString("watchId")
        client.getString("watchTrackId")
        val media = json.getJSONObject("media")
        if (media.isNull("delivery")) {
            val domand = media.getJSONObject("domand")
            domand.getString("accessRightKey")
            domand.getJSONArray("videos")
            domand.getJSONArray("audios")
        }
        json.getJSONObject("payment").getJSONObject("video").getString("billingType")
        val comment = json.getJSONObject("comment").getJSONObject("nvComment")
        comment.getString("server")
        comment.getJSONObject("params")
        comment.getString("threadKey")
        val tags = json.getJSONObject("tag").getJSONArray("items")
        for (index in 0 until tags.length()) {
            tags.getJSONObject(index).getString("name")
            tags.getJSONObject(index).getBoolean("isLocked")
        }
        json.optJSONObject("owner")?.let {
            it.getString("id")
            it.getString("nickname")
            it.getString("iconUrl")
        }
        json.optJSONObject("channel")?.let {
            it.getString("id")
            it.getString("name")
            it.getJSONObject("thumbnail").getString("url")
        }
        return json
    }
}
