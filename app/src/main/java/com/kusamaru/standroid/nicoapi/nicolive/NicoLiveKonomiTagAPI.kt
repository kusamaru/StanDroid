package com.kusamaru.standroid.nicoapi.nicolive

import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveKonomiTagData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * 好みタグAPI まとめ
 *
 * 好みタグは、番組単位ではなくユーザー単位で設定されるのでそこで差別化されてるんじゃないかと
 * */
class NicoLiveKonomiTagAPI {

    /** シングルトンなOkHttp */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 好みタグから番組を検索する。HTMLを返す。パースは[parseSearchProgramList]で
     *
     * もしくは自分がフォローしているタグを取得するときにも使える（[getFollowingTag]はユーザーIDが必要なので）
     *
     * @param userSession ユーザーセッション
     * @param tagId タグのID。「syamu game」なら「5263954」。自分のフォローしているタグが欲しい場合はnullでいいです
     * */
    suspend fun searchProgramFromKonomiTag(userSession: String, tagId: String? = null) = withContext(Dispatchers.Default) {
        val request = Request.Builder().apply {
            if (tagId != null) {
                url("https://live.nicovideo.jp/recent?hasKonomiTag=true&nicopediaArticleId=$tagId")
            } else {
                url("https://live.nicovideo.jp/recent?hasKonomiTag=true")
            }
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [searchProgramFromKonomiTag]をから検索結果の番組一覧を返す
     *
     * HTML内にJSONが潜んでいるのでそれを使ってパース。良かったスクレイピングじゃなくて
     *
     * @param html HTML
     * @return 番組データクラス
     * */
    suspend fun parseSearchProgramList(html: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultProgramList = arrayListOf<NicoLiveProgramData>()
        val document = Jsoup.parse(html)
        val jsonElement = document.getElementById("embedded-data").attr("data-props")
        val jsonObject = JSONObject(jsonElement)
        // 番組のJSON配列
        val programJSONArray = jsonObject.getJSONObject("view").getJSONObject("recentPrograms").getJSONObject("nicoProgramListState").getJSONArray("programList")
        for (i in 0 until programJSONArray.length()) {
            val programJSONObject = programJSONArray.getJSONObject(i)
            val beginAt = programJSONObject.getLong("beginAt")
            val endAt = programJSONObject.getLong("endAt")
            val programId = programJSONObject.getString("id")
            val liveCycle = programJSONObject.getString("liveCycle")
            val communityName = programJSONObject.getJSONObject("socialGroup").getString("name")
            val broadCasterName = programJSONObject.getJSONObject("programProvider").getString("name")
            val title = programJSONObject.getString("title")
            val thumbUrl = programJSONObject.getJSONObject("screenshotThumbnail").getString("liveScreenshotThumbnailUrl")
            resultProgramList.add(
                NicoLiveProgramData(
                    title = title,
                    beginAt = beginAt.toString(),
                    endAt = endAt.toString(),
                    communityName = communityName,
                    programId = programId,
                    broadCaster = broadCasterName,
                    lifeCycle = liveCycle,
                    thum = thumbUrl,
                    isOfficial = false
                )
            )
        }
        return@withContext resultProgramList
    }

    /**
     * [searchProgramFromKonomiTag]から自分かフォローしているタグを取り出す
     *
     * @param html HTML
     * @return 好みタグの配列
     * */
    suspend fun parseMyFollowingTagList(html: String?) = withContext(Dispatchers.Default) {
        // 返す配列
        val resultFollowingTagList = arrayListOf<NicoLiveKonomiTagData>()
        val document = Jsoup.parse(html)
        val jsonElement = document.getElementById("embedded-data").attr("data-props")
        val jsonObject = JSONObject(jsonElement)
        val followingUserObject = jsonObject.getJSONObject("view").getJSONObject("userFolloweeKonomiTag").getJSONObject("relations")
        // ユーザーIDがキーになっているのでキーを取る
        val userId = followingUserObject.keys().next()
        val followingJSONArray = followingUserObject.getJSONArray(userId)
        for (i in 0 until followingJSONArray.length()) {
            val followingJSONObject = followingJSONArray.getJSONObject(i)
            val name = followingJSONObject.getString("name")
            val followersCount = followingJSONObject.getInt("followersCount")
            val id = followingJSONObject.getString("id")
            resultFollowingTagList.add(
                NicoLiveKonomiTagData(
                    name = name,
                    tagId = id,
                    followersCount = followersCount,
                    isFollowing = true,
                )
            )
        }
        return@withContext resultFollowingTagList
    }

    /**
     * おすすめ好みタグAPIを叩く
     *
     * パースは [parseKonomiTag]
     *
     * @param userSession ユーザーセッション
     * */
    suspend fun getRecommendKonomiTag(userSession: String) = withContext(Dispatchers.IO) {
        // 取得だからGETだと思った？ざんねーんPOSTでした（は？）
        val postJSON = JSONObject().apply {
            put("recommend_recipe", JSONObject().apply {
                put("recipe_id", "live_konomi_tag")
                put("recipe_version", "1")
                put("site_id", "nicolive")
            })
        }
        val request = Request.Builder().apply {
            url("https://api.live2.nicovideo.jp/api/v1/konomiTags/GetRecommended")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            header("x-service-id", "nicolive")
            post(postJSON.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * タグをフォローする
     *
     * @param userSession ユーザーセッション
     * @param tagId タグのID
     * @return ステータスコードが200なら成功。特にレスポンスボディーはない
     * */
    suspend fun postFollowTag(userSession: String, tagId: String) = withContext(Dispatchers.IO) {
        val postJSON = JSONObject().apply {
            put("tag_ids", JSONArray().apply {
                put(JSONObject().apply {
                    put("value", tagId)
                })
            })
        }
        val request = Request.Builder().apply {
            url("https://api.live2.nicovideo.jp/api/v1/konomiTags/Follow")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            header("x-service-id", "nicolive")
            post(postJSON.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * タグのフォローを外す
     *
     * @param userSession ユーザーセッション
     * @param tagId タグのID
     * @return ステータスコードが200なら成功。特にレスポンスボディーはない
     * */
    suspend fun postRemoveFollowTag(userSession: String, tagId: String) = withContext(Dispatchers.IO) {
        val postJSON = JSONObject().apply {
            put("tag_ids", JSONArray().apply {
                put(JSONObject().apply {
                    put("value", tagId)
                })
            })
        }
        val request = Request.Builder().apply {
            url("https://api.live2.nicovideo.jp/api/v1/konomiTags/Unfollow")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            header("x-service-id", "nicolive")
            post(postJSON.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * ユーザーがフォローしている好みタグを返す。
     *
     * パースは[parseKonomiTag]
     *
     * @param userId ユーザーID
     * @param type そのままで
     * @param userSession ユーザーセッション
     * */
    suspend fun getFollowingTag(userSession: String, userId: String, type: String = "USER") = withContext(Dispatchers.IO) {
        // なんでPOSTなんだよ
        val postJSON = JSONObject().apply {
            put("follower_id", JSONObject().apply {
                put("value", userId)
                put("type", type)
            })
        }
        val request = Request.Builder().apply {
            url("https://api.live2.nicovideo.jp/api/v1/konomiTags/GetFollowing")
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            header("x-service-id", "nicolive")
            post(postJSON.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * おすすめ好みタグAPI、好みタグ取得API をパースする
     *
     * @param responseString レスポンスヘッダー
     * */
    suspend fun parseKonomiTag(responseString: String?) = withContext(Dispatchers.Default) {
        val resultRecommentTagList = arrayListOf<NicoLiveKonomiTagData>()
        val jsonObject = JSONObject(responseString)
        val konomiTagJSONArray = jsonObject.getJSONArray("konomi_tags")
        for (i in 0 until konomiTagJSONArray.length()) {
            val konomiTagJSONObject = konomiTagJSONArray.getJSONObject(i)
            val name = konomiTagJSONObject.getString("name")
            val followersCount = konomiTagJSONObject.getInt("followers_count")
            val tagId = konomiTagJSONObject.getJSONObject("tag_id").getString("value")
            resultRecommentTagList.add(
                NicoLiveKonomiTagData(
                    tagId = tagId,
                    name = name,
                    followersCount = followersCount,
                    isFollowing = false,
                )
            )
        }
        return@withContext resultRecommentTagList
    }

}