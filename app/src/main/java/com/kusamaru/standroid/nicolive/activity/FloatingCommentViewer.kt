package com.kusamaru.standroid.nicolive.activity

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.LocusId
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicolive.CommentFragment
import com.kusamaru.standroid.nicolive.compose.JCNicoLiveFragment
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.LanguageTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.*

/**
 * フローティングコメビュ。[CommentFragment]を上に乗っけてる。
 * */
class FloatingCommentViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード対応
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        val prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_floating_comment_viewer)

        // ToolBar消す
        supportActionBar?.hide()

        val liveId = intent.getStringExtra("liveId")

        //Fragment設置
        val trans = supportFragmentManager.beginTransaction()
        val commentFragment = JCNicoLiveFragment()
        //LiveID詰める
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        commentFragment.arguments = bundle
        trans.replace(R.id.activity_floating_comment_viewer_linearlayout, commentFragment, liveId)
        trans.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 追加したダイナミックショートカットを消す
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            shortcutManager.removeAllDynamicShortcuts()
        }
    }

    companion object {

        /**
         * フローティングコメントビューワー（アーかもしれない）を起動する関数。Android 10以降で利用可能です。
         * あんまり使わなそう
         * @param context こんてきすと
         * @param liveId 生放送ID
         * @param thumbUrl サムネイルのURL
         * @param title 番組たいとる
         * @param watchMode 視聴モード。以下のどれか。
         *        comment_viewer | コメントビューアー
         *        comment_post   | コメント投稿モード
         *        nicocas        | ニコキャス式コメント投稿モード
         * */
        fun showBubbles(context: Context?, liveId: String, title: String, thumbUrl: String) {
            // Android Q以降で利用可能
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                GlobalScope.launch(Dispatchers.Main) {
                    // フローリングするActivity
                    val intent = Intent(context, FloatingCommentViewer::class.java)
                    intent.action = Intent.ACTION_MAIN
                    intent.putExtra("liveId", liveId)
                    // アイコン取得など
                    val filePath = getThumb(context, thumbUrl, liveId)
                    // 一旦Bitmapに変換したあと、Iconに変換するとうまくいく。
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    val icon = Icon.createWithAdaptiveBitmap(bitmap)
                    val bubbleIntent = PendingIntent.getActivity(context, 25, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
                    // 通知作成？
                    val bubbleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Notification.BubbleMetadata.Builder(bubbleIntent, icon)
                            .setDesiredHeight(1200)
                            .setIntent(bubbleIntent)
                            .build()
                    } else {
                        Notification.BubbleMetadata.Builder()
                            .setDesiredHeight(1200)
                            .setIcon(icon)
                            .setIntent(bubbleIntent)
                            .build()
                    }
                    val supplierPerson = Person.Builder().setName(title).setIcon(icon).build()
                    // 通知送信
                    val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    // 通知チャンネル作成
                    val notificationId = "floating_comment_viewer"
                    if (notificationManager.getNotificationChannel(notificationId) == null) {
                        // 作成
                        val notificationChannel = NotificationChannel(notificationId, context?.getString(R.string.floating_comment_viewer), NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(notificationChannel)
                    }
                    // Android 11 から Shortcutを作ってLocusIdってのを通知に付けないといけないらしい
                    val shortcut = ShortcutInfo.Builder(context, liveId).apply {
                        setShortLabel(liveId)
                        setLongLabel(title)
                        setIcon(icon)
                        setIntent(intent)
                        setLocusId(LocusId(liveId))
                        setLongLived(true) // これ忘れんな
                        setPerson(supplierPerson)
                    }.build()
                    val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
                    shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut))
                    // 通知作成
                    val notification = Notification.Builder(context, notificationId)
                        .setContentText(context?.getString(R.string.floating_comment_viewer_description))
                        .setContentTitle(context?.getString(R.string.floating_comment_viewer))
                        .setSmallIcon(R.drawable.ic_library_books_24px)
                        .setBubbleMetadata(bubbleData)
                        .addPerson(supplierPerson)
                        .setShortcutId(shortcut.id)
                        .setStyle(Notification.MessagingStyle(supplierPerson).apply {
                            conversationTitle = title
                        })
                        .build()
                    // 送信
                    notificationManager.notify(5, notification)
                }
            } else {
                // Android Pieなので..
                Toast.makeText(context, context?.getString(R.string.floating_comment_viewer_version), Toast.LENGTH_SHORT).show()
            }
        }

        /** サムネイル取得してキャッシュ領域へ保存する。suspend関数なので取得終わるまで一時停止する。 */
        private suspend fun getThumb(context: Context?, thumbUrl: String, liveId: String) = GlobalScope.async {
            val request = Request.Builder().apply {
                url(thumbUrl)
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
            val file = File("${context?.externalCacheDir?.path}/$liveId.png")
            file.createNewFile()
            try {
                file.writeBytes(response?.body?.bytes() ?: return@async null)
                return@async file.path
            } catch (e: IOException) {
                e.printStackTrace()
                return@async null
            }
        }.await()

    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}
