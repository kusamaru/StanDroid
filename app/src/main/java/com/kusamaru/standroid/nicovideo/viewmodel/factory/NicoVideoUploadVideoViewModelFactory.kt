package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoUploadVideoViewModel

/**
 * [NicoVideoUploadVideoViewModel]を初期化するくらす
 * */
class NicoVideoUploadVideoViewModelFactory(val application: Application, val userId: String?) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoUploadVideoViewModel(application, userId) as T
    }

}