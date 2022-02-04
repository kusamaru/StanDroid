package com.kusamaru.standroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoListAdapter
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoVideoSeriesViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoSeriesViewModel
import com.kusamaru.standroid.databinding.FragmentNicovideoSeriesBinding

/**
 * シリーズの動画一覧Fragment
 *
 * レイアウトはシリーズ一覧をそのまま使いまわしてる
 *
 * 入れてほしいもの
 * series_id    | String    | シリーズID。https://sp.nicovideo.jp/series/{ここの数字}
 * */
class NicoVideoSeriesFragment : Fragment() {

    /** データ取得などViewModel */
    private lateinit var nicoVideoSeriesViewModel: NicoVideoSeriesViewModel

    /** RecyclerViewにわたす配列 */
    private val nicoVideoList = arrayListOf<NicoVideoData>()

    /** RecyclerViewにセットするAdapter */
    private val nicoVideoListAdapter = NicoVideoListAdapter(nicoVideoList)

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoSeriesBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seriesId = arguments?.getString("series_id")!!
        nicoVideoSeriesViewModel = ViewModelProvider(this, NicoVideoSeriesViewModelFactory(requireActivity().application, seriesId)).get(NicoVideoSeriesViewModel::class.java)

        initRecyclerView()

        // シリーズ動画一覧取得
        nicoVideoSeriesViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { list ->
            nicoVideoList.clear()
            nicoVideoList.addAll(list)
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // 読み込み
        nicoVideoSeriesViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoSeriesRefresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        viewBinding.fragmentNicovideoSeriesRefresh.setOnRefreshListener {
            nicoVideoSeriesViewModel.getSeriesVideoList()
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoSeriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = nicoVideoListAdapter
        }
    }

}