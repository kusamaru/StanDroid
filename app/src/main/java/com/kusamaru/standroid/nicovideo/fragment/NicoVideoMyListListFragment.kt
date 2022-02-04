package com.kusamaru.standroid.nicovideo.fragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.adapter.AllShowDropDownMenuAdapter
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoListAdapter
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoVideoMyListListViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoMyListListViewModel
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.databinding.FragmentNicovideoMylistListBinding

/**
 * マイリストの動画一覧表示Fragment。
 * ViewPagerで表示するFragmentです。
 * 入れてほしいもの↓
 * mylist_id   |String |マイリストのID。空の場合はとりあえずマイリストをリクエストします
 * mylist_is_me|Boolean|マイリストが自分のものかどうか。自分のマイリストの場合はtrue
 * */
class NicoVideoMyListListFragment : Fragment() {

    /** ViewModel */
    private lateinit var myListListViewModel: NicoVideoMyListListViewModel

    /** RecyclerViewへ渡す配列 */
    private val recyclerViewList = arrayListOf<NicoVideoData>()

    /** RecyclerViewへ入れるAdapter */
    val nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoMylistListBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myListId = arguments?.getString("mylist_id")!!
        val isMe = arguments?.getBoolean("mylist_is_me")!!
        myListListViewModel = ViewModelProvider(this, NicoVideoMyListListViewModelFactory(requireActivity().application, myListId, isMe)).get(NicoVideoMyListListViewModel::class.java)

        // ダークモード
        viewBinding.fragmentNicovideoMylistListAppBar.background = ColorDrawable(getThemeColor(requireContext()))

        // RecyclerView初期化
        initRecyclerView()

        // 並び替えメニュー初期化
        initSortMenu()

        // データ取得を待つ
        myListListViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { videoList ->
            recyclerViewList.clear()
            recyclerViewList.addAll(videoList)
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // くるくる
        myListListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoMylistListSwipe.isRefreshing = isLoading
        }

        // ひっぱって更新
        viewBinding.fragmentNicovideoMylistListSwipe.setOnRefreshListener {
            myListListViewModel.getMyListVideoList()
        }

    }

    /** 並び替えメニュー初期化 */
    private fun initSortMenu() {
        val sortList = arrayListOf(
            "登録が新しい順",
            "登録が古い順",
            "再生の多い順",
            "再生の少ない順",
            "投稿日時が新しい順",
            "投稿日時が古い順",
            "再生時間の長い順",
            "再生時間の短い順",
            "コメントの多い順",
            "コメントの少ない順",
            "マイリスト数の多い順",
            "マイリスト数の少ない順"
        )
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortList)
        viewBinding.fragmentNicovideoMylistListSort.apply {
            setAdapter(adapter)
            setOnItemClickListener { parent, view, position, id ->
                myListListViewModel.sort(position)
            }
            setText(sortList[0], false)
        }
    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoMylistListRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            // Adapterセット
            adapter = nicoVideoListAdapter
        }
    }

}