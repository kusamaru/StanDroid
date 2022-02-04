package com.kusamaru.standroid.nicolive.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.databinding.BottomFragmentNicovideoQualityBinding
import com.kusamaru.standroid.adapter.QualityAdapter

/**
 * ニコ生画質変更BottomFragment。
 * Bundleには何も入れなくていいから、[NicoLiveViewModel.qualityDataListLiveData]と[NicoLiveViewModel.currentQuality]の値を入れておいてね
 * */
class NicoLiveQualitySelectBottomSheet : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoQualityBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // CommentFragmentのViewModel取得する
        val commentFragmentViewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

        // 画質一覧取得
        commentFragmentViewModel.qualityDataListLiveData.observe(viewLifecycleOwner) { qualityList ->
            // RecyclerViewに入れる
            viewBinding.bottomFragmentNicovideoQualityRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = QualityAdapter(qualityList) { quality ->
                    //送信
                    commentFragmentViewModel.nicoLiveHTML.sendQualityMessage(quality.id)
                    dismiss()
                }
                // 区切り線
                if (itemDecorationCount == 0) {
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }
            }
        }
    }

}