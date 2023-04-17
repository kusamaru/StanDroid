package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSeriesAPI
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * シリーズの動画一覧Fragment[com.kusamaru.standroid.nicovideo.fragment.NicoVideoSeriesFragment]で使うViewModel
 *
 * @param seriesId シリーズのID
 * */
class NicoVideoSeriesViewModel(application: Application, private val seriesId: String) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** 動画一覧を送るLiveData */
    val nicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 読み込み中通知LiveData */
    val loadingLiveData = MutableLiveData(false)

    init {
        getSeriesVideoList()
    }

    /**
     * 指定したシリーズの動画一覧を取得する
     * */
    fun getSeriesVideoList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val seriesAPI = NicoVideoSeriesAPI()
            loadingLiveData.postValue(true)
            val response = seriesAPI.getSeriesVideoList(userSession, seriesId)
            response?.let {
                nicoVideoDataListLiveData.postValue(seriesAPI.parseSeriesVideoList(it))
                loadingLiveData.postValue(false)
            }
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