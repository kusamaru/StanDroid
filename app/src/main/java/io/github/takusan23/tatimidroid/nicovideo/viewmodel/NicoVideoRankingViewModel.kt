package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nguploader.NGUploaderTool
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import kotlinx.coroutines.*

/**
 * [io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoRankingFragment]のデータを保持するViewModel
 * */
class NicoVideoRankingViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** ランキングの配列 */
    val rankingVideoList = MutableLiveData<List<NicoVideoData>>()

    /** ランキングのタグ配列 */
    val rankingTagList = MutableLiveData<List<String>>()

    /** コルーチンキャンセル用 */
    private val coroutineJob = Job()

    /**
     * ランキングのHTMLをスクレイピングして配列に入れる
     * @param genre genre/all など。[io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML.NICOVIDEO_RANKING_GENRE]から選んで
     * @param time hour など。[io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoRankingHTML.NICOVIDEO_RANKING_TIME]から選んで
     * @param tag VOCALOID など。無くても良い
     * @return [rankingVideoList]等に入れます
     * */
    fun loadRanking(genre: String, time: String, tag: String? = null) {
        // 読み込み中ならキャンセル
        coroutineJob.cancelChildren()
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + coroutineJob + Dispatchers.Default) {
            val nicoVideoRankingHTML = NicoVideoRankingHTML()
            val response = nicoVideoRankingHTML.getRankingHTML(genre, time, tag)
            if (!response.isSuccessful) {
                showToast("${context.getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // パース
            val responseString = response.body?.string() ?: return@launch
            val rawVideoList = nicoVideoRankingHTML.parseRankingVideo(responseString)
            if (rawVideoList != null) {
                rankingVideoList.postValue(NGUploaderTool.filterNGUploaderVideoId(context, rawVideoList))
                rankingTagList.postValue(ArrayList(nicoVideoRankingHTML.parseRankingGenreTag(responseString)))
            } else {
                showToast(context.getString(R.string.error))
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}