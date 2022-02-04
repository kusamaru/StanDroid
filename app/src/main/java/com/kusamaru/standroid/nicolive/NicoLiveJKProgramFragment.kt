package com.kusamaru.standroid.nicolive

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kusamaru.standroid.nicolive.adapter.NicoLiveJKProgramViewPagerAdapter
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.databinding.FragmentNicoliveJkProgramBinding

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