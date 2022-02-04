package com.kusamaru.standroid.nicoad.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdAPI
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdData
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdHistoryUserData
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdRankingUserData
import com.kusamaru.standroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [NicoAdBottomFragment]で使うViewModel。
 *
 * [NicoAdViewModelFactory]を利用してください
 * */
class NicoAdViewModel(application: Application, val contentId: String) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** Preference */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "")!!

    /** ニコニ広告API */
    private val nicoAdAPI = NicoAdAPI()

    /** ニコニ広告の結果を送信するLiveData */
    val nicoAdDataLiveData = MutableLiveData<NicoAdData>()

    /** ニコニ広告の貢献度ランキングを送信するLiveData */
    val nicoAdRankingLiveData = MutableLiveData<ArrayList<NicoAdRankingUserData>>()

    /** ニコニ広告の広告履歴を送信するLiveData */
    val nicoAdHistoryLiveData = MutableLiveData<ArrayList<NicoAdHistoryUserData>>()

    /** 動画 or 生放送 */
    private val isLive = contentId.contains("lv")

    /** [com.kusamaru.standroid.nicoapi.nicoad.NicoAdAPI]で使う動画か生放送かのやつ */
    private val type = if (isLive) NicoAdAPI.NICOAD_API_LIVE else NicoAdAPI.NICOAD_API_VIDEO

    init {
        // データ取得関数を呼ぶ
        getNicoAd()
    }

    /**
     * ニコニ広告API一式叩く
     * */
    fun getNicoAd() {
        getNicoAdData()
        getNicoAdHistory()
        getNicoAdRanking()
    }

    /**
     * ニコニ広告APIを叩く。
     * 結果は[nicoAdLiveData]へ送信されます
     * */
    fun getNicoAdData() {
        viewModelScope.launch {
            val response = nicoAdAPI.getNicoAd(userSession, contentId, type)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // 結果を送信
            val data = withContext(Dispatchers.Default) { nicoAdAPI.parseNicoAd(response.body?.string()) }
            nicoAdDataLiveData.postValue(data)
        }
    }

    /**
     * ニコニ広告の貢献度APIを叩く。
     * 結果は[nicoAdRankingLiveData]へ送信されます
     * */
    fun getNicoAdRanking() {
        viewModelScope.launch {
            val response = nicoAdAPI.getNicoAdRanking(userSession, contentId, type)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // 結果を送信
            val data = withContext(Dispatchers.Default) { nicoAdAPI.parseNicoAdRanking(response.body?.string()) }
            nicoAdRankingLiveData.postValue(data)
        }
    }

    /**
     * ニコニ広告の貢献者履歴APIを叩く
     * 結果は[nicoAdHistoryLiveData]へ送信されます
     * */
    fun getNicoAdHistory() {
        viewModelScope.launch {
            val response = nicoAdAPI.getNicoAdHistory(userSession, contentId, type)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // 結果を送信
            val data = withContext(Dispatchers.Default) { nicoAdAPI.parseNicoAdHistory(response.body?.string()) }
            nicoAdHistoryLiveData.postValue(data)
        }
    }

    /** Toast表示関数 */
    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }


}