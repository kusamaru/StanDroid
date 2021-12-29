package io.github.takusan23.tatimidroid.nicovideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoRecommendBinding

/**
 * 関連動画を表示するFragment
 * */
class NicoVideoRecommendFragment : Fragment() {

    private lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoRecommendBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

        // 関連動画監視
        viewModel.recommendList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
        }
    }

    fun initRecyclerView(list: ArrayList<NicoVideoData>) {
        viewBinding.fragmentNicovideoRecommendRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

}
