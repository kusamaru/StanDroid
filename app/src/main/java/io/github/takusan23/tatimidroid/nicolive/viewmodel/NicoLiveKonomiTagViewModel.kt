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
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveKonomiTagAPI
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveKonomiTagData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 好みタグ番組一覧Fragmentで使うViewModel
 * */
class NicoLiveKonomiTagViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** APIまとめ */
    private val konomiTagAPI = NicoLiveKonomiTagAPI()

    /** フォロー中好みタグを返すLiveData */
    val followingKonomiTagListLiveData = MutableLiveData<List<NicoLiveKonomiTagData>>()

    /** 動画一覧を返す */
    val konomiTagProgramListLiveData = MutableLiveData<List<NicoLiveProgramData>>()

    /** 前回検索したタグのID */
    var beforeSearchTagId: String? = null

    init {
        // フォロー中好みタグを取得する
        getMyFollowingKonomiTag()
    }

    /** （自分の）好みタグのフォロー中タグを取得して、LiveDataへ送信する */
    fun getMyFollowingKonomiTag() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val response = konomiTagAPI.searchProgramFromKonomiTag(userSession, null)
            // エラー時
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            withContext(Dispatchers.Default) {
                followingKonomiTagListLiveData.postValue(konomiTagAPI.parseMyFollowingTagList(response.body?.string()))
            }
        }
    }

    /**
     * 好みタグから番組を検索する。結果はLiveDataへ
     *
     * @param konomiTagId 好みタグのId
     * */
    fun searchProgramFromKonomiTag(konomiTagId: String) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val response = konomiTagAPI.searchProgramFromKonomiTag(userSession, konomiTagId)
            // エラー時
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            withContext(Dispatchers.Default) {
                konomiTagProgramListLiveData.postValue(konomiTagAPI.parseSearchProgramList(response.body?.string()))
                beforeSearchTagId = konomiTagId
            }
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