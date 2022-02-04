package com.kusamaru.standroid.nicovideo.compose

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kusamaru.standroid.nicovideo.NicoVideoCommentFragment
import com.kusamaru.standroid.nicovideo.viewmodel.factory.NicoVideoViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.databinding.FragmentNicovideoCommentOnlyBinding

/**
 * コメント一覧を表示するFragmentを置くだけのFragment。
 * [JCNicoVideoFragment]にコメントのみを実装するのは困難そうなので
 *
 * 入れてほしいもの
 *
 * id   | String    | 動画ID
 * */
class JCNicoVideoCommentOnlyFragment : Fragment() {

    /** Fragmentを置くFrameLayout */
    private val viewBinding by lazy { FragmentNicovideoCommentOnlyBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initViewModel()

        viewBinding.fragmentNicovideoCommentOnlyParentFrameLayout.background = ColorDrawable(getThemeColor(requireContext()))

        // コメント一覧Fragment設置
        childFragmentManager
            .beginTransaction()
            .replace(viewBinding.fragmentNicovideoCommentOnlyParentFrameLayout.id, NicoVideoCommentFragment())
            .commit()
    }

    /** ViewModel初期化 */
    fun initViewModel() {
        // 動画ID
        val videoId = arguments?.getString("id")
        // キャッシュ再生
        val isCache = arguments?.getBoolean("cache")
        // ViewModel用意
        ViewModelProvider(this, NicoVideoViewModelFactory(requireActivity().application, videoId, isCache, false, false, false, null,0)).get(NicoVideoViewModel::class.java)
    }
}
