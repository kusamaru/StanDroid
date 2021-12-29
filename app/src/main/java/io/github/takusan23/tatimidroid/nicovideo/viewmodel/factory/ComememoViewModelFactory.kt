package io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.ComememoViewModel

/**
 * コメメモBottomFragmentで使うViewModelを用意するためのクラス
 * */
class ComememoViewModelFactory(val application: Application, private val playerImageFilePath: String, private val commentImageFilePath: String, private val drawTextList: List<String>? = null, private val fileName: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ComememoViewModel(application, playerImageFilePath, commentImageFilePath, drawTextList, fileName) as T
    }

}