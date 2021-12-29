package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoRepoViewModel

/**
 * [NicoRepoViewModel]を初期化するくらす
 * */
class NicoRepoViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoRepoViewModel(application, userId) as T
    }

}