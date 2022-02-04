package com.kusamaru.standroid.nicolive

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.nicolive.adapter.CommentRecyclerViewAdapter
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.R
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.databinding.FragmentCommentRoomBinding

/**
 * 部屋別表示
 * */
class CommentRoomFragment : Fragment() {

    // CommentFragmentとそれのViewModel
    val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    /** コメント配列 */
    var recyclerViewList = arrayListOf<CommentJSONParse>()

    /** コメント一覧RecyclerViewAdapter */
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentCommentRoomBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initRecyclerView()

        viewBinding.commentRoomTabLayout.background = ColorDrawable(getThemeColor(context))

        // LiveDataで新規コメント監視
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
            if (comment.roomName == viewBinding.commentRoomTabLayout.getTabAt(viewBinding.commentRoomTabLayout.selectedTabPosition)?.text) {
                recyclerViewList.add(0, comment)
                commentRecyclerViewAdapter.notifyItemInserted(0)
                recyclerViewScrollPos()
            }
        }

    }

    // Fragmentに来たら
    override fun onResume() {
        super.onResume()
        // 今つながってる部屋分TabItem生成する
        viewBinding.commentRoomTabLayout.apply {
            removeAllTabs()
            // 部屋統合+Store
            addTab(newTab().setText(getString(R.string.room_integration)))
            addTab(newTab().setText(getString(R.string.room_limit)))
            //TabLayout選択
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {

                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    // 接続
                    if (tab?.text == getString(R.string.room_integration)) {
                        setCommentList(true)
                    } else {
                        setCommentList(false)
                    }
                }
            })
            setCommentList(true)
        }
    }

    /**
     * コメント一覧を作成する。コメント内容はViewModelとLiveDataからもらうので、WebSocket接続はしない
     * @param isAllRoom 部屋統合のコメントを表示する場合はtrue
     * */
    private fun setCommentList(isAllRoom: Boolean = true) {
        val roomName = if (isAllRoom) getString(R.string.room_integration) else getString(R.string.room_limit)
        val list = viewModel.commentList.filter { commentJSONParse: CommentJSONParse? -> commentJSONParse?.roomName == roomName }
        recyclerViewList.clear()
        recyclerViewList.addAll(list)
        commentRecyclerViewAdapter.notifyDataSetChanged()
    }


    /**
     * スクロール位置をゴニョゴニョする関数。追加時に呼んでね
     * もし一番上なら->新しいコメント来ても一番上に追従する
     * 一番上にいない->この位置に留まる
     * */
    private fun recyclerViewScrollPos() {
        if (!isAdded) return
        // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
        val pos = (viewBinding.commentRoomRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var top = 0
        if ((viewBinding.commentRoomRecyclerView.layoutManager as LinearLayoutManager).childCount > 0) {
            top = (viewBinding.commentRoomRecyclerView.layoutManager as LinearLayoutManager).getChildAt(0)!!.top
        }
        //一番上なら追いかける
        if (pos == 0 || pos == 1) {
            viewBinding.commentRoomRecyclerView.scrollToPosition(0)
        } else {
            viewBinding.commentRoomRecyclerView.post {
                (viewBinding.commentRoomRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    pos + 1,
                    top
                )
            }
        }
    }

    private fun initRecyclerView() {
        viewBinding.commentRoomRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList, requireParentFragment())
            adapter = commentRecyclerViewAdapter
            itemAnimator = null
            //区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}