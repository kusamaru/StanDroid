package io.github.takusan23.tatimidroid.setting.dbclear

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.setting.dbclear.compose.DatabaseClearSettingScreen
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * データベース削除Fragment
 * */
class DatabaseClearFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    DatabaseClearSettingScreen(
                        onBackClick = {
                            requireActivity().onBackPressed()
                        }
                    )
                }
            }
        }
    }

}