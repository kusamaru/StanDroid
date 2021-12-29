package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.nicoapi.cache.CacheJSON
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicoapi.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.launch
import okhttp3.internal.format

/**
 * キャッシュFragment（[io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoCacheFragment]）で使うViewModel
 *
 * 画面回転時に再読み込みをしないためのViewModel
 *
 * */
class NicoVideoCacheFragmentViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** キャッシュ関連 */
    val nicoVideoCache = NicoVideoCache(context)

    /** キャッシュ一覧。これはフィルターする前 */
    val cacheVideoList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** RecyclerViewにわたす配列。フィルターした後 */
    val recyclerViewList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** キャッシュ利用合計容量 */
    val totalUsedStorageGB = MutableLiveData<String>()

    /** 保存先LiveData */
    val storageMessage = MutableLiveData<String>()

    init {
        init()
    }

    /** 読み込みしたい時に使って */
    fun init() {
        viewModelScope.launch {
            val list = arrayListOf<NicoVideoData>()
            nicoVideoCache.loadCache().forEach {
                list.add(it)
            }
            cacheVideoList.value = list
            recyclerViewList.value = list
            // フィルター適用する
            applyFilter()
            // 保存先をLiveDataで送信する
            sendStorageMessage()
            // 合計サイズ
            initStorageSpace()
        }
    }

    /** 保存先メッセージをLiveDataで送信する */
    private fun sendStorageMessage() {
        storageMessage.value = if (nicoVideoCache.isEnableUseSDCard() && nicoVideoCache.canUseSDCard()) {
            // SDカードを利用 で SDカードが利用可能
            context.getString(R.string.nicovideo_cache_storage_sd_card)
        } else {
            context.getString(R.string.nicovideo_cache_storage_device)
        }
    }

    /** フィルターを適用する */
    fun applyFilter() {
        val filter = CacheJSON().readJSON(context)
        if (filter != null && cacheVideoList.value != null) {
            val list = nicoVideoCache.getCacheFilterList(cacheVideoList.value!!, filter)
            // LiveData更新
            recyclerViewList.postValue(list)
        }
    }

    /** 合計容量を計算する */
    private fun initStorageSpace() {
        val byte = nicoVideoCache.cacheTotalSize.toFloat()
        val gbyte = byte / 1024 / 1024 / 1024 // Byte -> KB -> MB -> GB
        totalUsedStorageGB.postValue(format("%.1f", gbyte))
    }


}
