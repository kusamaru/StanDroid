package com.kusamaru.standroid.nguploader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kusamaru.standroid.nguploader.NGUploaderTool
import com.kusamaru.standroid.room.entity.NGUploaderUserIdEntity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * NG投稿者ID一覧BottomFragmentで使うViewModel
 *
 * ほとんどの処理は[NGUploaderTool]に書いてある
 * */
class NGUploaderViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** NG投稿者のデータベースを扱うクラス */
    val ngUploaderTool = NGUploaderTool(context)

    /** NG投稿者一覧配列が入るLiveData */
    val ngUploaderUserIdListLiveData = MutableLiveData<List<NGUploaderUserIdEntity>>()

    init {
        viewModelScope.launch {
            // NG投稿者データベースを監視する
            ngUploaderTool.getNGUploaderRealTime().collect { ngUploadrUserList ->
                // LiveDataへ
                ngUploaderUserIdListLiveData.postValue(ngUploadrUserList)
            }
        }
    }

    /**
     * NG投稿者として登録する
     *
     * @param userId ユーザーID
     * */
    fun addNGUploaderId(userId: String) {
        viewModelScope.launch { ngUploaderTool.addNGUploaderId(userId) }
    }

    /**
     * NG投稿者を削除する
     *
     * @param userId ユーザーID
     * */
    fun deleteNGUploaderId(userId: String) {
        viewModelScope.launch { ngUploaderTool.deleteNGUploaderId(userId) }
    }
}