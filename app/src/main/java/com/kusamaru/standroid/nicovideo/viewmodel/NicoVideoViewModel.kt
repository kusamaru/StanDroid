package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.R
import com.kusamaru.standroid.adapter.parcelable.TabLayoutData
import com.kusamaru.standroid.nicoapi.NicoVideoCache
import com.kusamaru.standroid.nicoapi.XMLCommentJSON
import com.kusamaru.standroid.nicoapi.dataclass.QualityData
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoTagItemData
import com.kusamaru.standroid.nicoapi.nicovideo.*
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoHTMLSeriesData
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import com.kusamaru.standroid.nicoapi.user.UserData
import com.kusamaru.standroid.room.entity.NGDBEntity
import com.kusamaru.standroid.room.entity.NicoHistoryDBEntity
import com.kusamaru.standroid.room.init.NGDBInit
import com.kusamaru.standroid.room.init.NicoHistoryDBInit
import com.kusamaru.standroid.tool.isConnectionInternet
import com.kusamaru.standroid.tool.isConnectionMobileDataInternet
import com.kusamaru.standroid.tool.isLoginMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.NumberFormatException

/**
 * [com.kusamaru.standroid.nicovideo.NicoVideoFragment]のViewModel。
 *
 * いままでは画面回転前にデータ詰めてたんだけどViewModelを使えばFragmentのライフサイクルに関係なく生存する。
 *
 * でも何をおいておけば良いのかよくわからんので散らばってる。
 *
 * @param videoId 動画ID。連続再生の[videoList]が指定されている場合はnullに出来ます。また、連続再生時にこの値に動画IDを入れるとその動画から再生を始めるようにします。
 * @param isCache キャッシュで再生するか。ただし最終的には[isOfflinePlay]がtrueの時キャッシュ利用再生になります。連続再生の[videoList]が指定されている場合はnullに出来ます。
 * @param isEco エコノミー再生ならtrue。なお、キャッシュを優先的に利用する設定等でキャッシュ再生になっている場合があるので[isOfflinePlay]を使ってください。なお連続再生時はすべての動画をエコノミーで再生します。
 * @param useInternet キャッシュが有っても強制的にインターネットを経由して取得する場合はtrue。
 * @param _videoList 連続再生するなら配列を入れてね。nullでもいい。動画一覧が必要な場合は[playlistLiveData]があるのでこっちを利用してください（ViewModel生成後に連続再生に切り替えられるように）。
 * @param startPos 開始位置を指定する場合は入れてね
 * */
