package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoAPIX
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoDataClass
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.nicofeed.NicoFeedAPI
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [com.kusamaru.standroid.nicovideo.fragment.NicoVideoNicoRepoFragment]で使うViewModel
 *
 * @param userId nullの場合は自分のデータを取りに行きます。
 * */
class NicoRepoViewModel(application: Application, val userId: String? = null) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /**
     * [com.kusamaru.standroid.nicoapi.NicoRepoAPIX.parseNicoRepoResponseToNicoLiveProgramData]系の結果が入るLiveData。
     * [Any]で型情報がないので、isで型を確認して使ってください。
     *
     * [com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData]か[com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData]です。
     * */
    val nicoRepoDataListLiveData = MutableLiveData<ArrayList<NicoRepoDataClass>>()

    /** とりあえずAPIを叩いてパースした結果を持っておく。このあとフィルターで初期状態の配列がほしいので */
    var nicoRepoDataListRaw = arrayListOf<NicoRepoDataClass>()

    /** 読み込み中LiveData */
    val loadingLiveData = MutableLiveData(false)

    /** 動画を一覧に表示する場合はtrue */
    var isShowVideo = true

    /** 生放送を一覧に表示する場合はtrue */
    var isShowLive = true

    /** 更新用のカーソル */
    var nextCursor: String? = null

    /** 終わり */
    var isEnd = false

    init {
        getNicoRepo()
    }

    /**
     * ニコレポを取得する
     * */
    fun getNicoRepo() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            // API叩く
            loadingLiveData.postValue(true)
//            val nicoRepoAPI = NicoRepoAPIX()
//            val response = nicoRepoAPI.getNicoRepoResponse(userSession, userId)
            val nicoFeedAPI = NicoFeedAPI()
            val response = nicoFeedAPI.getNicoFeedResponse(userSession, nextCursor)
            // 失敗時
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            loadingLiveData.postValue(false)
            val parsedData = nicoFeedAPI.parseNicoFeedResponse(response.body?.string())
            nicoRepoDataListRaw.addAll(parsedData.first)
            if (parsedData.second != null) {
                nextCursor = parsedData.second
            } else {
                isEnd = true
            }
            // フィルター適用とLiveData送信
            filterAndPostLiveData()
        }
    }

    /**
     * [isShowLive]、[isShowVideo]を適用してLiveDataへ送信する
     *
     * チェックが切り替わったら呼べばいいと思います。
     * */
    fun filterAndPostLiveData() {
        // 両方表示ならそのまま
        if (isShowLive && isShowVideo) {
            nicoRepoDataListLiveData.postValue(nicoRepoDataListRaw)
            return
        }
        val filterList = nicoRepoDataListRaw.filter { nicoRepoDataClass ->
            when {
                isShowVideo -> nicoRepoDataClass.isVideo
                isShowLive -> !nicoRepoDataClass.isVideo
                else -> false
            }
        }
        nicoRepoDataListLiveData.postValue(filterList as ArrayList<NicoRepoDataClass>?)
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