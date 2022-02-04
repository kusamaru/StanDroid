package com.kusamaru.standroid.nicovideo

import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentNicovideoListBinding
import com.kusamaru.standroid.nicovideo.fragment.*
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.tool.isConnectionInternet
import com.kusamaru.standroid.tool.isNotLoginMode

/**
 * ランキング、マイリスト等を表示するFragmentを乗せるFragment。
 * BottonNavBar押した時に切り替わるFragmentはこれ
 * */
class NicoVideoSelectFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoListBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        setDarkMode()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // インターネット接続確認
        // 何らかの方法でインターネットにはつながっている
        if (isConnectionInternet(context)) {
            // とりあえずランキング
            if (savedInstanceState == null) {
                // 画面回転時に回転前のFragmentをそのまま残しておくにはsavedInstanceStateがnullのときだけFragmentを生成する必要がある。
                setFragment(NicoVideoRankingFragment())
            }
        }

        // 未ログインで利用する場合
        if (isNotLoginMode(context)) {
            // ログインが必要なやつを非表示に
            viewBinding.fragmentNicovideoListMenu.menu.apply {
                findItem(R.id.nicovideo_select_menu_post).isVisible = false
                findItem(R.id.nicovideo_select_menu_mylist).isVisible = false
                findItem(R.id.nicovideo_select_menu_history).isVisible = false
                findItem(R.id.nicovideo_select_menu_nicorepo).isVisible = false
                findItem(R.id.nicovideo_select_menu_account).isVisible = false
            }
        }

        // メニュー押したとき
        viewBinding.fragmentNicovideoListMenu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicovideo_select_menu_ranking -> setFragment(NicoVideoRankingFragment())
                R.id.nicovideo_select_menu_mylist -> setFragment(NicoVideoMyListFragment())
                R.id.nicovideo_select_menu_history -> setFragment(NicoVideoHistoryFragment())
                R.id.nicovideo_select_menu_search -> setFragment(NicoVideoSearchFragment())
                R.id.nicovideo_select_menu_nicorepo -> setFragment(NicoVideoNicoRepoFragment())
                R.id.nicovideo_select_menu_account -> setFragment(NicoAccountFragment())
                R.id.nicovideo_select_menu_post -> {
                    setFragment(NicoVideoUploadVideoFragment().apply {
                        arguments = Bundle().apply {
                            putBoolean("my", true)
                        }
                    })
                }
            }
            true
        }

        // 戻るキー押したとき。アカウントFragmentから他Fragmentへ遷移した際、戻るキーでもとのFragmentに戻るために使う
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (childFragmentManager.backStackEntryCount > 0) {
                // Fragmentが積み上がっている場合は戻す
                childFragmentManager.popBackStack()
            } else {
                // コールバック消す
                this.remove()
            }
        }

    }

    private fun setDarkMode() {
        val darkModeSupport = DarkModeSupport(requireContext())
        viewBinding.fragmentVideoListLinearlayout.background = ColorDrawable(getThemeColor(darkModeSupport.context))
        viewBinding.fragmentVideoBar?.background?.setTint(getThemeColor(requireContext()))
    }

    /**
     * Fragmentを設置、置き換える関数
     * @param fragment Fragment
     * @param popbackstack バックキーで戻れるように。適当な値を引数に入れると戻れるようになります。
     * */
    fun setFragment(fragment: Fragment, popbackstack: String? = null) {
        // Handler(UIスレッド指定)で実行するとダークモード、画面切り替えに耐えるアプリが作れる。
        Handler(Looper.getMainLooper()).post {
            if (isAdded) {
                // 縦画面時親はMotionLayoutになるんだけど、横画面時はLinearLayoutなのでキャストが必要
                (viewBinding.fragmentNicovideoListMotionlayout as? MotionLayout)?.transitionToStart()
                childFragmentManager.beginTransaction().apply {
                    replace(viewBinding.fragmentVideoListLinearlayout.id, fragment)
                    // fragmentを積み上げる。
                    if (popbackstack != null) {
                        addToBackStack(popbackstack)
                    }
                    commit()
                }
            }
        }
    }

}