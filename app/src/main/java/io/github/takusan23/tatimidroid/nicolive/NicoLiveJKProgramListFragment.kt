package io.github.takusan23.tatimidroid.nicolive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.nicolive.adapter.CommunityRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment.Companion.NICOLIVE_JK_PROGRAMLIST_OFFICIAL
import io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment.Companion.NICOLIVE_JK_PROGRAMLIST_TAG
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveJKProgramListViewModel
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveJKProgramListViewModelFactory
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveCommunityBinding

/**
 * ニコニコ実況の番組一覧表示Fragment
 *
 * 入れてほしいもの
 * type | String | 公式で用意しているやつの場合は[NICOLIVE_JK_PROGRAMLIST_OFFICIAL]。ユーザーが有志で作ったやつは[NICOLIVE_JK_PROGRAMLIST_TAG]
 * */
class NicoLiveJKProgramListFragment : Fragment() {

    companion object {
        /** このFragmentのArgumentsにtypeって名前でこれを入れると、公式で用意している実況一覧を返します */
        const val NICOLIVE_JK_PROGRAMLIST_OFFICIAL = "official"

        /** このFragmentのArgumentsにtypeって名前でこれを入れると、有志が作ってるニコニコ実況タグの付いた番組を返します */
        const val NICOLIVE_JK_PROGRAMLIST_TAG = "tag"
    }

    /** RecyclerViewのAdapter */
    private val programListAdapter = CommunityRecyclerViewAdapter(arrayListOf())

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveCommunityBinding.inflate(layoutInflater) }

    /** ViewModel */
    private val viewModel by lazy {
        // 公式 or ニコニコ実況タグ付きのユーザー番組
        val type = arguments?.getString("type") ?: NICOLIVE_JK_PROGRAMLIST_OFFICIAL
        ViewModelProvider(this, NicoLiveJKProgramListViewModelFactory(requireActivity().application, type)).get(NicoLiveJKProgramListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        // LiveData監視
        viewModel.isLoadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = isLoading
        }

        // 番組一覧
        viewModel.programListLiveData.observe(viewLifecycleOwner) { programList ->
            programListAdapter.programList.clear()
            programListAdapter.programList.addAll(programList)
            viewBinding.fragmentNicoliveCommunityRecyclerView.adapter?.notifyDataSetChanged()
        }

        // ひっぱって更新
        viewBinding.fragmentNicoliveCommunitySwipe.setOnRefreshListener {
            viewModel.getProgramList()
        }

    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicoliveCommunityRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = programListAdapter
        }
    }

}