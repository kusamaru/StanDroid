package com.kusamaru.standroid.homewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.R
import java.text.SimpleDateFormat

/**
 * ニコレポウイジェット
 */
class NicoRepoHomeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAppWidget(context)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    /** ブロードキャスト（更新してくれー）を受け取る */
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context != null) {
            updateAppWidget(context)
        }
    }

    companion object {
        /**
         * ウイジェットを更新する
         *
         * @param context Context
         * */
        fun updateAppWidget(context: Context) {
            // RemoteView
            val views = RemoteViews(context.packageName, R.layout.home_widget_nicorepo)
            // Contextあれば更新できる！
            val componentName = ComponentName(context, NicoRepoHomeWidget::class.java)
            val manager = AppWidgetManager.getInstance(context)

            // 最終更新
            val updateTime = SimpleDateFormat("HH:mm").format(System.currentTimeMillis())
            views.setTextViewText(R.id.home_widget_nicorepo_latest_update_text_view, updateTime)

            // タイトル部分を押すとアプリが起動するように
            val mainActivityIntent = Intent(context, MainActivity::class.java)
            val mainActivityLaunchPendingIntent = PendingIntent.getActivity(context, 64, mainActivityIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.home_widget_nicorepo_title_text_view, mainActivityLaunchPendingIntent)

            // 更新ボタン。このServiceに向かってブロードキャストを送信する
            val updateBroadcastIntent = Intent(context, NicoRepoHomeWidget::class.java)
            val updatePendingIntent = PendingIntent.getBroadcast(context, 128, updateBroadcastIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.home_widget_nicorepo_latest_update_text_view, updatePendingIntent)

            // ListViewの設定。詳細は NicoRepoHomeWidgetRemoteViewService 参照
            views.setRemoteAdapter(R.id.home_widget_nicorepo_list_view, Intent(context, NicoRepoHomeWidgetRemoteViewService::class.java))
            // ListView押したとき
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 256, intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
            // とりあえず押しますよってことで登録（動画IDとかを渡すのは RemoteViewService で）
            views.setPendingIntentTemplate(R.id.home_widget_nicorepo_list_view, pendingIntent)

            // ウイジェット更新
            manager.getAppWidgetIds(componentName).forEach { id ->
                manager.updateAppWidget(id, views)
                // ListView更新
                manager.notifyAppWidgetViewDataChanged(id, R.id.home_widget_nicorepo_list_view)
            }
        }
    }

}