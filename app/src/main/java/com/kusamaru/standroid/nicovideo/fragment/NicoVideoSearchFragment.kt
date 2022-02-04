package com.kusamaru.standroid.nicovideo.fragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentNicovideoSearchBinding
import com.kusamaru.standroid.nguploader.bottomfragment.NGUploaderBottomFragment
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSearchHTML
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.adapter.AllShowDropDownMenuAdapter
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoListAdapter
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoSearchHistoryBottomFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoSearchViewModel
import com.kusamaru.standroid.tool.getThemeColor

/**
 * ニコ動検索Fragment
 * argumentにputString("search","検索したい内容")を入れるとその値を検索します。なおタグ検索、人気の高い順です。
 *
 * search       | String | 検索したい内容
 * search_hide  | Boolean| 検索領域を非表示にする場合はtrue
 * sort_show    | Boolean| 並び替えを初めから表示する場合はtrue。なおタグ/キーワードの変更は出ない
 * */
class NicoVideoSearchFragment : Fragment() {

    // RecyclerView
    private val nicoVideoList = arrayListOf<NicoVideoData>()
    private val nicoVideoListAdapter = NicoVideoListAdapter(nicoVideoList)

    // RecyclerView位置
    var position = 0
    var yPos = 0

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoSearchViewModel>()

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoSearchBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        viewBinding.fragmentNicovideoSearchAppBar.background = ColorDrawable(getThemeColor(requireContext()))

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        // 検索結果LiveData
        viewModel.searchResultNicoVideoDataListLiveData.observe(viewLifecycleOwner) { list ->
            nicoVideoList.clear()
            nicoVideoList.addAll(list)
            nicoVideoListAdapter.notifyDataSetChanged()
            // スクロール位置復元
            viewBinding.fragmentNicovideoSearchRecyclerView.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, yPos)
            }
            // 検索ワードなどをTextViewへ入れる
            viewBinding.fragmentNicovideoSearchInput.setText(viewModel.currentSearchWord ?: "")
            viewBinding.fragmentNicovideoSearchSortChip.text = viewModel.currentSearchSortName ?: ""
            viewBinding.fragmentNicovideoSearchToggleGroup.check(if (viewModel.currentSearchIsTagSearch == true) R.id.fragment_nicovideo_search_tag_button else R.id.fragment_nicovideo_search_word_button)
        }

        // 検索結果タグ配列
        viewModel.searchResultTagListLiveData.observe(viewLifecycleOwner) { tagList ->
            // 空っぽにする
            viewBinding.fragmentNicovideoSearchTagsChipLinearLayout.removeAllViews()
            // 入れていく
            tagList.forEach { tagName ->
                val chip = (layoutInflater.inflate(R.layout.include_chip, viewBinding.fragmentNicovideoSearchTagsChipLinearLayout, false) as Chip).apply {
                    text = tagName
                    chipIcon = requireContext().getDrawable(R.drawable.ic_outline_search_24)
                    // 押したら読み込み
                    setOnClickListener {
                        viewBinding.fragmentNicovideoSearchInput.setText(tagName)
                        search()
                    }
                }
                viewBinding.fragmentNicovideoSearchTagsChipLinearLayout.addView(chip)
            }
        }


        // 読み込み中LiveData
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoSearchSwipeRefresh.isRefreshing = isLoading
        }

        // 検索ボタン
        viewBinding.fragmentNicovideoSearchImageView.setOnClickListener {
            search()
        }

        // サジェスト
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_list_item_1, arrayListOf())
        viewBinding.fragmentNicovideoSearchInput.apply {
            threshold = 1
            setAdapter(adapter)
            setOnItemClickListener { parent, view, position, id ->
                // サジェスト押したら検索
                adapter.getItem(position)?.let { viewModel.search(it) }
                // フォーカスを外す
                clearFocus()
            }
        }
        // サジェスト結果を受け取る
        viewModel.suggestListLiveData.observe(viewLifecycleOwner) { suggestList ->
            adapter.clear()
            adapter.addAll(suggestList)
        }

        // サジェスト送信
        viewBinding.fragmentNicovideoSearchInput.addTextChangedListener { text -> viewModel.getSuggest(text.toString()) }

        // argumentの値を使って検索。
        val searchText = arguments?.getString("search")
        if (searchText != null && searchText.isNotEmpty()) {
            viewBinding.fragmentNicovideoSearchInput.setText(searchText)
            search()
        }

        // 非表示オプション（再生中にタグ検索する時に使う）
        val isSearchHide = arguments?.getBoolean("search_hide") ?: false
        if (isSearchHide) {
            (viewBinding.fragmentNicovideoSearchInput.parent as View).visibility = View.GONE
        }

        // 引っ張って更新
        viewBinding.fragmentNicovideoSearchSwipeRefresh.setOnRefreshListener {
            search(1)
        }

        // エンターキー押したら検索実行
        viewBinding.fragmentNicovideoSearchInput.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    search()
                    true
                }
                else -> false
            }
        }

        // たぐ、並び替えメニュー押しても検索できるように
        viewBinding.fragmentNicovideoSearchTagButton.setOnClickListener { search() }
        viewBinding.fragmentNicovideoSearchWordButton.setOnClickListener { search() }

        // NG投稿者機能（仮）
        viewBinding.fragmentNicovideoNgUploaderImageView.setOnClickListener {
            NGUploaderBottomFragment().show(childFragmentManager, "ng_uploader")
        }

        // 検索履歴
        viewBinding.fragmentNicovideoSearchHistoryImageView.setOnClickListener {
            NicoVideoSearchHistoryBottomFragment().show(childFragmentManager, "history")
        }

    }

    /** 検索をする。ViewModelの方も見て */
    fun search(page: Int = 1) {
        // スクロール位置リセット
        if (page == 1) {
            position = 0
            yPos = 0
        }
        viewModel.search(
            searchText = viewBinding.fragmentNicovideoSearchInput.text.toString(),
            page = page,
            isTagSearch = viewBinding.fragmentNicovideoSearchToggleGroup.checkedButtonId == R.id.fragment_nicovideo_search_tag_button,
            sortName = viewBinding.fragmentNicovideoSearchSortChip.text.toString()
        )
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoSearchRecyclerView.apply {
            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(context)
            layoutManager = linearLayoutManager
            adapter = nicoVideoListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = recyclerView.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition()
                    //最後までスクロールしたときの処理
                    if (firstVisibleItem + visibleItemCount == totalItemCount && viewModel.isLoadingLiveData.value == false && !viewModel.isEnd) {
                        // 次のページ検索する
                        viewModel.getNextPage()
                        // スクロール位置保持
                        position = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        yPos = getChildAt(0).top
                    }
                }
            })
        }
    }

    // Spinner初期化
    private fun initDropDownMenu() {
        // ポップアップメニュー
        val menu = PopupMenu(requireContext(), viewBinding.fragmentNicovideoSearchSortChip)
        // 並び替え
        NicoVideoSearchHTML.NICOVIDEO_SEARCH_ORDER.forEach { name -> menu.menu.add(name) }
        viewBinding.fragmentNicovideoSearchSortChip.setOnClickListener {
            // メニュー展開
            menu.show()
        }
        // メニュー押した時
        menu.setOnMenuItemClickListener { item ->
            // Chipへテキスト入れる
            viewBinding.fragmentNicovideoSearchSortChip.text = item.title
            // 再検索
            search()
            false
        }
    }

}