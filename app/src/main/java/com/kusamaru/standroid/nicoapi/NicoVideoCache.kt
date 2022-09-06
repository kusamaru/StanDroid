package com.kusamaru.standroid.nicoapi

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.cache.CacheFilterDataClass
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHTML
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoCacheFilterBottomFragment
import com.kusamaru.standroid.tool.DownloadPocket
import com.kusamaru.standroid.tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * キャッシュ取得など。
 *
 * APIじゃないけど置く場所ないのでここで
 * */
class NicoVideoCache(val context: Context?) {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /** キャッシュ合計サイズ。注意：loadCache()を呼ぶまで0です */
    var cacheTotalSize = 0L

    /**
     * SDカードを保存先に設定している場合はtrue。
     *
     * SDカードが刺さる端末ほしい（持ってるけど古い）
     * */
    fun isEnableUseSDCard() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("setting_nicovideo_cache_use_sd_card", false)

    /**
     * キャッシュ用フォルダからデータ持ってくる。
     * [Dispatchers.IO]だとスレッド数が多いらしい←？
     * 多分重いのでコルーチンです。
     * @return NicoVideoDataの配列
     * */
    suspend fun loadCache() = withContext(Dispatchers.IO) {
        cacheTotalSize = 0
        val list = arrayListOf<NicoVideoData>()
        // データクラス変換のためだけ
        val nicoVideoHTML = NicoVideoHTML()
        // 端末固有ストレージ
        val cacheFolderPath = getCacheFolderPath()
        if (cacheFolderPath != null) {
            // 動画キャッシュようフォルダ作成
            val cacheFolder = File(cacheFolderPath)
            // 一覧取得
            cacheFolder.listFiles()?.forEach {
                it.listFiles()?.forEach {
                    cacheTotalSize += it.length()
                }
                // それぞれの動画フォルダ
                val videoFolder = it
                // 動画ID
                val videoId = videoFolder.name
                // 動画情報JSONパース
                val jsonString = File("${videoFolder.path}/${videoId}.json")
                // 2021/03/15以降に取得したか
                val isNewJSONFormat = if (jsonString.exists()) {
                    checkNewJSONFormat(jsonString.readText())
                } else false

                if (isNewJSONFormat) {
                    // まれによく落ちるので。ファイルあるって言ってんのに何で無いっていうの？
                    val jsonObject = try {
                        JSONObject(jsonString.readText())
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        return@forEach
                    }
                    // キャッシュ取得日時
                    val cacheAddedDate = it.lastModified()
                    // タグJSON
                    val tagsJSONArray = nicoVideoHTML.parseTagDataList(jsonObject).map { nicoTagItemData -> nicoTagItemData.tagName } as ArrayList
                    // サムネ
                    val thum = "${videoFolder.path}/${videoId}.jpg"
                    // データクラス
                    val nicoVideoData = nicoVideoHTML.createNicoVideoData(jsonObject, true).copy(
                        cacheAddedDate = cacheAddedDate,
                        videoTag = tagsJSONArray,
                        thum = thum
                    )
                    list.add(nicoVideoData)
                } else {

                    /**
                     * 動画情報JSON（2021/03/15より前に取得した場合も含めて）、サムネイルがない場合で読み込みたいときに使う。主にニコ生TSを見るときに使って。
                     * */
                    val isCache = true
                    val isMylist = false
                    val title = getCacheFolderVideoFileName(videoId) ?: it.name
                    val videoId = it.name
                    val thumbFile = File("${videoFolder.path}/${videoId}.jpg")
                    val thum = if (thumbFile.exists()) thumbFile.path else ""
                    val date = it.lastModified()
                    val viewCount = "-1"
                    val commentCount = "-1"
                    val mylistCount = "-1"
                    val mylistItemId = ""
                    val duration = 0L
                    // 動画からサムネイルを取得する
                    val data = NicoVideoData(isCache = isCache, isMylist = false, title = title, videoId = videoId, thum = thum, date = date, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = mylistItemId, mylistAddedDate = null, duration = duration, cacheAddedDate = date)
                    list.add(data)
                }
            }
        }
        // 新たしい順にソート
        list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
        list
    }

    /**
     * 動画の再生時間を取得する。ミリ秒ではなく秒です。
     * 重そう（小並感
     * @param videoId 動画ID
     * */
    fun getVideoDurationSec(videoId: String): Long {
        // 動画パス
        val videoFile = File("${context?.getExternalFilesDir(null)?.path}/cache/$videoId/${getCacheFolderVideoFileName(videoId)}")
        if (!videoFile.exists()) {
            return 0L
        }
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(videoFile.path)
        val time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: return 0L
        mediaMetadataRetriever.release()
        return time.toLong() / 1000
    }

