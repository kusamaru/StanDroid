package io.github.takusan23.tatimidroid.bottomfragment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.NicoHistoryDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * 端末内履歴Fragmentで使うViewModel
 * */
class NicoHistoryViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** 表示する履歴の配列を送信するLiveData */
    val historyListLiveData = MutableLiveData<List<NicoHistoryDBEntity>>()

    /** 合計数とかが入ってる配列を送信するLiveData */
    val countTextListLiveData = MutableLiveData<List<String>>()

    init {
        getHistoryList()
    }

    /**
     * データベースから取得する。結果はLiveDataで返ってきます
     *
     * @param isIncludeLive 生放送を含めるか
     * @param isIncludeVideo 動画を含めるか
     * @param isRemoveDistinct 重複を消すか
     * @param isFilterToDay 今日のみを表示するか
     * */
    fun getHistoryList(isIncludeLive: Boolean = true, isIncludeVideo: Boolean = true, isFilterToDay: Boolean = false, isRemoveDistinct: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // DBから取り出す
            val rawHistoryList = NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().getAll() as ArrayList
            rawHistoryList.sortByDescending { it.unixTime }
            // フィルター
            var filteredList = when {
                isIncludeLive && isIncludeVideo -> rawHistoryList.filter { it.type == "video" || it.type == "live" }
                isIncludeVideo -> rawHistoryList.filter { it.type == "video" }
                isIncludeLive -> rawHistoryList.filter { it.type == "live" }
                else -> rawHistoryList
            }
            // 重複消す
            if (isRemoveDistinct) {
                filteredList = filteredList.distinctBy { it.userId }
            }
            // 今日のみ
            if (isFilterToDay) {
                // から
                val calender = Calendar.getInstance().apply {
                    set(Calendar.HOUR, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val from = calender.time.time / 1000L
                // まで
                val to = System.currentTimeMillis() / 1000L
                // 範囲に入ってるか
                filteredList = filteredList.filter { it.unixTime in from..to }
            }
            // LiveDataへ送信
            historyListLiveData.postValue(filteredList)
            // 各件数も
            val totalCount = rawHistoryList.size
            val showCount = filteredList.size
            val videoCount = filteredList.count { it.type == "video" }
            val liveCount = filteredList.count { it.type == "live" }
            // 合計数関係
            val countTextList = arrayListOf(
                "${context.getString(R.string.local_history_total_count)}\n$totalCount",
                "${context.getString(R.string.local_history_show_count)}\n$showCount",
                "${context.getString(R.string.local_history_video_count)}\n$videoCount (${calcPercent(showCount, videoCount)}%)",
                "${context.getString(R.string.local_history_live_count)}\n$liveCount (${calcPercent(showCount, liveCount)}%)",
            )
            countTextListLiveData.postValue(countTextList)
        }
    }

    /**
     * パーセントを出す
     * @param total トータル
     * @param count かず
     * */
    private fun calcPercent(total: Int, count: Int): Int {
        return ((count / total.toFloat()) * 100).toInt()
    }

}