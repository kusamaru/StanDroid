package io.github.takusan23.tatimidroid.tool

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.SurfaceView
import android.view.View
import java.text.SimpleDateFormat

/**
 * 共有画面を出す。
 *
 * 写真の保存先はPicturesフォルダになる
 *
 * あと、ニコ動はURLのパラメーター「from」を使うことで再生時間を指定できる
 *
 * 例：`https://nico.ms/sm157?from=30` // きしめんを30秒から再生
 *
 * @param context 共有インテントを飛ばすのに必要
 * */
class ContentShareTool(private val context: Context) {

    /**
     * 写真付きで共有をする。内部でコルーチンを使ってるから非同期です
     * @param commentCanvas コメントView。ReCommentCanvasでもCommentCanvasでもどうぞ
     * @param playerView ExoPlayerにセットしてるSurfaceView
     * @param message 共有時になにか追加したい場合は入れてね
     * @param fromTimeSecond 再生開始時間（指定したければ）。生放送ではnull、動画なら秒
     * @param contentId 番組 か 動画 ID
     * @param contentTitle 名前
     * */
    suspend fun showShareContentAttachPicture(playerView: SurfaceView, commentCanvas: View, contentId: String?, contentTitle: String?, fromTimeSecond: Int?, message: String? = "") {
        // 映像、コメントをキャプチャー
        val (playerImgPath, commentImgPath) = PlayerCommentPictureTool.captureView(playerView, commentCanvas)
        // 重ねる
        val saveBitmap = PlayerCommentPictureTool.makeBitmap(playerImgPath, commentImgPath, null)
        // MediaStoreへ格納
        val fileName = "${contentTitle}-${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(System.currentTimeMillis())}.png"
        if (saveBitmap != null) {
            val uri = PlayerCommentPictureTool.saveMediaStore(playerView.context, fileName, saveBitmap)
            //共有画面出す
            showShareContent(contentId, contentTitle, fromTimeSecond, uri, message)
        }
    }

    /**
     * 共有画面出す。写真なし
     * @param uri 画像を付ける場合はUriを入れてください。
     * @param message タイトル、ID、URL以外に文字列を入れたい場合は指定してください。
     * @param programId 番組 か 動画 ID
     * @param programName 名前
     * @param fromTimeSecond 再生開始時間（指定したければ）。生放送ではnull、動画なら秒
     * */
    fun showShareContent(programId: String?, programName: String?, fromTimeSecond: Int?, uri: Uri? = null, message: String? = "") {
        // 時間指定パラメーター付き？
        val url = if (fromTimeSecond != null) {
            "https://nico.ms/$programId?from=${fromTimeSecond}"
        } else {
            "https://nico.ms/$programId"
        }
        val sendMessage = "$programName\n#$programId\n$url\n$message"
        // Android Sharesheet を利用する
        // なんかおしゃれな共有画面が作れるらしい（TwitterとかのOGPみたいな）
        val shareIntent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sendMessage)
            putExtra(Intent.EXTRA_TITLE, sendMessage)
            if (uri != null) {
                // 画像添付のときはContentTypeを変更
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/jpeg"
                // 画像添付時のみ共有画面に画像を表示させる
                data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            } else {
                type = "text/plain"
            }
        }, null)
        context.startActivity(shareIntent)
    }
}