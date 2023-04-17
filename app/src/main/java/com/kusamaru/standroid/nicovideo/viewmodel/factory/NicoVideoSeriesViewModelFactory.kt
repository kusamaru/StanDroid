package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoSeriesViewModel

/**
 * [NicoVideoSeriesViewModel]を初期化するクラス
 *
 * @param seriesId シリーズID
 * */
class NicoVideoSeriesViewModelFactory(val application: Application, val seriesId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NicoVideoSeriesViewModel(application, seriesId) as T
    }

}