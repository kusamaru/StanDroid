package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSPMyListAPI
import com.kusamaru.standroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * マイリストの動画を表示するFragment[com.kusamaru.standroid.nicovideo.fragment.NicoVideoMyListListFragment]で使うViewModel
 *
 * @param isMe 自分のマイリスト動画を取得する場合はtrue。なんかAPIが違う？らしいので
 * @param myListId マイリストID。空っぽにするととりあえずマイリストを取りに行きます。
 * */
class NicoVideoMyListListViewModel(application: Application, private val myListId: String, private val isMe: Boolean) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** 動画一覧を送るLiveData */
    val nicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 読み込みLiveData */
    val loadingLiveData = MutableLiveData(false)

    /** APIの結果。そのままバージョン */
    var nicoVideoDataListRaw = arrayListOf<NicoVideoData>()

    init {
        getMyListVideoList()
    }

    /**
     * マイリストの中身を取得する
     * */
    fun getMyListVideoList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            loadingLiveData.postValue(true)
            // API叩く
            val spMyListAPI = NicoVideoSPMyListAPI()
            val response = when {
                myListId.isEmpty() -> {
                    // とりあえずマイリスト
                    spMyListAPI.getToriaezuMyListList(userSession)
                }
                isMe -> {
                    // わたし
                    spMyListAPI.getMyListItems(myListId, userSession)
                }
                else -> {
                    // ほかのひと
                    spMyListAPI.getOtherUserMyListItems(myListId, userSession)
                }
            }
            // 失敗時
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            val responseString = response.body?.string()
            // パース
            nicoVideoDataListRaw = withContext(Dispatchers.Default) {
                if (isMe) {
                    // わたし
                    spMyListAPI.parseMyListItems(myListId, responseString)
                } else {
                    // 他の人
                    spMyListAPI.parseOtherUserMyListJSON(responseString)
                }
            }
            // ソート
            sort(0)
            // LiveData送信
            nicoVideoDataListLiveData.postValue(nicoVideoDataListRaw)
            loadingLiveData.postValue(false)
        }
    }

    /**
     * ソートする
     * @param sortNumber [com.kusamaru.standroid.nicovideo.fragment.NicoVideoMyListListFragment.initSortMenu]参照。
     * */
    fun sort(sortNumber: Int) {
        // 選択
        when (sortNumber) {
            0 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.mylistAddedDate }
            1 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.mylistAddedDate }
            2 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> nicoVideoDataListRaw.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> nicoVideoDataListRaw.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
        nicoVideoDataListLiveData.postValue(nicoVideoDataListRaw)
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    private fun showToast(s: String) {
        viewModelScope.launch {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}