package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.databinding.BottomFragmentSkipCustomizeBinding

/**
 * 押したときのスキップ秒数を変更できるやつ
 * */
class NicoVideoSkipCustomizeBottomFragment : BottomSheetDialogFragment() {

    private lateinit var prefSetting: SharedPreferences

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentSkipCustomizeBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        viewBinding.bottomFragmentNicovideoSkipCustomizeEditText.setText(prefSetting.getString("nicovideo_skip_sec", "5"))

        // 保存
        viewBinding.bottomFragmentNicovideoSkipCustomizeEditText.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                // 空文字だと toLong() で落ちるので対策（toLongOrNull()使えば変換できない時にnullにしてくれる）
                prefSetting.edit { putString("nicovideo_skip_sec", it.toString()) }
                applyUI()
            }
        }

    }

    // ボタンのスキップ秒数設定反映
    private fun applyUI() {
        (requireParentFragment() as? NicoVideoFragment)?.apply {
            initController()
        }
    }

}