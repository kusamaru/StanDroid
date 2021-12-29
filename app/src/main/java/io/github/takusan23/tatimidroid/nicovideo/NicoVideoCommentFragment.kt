package io.github.takusan23.tatimidroid.nicovideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoCommentBinding
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * コメント表示Fragment
 * */
class NicoVideoCommentFragment : Fragment() {

    val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    var recyclerViewList = arrayListOf<CommentJSONParse>()
    private lateinit var nicoVideoAdapter: NicoVideoAdapter

    /**
     * 自動追従（自動でコメント一覧スクロール機能）を利用するか。
     * 上方向へスクロールすることでこの値はfalseになる。（RecyclerView#addOnScrollListener()の部分）
     * falseになったら「追いかけるボタン」を押すことで再度trueになります。
     * */
    var isAutoScroll = true

    /** ViewModel */
    val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoCommentBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // コメント監視
        viewModel.commentList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
        }

        if (prefSetting.getBoolean("setting_oikakeru_hide", false)) {
            // 追いかけるボタンを利用しない
            setFollowingButtonVisibility(false)
            lifecycleScope.launch {
                while (isActive) {
                    delay(1000)
                    scroll(viewModel.playerCurrentPositionMs)
                }
            }
        } else {
            // 追いかけるボタンを利用する
            // スクロールボタン。追従するぞい
            viewBinding.fragmentNicovideoCommentFollowingButton.setOnClickListener {
                // スクロール
                scroll(viewModel.playerCurrentPositionMs)
                // Visibilityゴーン。誰もカルロス・ゴーンの話しなくなったな
                setFollowingButtonVisibility(false)
                isAutoScroll = true
            }
            lifecycleScope.launch {
                while (isActive) {
                    delay(1000)
                    // スクロール
                    setScrollFollowButton(viewModel.playerCurrentPositionMs)
                }
            }
        }

    }

    /**
     * RecyclerView初期化とか。でもよく動かなくなるので表示できなかった場合は[onResume]でも初期化してる
     * @param commentList RecyclerViewに表示させる中身の配列
     * */
    fun initRecyclerView(commentList: ArrayList<CommentJSONParse>) {
        viewBinding.fragmentNicovideoCommentRecyclerView.apply {
            recyclerViewList = commentList
            setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            layoutManager = mLayoutManager
            // Adapter用意。
            nicoVideoAdapter = NicoVideoAdapter(commentList, parentFragmentManager, viewModel.isOfflinePlay.value ?: false, viewModel.nicoruAPI)
            adapter = nicoVideoAdapter
            //区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            if (itemDecorationCount == 0) {
                addItemDecoration(itemDecoration)
            }
            // スクロールイベント。上方向へスクロールをしたら自動追従を解除する設定にした。
            // これで自動スクロール止めたい場合は上方向へスクロールしてください。代わりに追いかけるボタンが表示されます。
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    // 自動スクロールが有効になってるときのみ監視する。自動スクロールOFFの状態でも動くようにすると勝手にスクロールされる問題があった。
                    if (isAutoScroll) {
                        isAutoScroll = dy >= 0
                    }
                }
            })
        }
    }

    /**
     * 追いかけるボタンを表示/非表示する関数。
     * @param milliSec 現在の再生時間。ミリ秒でたのんだ(ExoPlayer#currentPosition)
     * */
    fun setScrollFollowButton(milliSec: Long) {
        // Attachしてなければ落とす
        if (!isAdded) return
        // 追従有効時。この値は上方向スクロールをするとfalseになる。
        if (isAutoScroll) {
            // スクロール実行
            scroll(milliSec)
            // スクロールしたので追いかけるボタンを非表示にする
            setFollowingButtonVisibility(false)
        } else {
            // 追いかけるボタン表示
            setFollowingButtonVisibility(true)
        }
    }

    /**
     * RecyclerViewをスクロールする
     * @param milliSec 再生時間（ミリ秒！！！）。
     * */
    fun scroll(milliSec: Long) {
        if (!isAdded) return
        // スクロールしない設定 / ViewPagerまだ初期化してない
        if (prefSetting.getBoolean("nicovideo_comment_scroll", false)) {
            return
        }
        // Nullチェック
        val devNicoVideoCommentFragment = this
        viewBinding.fragmentNicovideoCommentRecyclerView.apply {
            val list = devNicoVideoCommentFragment.recyclerViewList
            // findを使って条件に合うコメントのはじめの位置を取得する。ここでは 今の時間から一秒引いた時間 と同じか大きいくて最初の値。
            var currentPosCommentFirst = list.indexOfFirst { commentJSONParse -> commentJSONParse.vpos.toInt() >= milliSec / 10 }
            // 上に合わせるんじゃなくて、下に合わせて欲しい。
            val visibleCount = getVisibleCommentList()?.size ?: 0
            currentPosCommentFirst -= visibleCount
            // スクロール
            (this.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentPosCommentFirst, 0)
        }
    }

    /**
     * 現在表示されているリストの中で一番下に表示されれいるコメントを返す
     * RecyclerViewが初期化されていない場合はnullになります。
     * こいつ関数名考えるの下手だな
     * */
    fun getCommentListVisibleLastItemComment(): CommentJSONParse? {
        // RecyclerView初期化してない時はnull
        return recyclerViewList[(viewBinding.fragmentNicovideoCommentRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()]
    }

    /**
     * 現在表示されているリストの中で一番下に表示されれいるコメントが現在再生中の位置のコメントの場合はtrue
     * @param sec 再生時間。10など
     * @return 表示中の中で一番最後のアイテムが 再生時間 と同じならtrue
     * */
    fun isCheckLastItemTime(sec: Long): Boolean {
        return sec / 10 == getCommentListVisibleLastItemComment()?.vpos?.toLong()
    }

    /**
     * コメント追いかけるボタンを表示、非表示する関数
     * @param visible 表示する場合はtrue。非表示にする場合はfalse
     * */
    fun setFollowingButtonVisibility(visible: Boolean) {
        // 現在と違うときのみ
        if (viewBinding.fragmentNicovideoCommentFollowingButton.isVisible != visible) {
            // 自作ライブラリ
            if (visible) {
                viewBinding.fragmentNicovideoCommentFollowingButton.toDropPopAlert().showAlert(DropPopAlert.ALERT_DROP)
            } else {
                viewBinding.fragmentNicovideoCommentFollowingButton.toDropPopAlert().hideAlert(DropPopAlert.ALERT_DROP)
            }
        }
    }

    /** LayoutManager取得。書くのめんどくさくなったので */
    private fun getRecyclerViewLayoutManager(): LinearLayoutManager? {
        if (viewBinding.fragmentNicovideoCommentRecyclerView.layoutManager !is LinearLayoutManager) return null
        return viewBinding.fragmentNicovideoCommentRecyclerView.layoutManager as LinearLayoutManager
    }

    /**
     * 現在コメントが表示中かどうか。
     * @param commentJSONParse コメント
     * @return 表示中ならtrue。
     * */
    fun checkVisibleItem(commentJSONParse: CommentJSONParse?): Boolean {
        val rangeItems = getVisibleCommentList() ?: return false
        return rangeItems.find { item -> item == commentJSONParse } != null
    }

    /**
     * 現在RecyclerViewに表示中のコメントを配列で取得する関数
     * 注意：LinearLayoutManager#findLastVisibleItemPosition()が中途半端に表示しているViewのことを数えないので１足してます。
     * */
    fun getVisibleCommentList(): MutableList<CommentJSONParse>? {
        val manager = getRecyclerViewLayoutManager() ?: return null
        // 一番最初+一番最後の場所
        val firstVisiblePos = manager.findFirstVisibleItemPosition()
        val lastVisiblePos = manager.findLastVisibleItemPosition() + 1
        // IndexOutOfBoundsException: fromIndex = -1 対策
        if (firstVisiblePos == -1 || lastVisiblePos == -1) {
            return null
        }
        return recyclerViewList.subList(firstVisiblePos, lastVisiblePos)
    }

    /**
     * 現在RecyclerViewに表示中のコメントがすべて同じ時間（職人のコメントとか長くなって複数行にまたがるからワンチャンある。というかあった）
     * かどうかを判断する関数。
     * 注意：秒レベルで判断します。vposとかは1s=100vposだけど秒になおして扱います。
     * */
    fun getVisibleListItemEqualsTime(): Boolean {
        // 表示中コメント
        val rangeItem = getVisibleCommentList() ?: return false
        // 一番最初の値
        val firstTime = rangeItem.first().vpos.toInt() / 100
        // 同じなら配列から消して、中身が０になれば完成。Array#none{}は全てに一致しなければtrueを返す
        return rangeItem.none { commentJSONParse -> commentJSONParse.vpos.toInt() / 100 != firstTime }
    }

}
