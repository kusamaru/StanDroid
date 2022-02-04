package com.kusamaru.standroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel
import com.kusamaru.standroid.databinding.BottomFragmentNicovideoLikeBinding

/**
 * いいね♡するBottomFragment。初見わからんから説明文付き。
 * 普及したら消します。
 *
 * [com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoViewModel]を利用するので、[JKNicoVideoFragment]が親のFragmentである必要があります。
 * */
class NicoVideoLikeBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoLikeBinding.inflate(layoutInflater) }

    /** [com.kusamaru.standroid.nicovideo.JCNicoVideoFragment]のViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // メッセージ
        viewBinding.bottomFragmentNicovideoLikeDescriptionTextView.text = HtmlCompat.fromHtml(
            """
            いいね機能 #とは<br>
            動画を応援できる機能。<br>
            ・いいねしたユーザーは投稿者のみ見ることができます。<br>
            ・いいね数はランキングに影響します。<br>
            ・一般会員でも使えるそうです。<br>
            <span style="color:#FFA500">いいねするとうｐ主からお礼のメッセージが見れます</span><br>
            <small>（設定してある場合のみ）</small>
        """.trimIndent(), HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        // Like押した
        viewBinding.bottomFragmentNicovideoLikeButton.setOnClickListener {
            // いいね登録
            viewModel.postLike()
        }

        // いいねできたら消す
        viewModel.isLikedLiveData.observe(viewLifecycleOwner) { isLiked ->
            if (isLiked) {
                dismiss()
            }
        }
    }

}