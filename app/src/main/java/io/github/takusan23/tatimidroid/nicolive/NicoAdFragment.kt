package io.github.takusan23.tatimidroid.nicolive

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.nicoapi.nicoad.NicoAdAPI
import io.github.takusan23.tatimidroid.nicoad.viewmodel.NicoAdViewModel
import io.github.takusan23.tatimidroid.nicoad.viewmodel.NicoAdViewModelFactory
import io.github.takusan23.tatimidroid.nicolive.adapter.NicoAdHistoryAdapter
import io.github.takusan23.tatimidroid.nicolive.adapter.NicoAdRankingAdapter
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveNicoadBinding

/**
 * ニコニ広告の履歴、ランキング表示Fragment
 * */
class NicoAdFragment : Fragment() {

    /** ニコニ広告ランキング表示Adapter */
    private val nicoAdRankingAdapter = NicoAdRankingAdapter(arrayListOf())

    /** ニコニ広告履歴表示Adapter */
    private val nicoAdHistoryAdapter = NicoAdHistoryAdapter(arrayListOf())

    /** [CommentFragment]のViewModel */
    private val parentFragmentViewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })
    private val liveId by lazy { parentFragmentViewModel.nicoLiveHTML.liveId }

    /** ニコニ広告Fragment用ViewModel。APIを叩くコードなどはこっち */
    private val viewModel by lazy {
        ViewModelProvider(this, NicoAdViewModelFactory(requireActivity().application, liveId)).get(NicoAdViewModel::class.java)
    }

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveNicoadBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // RecyclerView初期化
        viewBinding.fragmentNicoLiveNicoadRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            // 区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        viewBinding.fragmentNicoLiveNicoadTabLayout.setBackgroundColor(getThemeColor(context))

        // 更新ボタン
        viewBinding.fragmentNicoLiveNicoadUpdateImageView.setOnClickListener {
            viewModel.getNicoAd()
        }

        // ブラウザ起動
        viewBinding.fragmentNicoLiveNicoadOpenBrowserImageView.setOnClickListener {
            openBrowser()
        }

        // トータルポイント取得
        viewModel.nicoAdDataLiveData.observe(viewLifecycleOwner) { data ->
            // UIに反映
            viewBinding.fragmentNicoLiveGiftTotalPointTextView.text = "${data.totalPoint}pt"
            viewBinding.fragmentNicoLiveGiftActivePointTextView.text = "${data.activePoint}pt"
        }

        // 広告ランキングを取得
        viewModel.nicoAdRankingLiveData.observe(viewLifecycleOwner) {
            nicoAdRankingAdapter.rankingList.clear()
            nicoAdRankingAdapter.rankingList.addAll(it)
            // 最初の画面はランキングだと思うのでセット
            viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdRankingAdapter
            viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter?.notifyDataSetChanged()
        }

        // 広告履歴取得
        viewModel.nicoAdHistoryLiveData.observe(viewLifecycleOwner) {
            nicoAdHistoryAdapter.nicoAdHistoryList.clear()
            nicoAdHistoryAdapter.nicoAdHistoryList.addAll(it)
        }

        // TabLayout
        viewBinding.fragmentNicoLiveNicoadTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.nico_ad_history) -> {
                        viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdHistoryAdapter
                    }
                    getString(R.string.nico_ad_ranking) -> {
                        viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter = nicoAdRankingAdapter
                    }
                }
                viewBinding.fragmentNicoLiveNicoadRecyclerView.adapter?.notifyDataSetChanged()
            }
        })
    }

    /** ブラウザを起動する */
    private fun openBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, NicoAdAPI.generateURL(liveId).toUri())
        startActivity(intent)
    }


    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}