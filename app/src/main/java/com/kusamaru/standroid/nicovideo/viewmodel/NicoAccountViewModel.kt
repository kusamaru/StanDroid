package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.user.UserAPI
import com.kusamaru.standroid.nicoapi.user.UserData
import com.kusamaru.standroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [com.kusamaru.standroid.nicovideo.NicoAccountFragment]で使うViewModel
 *
 * @param userId ユーザーID。いつの間にか1億ID突破してた。nullを入れると自分のアカウント情報を取りに行きます。
 * */
class NicoAccountViewModel(application: Application, val userId: String?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ユーザー情報を送るLiveData */
    val userDataLiveData = MutableLiveData<UserData>()

    /** フォロー状態を返すLiveData */
    val followStatusLiveData = MutableLiveData<Boolean>()

    init {
        getUserData()
    }

    /**
     * ユーザーデータを取得する
     * */
    fun getUserData() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val userData = withContext(Dispatchers.IO) {
                val userAPI = UserAPI()
                // userIdがnullなら自分の情報を取りに行く
                val response = userAPI.getUserData(userSession, userId)
                userAPI.parseUserData(response.body?.string())
            }
            userDataLiveData.value = userData
            followStatusLiveData.value = userData.isFollowing
        }
    }

    /**
     * フォロー、フォロー済みならフォロー解除APIを叩く関数
     * */
    fun postFollowRequest() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val isSuccessful = withContext(Dispatchers.IO) {
                val userAPI = UserAPI()
                val response = if (userDataLiveData.value?.isFollowing == true) {
                    // フォロー中なので解除
                    userAPI.postRemoveUserFollow(userSession, userId!!)
                } else {
                    // フォローする！ついてくぞ
                    userAPI.postUserFollow(userSession, userId!!)
                }
                userAPI.isSuccessfulFollowRequest(response.body?.string())
            }
            followStatusLiveData.value = isSuccessful
            // 再度ユーザー情報取得
            getUserData()
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

}