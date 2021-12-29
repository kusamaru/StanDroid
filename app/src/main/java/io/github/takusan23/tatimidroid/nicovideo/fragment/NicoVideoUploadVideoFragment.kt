package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoUploadVideoViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoUploadVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoPostBinding

/**
 * 投稿動画表示Fragment
 *
 * userId |String | ユーザーIDを入れるとそのユーザーの投稿した動画を取得しに行きます。ない場合は自分の投稿動画を取りに行きます
 * */
class NicoVideoUploadVideoFragment : Fragment() {

    /** データ取得とかデータ保持とか */
    private lateinit var nicoVideoUploadVideoViewModel: NicoVideoUploadVideoViewModel

    /** RecyclerViewへ渡す配列 */
    private val recyclerViewList = arrayListOf<NicoVideoData>()

    /** RecyclerViewへセットするAdapter */
    private val nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoPostBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        nicoVideoUploadVideoViewModel = ViewModelProvider(this, NicoVideoUploadVideoViewModelFactory(requireActivity().application, userId)).get(NicoVideoUploadVideoViewModel::class.java)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込み反映LiveData
        nicoVideoUploadVideoViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            viewBinding.fragmentNicovideoPostSwipeToRefresh.isRefreshing = isLoading
        }

        // データ受け取る
        nicoVideoUploadVideoViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { videoList ->
            // 更新
            recyclerViewList.addAll(videoList)
            nicoVideoListAdapter.notifyDataSetChanged()
            // スクロール
            viewBinding.fragmentNicovideoPostRecyclerView.apply {
                (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(nicoVideoUploadVideoViewModel.recyclerViewPos, nicoVideoUploadVideoViewModel.recyclerViewYPos)
            }
            // これで最後です。；；は配列の中身が一個以上あればな話。
            if (nicoVideoUploadVideoViewModel.isEnd && recyclerViewList.isNotEmpty()) {
                showToast(getString(R.string.end_scroll))
            }
            // からっぽなら投稿動画非公開メッセージ表示
            viewBinding.fragmentNicovideoPostPrivateMessageTextView.isVisible = recyclerViewList.isEmpty()

        }

        // スワイプ更新
        viewBinding.fragmentNicovideoPostSwipeToRefresh.setOnRefreshListener {
            nicoVideoUploadVideoViewModel.apply {
                // 位置直し
                recyclerViewPos = 0
                recyclerViewYPos = 0
                pageCount = 1
                // データリセット
                nicoVideoUploadVideoViewModel.nicoVideoDataListLiveData.value?.clear()
                recyclerViewList.clear()
                nicoVideoListAdapter.notifyDataSetChanged()
                // リクエスト
                getVideoList(pageCount)
            }
        }

    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoPostRecyclerView.apply {

            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(context)
            layoutManager = linearLayoutManager
            // Adapterセット
            adapter = nicoVideoListAdapter

            // 追加読み込み
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = recyclerView.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition()
                    //最後までスクロールしたときの処理
                    if (firstVisibleItem + visibleItemCount == totalItemCount && nicoVideoUploadVideoViewModel.loadingLiveData.value == false && !nicoVideoUploadVideoViewModel.isEnd) {
                        // 追加読み込み
                        nicoVideoUploadVideoViewModel.apply {
                            // 位置直し
                            recyclerViewPos = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                            recyclerViewYPos = getChildAt(0).top
                            // リクエスト
                            pageCount++
                            getVideoList(pageCount)
                        }
                    }
                }
            })
        }
    }

}