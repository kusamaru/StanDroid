package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoCacheJsonUpdateBinding

/**
 * ユーザーに動画情報JSONの更新をお願いするBottomFragment
 *
 * 仕様変更で新仕様に合わせたのはいいけど、キャッシュのJSONが古いままなので更新してもらう
 *
 * [io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel]に依存している
 * */
class NicoVideoCacheJSONUpdateRequestBottomFragment : BottomSheetDialogFragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    /** ViewBinding */
    private val viewBinding by lazy { BottomFragmentNicovideoCacheJsonUpdateBinding.inflate(layoutInflater) }

    /** Jetpack Composeと迷ったけどxmlで */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.bottomFragmentNicovideoCacheJsonUpdateUpdateButton.setOnClickListener {
            // 動画情報を更新する
            viewModel.requestUpdateCacheVideoInfoJSONFile()
        }

        viewBinding.bottomFragmentNicovideoCacheJsonUpdateNoUpdateButton.setOnClickListener {
            // 更新せず視聴
            dismiss()
        }

        viewModel.cacheVideoJSONUpdateLiveData.observe(viewLifecycleOwner) { isNeedUpdate ->
            // falseになったら閉じる
            if (!isNeedUpdate) {
                dismiss()
            }
        }

    }

}