    /**
     * キャッシュ一覧をCacheFilterDataClassでふるいにかけて返す。
     * @param cacheNicoVideoDataList loadCache()の返り値
     * @param filter フィルターかけるときに使って。
     * */
    fun getCacheFilterList(cacheNicoVideoDataList: ArrayList<NicoVideoData>, filter: CacheFilterDataClass): ArrayList<NicoVideoData> {

        // 部分一致検索。大文字小文字を無視するので強制大文字
        var filterList = cacheNicoVideoDataList.filter { nicoVideoData ->
            nicoVideoData.title.toUpperCase(Locale.getDefault()).contains(filter.titleContains.toUpperCase(Locale.getDefault()))
        } as ArrayList<NicoVideoData>

        // 指定中のタグソート
        filterList = filterList.filter { nicoVideoData ->
            nicoVideoData.videoTag?.containsAll(filter.tagItems) ?: false // 含まれているか
        } as ArrayList<NicoVideoData>

        // やったぜ。投稿者：でソート
        if (filter.uploaderName.isNotEmpty()) {
            filterList = filterList.filter { nicoVideoData ->
                filter.uploaderName == nicoVideoData.uploaderName
            } as ArrayList<NicoVideoData>
        }

        // たちみどろいどで取得したキャッシュのみを再生
        if (filter.isTatimiDroidGetCache) {
            filterList = filterList.filter { nicoVideoData ->
                nicoVideoData.commentCount != "-1"
            } as ArrayList<NicoVideoData>
        }

        // 並び替え
        sort(filterList, NicoVideoCacheFilterBottomFragment.CACHE_FILTER_SORT_LIST.indexOf(filter.sort))

        return filterList
    }

    /**
     * キャッシュを削除する
     * @param videoId 動画ID
     * */
    fun deleteCache(videoId: String?) {
        if (videoId == null) return
        // 削除
        val videoIdFolder = File("${getCacheFolderPath()}/$videoId")
        videoIdFolder.listFiles()?.forEach { it.delete() }
        videoIdFolder.delete()
    }

    /**
     * 2021/03/15？以降に取得したキャッシュかどうか。
     *
     * dmcInfoが置き換わったりしたけど、JSONの構造が良くなったと思う
     *
     * @param json 動画情報JSON
     * @return 2021/03/15以降のデータの場合はtrue
     * */
    fun checkNewJSONFormat(json: String): Boolean {
        val json = JSONObject(json)
        return json.has("media")
    }

    /**
     * 動画をダウンロードする[DownloadPocket]インスタンスを返す
     * */
    suspend fun getVideoDownloader(tmpFileFolder: File, videoIdFolder: File, videoId: String, url: String, userSession: String, nicoHistory: String, splitCount: Int) = withContext(Dispatchers.IO) {
        // 動画mp4ファイル作成
        val resultVideoFile = File("${videoIdFolder.path}/${videoId}.mp4")
        // ヘッダー
        val headers = arrayListOf(
            Pair("User-Agent", "Stan-Droid;@kusamaru_jp"),
            Pair("Cookie", "user_session=$userSession"),
            Pair("Cookie", nicoHistory),
        )
        // 並列ダウンロードするやつ
        return@withContext DownloadPocket(
            url = url,
            splitFileFolder = tmpFileFolder,
            resultVideoFile = resultVideoFile,
            headers = headers,
            splitCount = splitCount
        )
    }

    /**
     * js-initial-watch-dataのdata-api-dataを保存する。キャッシュ取得で動画情報を保存するときに使う。
     * privateな関数ではないので再取得にも使えます。
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * */
    suspend fun saveVideoInfo(videoIdFolder: File, videoId: String, json: String) = withContext(Dispatchers.Default) {
        // 動画情報JSON作成
        val videoJSONFile = File("${videoIdFolder.path}/$videoId.json")
        videoJSONFile.createNewFile()
        // Kotlinくっそ簡単やんけ！
        videoJSONFile.writeText(json)
        showToast("$videoId\n${context?.getString(R.string.get_cache_video_info_ok)}")
    }

