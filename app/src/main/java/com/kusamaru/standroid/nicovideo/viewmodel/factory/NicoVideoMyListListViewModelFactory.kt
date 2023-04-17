package com.kusamaru.standroid.nicovideo.viewmodel.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoMyListListViewModel

/**
 * [NicoVideoMyListListViewModel]を初期化するくらす
 *
 * @param isMe 自分の情報を取得する場合はtrue
 * @param myListId マイリストID
 * */
class NicoVideoMyListListViewModelFactory(val application: Application, val myListId: String, val isMe: Boolean) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NicoVideoMyListListViewModel(application, myListId, isMe) as T
    }

}