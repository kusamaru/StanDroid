package com.kusamaru.standroid.homewidget

import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoAPIX
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoDataClass
import com.kusamaru.standroid.tool.isConnectionInternet
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * ホーム画面に置くウイジェットでListViewを使うときに使う
 * */
class NicoRepoHomeWidgetRemoteViewService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {

        val context = applicationContext
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        /** ユーザーセッション */
        val userSession = prefSetting.getString("user_session", "")!!

        /** ニコレポデータ */
        val nicoRepoList = arrayListOf<NicoRepoHomeWidgetData>()

        return object : RemoteViewsFactory {
            override fun onCreate() {

            }

            /** データを取得する処理はここ */
            override fun onDataSetChanged() {
                // println("widget データ取得")
                // ドキュメントにも同期処理していいよって書いてあったので遠慮なく
                // インターネット接続確認
                if (isConnectionInternet(context)) {
                    runBlocking {
                        nicoRepoList.clear()
                        val nicoRepoAPIX = NicoRepoAPIX()
                        val response = nicoRepoAPIX.getNicoRepoResponse(userSession, null)
                        if (response.isSuccessful) {
                            nicoRepoAPIX.parseNicoRepoResponse(response.body?.string()).forEach { data ->
                                // Glide。同期処理
                                val iconAsync = async { getBitmapUseGlide(data.userIcon, 20) }
                                val thumbAsync = async { getBitmapUseGlide(data.thumbUrl, 0) }
                                val iconBitmap = iconAsync.await()
                                val thumbBitmap = thumbAsync.await()
                                nicoRepoList.add(NicoRepoHomeWidgetData(data, iconBitmap, thumbBitmap))
                            }
                        }
                    }
                }
            }

            /** 同期的にGlideを利用してBitmapを取得する */
            private fun getBitmapUseGlide(url: String, roundedCorners: Int): Bitmap {
                return try {
                    if (roundedCorners > 0) {
                        Glide.with(context).asBitmap().load(url).transform(RoundedCorners(roundedCorners)).submit().get()
                    } else {
                        Glide.with(context).asBitmap().load(url).submit().get()
                    }
                } catch (e: Exception) {
                    // エラー時の処理。このURLだとGlideがうまく動かない「https://secure-dcdn.cdn.nimg.jp/comch/community-icon/128x128/co5306436.jpg?1614315796」
                    e.printStackTrace()
                    context.getDrawable(R.drawable.ic_do_not_disturb_alt_black_24dp)!!.toBitmap(100, 100)
                }
            }

            override fun onDestroy() {
                nicoRepoList.clear()
            }

            /** リスト数を返す */
            override fun getCount() = nicoRepoList.size

            /** リストの個々のビューを返す */
            override fun getViewAt(position: Int): RemoteViews {
                val nicoRepoData = nicoRepoList[position]
                return RemoteViews(applicationContext.packageName, R.layout.home_widget_nicorepo_item).apply {
                    // 文字入れ
                    setTextViewText(R.id.home_widget_nicorepo_item_name_text_view, nicoRepoData.data.userName)
                    setTextViewText(R.id.home_widget_nicorepo_item_description_text_view, HtmlCompat.fromHtml(nicoRepoData.data.message, HtmlCompat.FROM_HTML_MODE_COMPACT))
                    setTextViewText(R.id.home_widget_nicorepo_item_title_text_view, nicoRepoData.data.title)
                    // 1時間前みたいな表記にする
                    setTextViewText(R.id.home_widget_nicorepo_item_time_text_view, DateUtils.getRelativeTimeSpanString(nicoRepoData.data.date))
                    // 画像
                    setImageViewBitmap(R.id.home_widget_nicorepo_item_avater_image_view, nicoRepoData.iconBitmap)
                    setImageViewBitmap(R.id.home_widget_nicorepo_item_thumb_image_view, nicoRepoData.thumbBitmap)
                    // 押したとき
                    val mainActivityIntent = Intent().apply {
                        if (nicoRepoData.data.isVideo) {
                            putExtra("videoId", nicoRepoData.data.contentId)
                        } else {
                            putExtra("liveId", nicoRepoData.data.contentId)
                        }
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    setOnClickFillInIntent(R.id.home_widget_nicorepo_item_root, mainActivityIntent)
                }
            }

            override fun getLoadingView(): RemoteViews? {
                return null
            }

            override fun getViewTypeCount(): Int {
                return 1
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun hasStableIds(): Boolean {
                return true
            }
        }
    }

    /**
     * ウイジェットのListView表示で渡すデータ
     *
     * @param data ニコレポのデータ
     * @param iconBitmap ユーザーアイコンのBitmap
     * @param thumbBitmap サムネのBitmap
     * */
    private data class NicoRepoHomeWidgetData(
        val data: NicoRepoDataClass,
        val iconBitmap: Bitmap,
        val thumbBitmap: Bitmap,
    )

}