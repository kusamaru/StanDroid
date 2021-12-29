package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveGiftHistoryUserData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveGiftItemData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveGiftRankingUserData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveGiftAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [io.github.takusan23.tatimidroid.nicolive.bottomfragment.NicoLiveGiftBottomFragment]で使うViewModel
 * */
class NicoLiveGiftViewModel(application: Application, val liveId: String) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** Preference */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "")!!

    /** 投げ銭API */
    private val nicoGiftAPI = NicoLiveGiftAPI()

    /** 投げ銭ランキングデータ送信LiveData */
    val nicoLiveGiftRankingUserListLiveData = MutableLiveData<ArrayList<NicoLiveGiftRankingUserData>>()

    /** 投げ銭履歴データ送信LiveData */
    val nicoLiveGiftHistoryUserListLiveData = MutableLiveData<ArrayList<NicoLiveGiftHistoryUserData>>()

    /** 投げ銭で投げられたアイテム一覧送信LiveData */
    val nicoLiveGiftItemListLiveData = MutableLiveData<ArrayList<NicoLiveGiftItemData>>()

    /** 投げ銭トータルポイント送信LiveData */
    val nicoLiveGiftTotalPointLiveData = MutableLiveData<Int>()

    init {
        getGiftData()
    }

    /**
     * 投げ銭API一式を叩く
     * */
    fun getGiftData() {
        getGiftRanking()
        getGiftHistory()
        getGiftTotalPoint()
        getGiftItemList()
    }

    /**
     * 投げ銭の投げられたアイテム一覧を取得する
     * */
    private fun getGiftItemList() {
        viewModelScope.launch {
            val response = nicoGiftAPI.getGiftItemList(userSession, liveId)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // LiveDataへ送信
            val historyList = withContext(Dispatchers.Default) { nicoGiftAPI.parseGiftItemList(response.body?.string()) }
            nicoLiveGiftItemListLiveData.postValue(historyList)
        }
    }

    /**
     * 投げ銭のトータルポイントを取得する
     * */
    private fun getGiftTotalPoint() {
        viewModelScope.launch {
            val response = nicoGiftAPI.getGiftTotalPoint(userSession, liveId)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // LiveDataへ送信
            val historyList = withContext(Dispatchers.Default) { nicoGiftAPI.parseGiftTotalPoint(response.body?.string()) }
            nicoLiveGiftTotalPointLiveData.postValue(historyList)
        }
    }

    /**
     * 投げ銭の履歴APIを叩く
     * */
    private fun getGiftHistory() {
        viewModelScope.launch {
            val response = nicoGiftAPI.getGiftHistory(userSession, liveId)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // LiveDataへ送信
            val historyList = withContext(Dispatchers.Default) { nicoGiftAPI.parseGiftHistory(response.body?.string()) }
            nicoLiveGiftHistoryUserListLiveData.postValue(historyList)
        }
    }

    /**
     * 投げ銭のランキングAPIを叩く
     * */
    private fun getGiftRanking() {
        viewModelScope.launch {
            val response = nicoGiftAPI.getGiftRanking(userSession, liveId)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // LiveDataへ送信
            val historyList = withContext(Dispatchers.Default) { nicoGiftAPI.parseGiftRanking(response.body?.string()) }
            nicoLiveGiftRankingUserListLiveData.postValue(historyList)
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