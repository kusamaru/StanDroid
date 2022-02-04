package com.kusamaru.standroid.nguploader.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.nguploader.compose.NGUploaderScreen
import com.kusamaru.standroid.nguploader.viewmodel.NGUploaderViewModel
import com.kusamaru.standroid.nicovideo.compose.DarkColors
import com.kusamaru.standroid.nicovideo.compose.LightColors
import com.kusamaru.standroid.tool.isDarkMode

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