    /**
     * 動画のサムネイルを取得する。OkHttp
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * @param userSession ユーザーセッション
     * */
    suspend fun getThumbnail(videoIdFolder: File, videoId: String, json: String, userSession: String) = withContext(Dispatchers.Default) {
        // JSONパース
        val jsonObject = JSONObject(json)
        val thumbnailURL = NicoVideoHTML().createNicoVideoData(jsonObject, true).thum
        // 動画サムネファイル作成
        val videoIdThum = File("${videoIdFolder.path}/$videoId.jpg")
        videoIdThum.createNewFile()
        // リクエスト
        val request = Request.Builder().apply {
            url(thumbnailURL)
            header("User-Agent", "Stan-Droid;@kusamaru_jp")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            // 保存
            val byte = response.body?.bytes()
            if (byte != null) {
                videoIdThum.writeBytes(byte)
                showToast("$videoId\n${context?.getString(R.string.get_cache_thum_ok)}")
            }
        }
    }

    /**
     * 動画のコメント取得。NicoVideoHTML#getComment()を使ってる。非同期
     * @param videoIdFolder 保存先フォルダー
     * @param videoId 動画ID
     * @param json data-api-data
     * @param userSession ユーザーセッション
     * */
    suspend fun getCacheComment(videoIdFolder: File, videoId: String, json: String, userSession: String) = withContext(Dispatchers.IO) {
        // POSTするJSON作成
        val response = NicoVideoHTML().getComment(userSession, JSONObject(json))
        if (response != null && response.isSuccessful) {
            // 動画コメントJSON作成
            val videoJSONFile = File("${videoIdFolder.path}/${videoId}_comment.json")
            videoJSONFile.createNewFile()
            // Kotlinくっそ簡単やんけ！
            videoJSONFile.writeText(response.body?.string()!!)
            showToast("$videoId\n${context?.getString(R.string.get_cache_comment_ok)}")
        }
    }

