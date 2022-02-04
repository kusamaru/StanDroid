package com.kusamaru.standroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveJKProgramListViewModel

/**
 * [NicoLiveJKProgramListViewModel]を初期化するやつ
 * */
class NicoLiveJKProgramListViewModelFactory(val application: Application, val type: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveJKProgramListViewModel(application, type) as T
    }

}