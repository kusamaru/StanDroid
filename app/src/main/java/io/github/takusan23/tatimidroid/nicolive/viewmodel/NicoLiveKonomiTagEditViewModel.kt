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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 好みタグを編集するBottomFragmentで使うViewModel
 *
 * 好みタグから番組を探すFragment(ViewModel)がまた似た名前であるので注意
 *
 * @param broadCasterUserId 放送者のユーザーID。nullの場合は放送者がフォロー中のタグは取得しません
 * */
class NicoLiveKonomiTagEditViewModel(application: Application, private val broadCasterUserId: String? = null) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** APIまとめ */
    private val konomiTagAPI = NicoLiveKonomiTagAPI()

    /** おすすめの好みタグを送信するLiveData */
    val recommendKonomiTagLiveData = MutableLiveData<List<NicoLiveKonomiTagData>>()

    /** 自分がフォローしている好みタグを送信するLiveData */
    val myFollowingKonomiTagLiveData = MutableLiveData<List<NicoLiveKonomiTagData>>()

    /** 放送者がフォローしている好みタグを返すLiveData */
    val broadCasterFollowingKonomiTagLiveData = MutableLiveData<List<NicoLiveKonomiTagData>>()

    /** [getBroadCaterFollowingKonomiTag]が利用可能かどうか */
    val hasBroadCasterUserId = broadCasterUserId != null

    init {
        init()
    }

    /** まとめてAPIを叩く関数 */
    fun init() {
        // 放送者がフォローしてるタグ
        if (hasBroadCasterUserId) {
            getBroadCaterFollowingKonomiTag()
        }
        // とりあえずおすすめの方も叩いておく
        getRecommendKonomiTag()
        // 最後に自分のも
        getMyFollowingKonomiTag()
    }

    /**
     * 自分がフォローしている好みタグを取得する
     * */
    private fun getMyFollowingKonomiTag() {
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
                myFollowingKonomiTagLiveData.postValue(konomiTagAPI.parseMyFollowingTagList(response.body?.string()))
            }
        }
    }

    /**
     * あなたへおすすめの好みタグを表示する
     * */
    fun getRecommendKonomiTag() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.Default) {
            val response = konomiTagAPI.getRecommendKonomiTag(userSession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            recommendKonomiTagLiveData.postValue(konomiTagAPI.parseKonomiTag(response.body?.string()))
        }
    }

    /**
     * 放送者がフォローしている好みタグを表示する
     * */
    fun getBroadCaterFollowingKonomiTag() {
        if (broadCasterUserId != null) {
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                // エラー時
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            viewModelScope.launch(errorHandler) {
                val response = konomiTagAPI.getFollowingTag(userSession, broadCasterUserId)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                broadCasterFollowingKonomiTagLiveData.postValue(konomiTagAPI.parseKonomiTag(response.body?.string()))
            }
        }
    }

    /**
     * 好みタグをフォローする。フォローしたら再度LiveDataに結果が入る
     *
     * @param konomiTagId 好みタグのId
     * */
    fun followKonomiTag(konomiTagId: String) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val response = konomiTagAPI.postFollowTag(userSession, konomiTagId)
            // 成功したらフォロー状態反転
            if (response.isSuccessful) {
                // 面倒だしAPI叩き直すか
                init()
                showToast(getString(R.string.nicolive_konomitag_follow_ok))
            } else {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    /**
     * 好みタグのフォローを外す。フォロー解除したら再度LiveDataに結果が入る
     *
     * @param konomiTagId 好みタグのId
     * */
    fun removeFollowKonomiTag(konomiTagId: String) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー時
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val response = konomiTagAPI.postRemoveFollowTag(userSession, konomiTagId)
            // 成功したらフォロー状態反転
            if (response.isSuccessful) {
                // 面倒だしAPI叩き直すか
                init()
                showToast(getString(R.string.nicolive_konomitag_remove_follow_ok))
            } else {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
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