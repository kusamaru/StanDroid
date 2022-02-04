package com.kusamaru.standroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoDataClass
import com.kusamaru.standroid.nicovideo.adapter.NicoRepoAdapter
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoRepoViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.NicoRepoViewModel
import com.kusamaru.standroid.databinding.FragmentNicovideoNicorepoBinding

/**
 * ニコレポFragment
 *　
 * userId |String | ユーザーIDを入れるとそのユーザーのニコレポを取りに行きます。ない場合は自分のニコレポを取りに行きます
 * --- にんい ---
 * show_video   | Boolean   | 初期状態から動画のチェックを入れたい場合は使ってください
 * show_live    | Boolean   | 初期状態から生放送のチェックを入れたい場合は使ってください
 * */
class NicoVideoNicoRepoFragment : Fragment() {

    /** データ取得とか保持とかのViewModel */
    private lateinit var nicoRepoViewModel: NicoRepoViewModel

    /** RecyclerViewへ渡す配列 */
    private val recyclerViewList = arrayListOf<NicoRepoDataClass>()

    /** RecyclerViewにセットするAdapter */
    private val nicoRepoAdapter = NicoRepoAdapter(recyclerViewList)

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoNicorepoBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        nicoRepoViewModel = ViewModelProvider(this, NicoRepoViewModelFactory(requireActivity().application, userId)).get(NicoRepoViewModel::class.java)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込みLiveDataうけとり
        nicoRepoViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoNicorepoSwipe.isRefreshing = isLoading
        }

        // 配列受け取り
        nicoRepoViewModel.nicoRepoDataListLiveData.observe(viewLifecycleOwner) { parseList ->
            recyclerViewList.clear()
            recyclerViewList.addAll(parseList)
            nicoRepoAdapter.notifyDataSetChanged()
        }

        // ひっぱって更新
        viewBinding.fragmentNicovideoNicorepoSwipe.setOnRefreshListener {
            // データ消す
            recyclerViewList.clear()
            nicoRepoViewModel.getNicoRepo()
        }

        // そーと
        viewBinding.fragmentNicovideoNicorepoFilterLiveChip.setOnCheckedChangeListener { buttonView, isChecked ->
            nicoRepoViewModel.apply {
                isShowLive = isChecked
                filterAndPostLiveData()
            }
        }
        viewBinding.fragmentNicovideoNicorepoFilterVideoChip.setOnCheckedChangeListener { buttonView, isChecked ->
            nicoRepoViewModel.apply {
                isShowVideo = isChecked
                filterAndPostLiveData()
            }
        }

        // argumentの値を適用する
        viewBinding.fragmentNicovideoNicorepoFilterLiveChip.isChecked = arguments?.getBoolean("show_live", true) ?: true
        viewBinding.fragmentNicovideoNicorepoFilterVideoChip.isChecked = arguments?.getBoolean("show_video", true) ?: true
    }

    // RecyclerViewを初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoNicorepoRecyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = nicoRepoAdapter
        }
    }

}