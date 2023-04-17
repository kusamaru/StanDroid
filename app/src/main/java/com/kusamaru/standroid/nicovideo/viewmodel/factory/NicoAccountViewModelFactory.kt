package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoAccountViewModel

/**
 * [NicoAccountViewModel]を初期化するクラス
 * */
class NicoAccountViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NicoAccountViewModel(application, userId) as T
    }

}