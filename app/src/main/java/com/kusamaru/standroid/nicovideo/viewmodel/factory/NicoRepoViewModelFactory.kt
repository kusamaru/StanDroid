package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoRepoViewModel

/**
 * [NicoRepoViewModel]を初期化するくらす
 * */
class NicoRepoViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NicoRepoViewModel(application, userId) as T
    }

}