package com.kusamaru.standroid.nicolive

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentProgramListBinding
import com.kusamaru.standroid.tool.getThemeColor

/**
 * 番組一覧Fragmentを乗せるFragment
 *
 * BundleにIntを"fragment"の名前で[R.id.nicolive_program_list_menu_nicolive_jk]等を入れておくことで指定したページを開くことができます。
 * */
class ProgramListFragment : Fragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentProgramListBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 背景色
        viewBinding.apply {
            fragmentProgramBackdropLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramBarLinearLayout?.background?.setTint(getThemeColor(context))
            fragmentProgramListLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramNavigationView.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
        }

        if (savedInstanceState == null) {
            setCommunityListFragment(CommunityListFragment.FOLLOW)
        }

        // メニュー押したとき
        viewBinding.fragmentProgramNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicolive_program_list_menu_follow -> setCommunityListFragment(CommunityListFragment.FOLLOW)
                R.id.nicolive_program_list_menu_osusume -> setCommunityListFragment(CommunityListFragment.RECOMMEND)
                R.id.nicolive_program_list_menu_ranking -> setCommunityListFragment(CommunityListFragment.RANKING)
                R.id.nicolive_program_list_menu_korekara -> setCommunityListFragment(CommunityListFragment.KOREKARA)
                R.id.nicolive_program_list_menu_yoyaku -> setCommunityListFragment(CommunityListFragment.YOYAKU)
                R.id.nicolive_program_list_menu_rookie -> setCommunityListFragment(CommunityListFragment.ROOKIE)
                R.id.nicolive_program_list_menu_nicorepo -> setCommunityListFragment(CommunityListFragment.NICOREPO)
                R.id.nicolive_program_list_menu_nicolive_jk -> setFragment(NicoLiveJKProgramFragment())
                R.id.nicolive_program_list_menu_konomi_tag -> setFragment(NicoLiveKonomiTagProgramListFragment())
            }
            true
        }

        // ページを指定して開く
        val menuId = arguments?.getInt("fragment")
        if (menuId != null) {
            viewBinding.fragmentProgramNavigationView.setCheckedItem(menuId)
            viewBinding.fragmentProgramNavigationView.menu.performIdentifierAction(menuId, 0)
        }
    }

    /**
     * Fragmentを置く関数
     * */
    private fun setFragment(fragment: Fragment) {
        if (isAdded) {
            childFragmentManager.beginTransaction().replace(R.id.fragment_program_list_linear_layout, fragment).commit()
            // 縦画面時、親はMotionLayoutになるんだけど、横画面時はLinearLayoutなのでキャストが必要
            (viewBinding.fragmentProgramListParent as? MotionLayout)?.transitionToStart()
        }
    }

    /**
     * [CommunityListFragment]を置く関数
     *
     * @param page [CommunityListFragment.FOLLOW] など
     * */
    private fun setCommunityListFragment(page: Int) {
        // Fragmentが設置されてなければ落とす
        if (isAdded) {
            val communityListFragment = CommunityListFragment()
            val bundle = Bundle()
            bundle.putInt("page", page)
            communityListFragment.arguments = bundle
            setFragment(communityListFragment)
        }
    }

}