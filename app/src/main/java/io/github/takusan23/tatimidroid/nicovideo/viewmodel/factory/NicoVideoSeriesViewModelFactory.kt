package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSeriesViewModel

/**
 * [NicoVideoSeriesViewModel]を初期化するクラス
 *
 * @param seriesId シリーズID
 * */
class NicoVideoSeriesViewModelFactory(val application: Application, val seriesId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NicoVideoSeriesViewModel(application, seriesId) as T
    }

}