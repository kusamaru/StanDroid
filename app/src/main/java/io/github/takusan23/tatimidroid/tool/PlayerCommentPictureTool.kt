package io.github.takusan23.tatimidroid.tool

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.core.view.drawToBitmap
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.PlayerCommentPictureTool.captureView
import io.github.takusan23.tatimidroid.tool.PlayerCommentPictureTool.makeBitmap
import io.github.takusan23.tatimidroid.tool.PlayerCommentPictureTool.saveMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * コメメモ機能
 *
 * 保存先がAndroid 10以降と違うので（10以降はフォルダができる。9以前はフォルダ生成できないので散らばる）
 *
 * 映像(SurfaceView)とコメント描画(CommentCanvas)を重ねた写真を作る関数と保存する関数がある。
 *
 * 一時的にファイル（映像部分、コメント部分）を生成する関数、文字を書き込む関数、保存する関数があります。
 *
 * [captureView]でViewを画像にして保存する→[makeBitmap]で映像の画像とコメントの画像を重ねる→[saveMediaStore]でMediaStoreに保存
 *
 * */
object PlayerCommentPictureTool {

    /** 文字の大きさ */
    private val FONT_SIZE = 45f

    /**
     * View（SurfaceView、CommentCanvas）を画像にして、一時的にファイルに保存します。
     *
     * 一時的に保存するので、二回目以降[captureView]を呼んだ場合は削除されます。
     *
     * @param playerView SurfaceView
     * @param commentCanvas CommentCanvas
     * @return Pairを返します。一個目のファイルパスは映像、二個目のファイルパスはコメント部分になります。
     * */
    suspend fun captureView(playerView: SurfaceView, commentCanvas: View): Pair<String, String> {
        val context = playerView.context
        // 一時的に保存するためそのためのフォルダ
        val tempPictureFolder = File(context.externalCacheDir, "kokosuki_temp").apply {
            if (!exists()) {
                // フォルダないときは生成
                mkdir()
            } else {
                // フォルダあるときは中身を消して準備
                listFiles()?.forEach { it.delete() }
            }
        }
        // それぞれBitmapを取得する
        val playerBitmap = capturePlayerView(playerView)
        val commentBitmap = captureCommentView(commentCanvas)
        // Bitmapを保存する
        val playerImageFile = File(tempPictureFolder, "player.png").apply { createNewFile() }
        val commentImageFile = File(tempPictureFolder, "comment.png").apply { createNewFile() }
        playerBitmap?.compress(Bitmap.CompressFormat.PNG, 100, playerImageFile.outputStream())
        commentBitmap.compress(Bitmap.CompressFormat.PNG, 100, commentImageFile.outputStream())
        // リソース開放
        playerBitmap?.recycle()
        commentBitmap.recycle()
        // Pairでファイルパスを返す
        return Pair(playerImageFile.path, commentImageFile.path)
    }

    /**
     * 画像を重ねて一個のBitmapにして返す関数
     *
     * @param playerImageFilePath 映像の画像のファイルパス
     * @param commentImageFilePath コメントの画像のファイルパス
     * @param drawTextList 右下に文字を入れる場合は一行ずつ配列にして渡してね。書きたくない場合はnullを入れてね
     * */
    fun makeBitmap(playerImageFilePath: String, commentImageFilePath: String, drawTextList: List<String>? = null): Bitmap? {
        val options = BitmapFactory.Options().apply { inMutable = true }
        // Bitmapにする（直接Bitmap扱わないのはメモリ消費量のせい）
        val playerBitmap = BitmapFactory.decodeFile(playerImageFilePath, options)
        val commentBitmap = BitmapFactory.decodeFile(commentImageFilePath, options)
        // コメントBitmapを重ねる
        val playerBitmapWidth = playerBitmap.width
        val commentBitmapWidth = commentBitmap.width
        // return用Bitmap
        val resultBitmap = Bitmap.createBitmap(commentBitmap.width, commentBitmap.height, Bitmap.Config.ARGB_8888)
        // 新UIはアスペクト比に関わらず16:9なので、4:3の場合は動画よりもコメントのほうが幅が大きいのでなおす
        val canvas = if (playerBitmapWidth < commentBitmapWidth) {
            // 新UI用
            Canvas(resultBitmap).apply {
                drawColor(Color.BLACK)
                drawBitmap(playerBitmap, (commentBitmapWidth - playerBitmapWidth) / 2f, 0f, null)
            }
        } else {
            Canvas(resultBitmap).apply {
                drawBitmap(playerBitmap, 0f, 0f, null)
            }
        }
        canvas.drawBitmap(commentBitmap, 0f, 0f, null)
        // 文字を書き込む
        if (drawTextList != null) {
            writeTextToCanvas(canvas, drawTextList)
        }
        // リサイクル
        commentBitmap.recycle()
        playerBitmap.recycle()
        // Bitmapを返す
        return resultBitmap
    }

