package io.github.takusan23.tatimidroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveGiftViewModel

/**
 * [NicoLiveGiftViewModel]を初期化するやつ
 * */
class NicoLiveGiftViewModelFactory(val application: Application, val liveId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveGiftViewModel(application, liveId) as T
    }

}