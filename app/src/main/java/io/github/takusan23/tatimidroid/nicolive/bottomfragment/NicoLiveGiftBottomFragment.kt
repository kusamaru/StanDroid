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
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveGiftScreen
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveGiftViewModel
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveGiftViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * 投げ銭の履歴、ランキング表示BottomFragment
 *
 * いれてほしいもの
 * live_id  | String    | 番組ID
 * */
class NicoLiveGiftBottomFragment : BottomSheetDialogFragment() {

    /** 番組ID */
    private val liveId by lazy { requireArguments().getString("live_id")!! }

    /** ViewModel */
    private val viewModel by lazy {
        ViewModelProvider(this, NicoLiveGiftViewModelFactory(requireActivity().application, liveId)).get(NicoLiveGiftViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,) {
                    NicoLiveGiftScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}