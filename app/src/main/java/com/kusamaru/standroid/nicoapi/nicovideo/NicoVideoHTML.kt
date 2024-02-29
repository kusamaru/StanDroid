package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.NvCommentJSONParse
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoTagItemData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoHTMLSeriesData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import com.kusamaru.standroid.nicoapi.user.UserData
import com.kusamaru.standroid.toCommentJSONParse
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動の情報取得
 * コルーチンで使ってね
 * */
class NicoVideoHTML {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /** コルーチンキャンセル用 */
    private var heartBeatJob: Job? = null

    /**
     * HTML取得
     * @param eco 「１」を入れるとエコノミー
     * */
    suspend fun getHTML(videoId: String, userSession: String, eco: String = "") = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/watch/$videoId?eco=$eco")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * js-initial-watch-dataのdata-api-dataのJSONをデータクラス（[NicoVideoData]）へ変換する。
     * なんとなくコルーチンです。
     * @param jsonObject [parseJSON]の返り値
     * @param isCache キャッシュならtrue。省略時false
     * */
    suspend fun createNicoVideoData(jsonObject: JSONObject, isCache: Boolean = false) = withContext(Dispatchers.Default) {
        // JSON化
        val videoObject = jsonObject.getJSONObject("video")
        val videoCountObject = videoObject.getJSONObject("count")
        // データクラス化
        NicoVideoData(
            isCache = isCache,
            isMylist = false,
            title = videoObject.getString("title"),
            videoId = videoObject.getString("id"),
            thum = videoObject.getJSONObject("thumbnail").getString("url"),
            date = registeredAtToUnixTime(videoObject.getString("registeredAt")),
            viewCount = videoCountObject.getString("view"),
            commentCount = videoCountObject.getString("comment"),
            mylistCount = videoCountObject.getString("mylist"),
            likeCount = videoCountObject.getInt("like"),
            isToriaezuMylist = false,
            duration = videoObject.getLong("duration"),
            uploaderName = getUploaderName(jsonObject),
        )
    }

    /**
     * 動画が暗号化されているか

     * 暗号化されているときはtrue
     * されてないときはfalse
     * @param json js-initial-watch-dataのdata-api-data
     * */
    fun isEncryption(json: String): Boolean {
        return when {
            JSONObject(json).getJSONObject("media").optJSONObject("delivery")?.isNull("encryption") == false
            || JSONObject(json).getJSONObject("payment").getJSONObject("video").getString("billingType") != "free" -> true
            else -> false // encryption が null なら暗号化されてない
        }
    }

    /**
     * Domandでしか再生できない動画か。
     */
    fun isDomandOnly(json: JSONObject): Boolean {
        return json.getJSONObject("media").isNull("delivery") // deliveryがないとDomandのみの動画になる
    }

    /**
     * レスポンスヘッダーのSet-Cookieの中からnicohistoryを取得する関数
     * @param response getHTML()の返り値
     * @return nicohistoryの値。見つからないときはnull
     * */
    fun getNicoHistory(response: Response?): String? {
        val setCookie = response?.headers("Set-Cookie")
        setCookie?.forEach {
            if (it.contains("nicohistory")) {
                return it
            }
        }
        return null
    }

    /**
     * js-initial-watch-dataからdata-api-dataのJSONを取る関数
     * */
    fun parseJSON(html: String?): JSONObject {
        val document = Jsoup.parse(html)
        val json = document.getElementById("js-initial-watch-data").attr("data-api-data")
        return JSONObject(json)
    }

    /**
     * [getSessionAPI]のレスポンスJSONから動画URLを取得する。
     *
     * 旧鯖（Smile鯖）は2021/03/15をもって卒業したっぽい？
     *
     * ハートビート処理はheartBeat()関数を一度呼んでください。後は勝手にハートビートをPOSTしてくれます。
     *
     * 注意
     * DMCサーバーの動画はハートビート処理が必要。
     *
     * @param sessionJSONObject callSessionAPI()の戻り値。Smileサーバーならnullでいいです。
     * @return 動画URL。取得できないときはnullです。
     * */
    fun parseContentURI(sessionJSONObject: JSONObject?): String {
        // 多分全部の動画がDMC鯖に移管された？（Smile鯖見つからないし）。
        val data = sessionJSONObject!!.getJSONObject("data")
        // 動画のリンク
        val contentUrl = data.getJSONObject("session").getString("content_uri")
        return contentUrl
    }

    fun parseContentURIDomand(sessionJSONObject: JSONObject): String {
        return sessionJSONObject.getJSONObject("data").getString("contentUrl")
    }

    /**
     * DMC サーバー or Smile　サーバー
     *
     * 旧鯖（Smile鯖）は2021/03/15をもって卒業したっぽい？卒業おめでとう。
     *
     * @param jsonObject parseJSON()の戻り値
     * @return dmcサーバーならtrue
     * */
    @Deprecated("Smile鯖が４んだ可能性。DMC鯖だけになったかも？", ReplaceWith("true"))
    fun isDMCServer(jsonObject: JSONObject): Boolean {
        return true
    }

