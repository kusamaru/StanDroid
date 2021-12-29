package io.github.takusan23.tatimidroid.nicolive.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveKonomiTagEditScreen
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveKonomiTagEditViewModel
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveKonomiTagEditViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * 好みタグを編集（フォローなど）をするBottomFragment
 *
 * 入れてほしいもの
 *
 * broadcaster_user_id  | String    | 放送者のユーザーID。ない場合はnullにしてください。おすすめ、フォロー中タグのみ表示します
 * */
class NicoLiveKonomiTagEditBottomFragment : BottomSheetDialogFragment() {

    /** ViewModel。インターネット通信など */
    private val viewModel by lazy {
        val broadCasterUserId = arguments?.getString("broadcaster_user_id")
        ViewModelProvider(this, NicoLiveKonomiTagEditViewModelFactory(requireActivity().application, broadCasterUserId)).get(NicoLiveKonomiTagEditViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    NicoLiveKonomiTagEditScreen(viewModel = viewModel)
                }
            }
        }
    }

}