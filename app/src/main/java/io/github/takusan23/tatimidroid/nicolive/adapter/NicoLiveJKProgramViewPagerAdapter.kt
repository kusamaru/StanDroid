package io.github.takusan23.tatimidroid.nicolive.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.nicolive.NicoLiveJKProgramListFragment
import io.github.takusan23.tatimidroid.R

/**
 * ニコニコ実況の公式、実況タグの切り替えViewPager
 *
 * MotionLayoutのせいなのか高さが戻らないので無印ViewPagerに戻した(ViewPager2が推奨なんだが)
 *
 * @param parentFragment [FragmentStateAdapter]の引数に必要
 * */
class NicoLiveJKProgramViewPagerAdapter(private val parentFragment: Fragment) : FragmentStatePagerAdapter(parentFragment.parentFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    /**
     * TabLayoutとViewPagerを連携する際はこの関数を利用することでタブの名前を取得できます
     *
     * @param position タブの位置
     * */
    fun getTabName(position: Int): String {
        return when (position) {
            0 -> parentFragment.context?.getString(R.string.nicolive_jk_program_official) ?: "公式"
            1 -> parentFragment.context?.getString(R.string.nicolive_jk_program_tag) ?: "ニコニコ実況タグ（有志）"
            else -> ""
        }
    }

    /** Fragmentの数を返す */
    override fun getCount(): Int = 2

    /** Fragmentを返す */
    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        // Fragmentにわたす値
        when (position) {
            0 -> bundle.putString("type", NicoLiveJKProgramListFragment.NICOLIVE_JK_PROGRAMLIST_OFFICIAL)
            1 -> bundle.putString("type", NicoLiveJKProgramListFragment.NICOLIVE_JK_PROGRAMLIST_TAG)
        }
        // Fragmentを返す
        return NicoLiveJKProgramListFragment().apply {
            arguments = bundle
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return getTabName(position)
    }

}