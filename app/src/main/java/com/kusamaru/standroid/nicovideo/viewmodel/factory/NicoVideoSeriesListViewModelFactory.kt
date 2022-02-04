package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoSeriesListViewModel

/**
 * [NicoVideoSeriesListViewModel]を初期化するクラス
 * */
class NicoVideoSeriesListViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoSeriesListViewModel(application, userId) as T
    }

}