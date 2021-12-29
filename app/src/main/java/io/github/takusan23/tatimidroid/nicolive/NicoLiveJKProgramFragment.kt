package io.github.takusan23.tatimidroid.nicolive

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.nicolive.adapter.NicoLiveJKProgramViewPagerAdapter
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveJkProgramBinding

/**
 * ニコニコ実況番組一覧Fragment（[NicoLiveJKProgramListFragment]）を乗せるためのFragment
 *
 * なのでこのFragmentにはViewPagerとTabLayoutしかないねん
 * */
class NicoLiveJKProgramFragment : Fragment() {

    /** ViewBinding */
    private val viewBinding by lazy { FragmentNicoliveJkProgramBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        viewBinding.fragmentNicoliveJkProgramTabLayout.background = ColorDrawable(getThemeColor(context))

        // ViewPager設定
        val adapter = NicoLiveJKProgramViewPagerAdapter(this)
        viewBinding.fragmentNicoliveJkProgramViewPager.adapter = adapter
        viewBinding.fragmentNicoliveJkProgramTabLayout.setupWithViewPager(viewBinding.fragmentNicoliveJkProgramViewPager)

    }
}