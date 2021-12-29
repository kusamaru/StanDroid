package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoSeriesAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoSeriesListViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSeriesListViewModel
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoSeriesBinding

/**
 * シリーズ一覧表示Fragment
 *
 * いれるもの
 * userId   |String | ユーザーID。なければ自分のを取ってくる
 * */
class NicoVideoSeriesListFragment : Fragment() {

    /** データ取得とか保持とかのViewModel */
    private lateinit var seriesListViewModel: NicoVideoSeriesListViewModel

    /** RecyclerViewへ渡す配列 */
    val seriesList = arrayListOf<NicoVideoSeriesData>()

    /** RecyclerViewのAdapter */
    val nicoVideoSeriesAdapter by lazy { NicoVideoSeriesAdapter(seriesList, this.id, parentFragmentManager) }

    /** findViewById駆逐。SeriesListもSeriesも同じレイアウト（RecyclerViewが一つあるだけ） */
    private val viewBinding by lazy { FragmentNicovideoSeriesBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        seriesListViewModel = ViewModelProvider(this, NicoVideoSeriesListViewModelFactory(requireActivity().application, userId)).get(NicoVideoSeriesListViewModel::class.java)

        initRecyclerView()

        // シリーズ一覧受け取り
        seriesListViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { list ->
            seriesList.clear()
            seriesList.addAll(list)
            nicoVideoSeriesAdapter.notifyDataSetChanged()
        }

        // 読み込み
        seriesListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoSeriesRefresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        viewBinding.fragmentNicovideoSeriesRefresh.setOnRefreshListener {
            seriesListViewModel.getSeriesList()
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoSeriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = nicoVideoSeriesAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

}