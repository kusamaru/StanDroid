package io.github.takusan23.tatimidroid.nguploader.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nguploader.compose.NGUploaderScreen
import io.github.takusan23.tatimidroid.nguploader.viewmodel.NGUploaderViewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * NG投稿者機能の編集用BottomFragment
 * */
class NGUploaderBottomFragment : BottomSheetDialogFragment() {

    /** データベースアクセスなどはViewModelに */
    private val viewModel by viewModels<NGUploaderViewModel>({ this })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    NGUploaderScreen(viewModel = viewModel)
                }
            }
        }
    }
}