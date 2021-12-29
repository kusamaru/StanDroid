package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoPlayListAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoPlaylistBinding


/**
 * ニコ動連続再生で動画一覧を表示する
 * */
class NicoVideoPlayListBottomFragment : BottomSheetDialogFragment() {

    private lateinit var playlistAdapter: NicoVideoPlayListAdapter

    /** [io.github.takusan23.tatimidroid.nicovideo.NicoVideoFragment]のViewModel取得 */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoPlaylistBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // データ受け取り
        viewModel.playlistLiveData.value?.also { videoList ->
            // RecyclerViewセット
            playlistAdapter = NicoVideoPlayListAdapter(videoList, viewModel)
            playlistAdapter.nicoVideoPlayListBottomFragment = this
            initRecyclerView()

            // トータル何分
            val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
            viewBinding.bottomFragmentNicovideoPlaylistDurationChip.text = "${getString(R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}"

            // 件数
            viewBinding.bottomFragmentNicovideoPlaylistCountChip.text = "${getString(R.string.video_count)}：${videoList.size}"

            // 逆にする
            viewBinding.bottomFragmentNicovideoPlaylistReverseChip.setOnClickListener {
                val videoListTemp = ArrayList(videoList)
                videoList.clear()
                videoList.addAll(videoListTemp.reversed())
                playlistAdapter.notifyDataSetChanged()
                scrollPlayingItem()
                viewModel.isReversed.value = !(viewModel.isReversed.value ?: false)
            }

            // しゃっふる
            viewBinding.bottomFragmentNicovideoPlaylistShuffleChip.setOnClickListener {
                if (viewBinding.bottomFragmentNicovideoPlaylistShuffleChip.isChecked) {
                    // シャッフル
                    val videoListTemp = ArrayList(videoList)
                    videoList.clear()
                    videoList.addAll(videoListTemp.shuffled())
                } else {
                    // シャッフル戻す。このために video_id_list が必要だったんですね
                    val idList = viewModel.originVideoSortList ?: return@setOnClickListener
                    /** [List.sortedWith]と[Comparator]を使うことで、JavaScriptの` list.sort(function(a,b){ return a - b } `みたいな２つ比べてソートができる。 */
                    val videoListTemp = ArrayList(videoList)
                    videoList.clear()
                    videoList.addAll(videoListTemp.sortedWith { a, b -> idList.indexOf(a.videoId) - idList.indexOf(b.videoId) }) // Kotlin 1.4で更に書きやすくなった
                }
                playlistAdapter.notifyDataSetChanged()
                scrollPlayingItem()
                viewModel.isShuffled.value = viewBinding.bottomFragmentNicovideoPlaylistShuffleChip.isChecked
            }

            scrollPlayingItem()
        }

        // 閉じるボタン
        viewBinding.bottomFragmentNicovideoPlaylistCloseButton.setOnClickListener {
            dismiss()
        }

        // Chipにチェックを入れる
        viewModel.isReversed.observe(viewLifecycleOwner) { isChecked ->
            viewBinding.bottomFragmentNicovideoPlaylistReverseChip.isChecked = isChecked
        }
        viewModel.isShuffled.observe(viewLifecycleOwner) { isChecked ->
            viewBinding.bottomFragmentNicovideoPlaylistShuffleChip.isChecked = isChecked
        }


    }

    /** 再生中の動画までスクロールする */
    private fun scrollPlayingItem() {
        val layoutManager = viewBinding.bottomFragmentNicovideoPlaylistRecyclerView.layoutManager as LinearLayoutManager
        // 位置を特定
        val pos = viewModel.playlistLiveData.value?.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == viewModel.playingVideoId.value } ?: 0
        layoutManager.scrollToPosition(pos)
    }

    /**
     * BottomFragmentをどこまで広げるか。
     * @param state [BottomSheetBehavior.STATE_HALF_EXPANDED] など
     * */
    fun setBottomFragmentState(state: Int) {
        (dialog as BottomSheetDialog).behavior.state = state
    }


    fun initRecyclerView() {
        viewBinding.bottomFragmentNicovideoPlaylistRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = playlistAdapter
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            requireContext().getDrawable(R.drawable.recyclerview_dividers)?.apply {
                itemDecoration.setDrawable(this) // 区切りの色変更
            }
            addItemDecoration(itemDecoration)
        }
    }

}