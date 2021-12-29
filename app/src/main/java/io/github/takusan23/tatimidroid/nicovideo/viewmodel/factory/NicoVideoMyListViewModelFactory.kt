package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoMyListViewModel

/**
 * [NicoVideoMyListViewModel]を初期化する関数
 *
 * @param userId ない場合は自分のマイリストを取りに行きます
 * */
class NicoVideoMyListViewModelFactory (val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoMyListViewModel(application, userId) as T
    }

}