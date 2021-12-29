package io.github.takusan23.tatimidroid.nicovideo.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.adapter.parcelable.TabLayoutData
import io.github.takusan23.tatimidroid.nicoapi.NicoVideoCache
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoCommentFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoInfoFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoMenuFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoRecommendFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSearchFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoUploadVideoFragment
import io.github.takusan23.tatimidroid.R

/**
 * ニコ動の方だけViewPager2になった。
 * Fragmentをフリックで切り替えられるやつ。２にしたおかげか動的追加できるようになった
 * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。ない場合は省略していいよ（そこにないなら無いですね）。主に画面回転復帰時に使う。
 * */
class NicoVideoRecyclerPagerAdapter(val fragment: Fragment, val videoId: String, val isCache: Boolean, val dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) : FragmentStateAdapter(fragment) {

    // 画面回転時に回転前に動的にFragmentを追加場合復元するからその時使う
    companion object {
        const val TAB_LAYOUT_DATA_SEARCH = "search"
        const val TAB_LAYOUT_DATA_MYLIST = "mylist"
        const val TAB_LAYOUT_DATA_POST = "post"
        const val TAB_LAYOUT_DATA_SERIES = "series"
    }

    /** Fragment一覧 */
    val fragmentList = arrayListOf<Fragment>()

    /** Fragment名一覧 */
    val fragmentTabName = arrayListOf<String>()

    val bundle = Bundle()

    // 動的にFragment追加して見る
    init {
        bundle.putString("id", videoId)
        bundle.putBoolean("cache", isCache)
        // 動画情報JSONがあるかどうか。なければ動画情報Fragmentを非表示にするため
        val nicoVideoCache = NicoVideoCache(fragment.requireContext())
        val exists = nicoVideoCache.hasCacheNewVideoInfoJSON(videoId)
        // インターネット接続とキャッシュ再生で分岐
        if (isCache) {
            val commentMenuFragment = NicoVideoMenuFragment().apply {
                arguments = bundle
            }
            val devNicoVideoCommentFragment = NicoVideoCommentFragment().apply {
                arguments = bundle
            }
            fragmentList.apply {
                add(commentMenuFragment)
                add(devNicoVideoCommentFragment)
            }
            fragmentTabName.apply {
                add(fragment.getString(R.string.menu))
                add(fragment.getString(R.string.comment))
            }
            if (exists) {
                // 動画情報JSONがあれば動画情報Fragmentを表示させる
                val nicoVideoInfoFragment = NicoVideoInfoFragment().apply {
                    arguments = bundle
                }
                fragmentList.add(nicoVideoInfoFragment)
                fragmentTabName.add(fragment.getString(R.string.nicovideo_info))
            }
        } else {
            val commentMenuFragment = NicoVideoMenuFragment().apply {
                arguments = bundle
            }
            val devNicoVideoCommentFragment = NicoVideoCommentFragment().apply {
                arguments = bundle
            }
            val nicoVideoInfoFragment = NicoVideoInfoFragment().apply {
                arguments = bundle
            }
            val nicoVideoRecommendFragment = NicoVideoRecommendFragment().apply {
                arguments = bundle
            }
            fragmentList.apply {
                add(commentMenuFragment)
                add(devNicoVideoCommentFragment)
                add(nicoVideoInfoFragment)
                add(nicoVideoRecommendFragment)
                // add(nicoContentTree)
            }
            fragmentTabName.apply {
                add(fragment.getString(R.string.menu))
                add(fragment.getString(R.string.comment))
                add(fragment.getString(R.string.nicovideo_info))
                add(fragment.getString(R.string.recommend_video))
                // add(activity.getString(R.string.parent_contents))
            }
        }

        // 引数に指定したFragmentがある場合
        dynamicAddFragmentList.toList().forEach { data ->
            // Fragment作る
            when (data.type) {
                TAB_LAYOUT_DATA_SEARCH -> NicoVideoSearchFragment()
                TAB_LAYOUT_DATA_POST -> NicoVideoUploadVideoFragment()
                TAB_LAYOUT_DATA_MYLIST -> NicoVideoMyListListFragment()
                TAB_LAYOUT_DATA_SERIES -> NicoVideoSeriesFragment()
                else -> null
            }?.let { fragment ->
                // Bundle詰める
                fragment.arguments = data.bundle
                fragmentList.add(fragment)
                fragmentTabName.add(data.text ?: "タブ")
                notifyDataSetChanged() // 更新
            }
        }

    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    /**
     * 動的にFragmentを追加する時に使う関数。
     * 注意：対応しているFragmentは以下のとおりです。未来の私へ。。。
     *  - DevNicoVideoPOSTFragment
     *  - DevNicoVideoSearchFragment
     *  - DevNicoVideoMyListListFragment
     *  - 将来的にはシリーズ機能も（一般とかはマイリスト制限きついからシリーズ機能使ってそう（しらんけど））
     * @param fragment 追加したいFragment
     * @param tabName TabLayoutで表示する名前
     * */
    fun addFragment(fragment: Fragment, tabName: String) {
        fragmentList.add(fragment)
        fragmentTabName.add(tabName)
        notifyDataSetChanged() // 更新
        dynamicAddFragmentList.add(TabLayoutData(getType(fragment), tabName, fragment.arguments))
    }

    /**
     * FragmentからTAB_LAYOUT_DATA_SEARCHとかを生成する
     * @param fragment addFragment()で追加可能なFragment
     * */
    private fun getType(fragment: Fragment): String {
        return when (fragment) {
            is NicoVideoUploadVideoFragment -> TAB_LAYOUT_DATA_POST
            is NicoVideoMyListListFragment -> TAB_LAYOUT_DATA_MYLIST
            is NicoVideoSearchFragment -> TAB_LAYOUT_DATA_SEARCH
            is NicoVideoSeriesFragment -> TAB_LAYOUT_DATA_SERIES
            else -> "" // ありえない
        }
    }

}