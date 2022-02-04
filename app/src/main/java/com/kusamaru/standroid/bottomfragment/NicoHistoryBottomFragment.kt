package com.kusamaru.standroid.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.adapter.NicoHistoryAdapter
import com.kusamaru.standroid.adapter.NicoHistoryHorizontalAdapter
import com.kusamaru.standroid.bottomfragment.viewmodel.NicoHistoryViewModel
import com.kusamaru.standroid.databinding.BottomFragmentHistoryBinding
import com.kusamaru.standroid.room.entity.NicoHistoryDBEntity

/**
 * 端末内履歴[com.kusamaru.standroid.room.database.NicoHistoryDB]を表示するボトムシート
 * */
class NicoHistoryBottomFragment : BottomSheetDialogFragment() {

    /** IDを入力するために */
    var editText: EditText? = null

    /** 履歴一覧 */
    var recyclerViewList = arrayListOf<NicoHistoryDBEntity>()

    /** 横スクロールするやつの配列 */
    var countTextList = arrayListOf<String>()

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentHistoryBinding.inflate(layoutInflater) }

    /** ViewModel */
    private val viewModel by viewModels<NicoHistoryViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView初期化
        initRecyclerView()

        // 履歴受け取り
        viewModel.historyListLiveData.observe(viewLifecycleOwner) { list ->
            recyclerViewList.clear()
            recyclerViewList.addAll(list)
            viewBinding.bottomFragmentHistoryRecyclerview.adapter?.notifyDataSetChanged()
        }
        viewModel.countTextListLiveData.observe(viewLifecycleOwner) { list ->
            countTextList.clear()
            countTextList.addAll(list)
            viewBinding.bottomFragmentHistoryHorizontalRecyclerView.adapter?.notifyDataSetChanged()
        }

        // chip押したとき
        viewBinding.bottomFragmentHistoryLiveChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryVideoChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryTodayChip.setOnClickListener { loadHistory() }
        viewBinding.bottomFragmentHistoryDistinctChip.setOnClickListener { loadHistory() }

    }


    /**
     * 履歴DB読み込み。
     * */
    private fun loadHistory() {
        // 動画、生放送フィルター
        val isVideo = viewBinding.bottomFragmentHistoryVideoChip.isChecked
        val isLive = viewBinding.bottomFragmentHistoryLiveChip.isChecked
        val isFilterToDay = viewBinding.bottomFragmentHistoryTodayChip.isChecked
        val isRemoveDistinct = viewBinding.bottomFragmentHistoryDistinctChip.isChecked
        // ViewModel側で処理をする
        viewModel.getHistoryList(
            isFilterToDay = isFilterToDay,
            isRemoveDistinct = isRemoveDistinct,
            isIncludeLive = isLive,
            isIncludeVideo = isVideo,
        )
    }

    private fun initRecyclerView() {
        viewBinding.bottomFragmentHistoryRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = NicoHistoryAdapter(recyclerViewList).apply {
                editText = this@NicoHistoryBottomFragment.editText
                bottomSheetDialogFragment = this@NicoHistoryBottomFragment
            }
            if (itemDecorationCount == 0) {
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
        }
        // 横スクロール版
        viewBinding.bottomFragmentHistoryHorizontalRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = NicoHistoryHorizontalAdapter(countTextList)
            isNestedScrollingEnabled = false
        }
    }

}