package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import io.github.takusan23.tatimidroid.nicoapi.cache.CacheFilterDataClass
import io.github.takusan23.tatimidroid.nicoapi.cache.CacheJSON
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoCacheFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoCacheFragmentViewModel
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoCacheFilterBinding


/**
 * キャッシュ一覧でフィルターを書けるときに使うBottomSheet。
 * もうスクロールしまくるのは嫌なんや；；
 * */
class NicoVideoCacheFilterBottomFragment : BottomSheetDialogFragment() {

    companion object {
        val CACHE_FILTER_SORT_LIST =
            arrayListOf("取得日時が新しい順", "取得日時が古い順", "再生の多い順", "再生の少ない順", "投稿日時が新しい順", "投稿日時が古い順", "再生時間の長い順", "再生時間の短い順", "コメントの多い順", "コメントの少ない順", "マイリスト数の多い順", "マイリスト数の少ない順")
    }

    /** [NicoVideoCacheFragment] */
    private val nicoVideoCacheFragment by lazy { requireParentFragment() as NicoVideoCacheFragment }

    /** [NicoVideoCacheFragmentViewModel]のViewModel */
    private val viewModel by viewModels<NicoVideoCacheFragmentViewModel>({ requireParentFragment() })

    private var uploaderNameList = arrayListOf<String>()

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoCacheFilterBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.cacheVideoList.observe(viewLifecycleOwner) {
            // 部分一致検索
            initContainsSearch()
            // 投稿者ソート
            initUploaderSort(it)
            // 新しい順とかソート機能
            initSortSpinner()
            // タグSpinner
            initTagSpinner(it)
            // スイッチ
            initSwitch()
            // リセット
            initResetButton()

            // JSONファイル（filter.json）読み込む
            readJSON()
            filter()
        }

    }

    // 投稿者ソート
    private fun initUploaderSort(cacheList: ArrayList<NicoVideoData>) {
        // RecyclerViewのNicoVideoDataの中から投稿者の配列を取る
        val nameList = cacheList.map { nicoVideoData ->
            nicoVideoData.uploaderName
        }.toList().distinct()
        nameList.forEach {
            if (it != null) {
                uploaderNameList.add(it)
            }
        }
        // Adapter作成
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_list_item_1, uploaderNameList)
        viewBinding.bottomFragmentCacheFilterUploaderAutocompleteTextView.setAdapter(adapter)
        viewBinding.bottomFragmentCacheFilterUploaderAutocompleteTextView.addTextChangedListener {
            filter()
        }
        viewBinding.bottomFragmentCacheFilterUploaderClearImageView.setOnClickListener {
            viewBinding.bottomFragmentCacheFilterUploaderAutocompleteTextView.setText("")
            filter()
        }
    }

    // タグのSpinner
    private fun initTagSpinner(cacheList: ArrayList<NicoVideoData>) {
        // RecyclerViewのNicoVideoDataの中からまずタグの配列を取り出す
        val tagVideoList = cacheList.map { nicoVideoData ->
            nicoVideoData.videoTag ?: arrayListOf()
        }
        // 全ての動画のタグを一つの配列にしてまとめる。そして被りを消してアルファベット順？
        val tagList = tagVideoList.flatten().distinct().sorted()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, tagList)
        viewBinding.bottomFragmentCacheFilterTagAutocomplete.setAdapter(adapter)
        viewBinding.bottomFragmentCacheFilterTagAutocomplete.setOnItemClickListener { adapterView, view, i, l ->
            // Chip追加
            val chip = Chip(context).apply {
                text = tagList[i]
                isCloseIconVisible = true // 閉じる追加
                setOnCloseIconClickListener {
                    viewBinding.bottomFragmentCacheFilterTagChipGroup.removeView(it)
                    filter()
                }
            }
            viewBinding.bottomFragmentCacheFilterTagChipGroup.addView(chip)
            viewBinding.bottomFragmentCacheFilterTagAutocomplete.setText("", true)
            filter()
        }
    }

    // スイッチ初期化
    private fun initSwitch() {
        viewBinding.bottomFragmentCacheFilterHasVideoJsonSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            filter()
        }
    }

    // 並び替え初期化。Spinnerって言うらしいよ。SpiCaではない。
    private fun initSortSpinner() {
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_list_item_1, CACHE_FILTER_SORT_LIST)
        viewBinding.bottomFragmentCacheFilterDropdownTextView.setAdapter(adapter)
        viewBinding.bottomFragmentCacheFilterDropdownTextView.setText(CACHE_FILTER_SORT_LIST[0], false)
        // 文字変更イベント
        viewBinding.bottomFragmentCacheFilterDropdownTextView.addTextChangedListener {
            filter()
        }
    }

    // 部分一致検索
    private fun initContainsSearch() {
        viewBinding.bottomFragmentCacheFilterTitleEditText.addTextChangedListener {
            filter()
        }
    }

    // リセットボタン
    private fun initResetButton() {
        viewBinding.bottomFragmentCacheFilterResetButton.setOnClickListener {
            dismiss()
            nicoVideoCacheFragment.filterDeleteMessageShow() // 本当に消していい？
        }
    }

    // 何かフィルター変更したらこれを呼べば解決！
    fun filter() {

        // 値をCacheFilterDataClassへ
        // 指定中のタグの名前配列
        val filterChipNameList = viewBinding.bottomFragmentCacheFilterTagChipGroup.children.map { view ->
            (view as Chip).text.toString()
        }.toList()
        // JSON化する
        val cacheJson = CacheJSON()
        val cacheFilter = CacheFilterDataClass(
            viewBinding.bottomFragmentCacheFilterTitleEditText.text.toString(),
            viewBinding.bottomFragmentCacheFilterUploaderAutocompleteTextView.text.toString(),
            filterChipNameList,
            viewBinding.bottomFragmentCacheFilterDropdownTextView.text.toString(),
            viewBinding.bottomFragmentCacheFilterHasVideoJsonSwitch.isChecked
        )
        // 保存
        cacheJson.saveJSON(context, cacheJson.createJSON(cacheFilter))

        // リスト操作
        viewModel.applyFilter()
    }

    // JSON取得
    private fun readJSON() {
        // filter.json取得
        val cacheJson = CacheJSON()
        val data = cacheJson.readJSON(context)
        data?.apply {
            viewBinding.bottomFragmentCacheFilterTitleEditText.setText(data.titleContains)
            viewBinding.bottomFragmentCacheFilterUploaderAutocompleteTextView.setText(data.uploaderName)
            viewBinding.bottomFragmentCacheFilterDropdownTextView.setText(data.sort)
            viewBinding.bottomFragmentCacheFilterHasVideoJsonSwitch.isChecked = data.isTatimiDroidGetCache
            // タグ
            data.tagItems.forEach {
                // Chip追加
                val chip = Chip(context).apply {
                    text = it
                    isCloseIconVisible = true // 閉じる追加
                    setOnCloseIconClickListener {
                        viewBinding.bottomFragmentCacheFilterTagChipGroup.removeView(it)
                        filter()
                    }
                }
                viewBinding.bottomFragmentCacheFilterTagChipGroup.addView(chip)
            }
        }
    }

    private fun sort(list: ArrayList<NicoVideoData>, position: Int) {
        // 選択
        when (position) {
            0 -> list.sortByDescending { nicoVideoData -> nicoVideoData.cacheAddedDate }
            1 -> list.sortBy { nicoVideoData -> nicoVideoData.cacheAddedDate }
            2 -> list.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> list.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> list.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> list.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> list.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> list.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> list.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> list.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> list.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> list.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
    }


}