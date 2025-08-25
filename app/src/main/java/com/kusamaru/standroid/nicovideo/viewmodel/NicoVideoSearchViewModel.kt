package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nguploader.NGUploaderTool
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSearchHTML
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSearchHTMLV2
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.room.entity.SearchHistoryDBEntity
import com.kusamaru.standroid.room.init.SearchHistoryDBInit
import kotlinx.coroutines.*

/**
 * [com.kusamaru.standroid.nicovideo.fragment.NicoVideoSearchFragment]で使うViewModel
 *
 * なんかきれいに書けなかった
 * */
class NicoVideoSearchViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = application.applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ニコ動検索とスクレイピング */
    private val searchHTML = NicoVideoSearchHTMLV2()

    /** 検索履歴DBのDAO */
    private val searchHistoryDAO = SearchHistoryDBInit.getInstance(context).searchHistoryDAO()

    /** 検索結果を送信するLiveData */
    val searchResultNicoVideoDataListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 関連タグを送信するLiveData */
    val searchResultTagListLiveData = MutableLiveData<List<String>>()

    /** 読み込み中LiveData */
    val isLoadingLiveData = MutableLiveData(false)

    /** サジェストを送信するLiveData */
    val suggestListLiveData = MutableLiveData<List<String>>()

    /** 終了（動画がもう取れない）ならtrue */
    var isEnd = false

    /** コルーチンキャンセル用 */
    private val coroutineJob = Job()

    /** 現在の位置 */
    var currentSearchPage = 1
        private set

    /** 検索ワード */
    var currentSearchWord: String? = null
        private set

    /** 並び順 */
    var currentSearchSortName: String? = null
        private set

    /** タグ検索かどうか */
    var currentSearchIsTagSearch: Boolean? = null
        private set

    /** サジェストAPIを叩きすぎないよう */
    private var prevSuggestText = ""

    /**
     * 検索する関数
     *
     * [searchText]と[sortName]と[isTagSearch]が前回の検索と同じ時のみ、[page]引数が適用されます。
     *
     * 前回と検索内容が違う場合は、[page]（[currentSearchPage]）は1にリセットされます
     *
     * @param searchText 検索ワード。なお文字列０([String.isEmpty])ならこの関数は終了します
     * @param page ページ（上の説明見て）
     * @param isTagSearch タグ検索の場合はtrue。キーワード検索ならfalse
     * @param sortName 並び順。入れられる文字は[NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER]の配列を参照
     * */
    fun search(searchText: String, page: Int = 1, isTagSearch: Boolean = true, sortName: String = NicoVideoSearchHTMLV2.NICOVIDEO_SEARCH_ORDER[0]) {

        if (searchText.isEmpty()) return

        // すでに検索してたならキャンセル
        coroutineJob.cancelChildren()
        // くるくる
        isLoadingLiveData.postValue(true)

        // 検索ワードが切り替わった時
        if (searchText != currentSearchWord && currentSearchSortName != sortName && currentSearchIsTagSearch != isTagSearch) {
            // ページを1にする
            currentSearchPage = 1
        } else {
            // 引数に従う
            currentSearchPage = page
        }

        // 控える
        currentSearchWord = searchText
        currentSearchSortName = sortName
        currentSearchIsTagSearch = isTagSearch

        // 例外処理。コルーチン内で例外出るとここに来るようになるらしい。あたまいい
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}${throwable}")
            isLoadingLiveData.postValue(false)
        }
        viewModelScope.launch(errorHandler + coroutineJob) {
            // ソート条件生成
            val sort = searchHTML.makeSortOrder(currentSearchSortName!!)
            // タグかキーワードか
            val tagOrKeyword = if (currentSearchIsTagSearch!!) "tag" else "search"

            // 検索結果html取りに行く
            val html = withContext(Dispatchers.Default) {
                val response = searchHTML.getHTML(
                    userSession = userSession,
                    searchText = currentSearchWord!!,
                    tagOrSearch = tagOrKeyword,
                    sort = sort.first,
                    order = sort.second,
                    page = currentSearchPage.toString()
                )
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${context.getString(R.string.error)}\n${response.code}")
                    // もう読み込まない
                    isEnd = response.code == 404 // 404でもうページが存在しない
                    isLoadingLiveData.postValue(false)
                    return@withContext null
                }
                response.body?.string()
            } ?: return@launch // nullなら終了

            // スクレイピングしてLiveDataへ送信
            searchResultTagListLiveData.postValue(searchHTML.parseTag(html))

            // NG投稿者機能（ベータ）
            val searchResultVideoList = filterNGUploaderUser(searchHTML.parseHTML(html)) as ArrayList

            // ページが2ページ以降の場合はこれまでの検索結果を保持する
            if (currentSearchPage >= 2) {
                // 保持する設定。次のページ機能で使う
                searchResultNicoVideoDataListLiveData.value!!.addAll(searchResultVideoList)
                searchResultNicoVideoDataListLiveData.postValue(searchResultNicoVideoDataListLiveData.value!!)
            } else {
                // 1ページ目
                searchResultNicoVideoDataListLiveData.postValue(searchResultVideoList)
                // データベースに追加
                insertSearchHistoryDB(searchText, isTagSearch, sortName)
            }
            // くるくるもどす
            isLoadingLiveData.postValue(false)
        }
    }

    /** 次のページのデータをリクエスト */
    fun getNextPage() {
        if (currentSearchWord != null && currentSearchIsTagSearch != null && currentSearchSortName != null) {
            if (!isEnd) {
                currentSearchPage++
                search(currentSearchWord!!, currentSearchPage, currentSearchIsTagSearch!!, currentSearchSortName!!)
            }
        }
    }

    /**
     * 検索履歴のデータベースに追加する
     *
     * @param searchText 検索ワード
     * @param isTagSearch タグ検索ならtrue
     * @param sortName [NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER]を参照
     * */
    private fun insertSearchHistoryDB(searchText: String, isTagSearch: Boolean, sortName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // すでにあれば上書き。なければ追加
            val alreadyHistory = searchHistoryDAO.getHistoryEntity(searchText)
            if (alreadyHistory != null) {
                val updateEntity = alreadyHistory.copy(
                    sort = sortName,
                    text = searchText,
                    isTagSearch = isTagSearch,
                    addTime = System.currentTimeMillis(),
                )
                searchHistoryDAO.update(updateEntity)
            } else {
                val insertHistory = SearchHistoryDBEntity(
                    pin = false,
                    addTime = System.currentTimeMillis(),
                    description = "",
                    service = "video",
                    sort = sortName,
                    text = searchText,
                    isTagSearch = isTagSearch,
                )
                searchHistoryDAO.insert(insertHistory)
            }
        }
    }

    /**
     * サジェストAPIを叩く
     * */
    fun getSuggest(searchText: String) {
        // 叩きすぎないよう
        if (prevSuggestText != searchText && searchText.isNotEmpty()) {
            prevSuggestText = searchText
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${context.getString(R.string.error)}${throwable}")
            }
            viewModelScope.launch(Dispatchers.Default + errorHandler) {
                val response = searchHTML.getSearchSuggest(userSession, searchText)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${context.getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                val suggestList = searchHTML.parseSearchSuggest(response.body?.string())
                suggestListLiveData.postValue(suggestList)
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * NG機能
     *
     * ただし、検索結果のスクレイピングでは投稿者IDまでは取れないので、別の手を打つ必要がある
     *
     * - スマホ版サイトをスクレイピングする
     *     - はいスマホ規制
     * - 検索で返ってきた動画を一個ずつ、動画情報取得APIを叩いて投稿者情報を手に入れる
     *     - さすがにない。無駄な通信
     * - 予めNGにした投稿者の投稿動画の動画IDを控えておいて、控えたID一覧に一致しない動画のみを表示する
     *     - 更新めんどいけどこれがベストプラクティス？1
     *
     * */
    private suspend fun filterNGUploaderUser(list: List<NicoVideoData>): List<NicoVideoData> = withContext(Dispatchers.Default) {
        // staticな関数になってる
        NGUploaderTool.filterNGUploaderVideoId(context, list)
    }

}