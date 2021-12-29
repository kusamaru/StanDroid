package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSeriesListViewModel

/**
 * [NicoVideoSeriesListViewModel]を初期化するクラス
 * */
class NicoVideoSeriesListViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoSeriesListViewModel(application, userId) as T
    }

}