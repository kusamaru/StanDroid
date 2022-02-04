package com.kusamaru.standroid.tool

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import com.kusamaru.standroid.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * 「ホーム画面に追加」をする関数
 * */
object CreateShortcutTool {

    /**
     * ホーム画面にショートカットを作成する
     *
     * Android 7.1以降では ShortcutManager 、
     * Android 7以前では INSTALL_SHORTCUT ブロードキャストの送信
     * で実装している。
     *
     * @param contentId コミュID
     * @param contentIdType liveId で（もしかしたら動画でも行けるかも）
     * @param contentTitle アイコンの名前
     * @param context Context
     * @param thumbUrl サムネURL
     * */
    suspend fun createHomeScreenShortcut(context: Context, contentIdType: String = "liveId", contentId: String, contentTitle: String, thumbUrl: String) = withContext(Dispatchers.Default) {
        // ショートカットを押したときのインテント
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            putExtra(contentIdType, contentId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 7.1 以降のみ対応
            val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            // サポート済みのランチャーだった
            if (shortcutManager.isRequestPinShortcutSupported) {
                // サムネイル取得！
                val iconBitmap = getThumb(context, thumbUrl, contentId)
                // 一旦Bitmapに変換したあと、Iconに変換するとうまくいく。
                val bitmap = BitmapFactory.decodeFile(iconBitmap)
                val icon = Icon.createWithAdaptiveBitmap(bitmap)
                // Android 11から？setShortLabelの値ではなくBuilder()の第二引数がタイトルに使われるようになったらしい
                val shortcut = ShortcutInfo.Builder(context, contentTitle).apply {
                    setShortLabel(contentTitle)
                    setLongLabel(contentTitle)
                    setIcon(icon)
                    setIntent(intent)
                }.build()
                shortcutManager.requestPinShortcut(shortcut, null)
            }
        } else {
            /**
             * Android 7以下も暗黙的ブロードキャストを送信することで作成できる
             *
             * 古いスマホならワンセグ目的で使えそうだし、ワンセグ+ニコニコ実況って使い方がいいのかも
             * */
            // コミュ画像取得
            val iconBitmap = getThumb(context, thumbUrl, contentId)
            val bitmap = BitmapFactory.decodeFile(iconBitmap)
            // ショートカット作成ブロードキャストインテント
            val broadcastIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, contentTitle)
                putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
            }
            // ブロードキャスト送信
            context.sendBroadcast(broadcastIntent)
            Toast.makeText(context, "ショートカットを作成しました", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * サムネイルをデバイスに保存する。キャッシュに保存すべきなのか永続の方に入れるべきなのかよくわからないけど、とりま永続の方に入れる
     *
     * @param context Context
     * @param fileName ファイル名。拡張子はjpg
     * @param thumbUrl 画像URL
     * @return アイコンのパス。失敗時はnull
     * */
    private suspend fun getThumb(context: Context?, thumbUrl: String, fileName: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url(thumbUrl)
            get()
        }.build()
        val response = try {
            OkHttpClientSingleton.okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        val iconFolder = File(context?.getExternalFilesDir(null), "icon")
        if (!iconFolder.exists()) {
            iconFolder.mkdir()
        }
        val iconFile = File(iconFolder, "$fileName.jpg")
        return@withContext try {
            iconFile.writeBytes(response?.body?.bytes() ?: return@withContext null)
            iconFile.path
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}