package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoUpload
import com.kusamaru.standroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [com.kusamaru.standroid.nicovideo.fragment.NicoVideoUploadVideoFragment]のViewModel。UI関係ないコードはここ
 *
 * @param userId ユーザーID。nullの場合は自分の投稿動画を取りに行きます。
 * */
class NicoVideoUploadVideoViewModel(application: Application, val userId: String?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** 投稿動画配列を送るLiveData */
    val nicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** もう取れない場合はtrue */
    var isEnd = false

    /** 現在までに取得しているページ数 */
    var pageCount = 1

    /** RecyclerViewの位置。追加取得の時使う */
    var recyclerViewPos = 0

    /** RecyclerViewの位置。追加取得の時使う */
    var recyclerViewYPos = 0

    /** 読み込みが終わったら呼ばれるLiveData */
    val loadingLiveData = MutableLiveData(false)

    init {
        // 投稿動画取得
        getVideoList(pageCount)
    }

    /**
     * 投稿動画取得。
     * @param page ページ数。
     * */
    fun getVideoList(page: Int) {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            pageCount = page
            loadingLiveData.postValue(true)

            // 投稿動画取得API
            val nicoVideoUpload = NicoVideoUpload()
            val response = nicoVideoUpload.getUploadVideo(userId, userSession, page)
            // 失敗時
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            val videoList = nicoVideoUpload.parseUploadVideo(response.body?.string())
            // もう取れない場合はLiveData送信
            isEnd = videoList.isEmpty()
            // 読み込みおしまい
            loadingLiveData.postValue(false)
            nicoVideoDataListLiveData.postValue(videoList)
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