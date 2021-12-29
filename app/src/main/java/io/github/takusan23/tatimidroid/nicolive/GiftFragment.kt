package io.github.takusan23.tatimidroid.nicolive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.nicolive.adapter.GiftHistoryRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.adapter.GiftRankingRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveGiftViewModel
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveGiftViewModelFactory
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveGiftBinding

/**
 * ギフト履歴、ギフトランキング、合計ポイントを表示するFragment
 * */
class GiftFragment : Fragment() {

    /** ギフト履歴のAdapter */
    private val giftHistoryAdapter = GiftHistoryRecyclerViewAdapter(arrayListOf())

    /** ギフトランキングのAdapter */
    private val giftRankingAdapter = GiftRankingRecyclerViewAdapter(arrayListOf())

    /** [CommentFragment]のViewModel */
    private val parentFragmentViewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })
    private val liveId by lazy { parentFragmentViewModel.nicoLiveHTML.liveId }

    /** このFragmentのViewModel。APIを叩くコードはここに書いてある */
    private val viewModel by lazy {
        ViewModelProvider(this, NicoLiveGiftViewModelFactory(requireActivity().application, liveId)).get(NicoLiveGiftViewModel::class.java)
    }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveGiftBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // RecyclerView初期化
        viewBinding.fragmentNicoliveGiftRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            // 区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        viewBinding.fragmentNicoliveGiftTabLayout.setBackgroundColor(getThemeColor(context))

        // 更新ボタン
        viewBinding.fragmentNicoliveGiftUpdateImageView.setOnClickListener {
            viewModel.getGiftData()
        }

        // 合計ポイントを受け取る
        viewModel.nicoLiveGiftTotalPointLiveData.observe(viewLifecycleOwner) { totalPoint ->
            viewBinding.fragmentNicoliveGiftTotalPointTextView.text = "${getString(R.string.total)}：$totalPoint pt"
        }

        // 投げ銭履歴を受け取る
        viewModel.nicoLiveGiftHistoryUserListLiveData.observe(viewLifecycleOwner) {
            giftHistoryAdapter.giftHistoryUserDataList.clear()
            giftHistoryAdapter.giftHistoryUserDataList.addAll(it)
            // RecyclerViewにセット（最初の画面は投げ銭履歴？）
            viewBinding.fragmentNicoliveGiftRecyclerView.adapter = giftHistoryAdapter
            viewBinding.fragmentNicoliveGiftRecyclerView.adapter?.notifyDataSetChanged()
        }

        // 投げ銭ランキングを受け取る
        viewModel.nicoLiveGiftRankingUserListLiveData.observe(viewLifecycleOwner) {
            giftRankingAdapter.rankingUserData.clear()
            giftRankingAdapter.rankingUserData.addAll(it)
        }

        // TabLayout切り替え
        viewBinding.fragmentNicoliveGiftTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.gift_history) -> {
                        // 投げ銭履歴
                        viewBinding.fragmentNicoliveGiftRecyclerView.adapter = giftHistoryAdapter
                    }
                    getString(R.string.gift_ranking) -> {
                        // 投げ銭ランキング
                        viewBinding.fragmentNicoliveGiftRecyclerView.adapter = giftRankingAdapter
                    }
                }
                viewBinding.fragmentNicoliveGiftRecyclerView.adapter?.notifyDataSetChanged()
            }
        })
    }
}