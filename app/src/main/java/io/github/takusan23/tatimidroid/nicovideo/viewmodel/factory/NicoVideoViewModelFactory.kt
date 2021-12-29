package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel

/**
 * [NicoVideoViewModel]は動画IDを引数に欲しいので独自のファクトリークラス？を作成する
 * */
class NicoVideoViewModelFactory(val application: Application, val videoId: String?, val isCache: Boolean?, val isEco: Boolean, val useInternet: Boolean, val startFullScreen: Boolean, val videoList: ArrayList<NicoVideoData>? = null, val startPos: Int?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoViewModel(application, videoId, isCache, isEco, useInternet, startFullScreen, videoList, startPos) as T
    }

}