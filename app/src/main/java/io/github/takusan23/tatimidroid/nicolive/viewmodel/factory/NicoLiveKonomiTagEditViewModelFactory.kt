package io.github.takusan23.tatimidroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveKonomiTagEditViewModel

/**
 * 好みタグ編集ViewModelを初期化する
 * */
class NicoLiveKonomiTagEditViewModelFactory(val application: Application, val broadCasterUserId: String? = null) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveKonomiTagEditViewModel(application, broadCasterUserId) as T
    }

}