    /**
     * キャッシュフォルダのパス取得
     *
     * @param isUseSDCard SDカードを保存先として利用する場合はtrue。ただしSDカードが刺さっていなければtrueでも変わらない。
     * @return Contextがnullならnull
     * */
    fun getCacheFolderPath(isUseSDCard: Boolean = isEnableUseSDCard()): String? {
        context ?: return null
        // 保存可能フォルダを取得
        val folderList = ContextCompat.getExternalFilesDirs(context, null)
        // 保存先選定
        val parentFolder = when {
            isUseSDCard && canUseSDCard() -> folderList[1] // SDカードを使う設定ならこれ
            else -> folderList[0] // SDカード未対応、もしくはあるけど端末のストレージ使う
        }
        // 動画キャッシュようフォルダ作成
        val cacheFolder = File(parentFolder, "cache")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir()
        }
        return cacheFolder.path
    }

    /**
     * SDカードが利用できるか。SDカードスロットないとか利用できない場合はfalse

     * @return アクセスできる場合はtrue
     * */
    fun canUseSDCard(): Boolean {
        context ?: return false
        val canAccessFolderList = ContextCompat.getExternalFilesDirs(context, null)
        if (canAccessFolderList.size <= 1) {
            // SDカード入らない
            return false
        }
        val sdCardFolder = canAccessFolderList[1]
        return Environment.getExternalStorageState(sdCardFolder) == Environment.MEDIA_MOUNTED
    }

    /** キャッシュ取得の際に一時的に使えるフォルダのパス取得 */
    fun getCacheTempFolderPath(): String? {
        val media = context?.externalCacheDir
        // 動画キャッシュようフォルダ作成
        val cacheFolder = File(media, "cache_tmp")
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir()
        }
        return cacheFolder.path
    }

    // Toast表示
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * キャッシュフォルダから動画のパスを取得する
     * @param 動画ID
     * */
    fun getCacheFolderVideoFilePath(videoId: String): String {
        return "${getCacheFolderPath()}/$videoId/${getCacheFolderVideoFileName(videoId)}"
    }

    /**
     * キャッシュフォルダから動画情報JSONファイルの中身を取得する。JSONファイルの中身ですよ！
     * @param videoId 動画ID
     * */
    fun getCacheFolderVideoInfoText(videoId: String): String {
        return File("${getCacheFolderPath()}/$videoId/$videoId.json").readText()
    }

    /**
     * 動画情報JSONがあるかどうか。
     *
     * 新仕様（2021/03/15）の動画情報JSONファイルが有るかどうかの判断では[hasCacheNewVideoInfoJSON]を使ってください。
     *
     * @param videoId 動画ID
     * */
    fun hasCacheVideoInfoJSON(videoId: String): Boolean {
        return File("${getCacheFolderPath()}/$videoId/$videoId.json").exists()
    }

    /**
     * 新仕様（2021/03/15以降）に生成された動画情報JSONがある場合はtrueを返す
     *
     * @param videoId 動画ID
     * */
    fun hasCacheNewVideoInfoJSON(videoId: String): Boolean {
        return if (hasCacheVideoInfoJSON(videoId)) {
            // 読み込み
            val jsonText = getCacheFolderVideoInfoText(videoId)
            // 新仕様JSONかどうか
            checkNewJSONFormat(jsonText)
        } else {
            // そもそもない
            false
        }
    }

    /**
     * キャッシュフォルダから動画のサムネイルのパスを取得する。
     * @param videoId 動画ID
     * */
    fun getCacheFolderVideoThumFilePath(videoId: String): String {
        return "${getCacheFolderPath()}/$videoId/$videoId.jpg"
    }

    /**
     * コメントJSONファイルのFileを返す
     * */
    fun getCacheFolderVideoCommentFile(videoId: String): File {
        return File("${getCacheFolderPath()}/$videoId/${videoId}_comment.json")
    }

    /**
     * キャッシュフォルダから動画のコメントJSONファイルの中身を取得する。JSONファイルの中身ですよ！
     * */
    fun getCacheFolderVideoCommentText(videoId: String): String {
        return getCacheFolderVideoCommentFile(videoId).readText()
    }

    /**
     * キャッシュフォルダから動画の名前を取得する関数。ニコ生のTSのときに使って。
     * @return 動画ファイルの名前。ない場合はnull
     * */
    fun getCacheFolderVideoFileName(videoId: String): String? {
        // 見つける
        val videoFolder = File("${getCacheFolderPath()}/$videoId").listFiles() ?: return null
        for (i in videoFolder.indices) {
            if (videoFolder[i].extension == "mp4") {
                return videoFolder[i].name
            }
        }
        return null
    }

    /**
     * 動画ファイルが存在するかどうか。
     * @param videoId 動画ID。
     * @return あればtrueを、なければfalse
     * */
    fun hasCacheVideoFile(videoId: String): Boolean {
        val videoFolder = File("${getCacheFolderPath()}/$videoId").listFiles()
        // mp4でフィルターかけて0じゃなければある判定。てか IntelliJ IDEA くん優秀すぎん？array()#any{}に置き換えられるとか知らんかったわ
        return videoFolder?.any { file -> file.extension == "mp4" } ?: false
    }

    /**
     * 動画情報、コメント再取得まとめたやつ。
     * 二回も書かないと行けないのでここに書いた。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @param context Context。ActivityなりTextViewとかのViewだとView#getContext()あるし。。。
     * @param completeFun 終了時に呼ばれる高階関数。
     * */
    fun getReGetVideoInfoComment(videoId: String, userSession: String, context: Context?, completeFun: (() -> Unit)? = null) {
        GlobalScope.launch {
            val nicoVideoHTML = NicoVideoHTML()
            // 動画HTML取得
            val response = nicoVideoHTML.getHTML(videoId, userSession)
            if (response.isSuccessful) {
                // 動画情報更新
                val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                val videoIdFolder = File("${getCacheFolderPath()}/${videoId}")
                saveVideoInfo(videoIdFolder, videoId, jsonObject.toString())
                // コメント取得
                val commentResponse = nicoVideoHTML.getComment(userSession, jsonObject)
                val commentString = commentResponse?.body?.string()
                if (commentResponse?.isSuccessful == true && commentString != null) {
                    // コメント更新
                    getCacheComment(videoIdFolder, videoId, jsonObject.toString(), userSession)
                    showToast(context?.getString(R.string.cache_update_ok) ?: "取得できたよ")
                    if (completeFun != null) {
                        completeFun()
                    }
                } else {
                    showToast("${context?.getString(R.string.error)}\n${response.code}")
                }
            } else {
                showToast("${context?.getString(R.string.error)}\n${response?.code}")
            }
        }
    }

    /**
     * 動画IDかどうか（smかsoかnmのもじが入ってるかどうか）
     * @param videoId 動画IDかどうか確かめたい文字列
     * @return 動画IDの場合はtrue。違ったらfalse
     * */
    fun checkVideoId(videoId: String): Boolean {
        return when {
            videoId.contains("sm") -> true
            videoId.contains("so") -> true
            videoId.contains("nm") -> true
            else -> false
        }
    }

    private fun sort(list: ArrayList<NicoVideoData>, position: Int) {
        // 選択
        when (position) {
            0 -> list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
            1 -> list.sortBy { nicoVideoData -> nicoVideoData.cacheAddedDate }
            2 -> list.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> list.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> list.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> list.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> list.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> list.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> list.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> list.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> list.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> list.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
    }

}