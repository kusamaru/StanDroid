package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.jk.NicoLiveJKHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment.Companion.NICOLIVE_JK_PROGRAMLIST_OFFICIAL
import io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment.Companion.NICOLIVE_JK_PROGRAMLIST_TAG
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment]で使うViewModel
 *
 * @param type 公式で用意しているやつの場合は[NICOLIVE_JK_PROGRAMLIST_OFFICIAL]。ユーザーが有志で作ったやつは[NICOLIVE_JK_PROGRAMLIST_TAG]
 * */
class NicoLiveJKProgramListViewModel(application: Application, private val type: String) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** Preference */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "")!!

    /** スクレイピング */
    private val nicoLiveJKHTML = NicoLiveJKHTML()

    /** 読み込み中LiveData */
    val isLoadingLiveData = MutableLiveData(false)

    /** 番組の一覧を送信するLiveData */
    val programListLiveData = MutableLiveData<List<NicoLiveProgramData>>()

    init {
        getProgramList()
    }

    /**
     * 番組を取得する
     *
     * ニコニコが用意している（公式）番組を返すか、ニコニコ実況タグの付いた有志の番組を返すかは[type]の値次第
     * */
    fun getProgramList() {
        isLoadingLiveData.postValue(true)

        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.Default) {
            // HTMLリクエスト
            val response = nicoLiveJKHTML.getNicoLiveJKProgramList(userSession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // LiveDataへ送信
            if (type == NICOLIVE_JK_PROGRAMLIST_OFFICIAL) {
                // ニコニコ公式
                programListLiveData.postValue(nicoLiveJKHTML.parseNicoLiveJKProgramList(response.body?.string()))
            } else {
                // ユーザー有志
                programListLiveData.postValue(nicoLiveJKHTML.parseNicoLiveJKTagProgramList(response.body?.string()))
            }
            isLoadingLiveData.postValue(false)
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