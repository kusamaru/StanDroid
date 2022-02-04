package com.kusamaru.standroid.nicolive.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kusamaru.standroid.nicolive.*
import com.kusamaru.standroid.R

/**
 * ニコ生（[com.kusamaru.standroid.nicolive.CommentFragment]）に置くViewPager2のAdapter
 * ところでViewPager2なんか感度高くね？
 * @param parentFragment ViewPager2に入れたFragmentで[Fragment.requireParentFragment]を呼んだ時に返すFragment。
 * @param liveId 生放送ID
 * @param isOfficial 公式番組の場合はtrue
 * */
class NicoLivePagerAdapter(val parentFragment: Fragment, val liveId: String, val isOfficial: Boolean = false) : FragmentStateAdapter(parentFragment) {

    /** Fragmentの配列。配列で管理する必要性は */
    val fragmentList = arrayListOf<Fragment>()
    val fragmentTabNameList = arrayListOf<String>()

    init {
        val context = parentFragment.requireContext()
        when {
            isOfficial -> {
                // 公式番組の時に使うFragmentを配列に
                fragmentList.addAll(
                    arrayListOf(
                        CommentMenuFragment().apply { arguments = liveIdBundle() },
                        CommentViewFragment().apply { arguments = liveIdBundle() },
                        GiftFragment().apply { arguments = liveIdBundle() },
                        NicoAdFragment().apply { arguments = liveIdBundle() },
                        ProgramInfoFragment().apply { arguments = liveIdBundle() },
                    )
                )
                fragmentTabNameList.addAll(
                    arrayListOf(
                        context.getString(R.string.menu),
                        context.getString(R.string.comment),
                        context.getString(R.string.gift),
                        context.getString(R.string.nicoads),
                        context.getString(R.string.program_info),
                    )
                )
            }
            else -> {
                // 公式/ニコニコ実況以外で使うFragment
                fragmentList.addAll(
                    arrayListOf(
                        CommentMenuFragment().apply { arguments = liveIdBundle() },
                        CommentViewFragment().apply { arguments = liveIdBundle() },
                        CommentRoomFragment().apply { arguments = liveIdBundle() },
                        GiftFragment().apply { arguments = liveIdBundle() },
                        NicoAdFragment().apply { arguments = liveIdBundle() },
                        ProgramInfoFragment().apply { arguments = liveIdBundle() },
                    )
                )
                fragmentTabNameList.addAll(
                    arrayListOf(
                        context.getString(R.string.menu),
                        context.getString(R.string.comment),
                        context.getString(R.string.room_comment),
                        context.getString(R.string.gift),
                        context.getString(R.string.nicoads),
                        context.getString(R.string.program_info),
                    )
                )
            }
        }
    }

    /** Fragmentに入れるBundle */
    private fun liveIdBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        return bundle
    }


    /** Fragmentの数 */
    override fun getItemCount(): Int {
        return when {
            isOfficial -> 5
            else -> 6
        }
    }

    /** Fragmentを返す */
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    /** 番組情報Fragmentを返す */
    fun requireProgramInfoFragment(): ProgramInfoFragment? {
        return fragmentList.find { fragment -> fragment is ProgramInfoFragment } as? ProgramInfoFragment
    }

    /** コメントFragmentを返す */
    fun requireCommentViewFragment(): CommentViewFragment? {
        return fragmentList.find { fragment -> fragment is CommentViewFragment } as? CommentViewFragment
    }

}