    /**
     * MediaStoreへ写真を保存する。保存先は Android10以降なら「sdcard/Pictures/TatimiDroid」、未満なら「sdcard/Pictures」
     *
     * DCIMはカメラ用？っぽいので
     *
     * 動画キャッシュの際は[Context.getExternalFilesDir]で端末固有ストレージ内に保存したが、固有の名の通りほかアプリ（ギャラリー等）からは見ることが出来ない。
     *
     * 写真など他のアプリと共有したい場合はMediaStoreに保存する必要がある。
     *
     * 保存の手順だが、MediaStoreに追加をすればUriが返ってくるのでそのUriを使ってoutputStreamを使って保存すればおk
     *
     * ちなみに、そんな事せずに直接写真フォルダに入れればええやんって話ですが、Android10からScopedStorageが導入されたせいで（ほんまこれクソ）直接入れることができなくなりました。
     *
     * MediaStoreでフォルダを作って保存をしようとするとAndroid 10以降とそれ以前で分岐が必要になるのでやっぱこのAPIはクソ。
     *
     * */
    suspend fun saveMediaStore(context: Context, pictureName: String, bitmap: Bitmap): Uri? = withContext(Dispatchers.Default) {
        val contentResolver = context.contentResolver
        // メタデータを入れる。タイトルぐらい？
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, pictureName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        // Android 10 以降、生（意味深）でのファイルアクセスができなくなったので別の方法フォルダを作成する
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            // 写真フォルダ内に「TatimiDroid」フォルダを作成してその中に保存する。Android 9以前はこれが使えないので考え中（でもこのためだけの権限もらうのもなんかあれ）
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TatimiDroid")
        }
        // データを渡してUriをもらう。
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null
        // Bitmapをpngで保存する。jpegの方が良いの？
        val outputStream = contentResolver.openOutputStream(uri)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream?.close()
        // Uriを返す
        return@withContext uri
    }

    /**
     * 保存先ファイルパスを返す。
     *
     * 注意：このパスを使ってファイルアクセスは出来ません（ScopedStorageのせいで）。MediaStoreを利用してください
     * */
    fun getSaveFolder(): String {
        return if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            "${Environment.DIRECTORY_PICTURES}/TatimiDroid"
        } else {
            Environment.DIRECTORY_PICTURES
        }
    }

    /**
     * 文字をCanvasに書く。
     *
     * @param canvas 書く対象のCanvas
     * @param textList 複数行書く場合は一行ずつ配列にして
     * */
    private fun writeTextToCanvas(canvas: Canvas, textList: List<String>) {
        // 普通の文字、枠取り文字をそれぞれ用意
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = FONT_SIZE
            style = Paint.Style.FILL
            // テキストに影をつける
            val shadow = 2f
            setShadowLayer(shadow, shadow, shadow, Color.BLACK)
        }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
            textSize = FONT_SIZE
            color = Color.BLACK
        }
        // Canvasの大きさ
        val canvasWidth = canvas.width
        val canvasHeight = canvas.height
        // 文字を入れる
        textList.reversed().forEachIndexed { index, text ->
            // 長さを計測
            val measureText = strokePaint.measureText(text)
            // 右下に書きたいので
            val xPos = canvasWidth - measureText
            val yPos = canvasHeight - (paint.textSize * index)
            // 第二引数、第三引数はそれぞれ左上
            canvas.drawText(text, xPos, yPos, strokePaint)
            canvas.drawText(text, xPos, yPos, paint)
        }
    }

    /**
     * 再生画面（動画とか生放送とか）をキャプチャーする。なおコールバックな関数を使ったためコルーチンです
     *
     * @param playerView SurfaceView
     * */
    private suspend fun capturePlayerView(playerView: SurfaceView) = withContext(Dispatchers.Main) {
        val context = playerView.context
        // コールバック形式な関数をコルーチン化できる有能
        suspendCoroutine<Bitmap?> { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bitmap = Bitmap.createBitmap(playerView.width, playerView.height, Bitmap.Config.ARGB_8888)
                val locationOfViewInWindow = IntArray(2)
                playerView.getLocationInWindow(locationOfViewInWindow)
                try {
                    PixelCopy.request(
                        playerView, bitmap, { copyResult ->
                            if (copyResult == PixelCopy.SUCCESS) {
                                result.resume(bitmap)
                            } else {
                                Toast.makeText(context, context.getString(R.string.bitmap_error), Toast.LENGTH_SHORT).show()
                                result.resume(null)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (e: IllegalArgumentException) {
                    // PixelCopy may throw IllegalArgumentException, make sure to handle it
                    e.printStackTrace()
                }
            } else {
                // Android 7以下のユーザー。
                playerView.isDrawingCacheEnabled = true
                result.resume(playerView.drawingCache)
                playerView.isDrawingCacheEnabled = false
            }
        }
    }

    /**
     * コメントのViewをキャプチャする
     *
     * @param commentCanvas キャプチャするView
     * */
    private fun captureCommentView(commentCanvas: View): Bitmap {
        return commentCanvas.drawToBitmap()
    }


}