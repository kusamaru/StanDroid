package com.kusamaru.standroid.nicolive.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel

/**
 * [NicoLiveViewModel]は生放送ID（とかいろいろ）を引数にほしいので独自に用意
 * */
class NicoLiveViewModelFactory(val application: Application, val liveId: String, val isLoginMode: Boolean) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoLiveViewModel(application, liveId, isLoginMode) as T
    }

}