class NicoVideoViewModel(application: Application, videoId: String? = null, isCache: Boolean? = null, val isEco: Boolean, val useInternet: Boolean, startFullScreen: Boolean, private val _videoList: ArrayList<NicoVideoData>?, private val startPos: Int?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション。ログイン無しで利用する場合は文字列を空に */
    val userSession = if (isLoginMode(context)) prefSetting.getString("user_session", "") ?: "" else ""

    /** 再生中の動画ID。動画変更の検知は[isOfflinePlay]をObserveしたほうが良いと思う（[playingVideoId]→[isOfflinePlay]の順番でLiveData通知が行く） */
    var playingVideoId = MutableLiveData<String>()

    /** 結局インターネットで取得するのかローカルなのか。trueならキャッシュ再生。ちなみにLiveDataの通知順だと、[playingVideoId]のほうが先にくる。 */
    var isOfflinePlay = MutableLiveData<Boolean>()

    /** ViewPager2に動的に追加したFragment。 */
    val dynamicAddFragmentList = arrayListOf<TabLayoutData>()

    /** Fragment(Activity)へメッセージを送信するためのLiveData。Activity終了など */
    val messageLiveData = MutableLiveData<String>()

    /** Snackbarを表示しろってメッセージを送るLiveData */
    val snackbarLiveData = MutableLiveData<String>()

    /** ニコ動APIまとめ */
    val nicoVideoHTML = NicoVideoHTML()

    /** 新API。こっち使うようにした方がいいかも */
    // val nicoVideoWatchAPI = NicoVideoWatchAPI()

    /** キャッシュまとめ */
    val nicoVideoCache = NicoVideoCache(context)

    /** Smileサーバーの動画を再生するのに使う */
    var nicoHistory = ""

    /** ニコ動のJSON。コメントサーバーの情報や動画鯖の情報など */
    val nicoVideoJSON = MutableLiveData<JSONObject>()

    /** 旧サーバー（Smile鯖）の場合はfalse。DMC（画質変更ができる）ならtrue */
    @Deprecated("そもそもSmile鯖が４んだ可能性")
    var isDMCServer = true

    /** SessionAPIを叩いたレスポンスJSON */
    var sessionAPIJSON = JSONObject()

    /** 現在の画質 */
    var currentVideoQuality: String? = ""

    /** 現在の音質 */
    var currentAudioQuality: String? = ""

    /** 動画URL */
    val contentUrl = MutableLiveData<String>()

    /** 動画情報LiveData */
    val nicoVideoData = MutableLiveData<NicoVideoData>()

    /** 動画説明文LiveData */
    val nicoVideoDescriptionLiveData = MutableLiveData<String>()

    /** 画質一覧LiveData。映像のみ */
    val qualityDataListLiveData = MutableLiveData<List<QualityData>>()

    /** いいね済みかどうか。キャッシュ再生では使えない。 */
    val isLikedLiveData = MutableLiveData(false)

    /** いいねしたときのお礼メッセージを送信するLiveData */
    val likeThanksMessageLiveData = MutableLiveData<String>()

    /** 画面回転時にもいいねメッセージがサイド表示されるのでそれの制御 */
    var isAlreadyShowThanksMessage = false

    /** ユーザー情報LiveData */
    val userDataLiveData = MutableLiveData<UserData>()

    /** タグ送信LiveData */
    val tagListLiveData = MutableLiveData<ArrayList<NicoTagItemData>>()

    /** コメントAPIの結果。 */
    var rawCommentList = arrayListOf<CommentJSONParse>()

    /** NGを適用したコメント。LiveDataで監視できます */
    var commentList = MutableLiveData<ArrayList<CommentJSONParse>>()

    /** NG配列 */
    private var ngList = listOf<NGDBEntity>()

    /** 関連動画配列。なんで個々にあるのかは不明 */
    val recommendList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 現在再生中の位置 */
    var currentPosition = 0L

    /** 全画面再生 */
    var isFullScreenMode = startFullScreen

    /** ミニプレイヤーかどうか */
    var isMiniPlayerMode = MutableLiveData(false)

    /** 連続再生かどうか。連続再生ならtrue。なお、後から連続再生に切り替える機能をつけたいのでLiveDataになっています。*/
    val isPlayListMode = MutableLiveData(false)

    /** 連続再生時に、再生中の動画が[videoList]から見てどこの位置にあるかが入っている */
    val playListCurrentPosition = MutableLiveData(0)

    /** 連続再生時に逆順再生が有効になっているか。trueなら逆順 */
    val isReversed = MutableLiveData(false)

    /** 連続再生プレイリストLiveData。並び順変わった時なども通知が行く。[videoList]じゃなくてこっちを利用してください。 */
    val playlistLiveData = MutableLiveData(arrayListOf<NicoVideoData>())

    /** 連続再生の最初の並び順が入っている */
    val originVideoSortList = arrayListOf<String>()

    /** 連続再生時にシャッフル再生が有効になってるか。trueならシャッフル再生 */
    val isShuffled = MutableLiveData(false)

    /** コメントのみ表示する場合はtrue */
    var isCommentOnlyMode = prefSetting.getBoolean("setting_nicovideo_comment_only", false)

    /** 映像なしでコメントを流すコメント描画のみ、映像なしモード。ニコニコ実況みたいな */
    val isNotPlayVideoMode = MutableLiveData(false)

    /** プレイヤーの再生状態を通知するLiveData。これ経由で一時停止等を操作する。trueで再生 */
    val playerIsPlaying = MutableLiveData(true)

    /** 現在の再生位置。ミリ秒。LiveData版 */
    val playerCurrentPositionMsLiveData = MutableLiveData(0L)

    /**
     * 現在の再生位置。LiveDataではないので定期的に値を入れてください。ミリ秒
     * [isNotPlayVideoMode]がtrueの場合（コメントのみを流すモードの時）は[initNotPlayVideoMode]を呼んで動画を再生している" つもり" になって値を入れてあげてください。
     * 呼ばないとコメントが流れません。
     * */
    var playerCurrentPositionMs = 0L

    /** 動画をシークする際に使うLiveData。再生時間の取得には[playerCurrentPositionMs]を使ってくれ。ミリ秒 */
    val playerSetSeekMs = MutableLiveData<Long>()

    /** リピートモードを設定するLiveData。これ経由でリピートモードの設定をする。trueでリピート */
    val playerIsRepeatMode = MutableLiveData(prefSetting.getBoolean("nicovideo_repeat_on", true))

    /** 動画の時間を通知するLiveData。ミリ秒 */
    val playerDurationMs = MutableLiveData<Long>()

    /** 再生中ならfalse。バッファリング中（先読みとか）はtrue。*/
    val playerIsLoading = MutableLiveData(false)

    /** 音量調整をLiveData経由で行う。1fまで */
    val volumeControlLiveData = MutableLiveData<Float>()

    /** [isNotPlayVideoMode]がtrueのときにコルーチンを使うのでそれ */
    private val notVideoPlayModeCoroutineContext = Job()

    /** 動画の幅。ExoPlayerで取得して入れておいて */
    var videoWidth = 16

    /** 動画の高さ。同じくExoPlayerで取得して入れておいて */
    var videoHeight = 9

    /** コメント一覧表示してくれ～LiveData */
    val commentListShowLiveData = MutableLiveData(false)

    /** ニコるくんAPI */
    var nicoruAPI: NicoruAPI? = null

    /** シリーズが設定されていればシリーズの情報が入ってくる */
    val seriesDataLiveData = MutableLiveData<NicoVideoSeriesData>()

    /** シリーズが設定されていればシリーズの情報が入ってくる */
    val seriesHTMLDataLiveData = MutableLiveData<NicoVideoHTMLSeriesData?>()

    /** キャッシュ再生時にデータの再取得が必要なときに送信するLiveData */
    val cacheVideoJSONUpdateLiveData = MutableLiveData(false)

    /** コメント一覧を自動で展開しない設定かどうか */
    val isAutoCommentListShowOff = prefSetting.getBoolean("setting_nicovideo_jc_comment_auto_show_off", true)

    /** domand鯖での再生に使う */
    var domandCookie: String? = null

    init {

        // 最初の動画。
        load(videoId!!, isCache!!, isEco, useInternet)

        // 開始位置シーク
        if (startPos != null) {
            playerSetSeekMs.postValue(startPos * 1000L)
        }

        // NGデータベースを監視する
        viewModelScope.launch {
            NGDBInit.getInstance(context).ngDBDAO().flowGetNGAll().collect {
                ngList = it
                // コメントフィルター通す
                commentFilter()
            }
        }

        viewModelScope.launch {
            // 連続再生？
            if (_videoList != null) {
                startPlaylist(_videoList)
            }
        }

    }

    /**
     * 再生する関数。メインスレッドで呼べよ！！！
     *
     * @param videoId 動画ID
     * */
    fun load(videoId: String, isCache: Boolean, isEco: Boolean, useInternet: Boolean) {
        onCleared()
        playerSetSeekMs.postValue(0)
        notVideoPlayModeCoroutineContext.cancelChildren()
        playerCurrentPositionMs = 0
        playerCurrentPositionMsLiveData.postValue(0)

        // 動画ID変更を通知
        playingVideoId.value = videoId
        if (playlistLiveData.value != null) {
            playListCurrentPosition.value = playlistLiveData.value!!.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == videoId }
        }
        // どの方法で再生するか
        // キャッシュを優先的に使う設定有効？
        val isPriorityCache = prefSetting.getBoolean("setting_nicovideo_cache_priority", false)
        // キャッシュ再生が有効ならtrue
        isOfflinePlay.value = when {
            useInternet -> false // オンライン
            isCache -> true // キャッシュ再生
            NicoVideoCache(context).hasCacheVideoFile(videoId) && isPriorityCache -> true // キャッシュ優先再生が可能
            else -> false // オンライン
        }
        // 強制エコノミーの設定有効なら
        val isPreferenceEconomyMode = prefSetting.getBoolean("setting_nicovideo_economy", false)
        // エコノミー再生するなら
        val isEconomy = isEco
        // 再生準備を始める
        when {
            // キャッシュを優先的に使う&&キャッシュ取得済みの場合 もしくは　キャッシュ再生時
            isOfflinePlay.value ?: false -> cachePlay()
            // エコノミー再生？
            isEconomy || isPreferenceEconomyMode -> coroutine(true, null, null, true)
            // それ以外：インターネットで取得
            else -> coroutine()
        }
    }

    /** キャッシュから再生する */
    private fun cachePlay() {
        val videoId = playingVideoId.value ?: return
        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(context, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            messageLiveData.postValue(getString(R.string.xml_comment_play))
            return
        } else {
            // エラー時
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            viewModelScope.launch(Dispatchers.IO + errorHandler) {

                // 動画ファイルが有るか
                if (nicoVideoCache.hasCacheVideoFile(videoId)) {
                    val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                    contentUrl.postValue("${nicoVideoCache.getCacheFolderPath()}/${playingVideoId.value}/$videoFileName")
                } else {
                    // 動画無しでコメントだけを流すモードへ切り替える
                    withContext(Dispatchers.Main) {
                        showToast(context.getString(R.string.nicovideo_not_play_video_mode))
                        messageLiveData.postValue(context.getString(R.string.nicovideo_not_play_video_mode))
                        isNotPlayVideoMode.postValue(true)
                    }
                }

                // 動画情報JSONがあるかどうか（この関数が新仕様JSONかどうかも判断する）
                if (nicoVideoCache.hasCacheVideoInfoJSON(videoId)) {
                    val jsonText = nicoVideoCache.getCacheFolderVideoInfoText(videoId)
                    // 2021/03/15以降のみ対応
                    if (nicoVideoCache.checkNewJSONFormat(jsonText)) {
                        val jsonObject = JSONObject(jsonText)
                        nicoVideoJSON.postValue(jsonObject)
                        nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(jsonObject, isOfflinePlay.value ?: false))
                        // 動画説明文
                        nicoVideoDescriptionLiveData.postValue(jsonObject.getJSONObject("video").getString("description"))
                        // ユーザー情報LiveData
                        nicoVideoHTML.parseUserData(jsonObject)?.let { data -> userDataLiveData.postValue(data) }
                        // タグLiveData
                        tagListLiveData.postValue(nicoVideoHTML.parseTagDataList(jsonObject))
                        // シリーズが設定されていればシリーズ情報を返す
                        seriesDataLiveData.postValue(nicoVideoHTML.getSeriesData(jsonObject))
                        // シリーズのJSON解析してデータクラスにする
                        seriesHTMLDataLiveData.postValue(nicoVideoHTML.getSeriesHTMLData(jsonObject))
                    } else {
                        /** JSONパーサーが2021/03/15以降のJSONにしか対応してないので、なるはやアップデートしてって表示させる */
                        if (isConnectionInternet(context)) {
                            // インターネット接続時のみ表示
                            cacheVideoJSONUpdateLiveData.postValue(true)
                        }
                    }
                }

                // コメントが有るか
                if (xmlCommentJSON.commentJSONFileExists(videoId)) {
                    // コメント取得。
                    launch {
                        val commentJSONFilePath = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                        val loadCommentAsync = nicoVideoHTML.parseCommentJSON(commentJSONFilePath, videoId)
                        // フィルターで3ds消したりする。が、コメントは並列で読み込んでるので、並列で作業してるコメント取得を待つ（合流する）
                        rawCommentList = ArrayList(loadCommentAsync)
                        commentFilter(true)

                        // 動画なしモードと発覚した場合は自前で再生時間等を作成する
                        if (isNotPlayVideoMode.value == true) {
                            initNotPlayVideoMode()
                        }
                    }
                }
            }
        }
    }

    /**
     * インターネットから取得して再生する
     * @param videoId 動画ID
     * @param isGetComment コメントを取得する場合はtrue。基本true
     * @param videoQualityId 画質変更をする場合は入れてね。こんなの「archive_h264_4000kbps_1080p」
     * @param audioQualityId 音質変更をする場合は入れてね。
     * @param smileServerLowRequest Smile鯖で低画質をリクエストする場合はtrue。
     * */
    fun coroutine(isGetComment: Boolean = true, videoQualityId: String? = null, audioQualityId: String? = null, smileServerLowRequest: Boolean = false) {
        val videoId = playingVideoId.value ?: return
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            // smileサーバーの動画は多分最初の視聴ページHTML取得のときに?eco=1をつけないと低画質リクエストできない
            val eco = if (smileServerLowRequest) "1" else ""
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco)
            // 失敗したら落とす
            if (!response.isSuccessful) {
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val jsonObject = withContext(Dispatchers.Default) {
                // println("response: ${response.body?.string()}")
                nicoVideoHTML.parseJSON(response.body?.string())
            }

            // APIサーバーからデータ拾うよ
//            val response = nicoVideoWatchAPI.getVideoDataV3(videoId, userSession)
//            if (response == null) {
//                showToast("${getString(R.string.error)}\n")
//                return@launch
//            }
//            val jsonObject = response.first
//            val nicosIdCookie = response.second
            val nicosIdCookie = null

            nicoVideoJSON.postValue(jsonObject)
            // 動画説明文
            nicoVideoDescriptionLiveData.postValue(jsonObject.getJSONObject("video").getString("description"))
            // いいね済みかどうか
            isLikedLiveData.postValue(nicoVideoHTML.isLiked(jsonObject))
            // ユーザー情報LiveData
            nicoVideoHTML.parseUserData(jsonObject)?.let { data -> userDataLiveData.postValue(data) }
            // タグLiveData
            tagListLiveData.postValue(nicoVideoHTML.parseTagDataList(jsonObject))

            when {
                nicoVideoHTML.isDomandOnly(jsonObject) -> { // domand
                    // 公式アニメは暗号化されてて見れないので落とす
                    if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                        showToast(context.getString(R.string.encryption_video_not_play))
                        // FragmentにおいたLiveDataのオブザーバーへActivity落とせってメッセージを送る
                        messageLiveData.postValue(context.getString(R.string.encryption_video_not_play))
                        return@launch
                    } else {
                        // 再生可能
                        var videoQuality = videoQualityId
                        var audioQuality = audioQualityId
                        // 画質を指定している場合はモバイルデータ接続で最低画質の設定は無視
                        if (videoQuality == null && audioQuality == null) {
                            // モバイルデータ接続のときは強制的に低画質にする か エコノミー時は低画質
                            if ((prefSetting.getBoolean("setting_nicovideo_low_quality", false) && isConnectionMobileDataInternet(context)) || smileServerLowRequest) {
                                val videoQualityList = nicoVideoHTML.parseVideoQualityDomand(jsonObject)
                                val audioQualityList = nicoVideoHTML.parseAudioQualityDomand(jsonObject)
                                videoQuality = videoQualityList.getJSONObject(videoQualityList.length() - 1).getString("id")
                                audioQuality = audioQualityList.getJSONObject(audioQualityList.length() - 1).getString("id")
                            }
                            if (videoQuality != null) {
                                // 画質通知
                                showSnackBar("${getString(R.string.quality)}：$videoQuality")
                            }
                        }
                        // https://nvapi.nicovideo.jp/v1/watch のレスポンス
                        val sessionAPIResponse = nicoVideoHTML.getSessionAPIDomand(jsonObject, videoQuality, audioQuality, nicosIdCookie, userSession)
                        if (sessionAPIResponse != null) {
                            // Domand用のクッキーを確保
                            domandCookie = sessionAPIResponse.second.find { it.contains("domand_bid") }
                            sessionAPIJSON = sessionAPIResponse.first
                            // 動画URL
                            contentUrl.postValue(nicoVideoHTML.parseContentURIDomand(sessionAPIJSON))
                            // 選択中の画質、音質控える
                            currentVideoQuality = videoQuality
                            currentAudioQuality = audioQuality
                        }
                    }

                    // 画質一覧をLiveDataへ送信
                    setQualityListDomand(jsonObject.getJSONObject("media").getJSONObject("domand").getJSONArray("videos"))
                    // データクラスへ詰める
                    nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(jsonObject, isOfflinePlay.value ?: false))
                    // データベースへ書き込む
                    insertDB()

                    // コメント取得など
                    if (isGetComment) {
                        val commentJSON = async {
                            nicoVideoHTML.getComment(userSession, jsonObject)
                        }
                        rawCommentList = withContext(Dispatchers.Default) {
                            ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.await()?.body?.string()!!, videoId))
                        }
                        // フィルターで3ds消したりする
                        commentFilter(true)
                    }
                    // 関連動画
                    launch { getRecommend(jsonObject) }
                    // ニコるくん
                    if (nicoVideoHTML.isPremium(jsonObject)) {
                        launch {
                            // threadId取得
                            val threadId = nicoVideoHTML.getThreadId(jsonObject)
                            if (threadId != null) {
                                nicoruAPI = NicoruAPI(
                                    userSession = userSession,
                                    threadId = threadId,
                                    isPremium = nicoVideoHTML.isPremium(jsonObject),
                                    userId = nicoVideoHTML.getUserId(jsonObject)
                                )
                                nicoruAPI?.init()
                            }
                        }
                    }
                }
                else -> { // dmc
                    // 公式アニメは暗号化されてて見れないので落とす。最近プレ垢限定でアニメ配信してるんだっけ？
                    if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                        println("encrypted")
                        showToast(context.getString(R.string.encryption_video_not_play))
                        // FragmentにおいたLiveDataのオブザーバーへActivity落とせってメッセージを送る
                        messageLiveData.postValue(context.getString(R.string.encryption_video_not_play))
                        return@launch
                    } else {
                        // 再生可能
                        var videoQuality = videoQualityId
                        var audioQuality = audioQualityId
                        // 画質を指定している場合はモバイルデータ接続で最低画質の設定は無視
                        if (videoQuality == null && audioQuality == null) {
                            // モバイルデータ接続のときは強制的に低画質にする か エコノミー時は低画質
                            if ((prefSetting.getBoolean("setting_nicovideo_low_quality", false) && isConnectionMobileDataInternet(context)) || smileServerLowRequest) {
                                val videoQualityList = nicoVideoHTML.parseVideoQualityDMC(jsonObject)
                                val audioQualityList = nicoVideoHTML.parseAudioQualityDMC(jsonObject)
                                videoQuality = videoQualityList.getJSONObject(videoQualityList.length() - 1).getString("id")
                                audioQuality = audioQualityList.getJSONObject(audioQualityList.length() - 1).getString("id")
                            }
                            if (videoQuality != null) {
                                // 画質通知
                                showSnackBar("${getString(R.string.quality)}：$videoQuality")
                            }
                        }
                        // https://api.dmc.nico/api/sessions のレスポンス
                        val sessionAPIResponse = nicoVideoHTML.getSessionAPIDMC(jsonObject, videoQuality, audioQuality)
                        if (sessionAPIResponse != null) {
                            sessionAPIJSON = sessionAPIResponse
                            // 動画URL
                            contentUrl.postValue(nicoVideoHTML.parseContentURI(sessionAPIJSON))
                            // ハートビート処理。これしないと切られる。
                            nicoVideoHTML.startHeartBeat(sessionAPIJSON)
                            // 選択中の画質、音質控える
                            currentVideoQuality = nicoVideoHTML.getCurrentVideoQuality(sessionAPIJSON)
                            currentAudioQuality = nicoVideoHTML.getCurrentAudioQuality(sessionAPIJSON)
                        }
                    }

                    // 画質一覧をLiveDataへ送信
                    setQualityListDMC(nicoVideoHTML.parseVideoQualityDMC(jsonObject))
                    // データクラスへ詰める
                    nicoVideoData.postValue(nicoVideoHTML.createNicoVideoData(jsonObject, isOfflinePlay.value ?: false))
                    // データベースへ書き込む
                    insertDB()

                    // コメント取得など
                    if (isGetComment) {
                        val commentJSON = async {
                            nicoVideoHTML.getComment(userSession, jsonObject)
                        }
                        rawCommentList = withContext(Dispatchers.Default) {
                            ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.await()?.body?.string()!!, videoId))
                        }
                        // フィルターで3ds消したりする
                        commentFilter(true)
                    }
                    // 関連動画
                    launch { getRecommend(jsonObject) }
                    // ニコるくん
                    if (nicoVideoHTML.isPremium(jsonObject)) {
                        launch {
                            // threadId取得
                            val threadId = nicoVideoHTML.getThreadId(jsonObject)
                            if (threadId != null) {
                                nicoruAPI = NicoruAPI(
                                    userSession = userSession,
                                    threadId = threadId,
                                    isPremium = nicoVideoHTML.isPremium(jsonObject),
                                    userId = nicoVideoHTML.getUserId(jsonObject)
                                )
                                nicoruAPI?.init()
                            }
                        }
                    }
                }
            }

            // シリーズが設定されていればシリーズ情報を返す
            seriesDataLiveData.postValue(nicoVideoHTML.getSeriesData(jsonObject))
            // シリーズのJSON解析してデータクラスにする
            seriesHTMLDataLiveData.postValue(nicoVideoHTML.getSeriesHTMLData(jsonObject))
        }
    }

    /** 画質一覧をLiveDataへ入れる */
    private fun setQualityListDomand(parseVideoQualityDomand: JSONArray) {
        val qualityDataList = arrayListOf<QualityData>()
        repeat(parseVideoQualityDomand.length()) { index ->
            val qualityJSONObject = parseVideoQualityDomand.getJSONObject(index)
            val id = qualityJSONObject.getString("id")
            val isAvailable = qualityJSONObject.getBoolean("isAvailable")
            val label = qualityJSONObject.getString("label")
            val isSelected = id == currentVideoQuality
            qualityDataList.add(
                QualityData(
                    title = label,
                    id = id,
                    isSelected = isSelected,
                    isAvailable = isAvailable
                )
            )
        }
        // 送信
        qualityDataListLiveData.postValue(qualityDataList)
    }

    /** 画質一覧をLiveDataへ入れる */
    private fun setQualityListDMC(parseVideoQualityDMC: JSONArray) {
        val qualityDataList = arrayListOf<QualityData>()
        repeat(parseVideoQualityDMC.length()) { index ->
            val qualityJSONObject = parseVideoQualityDMC.getJSONObject(index)
            val id = qualityJSONObject.getString("id")
            val isAvailable = qualityJSONObject.getBoolean("isAvailable")
            val label = qualityJSONObject.getJSONObject("metadata").getString("label")
            val isSelected = id == currentVideoQuality
            qualityDataList.add(
                QualityData(
                    title = label,
                    id = id,
                    isSelected = isSelected,
                    isAvailable = isAvailable
                )
            )
        }
        // 送信
        qualityDataListLiveData.postValue(qualityDataList)
    }

    /**
     * 動画なしコメントのみを流すモードの初期化
     * ExoPlayerがいないので動画の時間を自分で進めるしか無い。
     * */
    private fun initNotPlayVideoMode() {
        viewModelScope.launch(notVideoPlayModeCoroutineContext) {
            // duration計算する
            val lastVpos = commentList.value?.maxOf { commentJSONParse -> commentJSONParse.vpos.toLong() }
            if (lastVpos != null) {
                playerDurationMs.postValue(lastVpos * 10) // 100vpos = 1s なので 10かけて 1000ms = 1s にする
            }
            while (isActive) {
                // プログレスバー進める
                delay(100)
                // 再生中 でなお 動画の時間がわかってるとき のみプログレスバーを進める
                if (playerIsPlaying.value == true && playerDurationMs.value != null) {
                    if (playerCurrentPositionMs < playerDurationMs.value!!) {
                        // 動画の長さのほうが大きい時は加算
                        playerCurrentPositionMs += 100
                    } else {
                        if (playerIsRepeatMode.value == true) {
                            // リピートモードなら0にして再生を続ける。
                            playerIsPlaying.value = true
                            playerCurrentPositionMs = 0L
                        } else {
                            // おしまい
                            playerIsPlaying.value = false
                        }
                    }
                }
                playerCurrentPositionMsLiveData.postValue(playerCurrentPositionMs)
            }
        }
    }


    /**
     * 関連動画取得
     * @param jsonObject 動画情報JSON
     * */
    private suspend fun getRecommend(jsonObject: JSONObject) = withContext(Dispatchers.Default) {
        // 動画情報
        val nicoVideoData = nicoVideoHTML.createNicoVideoData(jsonObject, false) // オンライン時のみ関連動画取得するので
        // 投稿者
        val userData = nicoVideoHTML.parseUserData(jsonObject)
        // 関連動画取得。
        val nicoVideoRecommendAPI = NicoVideoRecommendAPI()
        val recommendAPIResponse = nicoVideoRecommendAPI.getVideoRecommend(
            userSession = userSession,
            videoId = nicoVideoData.videoId,
            channelId = userData?.userId,
        )
        if (!recommendAPIResponse.isSuccessful) {
            // 失敗時
            showToast("${getString(R.string.error)}\n${recommendAPIResponse.code}")
            return@withContext
        }
        // パース
        withContext(Dispatchers.Default) {
            recommendList.postValue(nicoVideoRecommendAPI.parseVideoRecommend(recommendAPIResponse.body?.string()))
        }
    }

    /** コメントNGを適用したりする
     *  NicoVideoPlayService.kt:420~に半分コピペした、こっち変えたらあっちもいい感じにしてね */
    fun commentFilter(isShowToast: Boolean = false) {
        // 3DSけす？
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        // Switchを消す
        val isSwitchCommentHidden = prefSetting.getBoolean("nicovideo_comment_switch_hidden", false)
        // NGスコア依存のやつ
        val isNGScoreCommentHidden = prefSetting.getBoolean("setting_ng_by_ng_score", false)
        // NGスコアの閾値を拾う。stringなので例外対策はする
        val NGScoreLimit = try {
            prefSetting.getString("setting_ng_score_limit", "0")!!.toInt()
        } catch (i: NumberFormatException) { 0 }

        /**
         * かんたんコメントを消す。forkの値が2の場合はかんたんコメントになる。
         * どうでもいいんだけどあの機能、関係ないところでうぽつとかできるから控えめに言ってあらし機能だろあれ。てかROM専は何してもコメントしないぞ
         * */
        val isHideKantanComment = prefSetting.getBoolean("nicovideo_comment_kantan_comment_hidden", false)

        // NGコメント。ngList引数が省略時されてるときはDBから取り出す
        val ngCommentList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // NGユーザー。ngList引数が省略時されてるときはDBから取り出す
        val ngUserList = ngList.map { ngdbEntity -> ngdbEntity.value }
        // はい、NGでーす
        val filteredList = rawCommentList
            .filter { commentJSONParse -> if (is3DSCommentHidden) !commentJSONParse.mail.contains("device:3DS") else true }
            .filter { commentJSONParse -> if (isSwitchCommentHidden) !commentJSONParse.mail.contains("device:Switch") else true}
            .filter { commentJSONParse -> if (isHideKantanComment) commentJSONParse.fork != 2 else true } // fork == 2 が かんたんコメント
            .filter { commentJSONParse ->
                if (isNGScoreCommentHidden && commentJSONParse.score != "") !(commentJSONParse.score.toInt() <= NGScoreLimit)
                else true } // NGScoreLimitよりもscore値が小さいコメントだけを引き出す
            .filter { commentJSONParse -> !ngCommentList.contains(commentJSONParse.comment) }
            .filter { commentJSONParse -> !ngUserList.contains(commentJSONParse.userId) } as ArrayList<CommentJSONParse>
        commentList.postValue(filteredList)
        if (isShowToast) {
            showToast("${getString(R.string.get_comment_count)}：${filteredList.size}")
        }
    }

    /** 履歴データベースへ書き込む */
    private fun insertDB() {
        val videoId = playingVideoId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val unixTime = System.currentTimeMillis() / 1000
            // 入れるデータ
            val publisherId = nicoVideoHTML.getUploaderId(nicoVideoJSON.value!!)
            val nicoHistoryDBEntity = NicoHistoryDBEntity(
                type = "video",
                serviceId = videoId,
                userId = publisherId,
                title = nicoVideoJSON.value?.getJSONObject("video")?.getString("title") ?: videoId,
                unixTime = unixTime,
                description = ""
            )
            // 追加
            NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
        }
    }

    /** 連続再生時に次の動画に行く関数。連続再生じゃない場合は何も起きません。 */
    fun nextVideo() {
        if (playlistLiveData.value != null && isPlayListMode.value == true) {
            // 連続再生時のみ利用可能
            val currentPos = playlistLiveData.value!!.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == playingVideoId.value }
            val nextVideoPos = if (currentPos + 1 < playlistLiveData.value!!.size) {
                // 次の動画がある
                currentPos + 1
            } else {
                // 最初の動画にする
                0
            }
            val videoData = playlistLiveData.value!!.getOrNull(nextVideoPos) ?: return
            load(videoData.videoId, videoData.isCache, isEco, useInternet)
        }
    }

    /** 連続再生で前の動画に戻る関数。連続再生じゃない場合は何も起きません。 */
    fun prevVideo() {
        // 連続再生時のみ利用可能
        if (playlistLiveData.value != null && isPlayListMode.value == true) {
            val currentPos = playlistLiveData.value!!.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == playingVideoId.value }
            val prevVideoPos = if (currentPos - 1 >= 0) {
                // 次の動画がある
                currentPos - 1
            } else {
                // 最初の動画にする
                playlistLiveData.value!!.size - 1
            }
            val videoData = playlistLiveData.value!!.getOrNull(prevVideoPos) ?: return
            load(videoData.videoId, videoData.isCache, isEco, useInternet)
        }
    }

    /** 連続再生時に動画IDを指定して切り替える関数 */
    fun playlistGoto(videoId: String) {
        if (playlistLiveData.value != null && isPlayListMode.value == true) {
            // 動画情報を見つける
            val videoData = playlistLiveData.value!!.find { nicoVideoData -> nicoVideoData.videoId == videoId } ?: return
            load(videoData.videoId, videoData.isCache, isEco, useInternet)
        }
    }

    /**
     * いいねする関数
     * 結果はLiveDataへ送信されます
     * */
    fun postLike() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            val likeAPI = NicoLikeAPI()
            val likeResponse = withContext(Dispatchers.IO) {
                // いいね なのか いいね解除 なのか
                likeAPI.postLike(userSession, playingVideoId.value!!)
            }
            if (!likeResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${likeResponse.code}")
                return@launch
            }
            // いいね登録
            val thanksMessage = withContext(Dispatchers.Default) {
                // お礼メッセージパース
                likeAPI.parseLike(likeResponse.body?.string())
            }
            // 文字列 "null" の可能性
            val message = if (thanksMessage == "null") getString(R.string.like_ok) else thanksMessage
            likeThanksMessageLiveData.postValue(message)
            // 登録した
            isLikedLiveData.postValue(true)
        }
    }

    /**
     * いいねを解除する関数
     * 結果はLiveDataでわかります。
     * */
    fun removeLike() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            val likeAPI = NicoLikeAPI()
            val likeResponse = withContext(Dispatchers.IO) {
                // いいね なのか いいね解除 なのか
                likeAPI.deleteLike(userSession, playingVideoId.value!!)
            }
            if (!likeResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${likeResponse.code}")
                return@launch
            }
            // 解除した
            isLikedLiveData.postValue(false)
        }
    }

    /** あとでみるに追加する。マイリスト追加は[com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoAddMylistBottomFragment]を参照 */
    fun addAtodemiruList() {
        // あとで見るに追加する
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            // あとで見る追加APIを叩く
            val spMyListAPI = NicoVideoSPMyListAPI()
            val atodemiruResponse = spMyListAPI.addAtodemiruListVideo(userSession, playingVideoId.value!!)
            if (!atodemiruResponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${atodemiruResponse.code}")
                return@launch
            }
            // 成功したか
            when (atodemiruResponse.code) {
                201 -> {
                    // 成功時
                    showToast(getString(R.string.atodemiru_ok))
                }
                200 -> {
                    // すでに追加済み
                    showToast(getString(R.string.already_added))
                }
                else -> {
                    // えらー
                    showToast(getString(R.string.error))
                }
            }
        }
    }

    /**
     * 連続再生で順番を逆にするかどうか
     * */
    fun setPlaylistReverse() {
        if (playlistLiveData.value != null && isPlayListMode.value == true) {
            val videoList = playlistLiveData.value ?: return
            val videoListTemp = ArrayList(videoList)
            videoList.clear()
            videoList.addAll(videoListTemp.reversed())
            // LiveData送信
            playlistLiveData.postValue(videoList)
        }
    }

    /**
     * 連続再生でシャッフルを有効にするかどうか
     * @param isShuffle シャッフルを有効にするならtrue。そうじゃなければfalse
     * */
    fun setPlaylistShuffle(isShuffle: Boolean) {
        if (playlistLiveData.value != null && isPlayListMode.value == true) {
            val videoList = playlistLiveData.value ?: return
            if (isShuffle) {
                // シャッフル
                val videoListTemp = ArrayList(videoList)
                videoList.clear()
                videoList.addAll(videoListTemp.shuffled())
            } else {
                // シャッフル戻す。このために video_id_list が必要だったんですね
                val idList = originVideoSortList ?: return

                /** [List.sortedWith]と[Comparator]を使うことで、JavaScriptの` list.sort(function(a,b){ return a - b } `みたいな２つ比べてソートができる。 */
                val videoListTemp = ArrayList(videoList)
                videoList.clear()
                videoList.addAll(videoListTemp.sortedWith { a, b -> idList.indexOf(a.videoId) - idList.indexOf(b.videoId) }) // Kotlin 1.4で更に書きやすくなった
            }
            // LiveData送信
            playlistLiveData.postValue(videoList)
        }
    }

    /**
     * 連続再生を有効にする。ViewModel生成後でも連続再生に切り替えができます。
     *
     * @param nicoVideoDataList 連続再生リスト
     * */
    fun startPlaylist(nicoVideoDataList: ArrayList<NicoVideoData>) {
        // nullの場合は配列作成
        if (playlistLiveData.value == null) {
            playlistLiveData.value = arrayListOf()
        }
        playlistLiveData.value!!.apply {
            clear()
            addAll(nicoVideoDataList)
        }
        isPlayListMode.postValue(true)
        // ソート前の並び順を控える
        originVideoSortList.apply {
            clear()
            addAll(playlistLiveData.value!!.map { nicoVideoData -> nicoVideoData.videoId })
        }
        // 現在再生中の動画がどこの位置なのか
        val index = playlistLiveData.value!!.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == playingVideoId.value!! }
        if (index != -1) {
            playListCurrentPosition.postValue(index)
        }
    }

    /**
     * 指定したシリーズを連続再生に追加する
     * @param seriesId シリーズID
     * */
    fun addSeriesPlaylist(seriesId: String) {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // HTML取得
        viewModelScope.launch(errorHandler) {
            val seriesAPI = NicoVideoSeriesAPI()
            val videoList = withContext(Dispatchers.Default) {
                val response = seriesAPI.getSeriesVideoList(userSession, seriesId)
                if (response == null) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n")
                    return@withContext null
                }
                return@withContext seriesAPI.parseSeriesVideoList(response)
            } ?: return@launch
            // プレイリストに追加
            startPlaylist(videoList)
        }
    }

    /**
     * キャッシュの動画情報JSONを更新する
     * */
    fun requestUpdateCacheVideoInfoJSONFile() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(Dispatchers.Default + errorHandler) {
            val videoId = playingVideoId.value ?: return@launch
            // 動画HTML取得
            val response = nicoVideoHTML.getHTML(videoId, userSession)
            if (response.isSuccessful) {
                // 動画情報更新
                val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                val videoIdFolder = File("${nicoVideoCache.getCacheFolderPath()}/${videoId}")
                nicoVideoCache.saveVideoInfo(videoIdFolder, videoId, jsonObject.toString())
            } else {
                showToast("${context?.getString(R.string.error)}\n${response.code}")
            }
            // LiveData更新
            cacheVideoJSONUpdateLiveData.postValue(false)
            // 再読み込み
            withContext(Dispatchers.Main) {
                load(videoId, true, isEco, useInternet)
            }
        }
    }

    /**
     * SnackBar表示関数。予めFragmentでLiveDataをオブザーバーでつなげておいてね
     * */
    private fun showSnackBar(message: String?) {
        snackbarLiveData.postValue(message)
    }

    /** Toast表示関数 */
    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    /** ViewModel終了時 */
    override fun onCleared() {
        super.onCleared()
        nicoVideoHTML.destroy()
    }

}