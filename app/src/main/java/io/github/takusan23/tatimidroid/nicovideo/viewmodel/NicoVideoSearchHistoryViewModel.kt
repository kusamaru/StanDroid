package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import io.github.takusan23.tatimidroid.room.init.SearchHistoryDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 検索履歴を表示するBottomFragmentのViewModel
 * */
class NicoVideoSearchHistoryViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** データベース */
    private val searchHistoryDB = SearchHistoryDBInit.getInstance(context)

    /** DAO */
    private val searchHistoryDAO = searchHistoryDB.searchHistoryDAO()

    /** [filter]の結果を送信するLiveData */
    val searchHistoryLiveData = MutableLiveData<List<SearchHistoryDBEntity>>()

    init {
        // データ取得
        filter()
    }

    /**
     * DBから取得してフィルターを通す
     *
     * @param isPinnedOnly ピン留めのみを取得
     * @param isIncludeTagSearch タグ検索を含めるか
     * @param isIncludeKeywordSearch キーワード検索を含めるか
     * */
    fun filter(
        isPinnedOnly: Boolean = false,
        isIncludeTagSearch: Boolean = true,
        isIncludeKeywordSearch: Boolean = true,
    ) {
        // データベースの中身をLiveDataで送信
        viewModelScope.launch(Dispatchers.IO) {
            // 日付の新しい順に並び替え
            var resultHistoryList = searchHistoryDAO.getAll().sortedByDescending { searchHistoryDBEntity -> searchHistoryDBEntity.addTime }
            // ピン留めのみ
            if (isPinnedOnly) {
                resultHistoryList = resultHistoryList.filter { searchHistoryDBEntity -> searchHistoryDBEntity.pin } as ArrayList<SearchHistoryDBEntity>
            }
            // タグ検索のみとか
            resultHistoryList = when {
                isIncludeTagSearch && isIncludeKeywordSearch -> resultHistoryList.filter { true }
                isIncludeTagSearch -> resultHistoryList.filter { it.isTagSearch }
                isIncludeKeywordSearch -> resultHistoryList.filter { !it.isTagSearch }
                else -> resultHistoryList
            } as ArrayList<SearchHistoryDBEntity>
            searchHistoryLiveData.postValue(resultHistoryList)
        }
    }

    /**
     * 検索履歴をピン止め、解除する
     *
     * @param searchHistoryDBEntity 変更対象のデータ
     * @param isPin ピン止めするならtrue。解除するならfalse
     * */
    fun setPin(searchHistoryDBEntity: SearchHistoryDBEntity, isPin: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertData = searchHistoryDBEntity.copy(pin = isPin)
            searchHistoryDAO.update(insertData)
        }
    }

    /** 検索履歴をすべて飛ばす */
    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            searchHistoryDAO.deleteAll()
        }
    }

}