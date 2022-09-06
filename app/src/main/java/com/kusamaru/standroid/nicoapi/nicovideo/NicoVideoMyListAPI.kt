package com.kusamaru.standroid.nicoapi.nicovideo

import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import kotlinx.coroutines.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * マイリストAPI
 * もう使えない（7/27の仕様変更でAPIのうらるが変わってたりしてる）
 * [NicoVideoSPMyListAPI]で同じことができると思います。（マイリストコメントとか使えなくなったけど誰も使ってねえしいいやろ）
 * */
@Deprecated("NicoVideoSPMyListAPIを使って")
class NicoVideoMyListAPI {

    /**
     * マイリストで使うトークンを取得するときに使うHTMLを取得する
     * ニコニコ新市場->動画引用だとこのTokenなしで取得できるAPIあるけど今回はPC版の方法で取得する
     * @param userSession ユーザーセッション
     * @return Response
     * */
    suspend fun getMyListHTML(userSession: String) = withContext(Dispatchers.IO) {
        val url = "https://www.nicovideo.jp/my/mylist"
        val request = Request.Builder().apply {
            url(url)
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * HTMLの中からTokenを取り出す
     * @param getMyListHTML()の戻り値
     * @return マイリスト取得で利用するToken。見つからなかったらnull
     * */
    fun getToken(string: String?): String? {
        //正規表現で取り出す。
        val regex = "NicoAPI.token = \"(.+?)\";"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(string)
        if (matcher.find()) {
            val token = matcher.group(1)
            //println("トークン　$token")
            return token
        }
        return null
    }

    /**
     * マイリスト一覧を取得する。
     * 注意　とりあえずマイリストはこの中に含まれません。
     * @param token getHTML()とgetToken()を使って取ったトークン
     * @param userSession ユーザーセッション
     * @return Response
     * */
    suspend fun getMyListList(token: String, userSession: String) = withContext(Dispatchers.IO) {
        val url = "https://www.nicovideo.jp/api/mylistgroup/list"
        // POSTする内容
        val post = FormBody.Builder()
            .add("token", token)
            .build()
        val request = Request.Builder().apply {
            url(url)
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            post(post)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * マイリストの中身を取得する
     * @param token getHTML()とgetToken()を使って取ったトークン。
     * @param mylistId マイリストのID。からの場合はとりあえずマイリストを取りに行きます
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun getMyListItems(token: String, mylistId: String, userSession: String): Deferred<Response> = GlobalScope.async {
        val post = FormBody.Builder().apply {
            add("token", token)
            //とりあえずマイリスト以外ではIDを入れる、
            if (mylistId.isNotEmpty()) {
                add("group_id", mylistId)
            }
        }.build()
        //とりあえずマイリストと普通のマイリスト。
        val url = if (mylistId.isEmpty()) {
            "https://www.nicovideo.jp/api/deflist/list"
        } else {
            "https://www.nicovideo.jp/api/mylist/list"
        }
        val request = Request.Builder().apply {
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            url(url)
            post(post)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * マイリストJSONパース
     * @param json getMyListItems()の戻り値
     * @return NicoVideoData配列
     * */
    fun parseMyListJSON(json: String?): ArrayList<NicoVideoData> {
        val myListList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val myListItem = jsonObject.getJSONArray("mylistitem")
        for (i in 0 until myListItem.length()) {
            val video = myListItem.getJSONObject(i)
            val itemId = video.getString("item_id")
            val addedDate = video.getLong("create_time")
            val itemData = video.getJSONObject("item_data")
            val title = itemData.getString("title")
            val videoId = itemData.getString("video_id")
            val thum = itemData.getString("thumbnail_url")
            val date = itemData.getLong("first_retrieve") * 1000 // ミリ秒へ
            val viewCount = itemData.getString("view_counter")
            val commentCount = itemData.getString("num_res")
            val mylistCount = itemData.getString("mylist_counter")
            val lengthSeconds = itemData.getLong("length_seconds")
            val data = NicoVideoData(isCache = false, isMylist = true, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = itemId, mylistAddedDate = addedDate, duration = lengthSeconds, cacheAddedDate = null)
            myListList.add(data)
        }
        return myListList
    }

    /**
     * マイリストに追加する。コルーチンです。
     * 注意：成功、失敗に関わらず200が帰ってくる模様。
     * {"status":"ok"}　なら成功
     * {"error":{"code":"EXIST","description":"\u3059\u3067\u306b\u767b\u9332\u3055\u308c\u3066\u3044\u307e\u3059"},"status":"fail"}
     * は失敗（descriptionがエスケープ文字になってるけど戻せば読めます。）
     * @param mylistId マイリストのID
     * @param threadId threadId。動画IDではない。js-initial-watch-dataのdata-api-dataのthread.ids.defaultの値。
     * @param description マイリストコメント。空白で良いんじゃね？
     * @param token getMyListHTML()とgetToken()を使って取得できるToken
     * @param userSession ユーザーセッション
     * */
    suspend fun mylistAddVideo(mylistId: String, threadId: String, description: String, token: String, userSession: String) = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder().apply {
            add("group_id", mylistId)
            add("item_type", "0") // 0が動画らしい
            add("item_id", threadId)
            add("description", description)
            add("token", token)
        }.build()
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/api/mylist/add")
            header("Cookie", "user_session=${userSession}")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            post(formBody)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * マイリストから動画を消す。
     * @param mylistId マイリストのID
     * @param itemId getMyListItems()で取得したJSONのJSON配列の中のitem_idの値。
     * @param token マイリストToken
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun mylistDeleteVideo(mylistId: String, itemId: String, token: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val form = FormBody.Builder().apply {
                add("group_id", mylistId)
                add("id_list[0][]", itemId)
                add("token", token)
            }.build()
            val request = Request.Builder().apply {
                url("https://www.nicovideo.jp/api/mylist/delete")
                header("Cookie", "user_session=${userSession}")
                header("User-Agent", "Stan-Droid;@kusamaru_jp")
                post(form)
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * 他の人のマイリストを取得する。コルーチンです。100件取れます。
     * @param id mylist/数字 の数字の部分だけ。
     * @param userSession ユーザーセッション。多分なくても（空文字でも）叩けるけど一応。
     * @param pageSize 省略時100件取れます。
     * */
    fun getOtherUserMylistItems(id: String, userSession: String = "", pageSize: Int = 100): Deferred<Response> = GlobalScope.async {
        //とりあえずマイリストと普通のマイリスト。
        val url = "https://nvapi.nicovideo.jp/v2/mylists/$id?pageSize=$pageSize"
        val request = Request.Builder().apply {
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            url(url)
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * 他の人のマイリストのJSONをパースする。
     * @param json getOtherUserMylistItems()の戻り値
     * @return NicoVideoData配列
     * */
    fun parseOtherUserMyListJSON(json: String?): ArrayList<NicoVideoData> {
        val myListList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val myListItem =
            jsonObject.getJSONObject("data").getJSONObject("mylist").getJSONArray("items")
        for (i in 0 until myListItem.length()) {
            val video = myListItem.getJSONObject(i)
            val itemId = video.getString("itemId")
            val videoObject = video.getJSONObject("video")
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thum = videoObject.getJSONObject("thumbnail").getString("url")
            val date = toUnixTime(videoObject.getString("registeredAt"))
            val countObject = videoObject.getJSONObject("count")
            val viewCount = countObject.getString("view")
            val commentCount = countObject.getString("comment")
            val mylistCount = countObject.getString("mylist")
            val addedAt = toUnixTime(video.getString("addedAt"))
            val duration = videoObject.getLong("duration")
            val data = NicoVideoData(isCache = false, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = itemId, mylistAddedDate = addedAt, duration = duration, cacheAddedDate = null)
            myListList.add(data)
        }
        return myListList
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

}