    /**
     * [getSessionAPI]のレスポンスJSONからハートビート用URL取得する関数。DMCサーバーの動画はハートビートしないと切られてしまうので。
     *
     * @param sessionJSONObject callSessionAPI()の戻り値
     * @return DMCの場合はハートビート用URLを返します。Smileサーバーの動画はnull
     * */
    private fun parseHeartBeatURL(sessionJSONObject: JSONObject): String {
        //DMC鯖
        val data = sessionJSONObject.getJSONObject("data")
        val id = data.getJSONObject("session").getString("id")
        //サーバーから切られないようにハートビートを送信する
        return "https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT"
    }

    // さようなら
//    suspend fun getSessionAPI(jsonObject: JSONObject, videoQualityId: String? = null, audioQualityId: String? = null) = withContext(Dispatchers.IO) {
//        return@withContext when {
//            // deliveryが存在しなかったらdomandっぽい
//            jsonObject.getJSONObject("media").isNull("delivery") -> getSessionAPIDomand(jsonObject, videoQualityId, audioQualityId)?.first
//            // あるならDMC
//            else -> getSessionAPIDMC(jsonObject, videoQualityId, audioQualityId)
//        }
//    }

    /**
     * JSONArrayから条件式にマッチするものを探す。
     * @param fn JSONObjectをとってBooleanを返すクロージャ
     * @return 無ければnull
     */
    fun JSONArray.findInObject(fn: (JSONObject) -> Boolean): JSONObject? {
        repeat(this.length()) {
            val obj = this.getJSONObject(it)
            if (fn(obj)) { return obj }
        }
        return null
    }

    /**
     * Domand鯖用の
     */
    suspend fun getSessionAPIDomand(
        jsonObject: JSONObject,
        videoQualityId: String? = null,
        audioQualityId: String? = null,
        nicosIdCookie: String?,
        userSession: String?
    ): Pair<JSONObject, List<String>>? = withContext(Dispatchers.IO) {
        val domandDataObject = jsonObject.getJSONObject("media").getJSONObject("domand")
        val clientObject = jsonObject.getJSONObject("client")

        val postAudioQualityJSONArray = if (audioQualityId != null) JSONArray().put(audioQualityId) else domandDataObject.getJSONArray("audios")
        val postVideoQualityJSONArray = if (videoQualityId != null) JSONArray().put(videoQualityId) else domandDataObject.getJSONArray("videos")

        // isAvailableな最高音質で行く
        // val audioFormat = audioQualityId ?: postAudioQualityJSONArray.findInObject { it.getBoolean("isAvailable") }?.getString("id")
        val audioFormat = audioQualityId ?: postAudioQualityJSONArray.getJSONObject(0)?.getString("id")
        val videoIdsList = JSONArray()
        for (i in 0 until postVideoQualityJSONArray.length()) {
            val obj = postVideoQualityJSONArray.getJSONObject(i)
            // if (!obj.getBoolean("isAvailable")) { continue } // !isAvailableなら無視
            videoIdsList.put(
                JSONArray().apply {
                    put(obj.getString("id"))
                    put(audioFormat)
                }
            )
        }

        val outputs = JSONObject().apply {
            put("outputs", videoIdsList)
        }
        val videoId = clientObject.getString("watchId")
        val actionTrackId = clientObject.getString("watchTrackId")
        val accessRightKey = domandDataObject.getString("accessRightKey")

        val url = "https://nvapi.nicovideo.jp/v1/watch/${videoId}/access-rights/hls?actionTrackId=$actionTrackId"
        val requestBody = outputs.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().apply {
            url(url)
            post(requestBody)
//            addHeader("Content-Type", "application/json")
//            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            addHeader("Cookie", "user_session=$userSession;")
//            if (nicosIdCookie != null) {
//                addHeader("Cookie", nicosIdCookie)
//            }
//            addHeader("Accept", "*/*")
//            addHeader("Accept_encoding", "br")
//            addHeader("Sec-Fetch-Dest", "empty")
            addHeader("X-Frontend-Id", "6")
            addHeader("X-Frontend-Version", "0")
            addHeader("X-Request-With", "https://www.nicovideo.jp")
            addHeader("X-Access-Right-Key", accessRightKey)
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            // cookie持ってかないとdomandが怒る
            val cookies = response.headers.filter { (k, _) -> k.lowercase() == "set-cookie" }.map { (_, v) -> v }
            val responseBody = response.body
            val jsonObject = JSONObject(responseBody?.string())
            responseBody?.close()
            println(jsonObject.toString())
            Pair(jsonObject, cookies)
        } else {
            null
        }
    }

    /**
     * ハートビートAPIを叩くときにPOSTする中身を返す。
     * @param sessionJSONObject callSessionAPI()の戻り値
     * @return DMCサーバーならPOSTする中身を返します。Smileサーバーならnullです。
     * */
    private fun getHearBeatJSONString(sessionJSONObject: JSONObject): String {
        val data = sessionJSONObject.getJSONObject("data")
        return data.toString()
    }

