package io.github.takusan23.tatimidroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveJKProgramListViewModel

/**
 * [NicoLiveJKProgramListViewModel]を初期化するやつ
 * */
class NicoLiveJKProgramListViewModelFactory(val application: Application, val type: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveJKProgramListViewModel(application, type) as T
    }

}