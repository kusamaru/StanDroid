package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.community.CommunityAPI
import io.github.takusan23.tatimidroid.nicoapi.dataclass.QualityData
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.nicoapi.nicolive.*
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.*
import io.github.takusan23.tatimidroid.nicoapi.user.UserData
import io.github.takusan23.tatimidroid.room.entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.room.entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.KotehanDBInit
import io.github.takusan23.tatimidroid.room.init.NGDBInit
import io.github.takusan23.tatimidroid.room.init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.tool.isConnectionMobileDataInternet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.internal.toLongOrDefault
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * [io.github.takusan23.tatimidroid.nicolive.CommentFragment]のViewModel
 *
 * いまだに何をおいておけば良いのかわからん
 *
 * @param liveIdOrCommunityId 番組IDかコミュIDかチャンネルID。どれが来るか知らんから、番組IDが欲しい場合は[NicoLiveHTML.liveId]を使ってね
 * @param isJK 実況の時はtrue
 * @param isLoginMode HTML取得時にログインする場合はtrue
 * */
class NicoLiveViewModel(application: Application, val liveIdOrCommunityId: String, val isLoginMode: Boolean) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** HTML取得からWebSocket接続など */
    val nicoLiveHTML = NicoLiveHTML()

    /** ニコ生のコメントサーバーへ接続する */
    val nicoLiveComment = NicoLiveComment()

    /** Snackbar表示用LiveData。複数行行ける */
    val snackbarLiveData = MutableLiveData<String>()

    /** ニコニコ実況だったら呼ばれるLiveData */
    val isNicoJKLiveData = MutableLiveData<String>()

    /** Fragment(Activity)へメッセージを送信するためのLiveData。Activity終了など。*/
    val messageLiveData = MutableLiveData<String>()

    /** ニコ生のHTML内にあるJSONを入れる */
    val nicoLiveJSON = MutableLiveData<JSONObject>()

    /** 番組情報 */
    val nicoLiveProgramData = MutableLiveData<NicoLiveProgramData>()

    /** 番組の説明送信LiveData */
    val nicoLiveProgramDescriptionLiveData = MutableLiveData<String>()

    /** 生主の情報送信LiveData */
    val nicoLiveUserDataLiveData = MutableLiveData<UserData>()

    /** コミュ情報送信LiveData */
    val nicoLiveCommunityOrChannelDataLiveData = MutableLiveData<CommunityOrChannelData>()

    /** コミュフォロー中かどうかLiveData */
    val isCommunityOrChannelFollowLiveData = MutableLiveData<Boolean>()

    /** タグ配列を送信するLiveData */
    val nicoLiveTagDataListLiveData = MutableLiveData<NicoLiveTagData>()

    /** 好みタグの文字列配列LiveData */
    val nicoLiveKonomiTagListLiveData = MutableLiveData<List<NicoLiveKonomiTagData>>()

    /** コメントを送るLiveData。ただ配列に入れる処理はこっちが担当するので、コメントが来た時に処理したい場合はどうぞ（RecyclerView更新など） */
    val commentReceiveLiveData = MutableLiveData<CommentJSONParse>()

    /** RecyclerViewを更新するLiveData？ */
    val updateRecyclerViewLiveData = MutableLiveData<String>()

    /** コメント配列 */
    val commentList = arrayListOf<CommentJSONParse>()

    /** 運営コメントを渡すLiveData。 */
    val unneiCommentLiveData = MutableLiveData<String>()

    /** アンケートがあったら表示するLiveData。（Jetpack Composeでは使ってない） */
    val enquateLiveData = MutableLiveData<String>()

    /** アンケートが開始されたら呼ばれるLiveData */
    val startEnquateLiveData = MutableLiveData<List<String>>()

    /** アンケートの開票LiveData */
    val openEnquateLiveData = MutableLiveData<List<String>>()

    /** アンケート終了LiveData */
    val stopEnquateLiveData = MutableLiveData<String>()

    /** 来場者、コメント数等の来場者数を送るLiveData */
    val statisticsLiveData = MutableLiveData<StatisticsDataClass>()

    /** 一分間にコメントした人数（ユニークユーザー数ってやつ。同じIDは１として数える）。 */
    val activeCommentPostUserLiveData = MutableLiveData<String>()

    /** 経過時間をLiveDataで送る。変換はTimeFormatTool.ktを参照 */
    val programCurrentPositionSecLiveData = MutableLiveData<Long>()

    /** 番組の期間。変換はTimeFormatTool.ktを参照*/
    val programDurationTimeLiveData = MutableLiveData<Long>()

    /** 画質が切り替わったら飛ばすLiveData。多分JSON配列 */
    val changeQualityLiveData = MutableLiveData<String>()

    /** タイムシフト予約済みかどうか。なんですけど、API叩くまでは fasle です */
    val isTimeShiftRegisteredLiveData = MutableLiveData(false)

    /** [changeQualityLiveData]で二回目から使うので制御用 */
    private var isNotFirstQualityMessage = false

    /** 番組名 */
    var programTitle = ""

    /** コミュID */
    var communityId = ""

    /** サムネURL */
    var thumbnailURL = ""

    /** 現在の画質 */
    var currentQuality = ""

    /** 画質データクラス */
    var qualityDataListLiveData = MutableLiveData<List<QualityData>>()

    /** HLSアドレス */
    val hlsAddressLiveData = MutableLiveData<String>()

    /** 番組終了時刻。こっちはUnixTime。UI（Fragment）で使うこと無いしLiveDataじゃなくていっか！ */
    var programEndUnixTime = 0L

    /** 延長検知。視聴セッション接続後すぐに送られてくるので一回目はパス */
    private var isNotFirstEntyouKenti = false

    /** 運営コメントを消すときはtrue */
    var isHideInfoUnnkome = MutableLiveData(false)

    /** 匿名コメントを表示しない場合はtrue */
    var isHideTokumei = MutableLiveData(false)

    /** エモーションを表示しない場合はtrue */
    val isHideEmotion = MutableLiveData(prefSetting.getBoolean("setting_nicolive_hide_emotion", false))

    /** NGコメント配列。Room+Flowで監視する */
    var ngCommentList = listOf<String>()

    /** NGのID配列。Room+Flowで監視する */
    var ngIdList = listOf<String>()

    /** 全画面再生時はtrue */
    var isFullScreenMode = false

    /** ミニプレイヤーかどうか。 */
    val isMiniPlayerMode = MutableLiveData(false)

    /**
     * コメントのみを表示させ、生放送を見ない。見ない場合はtrue
     * 設定項目、「setting_watch_live」の値を反転している
     * */
    var isCommentOnlyMode = !prefSetting.getBoolean("setting_watch_live", true)

    /**
     * 映像を取得しないモードならtrue。[isCommentOnlyMode]との違いはコメントは描画し続けるというところ。ニコニコチャンネルになった実況用
     * ななはらでも3分で2MBぐらい？
     * */
    var isNotReceiveLive = MutableLiveData(false)

    /** コメント一覧表示してくれ～LiveData */
    val commentListShowLiveData = MutableLiveData(false)

    /** コメント一覧を自動で展開しない設定かどうか */
    val isAutoCommentListShowOff = prefSetting.getBoolean("setting_nicovideo_jc_comment_auto_show_off", true)

    /** ExoPlayerの音量を持っておくLiveData */
    val exoplayerVolumeLiveData = MutableLiveData(1f)

    /** ニコ生WebViewを表示中か */
    val isUseNicoNamaWebView = MutableLiveData(false)

    /** TS予約が可能かどうか */
    val isAllowTSRegister = MutableLiveData(true)

    /** タイムシフト再生中？ */
    val isWatchingTimeShiftLiveData = MutableLiveData(false)

    /** タイムシフト用。生放送を再生しているか。 */
    var isTimeShiftPlaying = MutableLiveData(true)

    /** TS再生中のみ利用するクラス */
    private var nicoLiveTimeShiftComment = NicoLiveTimeShiftComment()

    init {
        // 匿名でコメントを投稿する場合
        nicoLiveHTML.isPostTokumeiComment = prefSetting.getBoolean("nicolive_post_tokumei", true)
        // エラーのとき（タイムアウトなど）はここでToastを出すなど
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }

        // 低遅延設定
        nicoLiveHTML.isLowLatency = prefSetting.getBoolean("nicolive_low_latency", false)
        // 初回の画質を低画質にする設定（モバイル回線で最低画質にする設定とか強制低画質モードとか）
        val mobileDataQualitySetting = prefSetting.getString("setting_nicolive_mobile_data_quality", "default")
        val isMobileDataLowQuality = (mobileDataQualitySetting == "super_low_quality") && isConnectionMobileDataInternet(context) // 有効時 でなお モバイルデータ接続時
        val isPreferenceLowQuality = prefSetting.getBoolean("setting_nicolive_quality_low", false)
        // モバイルデータ通信時に音声のみで再生する設定
        val isMobileDataAudioOnly = mobileDataQualitySetting == "audio_only" && isConnectionMobileDataInternet(context)
        if ((isMobileDataLowQuality || isPreferenceLowQuality) && !isMobileDataAudioOnly) {
            nicoLiveHTML.startQuality = "super_low"
        } else if (isMobileDataAudioOnly) {
            // モバイルデータ通信時に音声のみで再生する場合
            nicoLiveHTML.startQuality = "audio_high"
        }
        // ニコ生
        viewModelScope.launch(errorHandler + Dispatchers.Default) {
            // 情報取得。UIスレッドではないのでLiveDataはpostValue()を使おう
            val html = getNicoLiveHTML()
            val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(html)
            nicoLiveJSON.postValue(jsonObject)
            // 番組名取得など
            nicoLiveHTML.initNicoLiveData(jsonObject)
            programTitle = nicoLiveHTML.programTitle
            communityId = nicoLiveHTML.communityId
            thumbnailURL = nicoLiveHTML.thumb
            val programData = nicoLiveHTML.getProgramData(jsonObject)
            nicoLiveProgramData.postValue(programData)
            nicoLiveProgramDescriptionLiveData.postValue(nicoLiveHTML.getProgramDescription(jsonObject))
            nicoLiveUserDataLiveData.postValue(nicoLiveHTML.getUserData(jsonObject))
            val tagList = nicoLiveHTML.getTagList(jsonObject)
            // 6M (フルHD)が利用可能な場合はToastを出す
            checkFullHDQuality(tagList)
            nicoLiveTagDataListLiveData.postValue(tagList)
            nicoLiveKonomiTagListLiveData.postValue(nicoLiveHTML.getKonomiTagList(jsonObject))
            nicoLiveHTML.getCommunityOrChannelData(jsonObject).apply {
                nicoLiveCommunityOrChannelDataLiveData.postValue(this)
                isCommunityOrChannelFollowLiveData.postValue(isFollow)
            }
            // TS視聴中かどうか
            val isEnded = nicoLiveHTML.getProgramStatus(jsonObject) == "ENDED"
            val isWatchingTS = nicoLiveHTML.isPremium(jsonObject) && isEnded
            isWatchingTimeShiftLiveData.postValue(isWatchingTS)
            if (isWatchingTS) {
                // TS再生用意
                initTSWatching(jsonObject)
                // 経過時間計算
                setTSLiveTime()
                // WebSocketへ接続
                connectWebSocket(jsonObject, true)
            } else {
                // 通常の生配信
                // 経過時間
                setLiveTime()
                // WebSocketへ接続
                connectWebSocket(jsonObject)
            }
            // コメント人数を定期的に数える
            activeUserClear()
            // 履歴に追加
            launch { insertDB() }
            // getPlayerStatus叩く
            // launch { getPlayerStatus() }
            // TS予約が許可されているか
            checkIsAllowTSRegister(jsonObject)

            /**
             * すでにニコニコ実況チャンネルが存在する：https://ch.nicovideo.jp/jk1
             * ので文字列部分一致してたら生放送の映像受信を止めるかどうか尋ねる
             * */
            checkNicoJK(jsonObject)
        }

        // NGデータベースを監視する
        viewModelScope.launch {
            val dao = NGDBInit.getInstance(context).ngDBDAO()
            dao.flowGetNGAll().collect { ngList ->
                // NGユーザー追加/削除を検知
                ngCommentList = ngList.filter { ngdbEntity -> ngdbEntity.type == "comment" }.map { ngdbEntity -> ngdbEntity.value }
                ngIdList = ngList.filter { ngdbEntity -> ngdbEntity.type == "user" }.map { ngdbEntity -> ngdbEntity.value }
                // 取得済みコメントからも排除
                commentList.toList().forEach { commentJSONParse ->
                    if (ngCommentList.contains(commentJSONParse.comment) || ngIdList.contains(commentJSONParse.userId)) {
                        commentList.remove(commentJSONParse)
                    }
                }
                // 一覧更新。ただUIに更新しろって送りたいだけなので適当送る
                updateRecyclerViewLiveData.postValue("update")
            }
        }
    }

    /** フルHD対応番組の場合はToastを出す */
    private fun checkFullHDQuality(tagData: NicoLiveTagData) {
        tagData.tagList.forEach { nicoTagItemData ->
            if (nicoTagItemData.tagName == "フルHD配信") {
                showToast(getString(R.string.nicolive_support_full_hd))
            }
        }
    }

    /**
     * TS再生準備
     *
     * @param jsonObject [NicoLiveHTML.nicoLiveHTMLtoJSONObject]
     * */
    private fun initTSWatching(jsonObject: JSONObject) {
        if (nicoLiveHTML.isPremium(jsonObject)) {
            // TS視聴中メッセージ
            showToast(getString(R.string.nicolive_timeshift_mode))
            // 番組終了時間が公式だとWebSocketで流れてこない
            val programData = nicoLiveHTML.getProgramData(jsonObject)
            val programDuration = programData.endAt.toLong() - programData.beginAt.toLong()
            programDurationTimeLiveData.postValue(programDuration)
        } else {
            // プレ垢になってね
            showToast(getString(R.string.nicolive_timeshift_premium))
            // Activity終了
            messageLiveData.postValue("finish")
        }
    }

    /**
     * TS予約が可能かどうか。結果はLiveDataへ
     * @param jsonObject [NicoLiveHTML.nicoLiveHTMLtoJSONObject]
     * */
    private fun checkIsAllowTSRegister(jsonObject: JSONObject) {
        /**
         * タイムシフト機能が使えない場合
         * JSONに programTimeshift と programTimeshiftWatch が存在しない場合はTS予約が無効にされている？
         * 存在すればTS予約が利用できる
         * */
        val canTSReserve = jsonObject.has("programTimeshift") && jsonObject.has("programTimeshiftWatch")
        isAllowTSRegister.postValue(canTSReserve)
    }

    /**
     * 視聴中の番組が新ニコニコ実況かどう判断する
     * */
    private fun checkNicoJK(jsonObject: JSONObject) {
        // ニコニコ実況タグがついてれば実況判定
        if (nicoLiveHTML.getTagList(jsonObject).tagList.any { nicoTagItemData -> nicoTagItemData.tagName == "ニコニコ実況" }) {
            // LiveDataにjk1とかを送信。有志ならコミュID
            isNicoJKLiveData.postValue(nicoLiveHTML.getNicoJKIdFromChannelId(nicoLiveHTML.communityId) ?: nicoLiveHTML.communityId)
        }
    }

    /**
     * コメント投稿関数。実況でも使う
     * コメント送信処理は[NicoLiveHTML.sendPOSTWebSocketComment]でやってる
     * @param comment コメント内容。「お大事に」とか
     * @param isUseNicocasAPI コメント投稿にニコキャスのAPIを使う場合はtrue
     * @param size コメントの大きさ。これらは省略が可能
     * @param position コメントの位置。これらは省略が可能
     * @param color コメントの色。これらは省略が可能
     * */
    fun sendComment(comment: String, color: String = "white", size: String = "medium", position: String = "naka") {
        if (comment != "\n") {
            // 視聴セッションWebSocketにコメントを送信する
            nicoLiveHTML.sendPOSTWebSocketComment(comment, color, size, position)
        }
    }

    /**
     * アンケを押す
     * @param pos アンケの位置。多分一番目が0（配列みたいに）
     * ■■■■■■■■■■■
     * ■ 1 /             ■
     * ■ /               ■
     * ■  とても良かった  ■
     * ■                 ■
     * ■■ [ 98.6 ％ ] ■■   宇宙よりも遠い場所 2020/08/09 一挙アンケ
     * */
    fun enquatePOST(pos: Int) {
        val jsonObject = JSONObject().apply {
            put("type", "answerEnquete")
            put("data", JSONObject().apply {
                put("answer", pos)
            })
        }
        nicoLiveHTML.nicoLiveWebSocketClient.send(jsonObject.toString())
    }

    /** 経過時間計算 */
    private fun setLiveTime() {
        // 1秒ごとに
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                // 現在の時間
                val currentTimeSec = System.currentTimeMillis() / 1000L
                val programData = nicoLiveProgramData.value?.beginAt?.toLong() ?: 0
                programCurrentPositionSecLiveData.postValue(currentTimeSec - programData)
            }
        }
    }

    /** 経過時間計算。こっちはTS用 */
    private fun setTSLiveTime() {
        // 1秒ごとに
        viewModelScope.launch(Dispatchers.Main) {
            while (isActive) {
                var tsCurrentPos = programCurrentPositionSecLiveData.value ?: 0
                // 再生中のみ時間を足す
                if (isTimeShiftPlaying.value == true) {
                    // 足す
                    tsCurrentPos += 1
                    // 現在の時間
                    programCurrentPositionSecLiveData.value = tsCurrentPos
                }
                // タイムシフトコメント再現
                nicoLiveTimeShiftComment.apply {
                    currentPositionSec = tsCurrentPos
                    isPlaying = isTimeShiftPlaying.value ?: true
                }
                delay(1000)
            }
        }
    }

    /**
     * タイムシフト再生時のみ。シークをする関数
     * @param position シーク位置。現実世界の時間で
     * */
    fun tsSeekPosition(position: Long) {
        // まず再生時間を更新
        programCurrentPositionSecLiveData.value = position
        // HLSアドレスを加工してLiveData送信
        val hlsAddress = hlsAddressLiveData.value
        if (hlsAddress != null) {
            // startのパラメーターに再生時間を入れる。
            val httpUrl = hlsAddress.toHttpUrl().newBuilder().setQueryParameter("start", "$position").build().toString()
            hlsAddressLiveData.postValue(httpUrl)
            // コメント鯖再接続
            nicoLiveTimeShiftComment.seek(position)
        }
    }

    /** アクティブ人数を計算する。一分間間隔 */
    private fun activeUserClear() {
        // 1分でリセット
        viewModelScope.launch {
            // とりあえず一回目は10秒後計算
            delay(10000)
            calcToukei()
            while (isActive) {
                // あとは一分間間隔で計算する
                delay(60000)
                calcToukei()
            }
        }
    }

    /**
     * 統計情報を表示する。立ち見部屋の数が出なくなったの少し残念。人気番組の指数だったのに
     * @param showSnackBar SnackBarを表示する場合はtrue
     * */
    fun calcToukei(showSnackBar: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (commentList.isNotEmpty()) {
                // 一番新しいコメントを取得。
                val lastCommentDate = commentList.first().date.toLongOrNull() ?: 0
                // そこから一分前の時間を計算
                val prevTime = lastCommentDate - 60
                // 範囲内のコメントを取得する
                val timeList = commentList.toList().filter { comment ->
                    if (comment != null && comment.date.toFloatOrNull() != null) {
                        comment.date.toLong() in prevTime..lastCommentDate
                    } else {
                        false
                    }
                }
                // 同じIDを取り除く
                val idList = timeList.distinctBy { comment -> comment.userId }
                // 数えた結果
                activeCommentPostUserLiveData.postValue("${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}")
                // SnackBarで統計を表示する場合
                if (showSnackBar) {
                    // プレ垢人数
                    val premiumCount = idList.count { commentJSONParse -> commentJSONParse.premium == "\uD83C\uDD7F" }
                    // 生ID人数
                    val userIdCount = idList.count { commentJSONParse -> !commentJSONParse.mail.contains("184") }
                    // 平均コメント数
                    val commentLengthAverageDouble = timeList.map { commentJSONParse -> commentJSONParse.comment.length }.average()
                    val commentLengthAverage = if (!commentLengthAverageDouble.isNaN()) {
                        commentLengthAverageDouble.roundToInt()
                    } else {
                        -1
                    }
                    // 秒間コメントを取得する。なお最大値
                    val commentPerSecondMap = timeList.groupBy({ comment ->
                        // 一分間のコメント配列から秒、コメント配列のMapに変換するためのコード
                        // 例。51秒に投稿されたコメントは以下のように：51=[いいよ, がっつコラボ, ガッツ, 歓迎]
                        val programStartTime = nicoLiveHTML.programStartTime
                        comment.date.toLong() - programStartTime
                    }, { comment ->
                        comment
                    }).maxByOrNull { map ->
                        // 秒Mapから一番多いのを取る。
                        map.value.size
                    }
                    // 数えた結果
                    activeCommentPostUserLiveData.postValue("${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}")
                    // 統計情報表示
                    snackbarLiveData.postValue(
                        """${getString(R.string.one_minute_statistics)}
${getString(R.string.comment_per_second)}(${getString(R.string.max_value)}/${calcLiveTime(commentPerSecondMap?.key ?: 0)})：${commentPerSecondMap?.value?.size}
${getString(R.string.one_minute_statistics_premium)}：$premiumCount
${getString(R.string.one_minute_statistics_user_id)}：$userIdCount
${getString(R.string.one_minute_statistics_comment_length)}：$commentLengthAverage"""
                    )
                }
            }
        }
    }

    /**
     * 相対時間を計算する。25:25みたいなこと。
     * @param position 時間。秒で（番組開始からの時間）
     * */
    private fun calcLiveTime(position: Long): String {
        // 経過時間 - 番組開始時間
        val date = Date(position * 1000L)
        //時間はUNIX時間から計算する
        val hour = (position / 60 / 60)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "$hour:${simpleDateFormat.format(date.time)}"
    }

    /**
     * WebSocketへ接続する関数
     * @param jsonObject [NicoLiveHTML.nicoLiveHTMLtoJSONObject]のJSONObject
     * @param isTSWatching TS再生時はtrue
     * */
    private fun connectWebSocket(jsonObject: JSONObject, isTSWatching: Boolean = false) {
        val programData = nicoLiveHTML.getProgramData(jsonObject)
        val startTime = programData.beginAt.toLong()

        nicoLiveHTML.connectWebSocket(jsonObject) { command, message ->
            // WebSocketへ接続してHLSアドレス、コメント鯖の情報をもらう
            when (command) {
                "stream" -> {
                    // HLSアドレス取得
                    hlsAddressLiveData.postValue(nicoLiveHTML.getHlsAddress(message))
                    // 画質一覧と今の画質
                    currentQuality = nicoLiveHTML.getCurrentQuality(message)
                    // 選択可能な画質
                    setQualityList(nicoLiveHTML.getQualityListJSONArray(message))
                    // 二回目以降画質変更を通知する
                    if (isNotFirstQualityMessage) {
                        changeQualityLiveData.postValue(nicoLiveHTML.getCurrentQuality(message))
                    }
                    isNotFirstQualityMessage = true
                }
                "room" -> {
                    // コメントサーバーの情報
                    val commentMessageServerUri = nicoLiveHTML.getCommentServerWebSocketAddress(message)
                    val commentThreadId = nicoLiveHTML.getCommentServerThreadId(message)
                    val yourPostKey = if (isLoginMode && !isTSWatching) {
                        // ログイン時のみyourPostKeyが取れる。タイムシフト時は使わない？
                        nicoLiveHTML.getCommentYourPostKey(message)
                    } else {
                        // タイムシフト時はnullでいいっぽい
                        null
                    }
                    val commentRoomName = getString(R.string.room_integration) // ユーザーならコミュIDだけどもう立ちみないので部屋統合で統一
                    // コメントサーバーへ接続する
                    val commentServerData = CommentServerData(commentMessageServerUri, commentThreadId, commentRoomName, yourPostKey, nicoLiveHTML.userId)
                    if (isTSWatching) {
                        nicoLiveTimeShiftComment.connectCommentServerTimeShift(commentServerData, startTime, ::receiveCommentFun)
                    } else {
                        nicoLiveComment.connectCommentServerWebSocket(commentServerData = commentServerData, requestHistoryCommentCount = -100, onMessageFunc = ::receiveCommentFun)
                    }
                    // 流量制限コメント鯖へ接続する
                    if (!nicoLiveHTML.isOfficial && !isTSWatching) {
                        viewModelScope.launch(Dispatchers.Default) {
                            connectionStoreCommentServer(nicoLiveHTML.userId, yourPostKey)
                        }
                    }
                }
                "postCommentResult" -> {
                    // コメント送信結果。
                    showCommentPOSTResultSnackBar(message)
                }
                "statistics" -> {
                    // 総来場者数、コメント数を表示させる
                    initStatisticsInfo(message)
                }
                "schedule" -> {
                    // 延長を検知
                    showSchedule(message)
                }
            }
            // containsで部分一致にしてみた。なんで部分一致なのかは私も知らん
            if (command.contains("disconnect")) {
                //番組終了
                programEnd(message)
            }
        }
    }

    /** 画質のデータクラスを作成する */
    private fun setQualityList(qualityListJSONArray: JSONArray) {
        val qualityList = arrayListOf<QualityData>()
        repeat(qualityListJSONArray.length()) { index ->
            val text = qualityListJSONArray.getString(index)
            qualityList.add(
                QualityData(
                    title = getQualityText(text),
                    id = text,
                    isAvailable = true,
                    isSelected = text == currentQuality,
                )
            )
        }
        qualityDataListLiveData.postValue(qualityList)
    }

    /** 画質名を変換する */
    private fun getQualityText(text: String): String {
        return when (text) {
            "abr" -> getString(R.string.quality_auto)
            "super_high" -> "3Mbps"
            "high" -> "2Mbps"
            "normal" -> "1Mbps"
            "low" -> "384kbps"
            "super_low" -> "192kbps"
            "audio_high" -> getString(R.string.quality_audio)
            "6Mbps1080p30fps" -> "6Mbps 1080p 30fps"
            else -> text
        }
    }

    /** 番組終了。なお一般追い出しの場合はWebSocketが切断される */
    private fun programEnd(message: String) {
        // 理由？
        val because = JSONObject(message).getJSONObject("data").getString("reason")
        // 原因が追い出しの場合はToast出す
        if (because == "CROWDED") {
            showToast("${getString(R.string.oidashi)}\uD83C\uDD7F") // パーキングの絵文字
        }
        // Activity終了
        messageLiveData.postValue("finish")
    }

    /** 延長メッセージを受け取る */
    private fun showSchedule(message: String) {
        val scheduleData = nicoLiveHTML.getSchedule(message)
        //時間出す場所確保したので終了時刻書く。
        if (isNotFirstEntyouKenti) {
            // 終了時刻出す
            val time = nicoLiveHTML.getScheduleEndTime(message)
            val message = "${getString(R.string.entyou_message)}\n${getString(R.string.end_time)} $time"
            snackbarLiveData.postValue(message)
        } else {
            isNotFirstEntyouKenti = true
        }
        // 延長したら残り時間再計算する
        val calc = (scheduleData.endTime - scheduleData.beginTime) / 1000
        programDurationTimeLiveData.postValue(calc)
        // 番組終了時刻を入れる
        programEndUnixTime = scheduleData.endTime / 1000
    }

    /** 来場者数、コメント数などの統計情報を受け取る */
    private fun initStatisticsInfo(message: String) {
        statisticsLiveData.postValue(nicoLiveHTML.getStatistics(message))
    }

    /** コメントが送信できたか */
    private fun showCommentPOSTResultSnackBar(message: String) {
        viewModelScope.launch {
            val jsonObject = JSONObject(message)
            /**
             * 本当に送信できたかどうか。
             * 実は流量制限にかかってしまったのではないか（公式番組以外では流量制限コメント鯖（store鯖）に接続できるけど公式は無理）
             * 流量制限にかかると他のユーザーには見えない。ので本当に成功したか確かめる
             * */
            if (nicoLiveProgramData.value?.isOfficial == true) {
                val comment = jsonObject.getJSONObject("data").getJSONObject("chat").getString("content")
                delay(500)
                // 受信済みコメント配列から自分が投稿したコメント(yourpostが1)でかつ5秒前まで遡った配列を作る
                val nowTime = System.currentTimeMillis() / 1000
                val prevComment = commentList.filter { commentJSONParse ->
                    val time = nowTime - (commentJSONParse.date.toLongOrDefault(0))
                    time <= 5 && commentJSONParse.yourPost // 5秒前でなお自分が投稿したものを
                }.map { commentJSONParse -> commentJSONParse.comment }
                if (prevComment.contains(comment)) {
                    // コメント一覧に自分のコメントが有る
                    snackbarLiveData.postValue(getString(R.string.comment_post_success))
                } else {
                    // 無いので流量制限にかかった（他には見えない）
                    snackbarLiveData.postValue("${getString(R.string.comment_post_error)}\n${getString(R.string.comment_post_limit)}")
                }
            } else {
                // ユーザー番組ではコメント多いときもStore鯖に入るので検証はしない
                snackbarLiveData.postValue(getString(R.string.comment_post_success))
            }
        }
    }

    /** コメントを受け取る高階関数 */
    private fun receiveCommentFun(comment: String, roomName: String, isHistoryComment: Boolean) {
        // JSONぱーす
        val commentJSONParse = CommentJSONParse(comment, roomName, nicoLiveHTML.liveId)
        // どっちの部屋のコメントかどうか。trueで部屋統合
        val isArenaComment = roomName != getString(R.string.room_limit)

        // アンケートや運コメを表示させる。アリーナで生主コメのとき
        if (isArenaComment && commentJSONParse.premium == "生主") {
            if (commentJSONParse.comment.contains("/vote")) {
                // アンケート
                enquateLiveData.postValue(comment)
            }
            when {
                commentJSONParse.comment.contains("/vote start") -> {
                    // アンケート開始。/vote startの最後に空白入れるの忘れるなよ
                    startEnquateLiveData.postValue(commentJSONParse.comment.replace("/vote start ", "").split(" "))
                }
                commentJSONParse.comment.contains("/vote showresult per") -> {
                    // アンケート開票。％に変換する
                    openEnquateLiveData.postValue(
                        commentJSONParse.comment.replace("/vote showresult per ", "")
                            .split(" ")
                            // 176 を 17.6% って表記するためのコード。１桁増やして（9%以下とき対応できないため）２桁消す
                            .map { per -> "${(per.toFloat() * 10) / 100}%" }
                    )
                }
                commentJSONParse.comment.contains("/vote stop") -> {
                    // アンケート終了
                    stopEnquateLiveData.postValue("/vote stop")
                }
                commentJSONParse.comment.contains("/disconnect") -> {
                    // disconnect受け取ったらSnackBar表示
                    snackbarLiveData.postValue(getString(R.string.program_disconnect))
                }
            }
            if (isHideInfoUnnkome.value == false) {
                // 運営コメント非表示時
                if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
                    unneiCommentLiveData.postValue(comment)
                }
            }
        }
        // 匿名コメント落とすモード
        if (isHideTokumei.value == true && commentJSONParse.mail.contains("184")) {
            return
        }
        // NGユーザー/コメントの場合は配列に追加しない
        when {
            ngIdList.contains(commentJSONParse.userId) -> return
            ngCommentList.contains(commentJSONParse.comment) -> return
        }
        // LiveData送信！！！
        commentReceiveLiveData.postValue(commentJSONParse)
        // コメント配列に追加
        commentList.add(0, commentJSONParse)
        // コテハン登録
        registerKotehan(commentJSONParse)
    }

    /**
     * コテハンがコメントに含まれている場合はコテハンDBに追加する関数
     * コテハンmap反映もしている。
     * */
    private fun registerKotehan(commentJSONParse: CommentJSONParse) {
        val comment = commentJSONParse.comment
        if (comment.contains("@") || comment.contains("＠")) {
            // @の位置を特定
            val index = when {
                comment.contains("@") -> comment.indexOf("@") + 1 // @を含めないように
                comment.contains("＠") -> comment.indexOf("＠") + 1 // @を含めないように
                else -> -1
            }
            if (index != -1) {
                val kotehan = comment.substring(index)
                // データベースにも入れる。コテハンデータベースの変更は自動でやってくれる
                viewModelScope.launch(Dispatchers.IO) {
                    val dao = KotehanDBInit.getInstance(context).kotehanDBDAO()
                    // すでに存在する場合・・・？
                    val kotehanData = dao.findKotehanByUserId(commentJSONParse.userId)
                    if (kotehanData != null) {
                        // 存在した
                        val kotehanDBEntity = kotehanData.copy(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        dao.update(kotehanDBEntity)
                    } else {
                        // 存在してない
                        val kotehanDBEntity = KotehanDBEntity(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000), userId = commentJSONParse.userId)
                        dao.insert(kotehanDBEntity)
                    }
                }
            }
        }
    }

    /**
     * 流量制限コメントサーバーに接続する関数。コルーチンで
     * 流量制限コメントサーバーってのはコメントが多すぎてコメントが溢れてしまう際、溢れてしまったコメントが流れてくるサーバーのことだと思います。
     * ただまぁ超がつくほどの大手じゃないとここのWebSocketに接続しても特に流れてこないと思う。
     * 公式番組では利用できない。
     * @param userId ユーザーIDが取れれば入れてね。無くてもなんか動く
     * @param yourPostKey 視聴セッションから流れてくる。けど無くても動く（yourpostが無くなるけど）
     * */
    private suspend fun connectionStoreCommentServer(userId: String? = null, yourPostKey: String? = null) = withContext(Dispatchers.Default) {
        // コメントサーバー取得API叩く
        val allRoomResponse = nicoLiveComment.getProgramInfo(nicoLiveHTML.liveId, userSession)
        if (!allRoomResponse.isSuccessful) {
            showToast("${getString(R.string.error)}\n${allRoomResponse.code}")
            return@withContext
        }
        // Store鯖へつなぐ
        val storeCommentServerData = nicoLiveComment.parseStoreRoomServerData(allRoomResponse.body?.string(), getString(R.string.room_limit))
        if (storeCommentServerData != null) {
            // Store鯖へ接続する。（超）大手でなければ別に接続する必要はない
            nicoLiveComment.connectCommentServerWebSocket(commentServerData = storeCommentServerData, onMessageFunc = ::receiveCommentFun)
        }
    }

    /** 履歴DBに入れる */
    private suspend fun insertDB() = withContext(Dispatchers.IO) {
        val unixTime = System.currentTimeMillis() / 1000
        // 入れるデータ
        val nicoHistoryDBEntity = NicoHistoryDBEntity(
            type = "live",
            serviceId = nicoLiveHTML.liveId,
            userId = communityId,
            title = programTitle,
            unixTime = unixTime,
            description = ""
        )
        // 追加
        NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
    }

    /** ニコ生放送ページのHTML取得。コルーチンです */
    private suspend fun getNicoLiveHTML(): String? = withContext(Dispatchers.Default) {
        // ニコ生視聴ページリクエスト
        val livePageResponse = nicoLiveHTML.getNicoLiveHTML(liveIdOrCommunityId, userSession, isLoginMode)
        if (!livePageResponse.isSuccessful) {
            // 失敗のときは落とす
            messageLiveData.postValue("finish")
            showToast("${getString(R.string.error)}\n${livePageResponse.code}")
            null
        }
        // ログインモードで かつ ニコニコにログインできない場合は再ログインさせる
        if (!nicoLiveHTML.hasNiconicoID(livePageResponse) && isLoginMode) {
            // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
            val tmp = NicoLogin.secureNicoLogin(context)
            if (tmp != null) {
                userSession = tmp
            } else {
                // ログイン失敗（二段階認証とか普通に失敗したとか）
                messageLiveData.postValue("finish")
            }
            // 視聴モードなら再度視聴ページリクエスト
            if (isLoginMode) {
                getNicoLiveHTML()
            }
        }
        livePageResponse.body?.string()
    }

    /**
     * コミュをフォローする
     * @param communityId コミュID
     * */
    fun requestCommunityFollow(communityId: String) {
        val communityAPI = CommunityAPI()
        viewModelScope.launch(Dispatchers.Main) {
            val response = communityAPI.requestCommunityFollow(userSession, communityId)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            } else {
                // 成功時
                isCommunityOrChannelFollowLiveData.postValue(true)
                snackbarLiveData.postValue(getString(R.string.nicolive_account_follow_successful))
            }
        }
    }

    /**
     * コミュのフォローを解除する
     * @param communityId コミュID
     * */
    fun requestRemoveCommunityFollow(communityId: String) {
        val communityAPI = CommunityAPI()
        viewModelScope.launch(Dispatchers.Main) {
            val response = communityAPI.requestRemoveCommunityFollow(userSession, communityId)
            if (response.isSuccessful) {
                // 成功時
                isCommunityOrChannelFollowLiveData.postValue(false)
                snackbarLiveData.postValue(getString(R.string.nicolive_account_remove_follow_successful))
            } else {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    /** タグ一覧を取得する。結果はLiveDataへ... */
    fun getTagList() {
        viewModelScope.launch {
            val tagAPI = NicoLiveTagAPI()
            // 番組情報取得済みかどうか
            if (nicoLiveProgramData.value != null) {
                val response = tagAPI.getTags(nicoLiveProgramData.value!!.programId, userSession)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                val tagList = withContext(Dispatchers.Default) { tagAPI.parseTags(response.body?.string()) }
                // LiveData送信
                nicoLiveTagDataListLiveData.postValue(tagList)
            }
        }
    }

    /** タグを追加する */
    fun addTag(tagName: String) {
        viewModelScope.launch {
            val tagAPI = NicoLiveTagAPI()
            // 番組情報取得済みかどうか
            if (nicoLiveProgramData.value != null) {
                // 追加APIを叩く
                val response = tagAPI.addTag(nicoLiveProgramData.value!!.programId, userSession, tagName)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                // 再取得
                getTagList()
            }
        }
    }

    /** タグを削除する */
    fun deleteTag(tagName: String) {
        viewModelScope.launch {
            val tagAPI = NicoLiveTagAPI()
            // 番組情報取得済みかどうか
            if (nicoLiveProgramData.value != null) {
                // 削除APIを叩く
                val response = tagAPI.deleteTag(nicoLiveProgramData.value!!.programId, userSession, tagName)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                // 再取得
                getTagList()
            }
        }
    }

    /**
     * タイムシフト予約を行うAPIを叩く
     * 結果は[isTimeShiftRegisteredLiveData]へ送信されます。
     * */
    fun registerTimeShift() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val timeShiftAPI = NicoLiveTimeShiftAPI()
            // 番組情報取得済みか
            if (nicoLiveProgramData.value != null) {
                val liveId = nicoLiveProgramData.value!!.programId
                val response = timeShiftAPI.registerTimeShift(liveId, userSession)
                // 登録済みかどうか。登録済みの場合はステータスコードが500になる
                val isRegistered = response.code == 500 || response.isSuccessful
                // LiveDataへ
                isTimeShiftRegisteredLiveData.postValue(isRegistered)
                if (response.code == 500) {
                    // 予約済みだったよメッセージ
                    snackbarLiveData.postValue(getString(R.string.timeshift_reserved))
                } else if (response.isSuccessful) {
                    // 予約成功したよメッセージ
                    snackbarLiveData.postValue(getString(R.string.timeshift_reservation_successful))
                }
            }
        }
    }

    /**
     * タイムシフト予約を解除するAPIを叩く
     * 結果は[isTimeShiftRegisteredLiveData]へ送信されます。
     * */
    fun unRegisterTimeShift() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val timeShiftAPI = NicoLiveTimeShiftAPI()
            if (nicoLiveProgramData.value != null) {
                val liveId = nicoLiveProgramData.value!!.programId
                val response = timeShiftAPI.deleteTimeShift(liveId, userSession)
                // LiveData送信
                snackbarLiveData.postValue(getString(R.string.timeshift_delete_reservation_successful))
                // 成功したらfalseをLiveDataへ送信する
                isTimeShiftRegisteredLiveData.postValue(!response.isSuccessful)
            }
        }
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

    /** 終了時 */
    override fun onCleared() {
        super.onCleared()
        nicoLiveHTML.destroy()
        nicoLiveComment.destroy()
        nicoLiveTimeShiftComment.destroy()
    }
}