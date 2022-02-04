package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.login.NicoLogin
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoMyListData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSPMyListAPI
import com.kusamaru.standroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [com.kusamaru.standroid.nicovideo.fragment.NicoVideoMyListFragment]で使うViewModel
 *
 * マイリスト一覧を取得する
 * @param userId ない場合は自分のマイリストを取りに行きます
 * */
class NicoVideoMyListViewModel(application: Application, val userId: String? = null) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** マイリスト一覧を送信するLiveData */
    val myListDataLiveData = MutableLiveData<ArrayList<NicoVideoMyListData>>()

    /** 読み込み中LiveData */
    val loadingLiveData = MutableLiveData(false)

    init {
        getMyListList()
    }

    /**
     * マイリスト一覧を取得する
     * */
    fun getMyListList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            loadingLiveData.postValue(true)
            val spMyListAPI = NicoVideoSPMyListAPI()
            val response = if (userId == null) {
                // 自分
                spMyListAPI.getMyListList(userSession)
            } else {
                // 他の人
                spMyListAPI.getMyListList(userSession, userId)
            }
            // 再ログイン必須
            if (response.headers["x-niconico-id"] == null) {
                // ログインする
                val login = NicoLogin.secureNicoLogin(context)
                // ログインした
                if (login != null) {
                    userSession = login
                    showToast(getString(R.string.re_login_successful))
                    // 再試行
                    getMyListList()
                    return@launch
                }
            } else if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            val myListItems = spMyListAPI.parseMyListList(response.body?.string(), userId == null)
            // 並び替え
            if (prefSetting.getBoolean("setting_nicovideo_mylist_sort_itemcount", false)) {
                myListItems.sortByDescending { myListData -> myListData.itemsCount }
            }
            // 自分の場合は先頭にとりあえずマイリスト追加する
            if (userId == null) {
                // とりあえずマイリスト追加
                myListItems.add(0, NicoVideoMyListData(getString(R.string.atodemiru), "", 500, true, true))
            }
            // LiveData送信
            myListDataLiveData.postValue(myListItems)
            loadingLiveData.postValue(false)
        }
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