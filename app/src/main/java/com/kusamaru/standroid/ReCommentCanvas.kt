package com.kusamaru.standroid

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * ニコ動のために作り直されたコメント描画。
 *
 * シークバーいじることができる。
 * */
class ReCommentCanvas(ctx: Context, attributeSet: AttributeSet?) : View(ctx, attributeSet) {

    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    /** 鯖からもらったコメント一覧。[initCommentList]経由で入れられます */
    private var rawCommentList = arrayListOf<CommentJSONParse>()

    /** 流す or 流れた コメント一覧 */
    private val drawNakaCommentList = arrayListOf<ReDrawCommentData>()

    /** 上のコメント一覧 */
    private val drawUeCommentList = arrayListOf<ReDrawCommentData>()

    /** 下のコメント一覧 */
    private val drawShitaCommentList = arrayListOf<ReDrawCommentData>()

    /** アスキーアート（コメントアート・職人）のために使う。最後に追加しあ高さが入る */
    private var prevHeight = 0

    /** アスキーアート（コメントアート・職人）のために使う。最後に追加した移動速度[ReDrawCommentData.commentUpdateMsMoveSize]が入る*/
    private var prevCommentUpdateMsMoveSize = 0

    /** 黒枠 */
    private val blackPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2.0f
        style = Paint.Style.STROKE
        textSize = 20 * resources.displayMetrics.scaledDensity
        color = Color.BLACK
    }

    /** 白色 */
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 20 * resources.displayMetrics.scaledDensity
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    /** 当たり判定検証用にコメントに枠をつける */
    private val strokePaint = Paint().apply {
        strokeWidth = 5f
        color = Color.RED
        style = Paint.Style.STROKE
    }

    /** コメントの枠を表示する際はtrue。PC版とかで自分のコメントに枠が付くあれ。 */
    val watashiHaDeveloper = prefSetting.getBoolean("dev_setting_comment_canvas_text_rect", false)

    /** フォント変更 */
    var typeFace: Typeface? = null
        set(value) {
            paint.typeface = value
            blackPaint.typeface = value
            field = value
        }

    /** コメントを動かすTimer */
    private var commentDrawTimer = Timer()

    /** コメントを登録するTimer */
    private var commentAddTimer = Timer()

    /** 再生時間を入れてください。定期的に */
    var currentPos = 0L

    /** 再生中かどうか。再生状態が変わったらこちらも更新してください。 */
    var isPlaying = false

    /**
     * この２つはコメントが重複しないようにするためのもの
     * */
    private val drewedList = arrayListOf<Long>()
    private var tmpPosition = 0L

    /** コメント表示時間 */
    private val commentDrawTime = prefSetting.getString("setting_comment_canvas_show_time", "5")?.toInt() ?: 5

    /** 1秒で何回画面を更新するか。多分60FPSがデフォ */
    private val fps by lazy {
        // FPSを手動で変更するのかどうか
        val isEditFPS = prefSetting.getBoolean("setting_comment_canvas_edit_fps_enable", false)
        if (isEditFPS) {
            // ユーザー定義
            prefSetting.getString("setting_comment_canvas_edit_fps", "60")?.toFloat() ?: 60f
        } else {
            // 端末のリフレッシュレートを取得
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Context#getDisplay()を使うとポップアップ時に４ぬ
                display?.refreshRate ?: 60f // 取れない場合は60
            } else {
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
            }
        }.toInt()
    }

    /** 何ミリ秒で画面を更新するか */
    private val commentUpdateMs by lazy { (1000 / fps).toLong() }

    /** コメントの透明度 */
    private val commentAlpha = prefSetting.getString("setting_comment_alpha", "1.0")?.toFloat() ?: 1.0F

    /** コメントに影を付ける量 */
    private val textShadow = prefSetting.getString("setting_comment_canvas_text_shadow", "1.0")?.toFloatOrNull() ?: 1f

    /**
     * 高さ、幅決定版。getWidthとかが0を返すので対策
     * */
    private var finalWidth = width
    private var finalHeight = height

    /** コルーチン */
    private val coroutineJob = Job()

    /** FPSを計算するか。計算結果は[addFPSCallBack]で受け取れます */
    var isCalcFPS = false

    /** FPS。一秒間に何回画面を更新したか */
    private var fpsCount = 0

    /** FPS取得時の時間 */
    private var startTimeMs = System.currentTimeMillis()

    /** FPSコールバック関数の配列 */
    private val fpsCallBackList = arrayListOf<((fps: Int) -> Unit)>()

    init {
        // コメントを動かす
        commentDrawTimer.schedule(commentUpdateMs, commentUpdateMs) {
            if (isPlaying) {
                // 画面外のコメントは描画しない
                for (reDrawCommentData in drawNakaCommentList.toList()
                    .filter { reDrawCommentData -> reDrawCommentData?.rect?.right ?: 0 > -reDrawCommentData.measure }) {
                    if (reDrawCommentData != null && reDrawCommentData.rect != null) {
                        reDrawCommentData.rect.left -= reDrawCommentData.commentUpdateMsMoveSize
                        reDrawCommentData.rect.right -= reDrawCommentData.commentUpdateMsMoveSize
                        // なお画面外は消す
                        if (reDrawCommentData.rect.right < 0) {
                            drawNakaCommentList.remove(reDrawCommentData)
                        }
                    }
                }
                // 上コメ、下コメは３秒間表示させる
                drawUeCommentList.toList().forEach { reDrawCommentData ->
                    // マイナスの値は許されない。これしないとループ再生時に固定コメントが残る
                    val calc = currentPos - reDrawCommentData.videoPos
                    if (calc > 3000 || calc < -1) {
                        drawUeCommentList.remove(reDrawCommentData)
                    }
                }
                drawShitaCommentList.toList().forEach { reDrawCommentData ->
                    // マイナスの値は許されない。これしないとループ再生時に固定コメントが残る
                    val calc = currentPos - reDrawCommentData.videoPos
                    if (calc > 3000 || calc < -1) {
                        drawShitaCommentList.remove(reDrawCommentData)
                    }
                }
                postInvalidate()
            }
        }
        // コメントを追加する
        commentAddTimer.schedule(100, 100) {
            val currentVPos = currentPos / 100L
            val currentPositionSec = currentPos / 1000
            if (tmpPosition != currentPositionSec) {
                drewedList.clear()
                tmpPosition = currentPositionSec
            }
            val drawList = rawCommentList.filter { commentJSONParse ->
                (commentJSONParse.vpos.toLong() / 10L) == (currentVPos)
            }
            drawList.forEach {
                // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                // なんかしらんけど負荷がかかりすぎるとここで ConcurrentModificationException 吐くので Array#toList() を使う
                val isAddable = drewedList.toList()
                    .none { id -> if (it.commentNo.isEmpty()) it.dateUsec.toLong() == id else it.commentNo.toLong() == id } // 条件に合わなければtrue
                if (isAddable) {
                    // コメントIDない場合はdate_usecで代替する
                    drewedList.add(if (it.commentNo.isEmpty()) it.dateUsec.toLong() else it.commentNo.toLong())
                    // コメント登録。
                    drawComment(it, currentPos)
                }
            }
        }
        // Viewの大きさ決定版
        viewTreeObserver.addOnGlobalLayoutListener {
            finalHeight = height
            finalWidth = width
        }
    }

    /**
     * コメントの配列を準備する。コメントを追加する時間の計算をする
     *
     * 引数の配列はディープコピーされるので複数回呼んでも大丈夫？
     *
     * @param commentList コメント配列
     * @param videoDurationMs 動画の長さ。ミリ秒で
     * */
    fun initCommentList(commentList: List<CommentJSONParse>, videoDurationMs: Long) {
        if (videoDurationMs < 0 || commentList.isEmpty()) return
        clearCommentList()
        // View#getWidth()が0返すのでpost
        post {
            thread {
                // 動画の3秒前のvposを出す
                val endVpos = (videoDurationMs - 3000) / 10 // 100vpos = 1sec

                /**
                 * コメントのvposをずらす
                 * 10秒に表示するコメントを、10秒に画面外に追加してたら手遅れなのでずらす
                 *
                 * あとディープコピーしないと共有されるので
                 * */
                val deepCopyList = commentList.map { commentJSONParse -> commentJSONParse.deepCopy(commentJSONParse) }
                deepCopyList.forEach { commentJSON ->
                    // 固定コメントは特に何もしない
                    if (!checkUeComment(commentJSON.mail) && !commentJSON.mail.contains("shita")) {
                        // 大きさ計測
                        val fontSize = getCommandFontSize(commentJSON.mail).toInt()
                        // 画面外まではみ出るコメントは強制的にコメントキャンバスの幅に合わせる
                        val measure = min(getBlackCommentTextPaint(fontSize).measureText(commentJSON.comment).toInt(), finalWidth)
                        // 動かす範囲。画面外含めて
                        val widthMinusCommentMeasure = finalWidth + measure + measure
                        // FPSと表示時間を掛けて、コメントの幅で割ればおｋ
                        val moveSize = (widthMinusCommentMeasure / (commentDrawTime * fps))
                        // なんか0で割る時があるっぽい？私は再現できなかった。；；
                        if (moveSize > 0) {
                            // 引いておく
                            val minusVPos = ((measure / moveSize) * commentUpdateMs) / 10L
                            // 0以上で。
                            commentJSON.vpos = max((commentJSON.vpos.toInt() - minusVPos.toInt()), 0).toString()
                            // 動画終了3秒前のコメントはすべてまとめる
                            if (commentJSON.vpos.toLong() > endVpos) {
                                commentJSON.vpos = endVpos.toString()
                            }
                        }
                    }
                }
                rawCommentList = deepCopyList as ArrayList<CommentJSONParse>
            }
        }
    }

    /** FPSコールバックを追加する関数 */
    fun addFPSCallBack(fpsFunc: ((fps: Int) -> Unit)) {
        fpsCallBackList.add(fpsFunc)
    }

    /** コメント配列をクリアする */
    fun clearCommentList() {
        rawCommentList.clear()
        drawNakaCommentList.clear()
        drawShitaCommentList.clear()
        drawUeCommentList.clear()
    }

    /** Viewがおわった時 */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        commentDrawTimer.cancel()
        commentAddTimer.cancel()
        coroutineJob.cancelChildren()
    }

    /**
     * コメントシークした時に描画できるように
     * */
    fun seekComment() {
        thread {
            drawNakaCommentList.clear()
            drawUeCommentList.clear()
            drawShitaCommentList.clear()
            // コメント表示時間さかのぼって描画
            repeat(commentDrawTime) { sec ->
                val currentPosSec = currentPos / 1000
                val drawList = rawCommentList.filter { commentJSONParse ->
                    (commentJSONParse.vpos.toLong() / 100L) == (currentPosSec - sec)
                }
                drawList.forEach { commentJSON ->
                    // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                    // なんかしらんけど負荷がかかりすぎるとここで ConcurrentModificationException 吐くので Array#toList() を使う
                    val isAddable = drewedList.toList()
                        .none { id -> if (commentJSON.commentNo.isEmpty()) commentJSON.dateUsec.toLong() == id else commentJSON.commentNo.toLong() == id } // 条件に合わなければtrue
                    if (isAddable) {
                        // コメントIDない場合はdate_usecで代替する
                        drewedList.add(if (commentJSON.commentNo.isEmpty()) commentJSON.dateUsec.toLong() else commentJSON.commentNo.toLong())
                        // 大きさ計測
                        val fontSize = getCommandFontSize(commentJSON.mail).toInt()
                        // 画面外まではみ出るコメントは強制的にコメントキャンバスの幅に合わせる
                        val measure = min(getBlackCommentTextPaint(fontSize).measureText(commentJSON.comment).toInt(), finalWidth)
                        // 動かす範囲。画面外含めて
                        val widthMinusCommentMeasure = finalWidth + measure + measure
                        // 1秒で動かす大きさ
                        val moveSize = (widthMinusCommentMeasure / commentDrawTime)
                        // コメント登録。
                        drawComment(commentJSON, currentPos + (sec * 1000), (sec * moveSize))
                    }
                }
            }
            // ほな UIスレッド ちゃうかー
            postInvalidate()
        }
    }

    /** [invalidate]呼ぶとここに来る */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // 「ぬるぽ」「ガッ」なんて知らんわ
        if (canvas == null) return

        // FPS算出
        onDrawFPSCounter()

        // 中コメ
        for (reDrawCommentData in drawNakaCommentList.toList()) {
            if (reDrawCommentData != null && reDrawCommentData.rect != null) {
                setCommandPaint(reDrawCommentData.colorCode, reDrawCommentData.fontSize)
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    blackPaint
                )
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    paint
                )
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }

        // 上コメ
        for (reDrawCommentData in drawUeCommentList.toList()) {
            if (reDrawCommentData != null && reDrawCommentData.rect != null) {
                setCommandPaint(reDrawCommentData.colorCode, reDrawCommentData.fontSize)
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    blackPaint
                )
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    paint
                )
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }

        // 下コメ
        for (reDrawCommentData in drawShitaCommentList.toList()) {
            if (reDrawCommentData != null && reDrawCommentData.rect != null) {
                setCommandPaint(reDrawCommentData.colorCode, reDrawCommentData.fontSize)
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    blackPaint
                )
                canvas.drawText(
                    reDrawCommentData.comment,
                    reDrawCommentData.rect.left.toFloat(),
                    reDrawCommentData.rect.bottom.toFloat(),
                    paint
                )
                // 当たり判定検証ように枠をつける
                if (watashiHaDeveloper) {
                    canvas.drawRect(reDrawCommentData.rect, strokePaint)
                }
            }
        }
    }

    /** [onDraw]内で利用してください。FPSを数えます */
    private fun onDrawFPSCounter() {
        // 有効時のみ
        if (isCalcFPS) {
            if (System.currentTimeMillis() - startTimeMs > 1000) {
                fpsCallBackList.forEach { it.invoke(fpsCount) }
                fpsCount = 0
                startTimeMs = System.currentTimeMillis()
            } else {
                fpsCount += 1
            }
        }
    }

    /**
     * コメントを登録する
     * @param addPos シーク時なんかに使う。コメントの位置を先に進めるときに
     * */
    fun drawComment(commentJSONParse: CommentJSONParse, videoPos: Long, addPos: Int = 0) {
        // コメント消すのがオンになってたら帰るよ
        if (prefSetting.getBoolean("nicovideo_comment_canvas_hide", false) == true) { return }

        val isUe = checkUeComment(commentJSONParse.mail)
        val isShita = commentJSONParse.mail.contains("shita")
        val commentList = commentJSONParse.comment.split("\n")
        when {
            // うえ
            isUe -> {
                prevHeight = 0
                commentList.forEach { comment ->
                    commentJSONParse.comment = comment
                    drawUeComment(commentJSONParse, videoPos)
                }
            }
            // した
            isShita -> {
                prevHeight = 0
                commentList.reversed().forEach { comment ->
                    commentJSONParse.comment = comment
                    drawShitaComment(commentJSONParse, videoPos)
                }
            }
            // 通常
            else -> {
                prevHeight = 0
                commentList.forEach { comment ->
                    commentJSONParse.comment = comment
                    drawNakaComment(commentJSONParse, videoPos, addPos, commentList.size > 1, commentList.size)
                }
            }
        }
    }

    /**
     * 中コメントを登録する
     * @param asciiArt アスキーアート（複数行コメント）の場合はtrue
     * @param asciiArtLines コメントアートが何行になるのか
     * */
    private fun drawNakaComment(commentJSON: CommentJSONParse, videoPos: Long, addPos: Int = 0, asciiArt: Boolean = false, asciiArtLines: Int = -1) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // フォントサイズ。コマンド等も考慮して
        var fontSize = getCommandFontSize(command).toInt()
        val measure = getBlackCommentTextPaint(fontSize).measureText(comment)
        val lect = finalWidth - addPos
        // アスキーアートが画面に収まるように。ただし特に何もしなくても画面内に収まる場合は無視。改行多くて入らない場合のみ
        val isAsciiArtUseHeightMax = asciiArt && finalHeight < asciiArtLines * fontSize
        if (isAsciiArtUseHeightMax) {
            fontSize = finalHeight / asciiArtLines
        }
        // 当たり判定計算
        val addRect = Rect(lect, 0, (lect + measure).toInt(), fontSize)
        if (isAsciiArtUseHeightMax) {
            // 画面外に収まらないコメントの場合
            // コメントアート、アスキーアート
            addRect.top = prevHeight
            addRect.bottom = addRect.top + fontSize
            prevHeight = addRect.bottom
        } else {
            // 全パターん
            val tmpList = drawNakaCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData?.rect?.top }
            for (reDrawCommentData in tmpList) {
                if (reDrawCommentData?.rect != null) {
                    if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                        // あたっているので下へ
                        addRect.top = reDrawCommentData.rect.bottom
                        addRect.bottom = (addRect.top + fontSize)
                    }
                }
            }
            // なお画面外突入時はランダム
            if (addRect.bottom > finalHeight) {
                val randomValue = randomValue(fontSize.toFloat())
                addRect.top = randomValue
                addRect.bottom = addRect.top + fontSize
            }
        }
        // 動かす範囲。画面外含めて
        val widthMinusCommentMeasure = finalWidth + measure + measure
        // FPSと表示時間を掛けて、コメントの幅で割ればおｋ
        val moveSize = (widthMinusCommentMeasure / (commentDrawTime * fps)).toInt()
        // CAは同じ速度にする
        val aaSupportMoveSize = when {
            asciiArt && prevCommentUpdateMsMoveSize == 0 -> {
                prevCommentUpdateMsMoveSize = moveSize
                moveSize
            }
            asciiArt -> prevCommentUpdateMsMoveSize
            else -> {
                prevCommentUpdateMsMoveSize = 0
                moveSize
            }
        }
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "naka",
            fontSize = fontSize.toFloat(),
            measure = measure,
            asciiArt = asciiArt,
            colorCode = getColor(command),
            commentUpdateMsMoveSize = aaSupportMoveSize,
        )
        drawNakaCommentList.add(data)
    }

    /**
     * 上コメントを登録する
     * */
    private fun drawUeComment(commentJSON: CommentJSONParse, videoPos: Long) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(getBlackCommentTextPaint(getCommandFontSize(command).toInt()).measureText(comment), getCommandFontSize(command), comment)
        val measure = calcCommentSize.first
        // フォントサイズ。コマンド等も考慮して
        val fontSize = calcCommentSize.second.toInt()
        val lect = ((finalWidth - measure) / 2).toInt()
        // 当たり判定計算
        val addRect = Rect(lect, 0, (lect + measure).toInt(), fontSize)
        // 全パターん
        val tmpList = drawUeCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData?.rect?.top }
        for (reDrawCommentData in tmpList) {
            if (reDrawCommentData?.rect != null) {
                if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                    // あたっているので下へ
                    addRect.top = reDrawCommentData.rect.bottom
                    addRect.bottom = (addRect.top + reDrawCommentData.fontSize).roundToInt()
                }
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top > finalHeight) {
            val randomValue = randomValue(fontSize.toFloat())
            addRect.top = randomValue
            addRect.bottom = addRect.top + fontSize
        }
        // 動かす範囲。画面外含めて
        val widthMinusCommentMeasure = finalWidth + measure + measure
        // FPSと表示時間を掛けて、コメントの幅で割ればおｋ
        val moveSize = (widthMinusCommentMeasure / (commentDrawTime * fps)).toInt()
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "ue",
            measure = measure,
            fontSize = fontSize.toFloat(),
            colorCode = getColor(command),
            commentUpdateMsMoveSize = moveSize,
        )
        drawUeCommentList.add(data)
    }

    /**
     * 下コメントを登録する
     * */
    private fun drawShitaComment(commentJSON: CommentJSONParse, videoPos: Long) {
        val comment = commentJSON.comment
        val command = commentJSON.mail
        // コメントの大きさ調整
        val calcCommentSize = calcCommentFontSize(getBlackCommentTextPaint(getCommandFontSize(command).toInt()).measureText(comment), getCommandFontSize(command), comment)
        val measure = calcCommentSize.first
        // フォントサイズ。コマンド等も考慮して
        val fontSize = calcCommentSize.second.toInt()
        val lect = ((finalWidth - measure) / 2).toInt()
        // 当たり判定計算
        val addRect = Rect(lect, (finalHeight - fontSize), (lect + measure).toInt(), finalHeight)
        // 全パターん
        val tmpList = drawShitaCommentList.toList().sortedBy { reDrawCommentData -> reDrawCommentData.videoPos }
        for (reDrawCommentData in tmpList) {
            if (reDrawCommentData?.rect != null) {
                if (Rect.intersects(reDrawCommentData.rect, addRect)) {
                    // あたっているので下へ
                    addRect.top = (reDrawCommentData.rect.top - reDrawCommentData.fontSize).roundToInt()
                    addRect.bottom = (reDrawCommentData.rect.bottom - reDrawCommentData.fontSize).roundToInt()
                }
            }
        }
        // なお画面外突入時はランダム
        if (addRect.top < 0) {
            val randomValue = randomValue(fontSize.toFloat())
            addRect.bottom = randomValue + fontSize
            addRect.top = randomValue
        }
        // 動かす範囲。画面外含めて
        val widthMinusCommentMeasure = finalWidth + measure + measure
        // FPSと表示時間を掛けて、コメントの幅で割ればおｋ
        val moveSize = (widthMinusCommentMeasure / (commentDrawTime * fps)).toInt()
        // 配列に入れる
        val data = ReDrawCommentData(
            comment = comment,
            command = command,
            videoPos = videoPos,
            rect = addRect,
            pos = "shita",
            measure = measure,
            fontSize = fontSize.toFloat(),
            colorCode = getColor(command),
            commentUpdateMsMoveSize = moveSize,
        )
        drawShitaCommentList.add(data)
    }

    /**
     * コマンドに合った文字の大きさを返す
     * @param command big/small など
     * @return フォントサイズ
     * */
    fun getCommandFontSize(command: String): Float {
        // でふぉ
        val defaultFontSize = 20 * resources.displayMetrics.scaledDensity

        // コメント行を自由に設定する設定
        val isCustomCommentLine = prefSetting.getBoolean("setting_comment_canvas_custom_line_use", false)
        val customCommentLine = prefSetting.getString("setting_comment_canvas_custom_line_value", "10")?.toIntOrNull() ?: 20

        // CommentCanvasが小さくても最低限確保する行
        val isMinLineSetting = prefSetting.getBoolean("setting_comment_canvas_min_line", true)
        val minLineValue = prefSetting.getString("setting_comment_canvas_min_line_value", "10")?.toIntOrNull() ?: 10

        // 現在最大何行書けるか
        val currentCommentLine = finalHeight / defaultFontSize

        // 強制10行表示モード
        val is10LineMode = prefSetting.getBoolean("setting_comment_canvas_10_line", false)
        // フォントサイズ
        val fontSize = when {
            is10LineMode -> (finalHeight / 10).toFloat() // 強制10行確保
            isCustomCommentLine -> (finalHeight / customCommentLine).toFloat() // 自由に行設定
            isMinLineSetting && currentCommentLine < minLineValue -> (finalHeight / minLineValue).toFloat() // 最低限確保する設定。最低限確保する行を下回る必要がある
            else -> defaultFontSize // でふぉ
        }
        return when {
            command.contains("big") -> {
                (fontSize * 1.3).toFloat()
            }
            command.contains("small") -> {
                (fontSize * 0.8).toFloat()
            }
            else -> fontSize
        }
    }

    /** 高さをランダムで決める */
    private fun randomValue(fontSize: Float): Int {
        // 旧処理 エラー出たら戻す
        // ランダム決めるとこで落ちるっぽいからfinalHeightが0を返してる間はとりあえず決め打ちする
        // 一旦表示バグは考慮しない
//        if (finalHeight >= 0) {
//            return Random.nextInt(1, 5);
//        }

        return if (finalHeight > fontSize) {
            Random.nextInt(1, (height - fontSize).toInt())
        } else {
            Random.nextInt(
                1,
                if (finalHeight <= 0) { 5 } else finalHeight // finalHeightが0の場合は決め打ち
            )
        }
    }

    /**
     * 指定したフォントサイズのPaintを生成する関数
     * */
    private fun getBlackCommentTextPaint(fontSize: Int): Paint {
        val paint = Paint()
        paint.textSize = fontSize.toFloat()
        return paint
    }

    /**
     * コマンドの色にをPaintへセットする
     * @param colorCode カラーコード。16進数
     * */
    private fun setCommandPaint(colorCode: String, fontSize: Float) {
        paint.textSize = fontSize
        blackPaint.textSize = fontSize
        paint.color = Color.parseColor(colorCode)
        // 0 ~ 225 の範囲で指定するため 225かける。日経225
        paint.alpha = (commentAlpha * 225).toInt()
        blackPaint.alpha = (commentAlpha * 225).toInt()
        // テキストに影をつける
        paint.setShadowLayer(textShadow, textShadow, textShadow, Color.BLACK)
    }

    /**
     * コマンドから色の部分を取り出してカラーコードにして返す
     *
     * 大百科参照：https://dic.nicovideo.jp/a/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88
     * */
    private fun getColor(command: String): String {
        return when {
            // プレ垢限定色。
            command.contains("white2") -> "#CCCC99"
            command.contains("red2") -> "#CC0033"
            command.contains("pink2") -> "#FF33CC"
            command.contains("orange2") -> "#FF6600"
            command.contains("yellow2") -> "#999900"
            command.contains("green2") -> "#00CC66"
            command.contains("cyan2") -> "#00CCCC"
            command.contains("blue2") -> "#3399FF"
            command.contains("purple2") -> "#6633CC"
            command.contains("black2") -> "#666666"
            // 一般でも使えるやつ
            command.contains("red") -> "#FF0000"
            command.contains("pink") -> "#FF8080"
            command.contains("orange") -> "#FFC000"
            command.contains("yellow") -> "#FFFF00"
            command.contains("green") -> "#00FF00"
            command.contains("cyan") -> "#00FFFF"
            command.contains("blue") -> "#0000FF"
            command.contains("purple") -> "#C000FF"
            command.contains("black") -> "#000000"
            // カラーコード直
            command.contains("#") -> {
                // 正規表現で出す
                return ("#.{6}?").toRegex().find(command)?.value ?: "#FFFFFF"
            }
            // その他
            else -> "#FFFFFF"
        }
    }


    /**
     * コメントが入り切らないときに大きさを調整して画面内に収める関数
     * @param argMeasure 元のコメントの長さ
     * @param comment コメント
     * @param fontSize フォントサイズ
     * @return firstがコメントの長さ、secondがコメントのフォントサイズ
     * */
    private fun calcCommentFontSize(argMeasure: Float, fontSize: Float, comment: String): Pair<Float, Float> {
        // コメントがコメントキャンバスを超えるときの対応をしないといけない。
        var measure = argMeasure
        var commandFontSize = fontSize
        if (finalWidth < argMeasure) {
            // 超えるとき。私の時代はもう携帯代青天井とかは無いですね。
            // 一文字のフォントサイズ計算。収めるにはどれだけ縮めれば良いのか
            commandFontSize = (finalWidth.toFloat() / comment.length)
            // コメントの幅再取得
            measure = getBlackCommentTextPaint(commandFontSize.toInt()).measureText(comment)
        } else {
            // 超えない。10年前から携帯で動画見れた気がするけど結局10年経ってもあんまり外で動画見る人いない気がする
        }
        return Pair(measure, commandFontSize)
    }

    /**
     * 上コメントかどうかを検証する
     * 部分一致で「ue」で上か判定するともれなく「blue」が引っかかるので
     * */
    private fun checkUeComment(command: String): Boolean {
        return when {
            // blueでなおblueの文字を消してもueが残る場合は上コメント
            command.replace(Regex("blue2|blue|guest"), "").contains("ue") -> true
            // ちがう！！！
            else -> false
        }
    }

    /**
     * コメントキャンバスの更新頻度を返す
     * */
    private fun getCommentCanvasUpdateMs(): Long {
        // コメントの更新頻度をfpsで設定するかどうか
        val enableCommentSpeedFPS = prefSetting.getBoolean("setting_comment_canvas_speed_fps_enable", true)
        // コメントキャンバスの更新頻度
        return if (enableCommentSpeedFPS) {
            // fpsで設定
            val fps = prefSetting.getString("setting_comment_canvas_speed_fps", "60")?.toIntOrNull() ?: 60
            // 1000で割る （例：1000/60=16....）
            (1000 / fps)
        } else {
            // ミリ秒で指定
            prefSetting.getString("setting_comment_canvas_timer", "16")?.toIntOrNull() ?: 10
        }.toLong()
    }

}

/**
 * [com.kusamaru.standroid.ReCommentCanvas]で使うデータクラス。
 * @param command コマンド
 * @param comment コメント本文
 * @param pos ue naka shita のどれか
 * @param rect 当たり判定やコメント移動で使う
 * @param videoPos 再生位置。動画の時間
 * @param asciiArt コメントアートならtrue
 * @param fontSize 文字の大きさ
 * @param measure コメントの幅
 * @param colorCode コメントの色
 * @param commentUpdateMsMoveSize [commentUpdateMs]でどれだけ動かすか
 * */
class ReDrawCommentData(
    val comment: String,
    val command: String,
    val videoPos: Long,
    val rect: Rect?,
    val pos: String,
    val fontSize: Float,
    val measure: Float,
    val asciiArt: Boolean = false,
    val colorCode: String = "#ffffff",
    val commentUpdateMsMoveSize: Int,
)