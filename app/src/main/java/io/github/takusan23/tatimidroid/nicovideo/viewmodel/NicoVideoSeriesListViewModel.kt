package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoSeriesAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSeriesListFragment]で使うViewModel
 *
 * @param userId ユーザーID。nullなら自分のを取ってきます
 * */
class NicoVideoSeriesListViewModel(application: Application, val userId: String?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** シリーズ一覧を送るLiveData */
    val nicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoSeriesData>>()

    /** 読み込み中通知LiveData */
    val loadingLiveData = MutableLiveData(false)

    init {
        getSeriesList()
    }

    /** シリーズ一覧を取得する */
    fun getSeriesList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val seriesAPI = NicoVideoSeriesAPI()
            loadingLiveData.postValue(true)
            val response = seriesAPI.getSeriesList(userSession, userId)
            // 失敗時
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            // ぱーす
            nicoVideoDataListLiveData.postValue(seriesAPI.parseSeriesList(response.body?.string()))
            loadingLiveData.postValue(false)
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    /** Toastを表示する */
    private fun showToast(s: String) {
        viewModelScope.launch {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}