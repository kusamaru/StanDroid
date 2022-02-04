package com.kusamaru.standroid.nicolive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentNicoliveKonomitagProgramListBinding
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.nicolive.adapter.CommunityRecyclerViewAdapter
import com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveKonomiTagEditBottomFragment
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveKonomiTagViewModel
import java.util.ArrayList

/**
 * 好みタグから番組を検索する
 *
 * 好みタグをフォローするFragmentはこっち [com.kusamaru.standroid.nicolive.bottomfragment.NicoLiveKonomiTagEditBottomFragment]
 * */
class NicoLiveKonomiTagProgramListFragment : Fragment() {

    /** ViewBinding */
    private val viewBinding by lazy { FragmentNicoliveKonomitagProgramListBinding.inflate(layoutInflater) }

    /** ViewModel */
    private val viewModel by viewModels<NicoLiveKonomiTagViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 好みタグ
        viewModel.followingKonomiTagListLiveData.observe(viewLifecycleOwner) { konomiTagList ->
            // 全部消す
            viewBinding.fragmentNicoliveKonomitagProgramListChipGroup.removeAllViews()
            // 動的にViewを作る
            konomiTagList.forEach { nicoLiveKonomiTagData ->
                val chip = Chip(requireContext())
                chip.chipIcon = requireContext().getDrawable(R.drawable.ic_24px)
                chip.text = nicoLiveKonomiTagData.name
                // 押したとき
                chip.setOnClickListener {
                    viewModel.searchProgramFromKonomiTag(nicoLiveKonomiTagData.tagId)
                    viewBinding.fragmentNicoliveKonomitagProgramListSwipeToReflesh.isRefreshing = true
                }
                viewBinding.fragmentNicoliveKonomitagProgramListChipGroup.addView(chip)
            }
        }

        // 動画一覧
        viewModel.konomiTagProgramListLiveData.observe(viewLifecycleOwner) { videoList ->
            viewBinding.fragmentNicoliveKonomitagProgramListRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = CommunityRecyclerViewAdapter(videoList as ArrayList<NicoLiveProgramData>, true)
                if (itemDecorationCount == 0) {
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }
                viewBinding.fragmentNicoliveKonomitagProgramListSwipeToReflesh.isRefreshing = false
            }
        }

        // スワイプで更新
        viewBinding.fragmentNicoliveKonomitagProgramListSwipeToReflesh.setOnRefreshListener {
            viewModel.getMyFollowingKonomiTag()
            viewModel.beforeSearchTagId?.let { viewModel.searchProgramFromKonomiTag(it) }
        }

        // 好みタグフォロー用BottomFragment表示
        viewBinding.fragmentNicoliveKonomitagProgramListEditButton.setOnClickListener {
            NicoLiveKonomiTagEditBottomFragment().show(parentFragmentManager, "konomi_tag")
        }

    }

}