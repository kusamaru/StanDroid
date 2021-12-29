package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoMyListData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoMyListAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoMyListViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoMyListViewModel
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoMylistBinding

/**
 * マイリスト一覧Fragment。
 *
 * 動画一覧ではない。
 * */
class NicoVideoMyListFragment : Fragment() {

    /** ViewModel */
    private lateinit var myListViewModel: NicoVideoMyListViewModel

    /** RecyclerViewにわたす配列 */
    private val recyclerViewList = arrayListOf<NicoVideoMyListData>()

    /** RecyclerViewのAdapter */
    private val myListAdapter by lazy { NicoVideoMyListAdapter(recyclerViewList, this.id, parentFragmentManager) }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoMylistBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        myListViewModel = ViewModelProvider(this, NicoVideoMyListViewModelFactory(requireActivity().application, userId)).get(NicoVideoMyListViewModel::class.java)

        initRecyclerView()

        // マイリスト一覧受け取り
        myListViewModel.myListDataLiveData.observe(viewLifecycleOwner) { myListItems ->
            recyclerViewList.clear()
            recyclerViewList.addAll(myListItems)
            myListAdapter.notifyDataSetChanged()
        }

        // 読み込み
        myListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoMylistRefresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        viewBinding.fragmentNicovideoMylistRefresh.setOnRefreshListener {
            myListViewModel.getMyListList()
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoMylistRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = myListAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            if (itemDecorationCount == 0) {
                addItemDecoration(itemDecoration)
            }
        }
    }


}