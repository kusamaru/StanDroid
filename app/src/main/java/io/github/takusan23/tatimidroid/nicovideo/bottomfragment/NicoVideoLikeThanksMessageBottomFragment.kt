package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoLikeMessageScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * ニコ動のいいねのお礼メッセージ表示用BottomFragment
 *
 * レイアウトはComposeで
 * */
class NicoVideoLikeThanksMessageBottomFragment : BottomSheetDialogFragment() {

    /** [io.github.takusan23.tatimidroid.nicovideo.compose.JCNicoVideoFragment]のViewModel */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    // これでくくらないとなんかダークモード時に文字が白にならない
                    Surface {
                        NicoVideoLikeMessageScreen(nicoVideoViewModel = viewModel) {
                            // 二回目（画面回転時）に表示させない
                            viewModel.isAlreadyShowThanksMessage = true
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 閉じれんように
        isCancelable = false

    }

}