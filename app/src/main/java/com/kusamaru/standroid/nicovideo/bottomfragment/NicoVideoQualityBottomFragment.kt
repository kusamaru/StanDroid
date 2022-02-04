package com.kusamaru.standroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.adapter.QualityAdapter
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.databinding.BottomFragmentNicovideoQualityBinding

/**
 * ニコ動の画質変更BottomFragment
 *
 * ViewModelを使ってやり取りしているのでargumentには何も入れなくておk
 * */
class NicoVideoQualityBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoQualityBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModelでデータ受け取る
        val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })
        // 画質JSONパース
        val audioQualityJSONArray = viewModel.nicoVideoHTML.parseAudioQualityDMC(viewModel.nicoVideoJSON.value!!)
        // 音声は一番いいやつ？
        val audioId = audioQualityJSONArray.getJSONObject(0).getString("id")

        // 画質配列
        viewModel.qualityDataListLiveData.observe(viewLifecycleOwner) { qualityList ->
            // RecyclerView
            viewBinding.bottomFragmentNicovideoQualityRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = QualityAdapter(qualityList) { quality ->
                    // 画質変更して再リクエスト
                    viewModel.coroutine(false, quality.id, audioId)
                    this@NicoVideoQualityBottomFragment.dismiss()
                }
                // 区切り線
                if (itemDecorationCount == 0) {
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }
            }
        }

    }
}