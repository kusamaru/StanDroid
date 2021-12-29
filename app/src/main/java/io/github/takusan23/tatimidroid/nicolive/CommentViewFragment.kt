package io.github.takusan23.tatimidroid.nicolive

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.nicolive.adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentCommentviewBinding

/**
 * ニコ生コメント表示Fragment
 * */
class CommentViewFragment : Fragment() {

    private lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter
    lateinit var prefSetting: SharedPreferences

    // getString(R.string.arena)
    private lateinit var stringArena: String

    // CommentFragmentとそれのViewModel
    val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentCommentviewBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        stringArena = getString(R.string.arena)

        viewBinding.fragmentCommentRecyclerView.apply {
            // RecyclerView初期化
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            commentRecyclerViewAdapter = CommentRecyclerViewAdapter(viewModel.commentList, requireParentFragment())
            adapter = commentRecyclerViewAdapter
            itemAnimator = null
            //区切り線いれる
            if (itemDecorationCount == 0) {
                val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                addItemDecoration(itemDecoration)
            }
        }

        // スクロールボタン。追従するぞい
        viewBinding.fragmentCommentFollowingButton.setOnClickListener {
            // Fragmentはクソ！
            (viewBinding.fragmentCommentRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            // Visibilityゴーン。誰もカルロス・ゴーンの話しなくなったな
            setFollowingButtonVisibility(false)
        }

        // CommentFragmentのViewModelにあるコメントLiveDataを監視する
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
            commentRecyclerViewAdapter.notifyItemInserted(0)
            recyclerViewScrollPos()
        }
        viewModel.updateRecyclerViewLiveData.observe(viewLifecycleOwner) {
            commentRecyclerViewAdapter.notifyDataSetChanged()
        }

    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * スクロール位置をゴニョゴニョする関数。追加時に呼んでね
     * もし一番上なら->新しいコメント来ても一番上に追従する
     * 一番上にいない->この位置に留まる
     * */
    fun recyclerViewScrollPos() {
        // れいあうとまねーじゃー
        val linearLayoutManager = (viewBinding.fragmentCommentRecyclerView.layoutManager as LinearLayoutManager)
        // RecyclerViewで表示されてる中で一番上に表示されてるコメントの位置
        val visibleListFirstItemPos = linearLayoutManager.findFirstVisibleItemPosition()
        // 追いかけるボタンを利用するか
        if (prefSetting.getBoolean("setting_oikakeru_hide", false)) {
            // 利用しない
            //一番上なら追いかける
            if (visibleListFirstItemPos == 0 || visibleListFirstItemPos == 1) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0)
            }
            // 使わないので非表示
            setFollowingButtonVisibility(false)
        } else {
            // 利用する
            if (visibleListFirstItemPos == 0 || viewModel.commentList.isEmpty()) {
                viewBinding.fragmentCommentRecyclerView.scrollToPosition(0)
                // 追従ボタン非表示
                setFollowingButtonVisibility(false)
            } else {
                // 一番上じゃないので追従ボタン表示
                setFollowingButtonVisibility(true)
            }
        }
    }

    /**
     * コメント追いかけるボタンを表示、非表示する関数
     * @param visible 表示する場合はtrue。非表示にする場合はfalse
     * */
    private fun setFollowingButtonVisibility(visible: Boolean) {
        viewBinding.fragmentCommentFollowingButton.apply {
            if (isVisible != visible) {
                // 違うときのみ動作
                // ちなみに利用しているライブラリはこれ→https://github.com/takusan23/DropPopAlert
                if (visible) {
                    viewBinding.fragmentCommentFollowingButton.toDropPopAlert().showAlert(DropPopAlert.ALERT_DROP)
                } else {
                    viewBinding.fragmentCommentFollowingButton.toDropPopAlert().hideAlert(DropPopAlert.ALERT_DROP)
                }
            }
        }
    }

}