    /**
     * DMCサーバーの動画はもう一回APIを叩いてURLを手に入れる。
     *
     * [startHeartBeat]を呼んでハートビート処理を行う必要があります。
     *
     *  @param jsonObject parseJSON()の返り値
     *  @param videoQualityId 画質変更時は入れて。例：「archive_h264_4000kbps_1080p」。ない場合はJSONから。画質変更する場合のみ利用すればいいと思います。
     *  @param audioQualityId 音質変更時は入れて。例：「archive_aac_192kbps」。ない場合はJSONから。音質変更する場合のみ利用すればいいと思います。
     *
     *  @return APIのレスポンス。JSON形式
     * */
    suspend fun getSessionAPIDMC(jsonObject: JSONObject, videoQualityId: String? = null, audioQualityId: String? = null) = withContext(Dispatchers.IO) {
        val deliveryObject = jsonObject.getJSONObject("media").getJSONObject("delivery")
        // こっから情報をとっていく
        val sessionObject = deliveryObject.getJSONObject("movie").getJSONObject("session")
        // 音質。引数がnullならとりあえず最高画質
        val postAudioQualityJSONArray = if (audioQualityId != null) JSONArray().put(audioQualityId) else sessionObject.getJSONArray("audios")
        // 画質。引数がnullならとりあえず最高画質
        val postVideoQualityJSONArray = if (videoQualityId != null) JSONArray().put(videoQualityId) else sessionObject.getJSONArray("videos")
        //JSONつくる
        val sessionPOSTJSON = JSONObject().apply {
            put("session", JSONObject().apply {
                put("recipe_id", sessionObject.getString("recipeId"))
                put("content_id", sessionObject.getString("contentId"))
                put("content_type", "movie")
                put("content_src_id_sets", JSONArray().apply {
                    this.put(JSONObject().apply {
                        this.put("content_src_ids", JSONArray().apply {
                            this.put(JSONObject().apply {
                                this.put("src_id_to_mux", JSONObject().apply {
                                    // 画質指定
                                    this.remove("video_src_ids")
                                    this.remove("audio_src_ids")
                                    this.put("video_src_ids", postVideoQualityJSONArray)
                                    this.put("audio_src_ids", postAudioQualityJSONArray)
                                })
                            })
                        })
                    })
                })
                put("timing_constraint", "unlimited")
                put("keep_method", JSONObject().apply {
                    put("heartbeat", JSONObject().apply {
                        put("lifetime", 120000)
                    })
                })
                put("protocol", JSONObject().apply {
                    put("name", "http")
                    put("parameters", JSONObject().apply {
                        put("http_parameters", JSONObject().apply {
                            put("parameters", JSONObject().apply {
                                put("http_output_download_parameters", JSONObject().apply {
                                    put("use_well_known_port", "yes")
                                    put("use_ssl", "yes")
                                    // ログインしないモード対策
                                    val transferPresets = sessionObject.getJSONArray("transferPresets").optString(0, "")
                                    put("transfer_preset", transferPresets)
                                })
                            })
                        })
                    })
                })
                put("content_uri", "")
                put("session_operation_auth", JSONObject().apply {
                    put("session_operation_auth_by_signature", JSONObject().apply {
                        put("token", sessionObject.getString("token"))
                        put("signature", sessionObject.getString("signature"))
                    })
                })
                put("content_auth", JSONObject().apply {
                    put("auth_type", "ht2")
                    put("content_key_timeout", sessionObject.getInt("contentKeyTimeout"))
                    put("service_id", "nicovideo")
                    put("service_user_id", sessionObject.getString("serviceUserId"))
                })
                put("client_info", JSONObject().apply {
                    put("player_id", sessionObject.getString("playerId"))
                })
                put("priority", sessionObject.getDouble("priority"))
            })
        }
        //POSTする
        val requestBody =
            sessionPOSTJSON.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://api.dmc.nico/api/sessions?_format=json")
            .post(requestBody)
            .addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            .addHeader("Content-Type", "application/json")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseBody = response.body
            val jsonObject = JSONObject(responseBody?.string())
            responseBody?.close()
            jsonObject
        } else {
            null
        }
    }


    /**
     * ハートビート処理を行う。これをしないとサーバーから切られてしまう。最後にdestroy()呼ぶ必要があるのはこれを終了させるため
     * 40秒ごとに送信するらしい。
     * @param sessionAPIJSONObject callSessionAPI()の戻り値
     * */
    fun startHeartBeat(sessionAPIJSONObject: JSONObject) {
        val heartBeatURL = parseHeartBeatURL(sessionAPIJSONObject)
        val postData = getHearBeatJSONString(sessionAPIJSONObject)
        // 既存のハートビート処理はキャンセル
        heartBeatJob?.cancel()
        // 定期実行
        heartBeatJob = GlobalScope.launch {
            while (isActive) {
                // 40秒ごとにハートビート処理
                postHeartBeat(heartBeatURL, postData)
                // println("Angel Beats!")
                delay(40 * 1000L)
            }
        }
    }


    /**
     * getthumbinfoを叩く。コルーチン。
     * 再生時間を取得するのに使えます。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @return 取得失敗時はnull。成功時はResponse
     * */
    fun getThumbInfo(videoId: String, userSession: String): Deferred<Response?> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://ext.nicovideo.jp/api/getthumbinfo/$videoId")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            return@async response
        } else {
            return@async null
        }
    }


    /**
     * コメント取得に必要なJSONを作成する関数。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @param jsonObject js-initial-watch-dataのdata-api-dataのJSON
     * @return 取得失敗時はnull。成功時はResponse
     * */
    private suspend fun makeCommentAPIJSON(userSession: String, jsonObject: JSONObject) = withContext(Dispatchers.Default) {

        // userkey。2021/03/15のお昼ごろからJSONの中身が変わった模様
        val userkey = jsonObject.getJSONObject("comment").getJSONObject("keys").getString("userKey")
        // user_id
        val user_id = if (verifyLogin(jsonObject)) jsonObject.getJSONObject("viewer").getString("id") else ""

        // 動画時間（分）
        // duration(再生時間
        val length = jsonObject.getJSONObject("video").getInt("duration")
        // 必要なのは「分」なので割る
        // そして分に+1している模様
        // 一時間超えでも分を使う模様？66みたいに
        val minute = (length / 60) + 1

        //contentつくる。1000が限界？
        val content = "0-${minute}:100,1000,nicoru:100"

        /**
         * JSONの構成を指示してくれるJSONArray
         * threads[]の中になんのJSONを作ればいいかが書いてある。
         * */
        val commentComposite = jsonObject.getJSONObject("comment").getJSONArray("threads")
        // 投げるJSON
        val postJSONArray = JSONArray()
        for (i in 0 until commentComposite.length()) {
            val thread = commentComposite.getJSONObject(i)
            val thread_id = thread.getString("id")  //thread まじでなんでこの管理方法にしたんだ運営・・
            val fork = thread.getInt("fork")    //わからん。
            val isOwnerThread = thread.getBoolean("isOwnerThread")

            // 公式動画のみ？threadkeyとforce_184が必要かどうか
            val isThreadkeyRequired = thread.getBoolean("isThreadkeyRequired")

            // 公式動画のコメント取得に必須なthreadkeyとforce_184を取得する。
            var threadResponse: String? = ""
            var threadkey: String? = ""
            var force_184: String? = ""
            if (isThreadkeyRequired) {
                // 公式動画に必要なキーを取り出す。
                threadResponse = getThreadKeyForce184(thread_id, userSession)
                //なーんかUriでパースできないので仕方なく＆のいちを取り出して無理やり取り出す。
                val andPos = threadResponse?.indexOf("&")
                // threadkeyとforce_184パース
                threadkey = threadResponse?.substring(0, andPos!!)?.replace("threadkey=", "")
                force_184 = threadResponse?.substring(andPos!!, threadResponse.length)?.replace("&force_184=", "")
            }

            // threads[]のJSON配列の中に「isActive」がtrueなら次のJSONを作成します
            if (thread.getBoolean("isActive")) {
                val jsonObject = JSONObject().apply {
                    // 投稿者コメントも見れるように。「isOwnerThread」がtrueの場合は「version」を20061206にする？
                    if (isOwnerThread) {
                        put("version", "20061206")
                        put("res_from", -1000)
                    } else {
                        put("version", "20090904")
                    }
                    put("thread", thread_id)
                    put("fork", fork)
                    put("language", 0)
                    put("user_id", user_id)
                    put("with_global", 1)
                    put("score", 1)
                    put("nicoru", 3)
                    put("userkey", userkey)
                    // 公式動画（isThreadkeyRequiredはtrue）はthreadkeyとforce_184必須。
                    // threadkeyのときはもしかするとuserkeyいらない
                    if (isThreadkeyRequired) {
                        put("force_184", force_184)
                        put("threadkey", threadkey)
                    } else {
                        put("userkey", userkey) // 公式動画のときは userkey いらない模様
                    }
                }
                val post_thread = JSONObject().apply {
                    put("thread", jsonObject)
                }
                postJSONArray.put(post_thread)
            }
            // threads[]のJSON配列の中に「isLeafRequired」がtrueなら次のJSONを作成します
            if (thread.getBoolean("isLeafRequired")) {
                val jsonObject = JSONObject().apply {
                    put("thread", thread_id)
                    put("language", 0)
                    put("user_id", user_id)
                    put("content", content)
                    put("scores", 1)
                    put("nicoru", 3)
                    put("fork", fork) // これ指定するとなんか仕様変更耐えた
                    // 公式動画（isThreadkeyRequiredはtrue）はthreadkeyとforce_184必須。
                    // threadkeyのときはもしかするとuserkeyいらない
                    if (isThreadkeyRequired) {
                        put("force_184", force_184)
                        put("threadkey", threadkey)
                    } else {
                        put("userkey", userkey) // 公式動画のときは userkey いらない模様
                    }
                }
                val thread_leaves = JSONObject().apply {
                    put("thread_leaves", jsonObject)
                }
                postJSONArray.put(thread_leaves)
            }
        }
        postJSONArray
    }

    /**
     * コメント取得API。コルーチン。JSON形式の方です。
     * コメント取得くっっっっそめんどくせえ
     * @param userSession ユーザーセッション
     * @param jsonObject js-initial-watch-dataのdata-api-dataのJSON
     * @return 取得失敗時はnull。成功時はResponse
     * */
    suspend fun getComment(userSession: String, jsonObject: JSONObject) = withContext(Dispatchers.IO) {
        // 旧APIご臨終？っぽいのでnvcommentを利用する形に書き直した
        val nvComment = jsonObject.getJSONObject("comment").getJSONObject("nvComment")
        val postData = JSONObject().apply {
            put("params", nvComment.getJSONObject("params"))
            put("additionals", JSONObject())
            put("threadKey", nvComment.getString("threadKey"))
        }.toString().toRequestBody()
        // リクエスト
        val request = Request.Builder().apply {
            val commentServerUrl = nvComment.getString("server").plus("/v1/threads")
            url(commentServerUrl)
            header("Cookie", "user_session=${userSession}")
            header("Content-Type", "application/json")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("X-Frontend-Id", "6")
            header("X-Frontend-Version", "0")
            post(postData)
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response
        } else {
            println("failed: ${response.body?.string()}")
            null
        }
    }

    enum class CommentForkType(val _i: Int, val str: String, val typeId: Int) {
        OWNER(0, "owner", 1),
        MAIN(1, "main", 0),
        EASY(2, "easy", 2);

        companion object {
            fun fromInt(value: Int) = CommentForkType.values().first { it._i == value }
        }
    }
    /**
     * コメントJSONをパースする
     * @param responseString JSON
     * @return CommentJSONParseの配列
     * */
    fun parseCommentJSON(json: String, videoId: String): ArrayList<CommentJSONParse> {
        val commentList = arrayListOf<CommentJSONParse>()
//        val jsonArray = JSONArray(json)
//        for (i in 0 until jsonArray.length()) {
//            val jsonObject = jsonArray.getJSONObject(i)
//            if (jsonObject.has("chat") && !jsonObject.getJSONObject("chat").has("deleted")) {
//                val commentJSONParse = CommentJSONParse(jsonObject.toString(), "ニコ動", videoId)
//                commentList.add(commentJSONParse)
//            }
//        }
        // nvCommentを使う方式に書き直し
        val jsonObject = JSONObject(json)
        val threads = jsonObject.getJSONObject("data").getJSONArray("threads")
        for (i in 0 until threads.length()) {
            val forkType = CommentForkType.fromInt(i)
            val data = threads.getJSONObject(i)
            val comments = data.getJSONArray("comments")
            for (j in 0 until comments.length()) {
                val commentJSONParse = Json.decodeFromString<NvCommentJSONParse>(comments.getJSONObject(j).toString())
                commentJSONParse.toCommentJSONParse(videoId, forkType)?.let {
                    commentList.add(it)
                }
            }
        }
        // 新しい順に並び替え
        commentList.sortBy { commentJSONParse -> commentJSONParse.vpos.toInt() }
        return commentList
    }

    /**
     * 公式動画を取得するには別にAPIを叩く必要がある。この関数ね。コルーチンです。
     * @param threadId video.dmcInfo.thread.thread_id の値
     * @param userSession ユーザーセッション
     * @return 成功時threadKey。失敗時nullです
     * */
    private suspend fun getThreadKeyForce184(thread: String, userSession: String) = withContext(Dispatchers.IO) {
        val url = "https://flapi.nicovideo.jp/api/getthreadkey?thread=$thread"
        val request = Request.Builder()
            .url(url).get()
            .header("Cookie", "user_session=${userSession}")
            .header("User-Agent", "Stan-Droid;@kusamaru_jp")
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.string()
        } else {
            null
        }
    }

    /**
     * ハートビートをPOSTする関数。非同期処理です
     * @param url ハートビート用APIのURL
     * @param json [getHearBeatJSONString]の返り値
     * @param responseFun 成功時に呼ばれます。
     * */
    private fun postHeartBeat(url: String, json: String) {
        val request = Request.Builder().apply {
            url(url)
            post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            addHeader("User-Agent", "Stan-Droid;@kusamaru_jp")
            build()
        }.build()
        // エラーでまくる？なんで？なので非同期処理に切り替える
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {

            }
        })
    }

    /**
     * ログイン済みか。ログイン済みでもユーザーセッションは意外に早く無効化されてしまう。（多重ログイン等で）
     * 再ログインするときとかに使って
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return ログイン済みならtrue
     * */
    fun verifyLogin(jsonObject: JSONObject): Boolean {
        // 非ログイン時はviewerがnullになる
        return !jsonObject.isNull("viewer")
    }

    /**
     * プレミアム会員かどうか返す
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return プレミアム会員ならtrue
     * */
    fun isPremium(jsonObject: JSONObject): Boolean {
        return if (verifyLogin(jsonObject)) {
            jsonObject.getJSONObject("viewer").getBoolean("isPremium")
        } else false
    }

    /**
     * 公式動画かどうかを返す
     * 注意：なお公式動画だからといって全ての公式動画が暗号化とは限らないので、暗号化されてるかどうかは「isEncryption()」を使ってください。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return 公式動画ならtrue。
     * */
    fun isOfficial(jsonObject: JSONObject): Boolean {
        return !jsonObject.isNull("channel") // channelがnull以外ならおｋ
    }

    /**
     * ThreadIdを返す。ニコるKey取得とかコメント取得に使って
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @param isCommunityOrOfficial コミュニティの方のidを取得するときはtrue。公式動画のコメント（PC版でコメントリストの下の「チャンネルコメント」のドロップダウンメニューがある）をニコる場合はtrue。省略時は自動で判断します。
     * @return threadId
     * */
    fun getThreadId(jsonObject: JSONObject, isCommunityOrOfficial: Boolean = isOfficial(jsonObject)): String? {
        val threads = jsonObject.getJSONObject("comment").getJSONArray("threads")
        for (i in 0 until threads.length()) {
            val threadObject = threads.getJSONObject(i)
            val threadId = threadObject.getString("id")
            // 通常、投稿者コメ、かんたんコメント
            val label = threadObject.getString("label")
            when {
                (label == "community") && isCommunityOrOfficial -> {
                    // ちゃんねるコメント
                    return threadId
                }
                (label == "default") -> {
                    // 通常コメント
                    return threadId
                }
            }
        }
        return null
    }

    /**
     * ユーザーIDを取得する
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return userId
     * */
    fun getUserId(jsonObject: JSONObject): String {
        return jsonObject.getJSONObject("viewer").getString("id")
    }

    /**
     * 選択中の画質を取得する
     * @param sessionAPIJSONObject callSessionAPI()叩いたときのレスポンス
     * @return 画質。例：archive_h264_360p_low
     * */
    fun getCurrentVideoQuality(sessionAPIJSONObject: JSONObject): String? {
        val videoSrcId = sessionAPIJSONObject.getJSONObject("data").getJSONObject("session")
            .getJSONArray("content_src_id_sets").getJSONObject(0).getJSONArray("content_src_ids")
            .getJSONObject(0).getJSONObject("src_id_to_mux").getJSONArray("video_src_ids")
            .getString(0)
        return videoSrcId
    }

    /**
     * 選択中の音質を取得する
     * @param sessionAPIJSONObject callSessionAPI()叩いたときのレスポンス
     * @return 音質。例：archive_aac_192kbps
     * */
    fun getCurrentAudioQuality(sessionAPIJSONObject: JSONObject): String? {
        val audioQualityId = sessionAPIJSONObject.getJSONObject("data").getJSONObject("session")
            .getJSONArray("content_src_id_sets").getJSONObject(0).getJSONArray("content_src_ids")
            .getJSONObject(0).getJSONObject("src_id_to_mux").getJSONArray("audio_src_ids")
            .getString(0)
        return audioQualityId
    }

    /**
     * video.postedDateTimeの日付をUnixTime(ミリ秒)に変換する
     * */
    fun postedDateTimeToUnixTime(postedDateTime: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.parse(postedDateTime).time
    }

    /**
     * registeredAtの形式をUnixTime（ミリ秒）に変換する
     * */
    fun registeredAtToUnixTime(registeredAt: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(registeredAt).time
    }

    /**
     * 画質一覧を返す。

     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return media.domand.videos の値（配列）
     * */
    fun parseVideoQualityDomand(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("media").getJSONObject("domand").getJSONArray("videos")
    }

    /**
     * 音質一覧を返す。

     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return media.domand.audios の値（配列）
     * */
    fun parseAudioQualityDomand(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("media").getJSONObject("domand").getJSONArray("audios")
    }

    /**
     * 画質一覧を返す。

     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return media.delivery.movie.videos の値（配列）
     * */
    fun parseVideoQualityDMC(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("media").getJSONObject("delivery").getJSONObject("movie").getJSONArray("videos")
    }

    /**
     * 音質一覧を返す
     *
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return media.delivery.movie.audios の値（配列）
     * */
    fun parseAudioQualityDMC(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("media").getJSONObject("delivery").getJSONObject("movie").getJSONArray("audios")
    }

    /**
     * 投稿者のユーザーIDを取得する。
     * ユーザー投稿動画ならユーザーID。
     * 公式動画ならチャンネルID。
     * アカウント消した場合は空文字が返ってきます。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getUploaderId(jsonObject: JSONObject): String {
        return when {
            !jsonObject.isNull("owner") -> {
                jsonObject.getJSONObject("owner").getString("id") // ユーザーID
            }
            !jsonObject.isNull("channel") -> {
                jsonObject.getJSONObject("channel").getString("id") // 公式動画の時はチャンネルIDを
            }
            else -> "" // うｐ主が動画を消さずにアカウント消した場合は owner channel ともにnullになる。（というかアカウント消しても動画は残るんか）
        }
    }

    /**
     * 投稿者の名前を取得する。
     * もしアカウント消えてる場合はからの文字が返ってくる。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getUploaderName(jsonObject: JSONObject): String {
        return when {
            !jsonObject.isNull("owner") -> {
                jsonObject.getJSONObject("owner").getString("nickname") // ユーザー
            }
            !jsonObject.isNull("channel") -> {
                jsonObject.getJSONObject("channel").getString("name") // 公式動画
            }
            else -> "" // うｐ主が動画を消さずにアカウント消した場合は owner channel ともにnullになる。（というかアカウント消しても動画は残るんか）
        }
    }

    /**
     * [parseJSON]からユーザー情報を取り出してデータクラスに詰めて返す関数
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return ニコニコ動画、アカウント消しても動画は残る。のでアカウント情報が取れない場合はnullになります。
     * */
    fun parseUserData(jsonObject: JSONObject): UserData? {
        return if (isOfficial(jsonObject)) {
            // 公式
            val ownerObject = jsonObject.getJSONObject("channel")
            val userId = ownerObject.getString("id")
            val nickname = ownerObject.getString("name")
            val iconURL = ownerObject.getJSONObject("thumbnail").getString("url")
            UserData(
                description = "",
                isPremium = false,
                niconicoVersion = "",
                followeeCount = -1,
                followerCount = -1,
                userId = userId,
                nickName = nickname,
                isFollowing = false,
                currentLevel = 0,
                largeIcon = iconURL,
                isNotAPICallVer = true,
                isChannel = true,
            )
        } else if (!jsonObject.isNull("owner")) {
            // ユーザー
            val ownerObject = jsonObject.getJSONObject("owner")
            val userId = ownerObject.getString("id")
            val nickname = ownerObject.getString("nickname")
            val iconURL = ownerObject.getString("iconUrl")
            UserData(
                description = "",
                isPremium = false,
                niconicoVersion = "",
                followeeCount = -1,
                followerCount = -1,
                userId = userId,
                nickName = nickname,
                isFollowing = false,
                currentLevel = 0,
                largeIcon = iconURL,
                isNotAPICallVer = true,
            )
        } else {
            null
        }
    }

    /**
     * [parseJSON]からタグJSONをさがしてデータクラスに入れて配列で返す関数
     *
     * 多分生放送のJSON解析コードは使えない。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return [NicoTagItemData]の配列
     * */
    fun parseTagDataList(jsonObject: JSONObject): ArrayList<NicoTagItemData> {
        val tagDataClass = arrayListOf<NicoTagItemData>()
        val tagArray = jsonObject.getJSONObject("tag").getJSONArray("items")
        for (i in 0 until tagArray.length()) {
            val tagObject = tagArray.getJSONObject(i)
            val tagName = tagObject.getString("name")
            val isNicopediaExists = tagObject.getBoolean("isNicodicArticleExists")
            val isLocked = tagObject.getBoolean("isLocked")
            tagDataClass.add(
                NicoTagItemData(
                    tagName = tagName,
                    isLocked = isLocked,
                    type = "",
                    isDeletable = !isLocked,
                    hasNicoPedia = isNicopediaExists,
                    nicoPediaUrl = "https://dic.nicovideo.jp/a/$tagName"
                )
            )
        }
        return tagDataClass
    }

    /**
     * いいね済みかどうかを取得する。
     * 非ログイン時はfalseになる
     *
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun isLiked(jsonObject: JSONObject): Boolean {
        return if (verifyLogin(jsonObject)) {
            jsonObject.getJSONObject("video").getJSONObject("viewer").getJSONObject("like").getBoolean("isLiked")
        } else {
            false
        }
    }

    /**
     * シリーズが設定されている場合はシリーズIDを返す。なければnull
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getSeriesId(jsonObject: JSONObject): String? {
        return if (!jsonObject.isNull("series")) {
            // nullじゃない時
            jsonObject.getJSONObject("series").getString("id")
        } else {
            // Series設定してない
            null
        }
    }

    /**
     * シリーズが設定されている場合はシリーズ名を返す。なければnull
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getSeriesTitle(jsonObject: JSONObject): String? {
        return if (!jsonObject.isNull("series")) {
            jsonObject.getJSONObject("series").getString("title")
        } else {
            null
        }
    }

    /**
     * シリーズが設定されている場合はシリーズのデータクラスに詰めて返す。未設定ならnull
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return [NicoVideoSeriesData]。でもitemsCountはJSON解析では取れないので-1が返ります。
     * */
    suspend fun getSeriesData(jsonObject: JSONObject): NicoVideoSeriesData? = withContext(Dispatchers.Default) {
        if (!jsonObject.isNull("series")) {
            val seriesJSON = jsonObject.getJSONObject("series")
            val title = seriesJSON.getString("title")
            val seriesThumb = seriesJSON.getString("thumbnailUrl")
            val seriesId = seriesJSON.getInt("id").toString()
            NicoVideoSeriesData(title = title, seriesId = seriesId, itemsCount = -1, thumbUrl = seriesThumb)
        } else {
            null
        }
    }

    /**
     * ニコ動の視聴ページ内にあるシリーズのJSONオブジェクトをパースして、データクラスに入れる関数
     *
     * シリーズ最初の動画、次の動画、最後の動画の情報が入っています。
     *
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    suspend fun getSeriesHTMLData(jsonObject: JSONObject) = withContext(Dispatchers.Default) {
        return@withContext if (!jsonObject.isNull("series")) {
            val seriesJSON = jsonObject.getJSONObject("series")
            // シリーズのデータクラス
            val seriesData = getSeriesData(jsonObject)!!
            // 前後の動画のJSON
            val seriesVideoJSON = seriesJSON.getJSONObject("video")
            // 次の動画の情報
            val nextVideoData = if (seriesVideoJSON.isNull("next")) null else parseSeriesVideoData(seriesVideoJSON.getJSONObject("next"))
            // 前の動画の情報
            val prevVideoData = if (seriesVideoJSON.isNull("prev")) null else parseSeriesVideoData(seriesVideoJSON.getJSONObject("prev"))
            // 最初の動画
            val firstVideoData = if (seriesVideoJSON.isNull("first")) null else parseSeriesVideoData(seriesVideoJSON.getJSONObject("first"))
            // まとめてデータクラスへ
            NicoVideoHTMLSeriesData(seriesData, firstVideoData, nextVideoData, prevVideoData)
        } else {
            null
        }
    }

    /**
     * シリーズの次、前、最初の動画のJSONオブジェクトをパースする
     *
     * @param jsonObject js-initial-watch-dataのdata-api-data の series.firstVideo などの値
     * @return [NicoVideoData]
     * */
    private suspend fun parseSeriesVideoData(jsonObject: JSONObject) = withContext(Dispatchers.Default) {
        val videoId = jsonObject.getString("id")
        val title = jsonObject.getString("title")
        val thumb = jsonObject.getJSONObject("thumbnail").getString("largeUrl")
        val registerAt = jsonObject.getString("registeredAt")
        val viewObject = jsonObject.getJSONObject("count")
        val viewCount = viewObject.getInt("view")
        val commentCount = viewObject.getInt("comment")
        val mylistCount = viewObject.getInt("mylist")
        val duration = jsonObject.getInt("duration")
        return@withContext NicoVideoData(
            isCache = false,
            isMylist = false,
            title = title,
            videoId = videoId,
            thum = thumb,
            date = seriesRegisterAtToUnixTime(registerAt),
            viewCount = viewCount.toString(),
            commentCount = commentCount.toString(),
            mylistCount = mylistCount.toString(),
            duration = duration.toLong()
        )
    }

    // UnixTimeへ変換
    private fun seriesRegisterAtToUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

    /**
     * 動画の高さに合わせた画面の高さを返す関数。アスペクト比を考える
     * 1280 : 720 = 2180 : x  ←この式を解く
     * 720 * 2180 = 1280x   そとそとなかなかだっけ
     *
     * @param videoHeight 動画の縦の長さ。
     * @param videoWidth 動画の幅の長さ。
     * @param displayWidth 画面の幅。
     * @return 画面の幅とアスペクト比を考えて出した高さ。
     * */
    fun calcVideoHeightDisplaySize(videoWidth: Int, videoHeight: Int, displayWidth: Int): Float {
        return videoHeight.toFloat() / videoWidth * displayWidth
    }

    /**
     * 動画の幅に合わせた幅を返す関数。アスペクト比を考える
     *
     * [calcVideoHeightDisplaySize]の横幅版
     * @param videoHeight 動画の縦の長さ。
     * @param videoWidth 動画の幅の長さ。
     * @param displayHeight 画面の高さ。
     * @return 画面の幅とアスペクト比を考えて出した幅。
     * */
    fun calcVideoWidthDisplaySize(videoWidth: Int, videoHeight: Int, displayHeight: Int): Float {
        return videoWidth.toFloat() / videoHeight * displayHeight
    }

    /**
     * 終了時に呼んで
     *
     * もし動画連続再生を実装した場合、動画が切り替わる度にこの関数を呼んでください。
     * */
    fun destroy() {
        heartBeatJob?.cancel()
    }

}