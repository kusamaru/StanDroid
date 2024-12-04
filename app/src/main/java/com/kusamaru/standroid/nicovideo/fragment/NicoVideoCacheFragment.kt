package com.kusamaru.standroid.nicovideo.fragment

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.media3.common.Player
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.nicoapi.cache.CacheJSON
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoListAdapter
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoCacheFilterBottomFragment
import com.kusamaru.standroid.nicovideo.viewmodel.NicoVideoCacheFragmentViewModel
import com.kusamaru.standroid.R
import com.kusamaru.standroid.service.BackgroundPlaylistCachePlayService
import com.kusamaru.standroid.databinding.FragmentNicovideoCacheBinding
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * キャッシュ一覧を表示するFragment
 * */
class NicoVideoCacheFragment : Fragment() {

    // 必要なやつ
    lateinit var prefSetting: SharedPreferences
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    /** バックグラウンド連続再生のMediaSessionへ接続する */
    private var mediaBrowser: MediaBrowserCompat? = null

    /** ViewModel。画面回転時に再読み込みされるのつらいので */
    private val viewModel by viewModels<NicoVideoCacheFragmentViewModel>({ this })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoCacheBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // くるくる出す
        viewBinding.fragmentNicovideoCacheSwipeRefresh.isRefreshing = true
        viewBinding.fragmentNicovideoCacheSwipeRefresh.setOnRefreshListener {
            // F5
            viewModel.init()
        }

        // RecyclerView
        viewModel.recyclerViewList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
            viewBinding.fragmentNicovideoCacheSwipeRefresh.isRefreshing = false
            // 中身0だった場合
            viewBinding.fragmentNicovideoCacheEmptyMessageTextView.isVisible = list.isEmpty()
        }

        // 合計容量
        viewModel.totalUsedStorageGB.observe(viewLifecycleOwner) { gb ->
            // 保存先
            viewModel.storageMessage.observe(viewLifecycleOwner) { storageMsg ->
                viewBinding.fragmentNicovideoCacheStorageInfoTextView.text = """
                ${getString(R.string.cache_usage)}：$gb GB
                $storageMsg
                """.trimIndent()
            }
        }

        // フィルター / 並び替え BottomFragment
        viewBinding.fragmentNicovideoCacheMenuFilterTextview.setOnClickListener {
            val cacheFilterBottomFragment = NicoVideoCacheFilterBottomFragment()
            cacheFilterBottomFragment.show(childFragmentManager, "filter")
            viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
        }

        viewBinding.fragmentNicovideoCacheMenuPlaylistTextview.setOnClickListener {
            // 連続再生
            if (viewModel.recyclerViewList.value != null) {
                (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId = viewModel.recyclerViewList.value!![0].videoId, _videoList = viewModel.recyclerViewList.value)
            }
            // メニュー閉じる
            viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
        }

        // バックグラウンド連続再生
        viewBinding.fragmentNicovideoCacheMenuBackgroundTextview.setOnClickListener {
            lifecycleScope.launch {
                connectMediaSession()
                // このActivityに関連付けられたMediaSessionControllerを取得
                val controller = MediaControllerCompat.getMediaController(requireActivity())
                // 最後に再生した曲を なければ配列の最初。それもなければ再生しない
                val videoId = prefSetting.getString("cache_last_play_video_id", null) ?: viewModel.recyclerViewList.value?.first()?.videoId
                if (videoId != null) {
                    controller.transportControls.playFromMediaId(videoId, null)
                }
                viewBinding.fragmentNicovideoCacheCardMotionLayout.transitionToStart()
                // リピート、シャッフルボタンセット
                controller.registerCallback(object : MediaControllerCompat.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                        super.onPlaybackStateChanged(state)
                        initRepeatShuffleButton()
                    }
                })
            }
        }

        // キャッシュ用バックグラウンドに連続再生起動中の場合はリピート、シャッフルボタンを表示させる
        if (isCheckRunningBackgroundCachePlayService()) {
            initRepeatShuffleButton()
        }

    }

    /** リピート、シャッフルボタンを設定 */
    private fun initRepeatShuffleButton() {
        // 多分いる
        if (!isAdded) return

        /** リピートアイコンをセットする */
        fun setRepeatIcon(mode: Int) {
            val icon = when (mode) {
                Player.REPEAT_MODE_OFF -> {
                    requireContext().getDrawable(R.drawable.ic_arrow_downward_black_24dp)
                }
                Player.REPEAT_MODE_ALL -> {
                    requireContext().getDrawable(R.drawable.ic_repeat_black_24dp)
                }
                Player.REPEAT_MODE_ONE -> {
                    requireContext().getDrawable(R.drawable.ic_repeat_one_24px)
                }
                else -> requireContext().getDrawable(R.drawable.ic_trending_flat_black_24dp)
            }
            viewBinding.fragmentNicovideoCacheMenuBackgroundRepeatImageView.setImageDrawable(icon)
        }

        /** シャッフルアイコンをセットする */
        fun setShuffleIcon(isShuffleMode: Boolean) {
            val icon = if (isShuffleMode) {
                requireContext().getDrawable(R.drawable.ic_shuffle_black_24dp)
            } else {
                requireContext().getDrawable(R.drawable.ic_trending_flat_black_24dp)
            }
            viewBinding.fragmentNicovideoCacheMenuBackgroundShuffleImageView.setImageDrawable(icon)
        }

        // アイコン表示
        viewBinding.fragmentNicovideoCacheMenuBackgroundRepeatImageView.isVisible = true
        viewBinding.fragmentNicovideoCacheMenuBackgroundShuffleImageView.isVisible = true

        lifecycleScope.launch {
            if (mediaBrowser == null) {
                // MediaSession接続
                connectMediaSession()
            }

            // このActivityに関連付けられたMediaSessionControllerを取得
            val controller = MediaControllerCompat.getMediaController(requireActivity())

            // 初期化
            setRepeatIcon(prefSetting.getInt("cache_repeat_mode", 0))
            setShuffleIcon(prefSetting.getBoolean("cache_shuffle_mode", false))

            // リピートボタン押したとき
            viewBinding.fragmentNicovideoCacheMenuBackgroundRepeatImageView.setOnClickListener {
                val mode = when (prefSetting.getInt("cache_repeat_mode", 0)) {
                    Player.REPEAT_MODE_OFF -> {
                        // Off -> All Repeat
                        PlaybackStateCompat.REPEAT_MODE_ALL
                    }
                    Player.REPEAT_MODE_ALL -> {
                        // All Repeat -> Repeat One
                        PlaybackStateCompat.REPEAT_MODE_ONE
                    }
                    Player.REPEAT_MODE_ONE -> {
                        // Repeat One -> Off
                        PlaybackStateCompat.REPEAT_MODE_NONE
                    }
                    else -> PlaybackStateCompat.REPEAT_MODE_ALL
                }
                // アイコン用意
                controller.transportControls.setRepeatMode(mode)
                setRepeatIcon(mode)
            }
            // シャッフルボタン押したとき
            viewBinding.fragmentNicovideoCacheMenuBackgroundShuffleImageView.setOnClickListener {
                val mode = if (prefSetting.getBoolean("cache_shuffle_mode", false)) {
                    // シャッフルON -> OFF
                    PlaybackStateCompat.SHUFFLE_MODE_NONE
                } else {
                    // シャッフルOFF -> ON
                    PlaybackStateCompat.SHUFFLE_MODE_ALL
                }
                controller.transportControls.setShuffleMode(mode)
                setShuffleIcon(mode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
            }
        }

    }

    /**
     * [BackgroundPlaylistCachePlayService]が起動中かどうかを判断する
     * 判断方法は今ある通知の中から通知IDが[BackgroundPlaylistCachePlayService.NOTIFICATION_ID]と同じものがあるかどうか
     *
     * なおAndroid 6以降のみ対応。5の場合は強制true
     * */
    private fun isCheckRunningBackgroundCachePlayService(): Boolean {
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // キャッシュ用バックグラウンドに連続再生サービスの通知が出ているかどうかを確認
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.any { notification -> notification.id == BackgroundPlaylistCachePlayService.NOTIFICATION_ID }
        } else {
            true
        }
    }

    /**
     * バックグラウンド連続再生のMediaBrowserService（音楽再生サービス）（[BackgroundPlaylistCachePlayService]）へ接続する関数
     * コルーチンで使えるようにした。
     * */
    private suspend fun connectMediaSession() = suspendCoroutine<Unit> {
        val callback = object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                if (mediaBrowser != null) {
                    // MediaSession経由で操作するやつ
                    val mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowser!!.sessionToken)
                    // Activityと関連付けることで、同じActivityなら操作ができる？（要検証）
                    MediaControllerCompat.setMediaController(requireActivity(), mediaControllerCompat)
                    it.resume(Unit)
                }
            }
        }
        mediaBrowser = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), BackgroundPlaylistCachePlayService::class.java), callback, null)
        // 忘れてた
        mediaBrowser?.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaBrowser?.disconnect()
    }

    // フィルター削除関数
    fun filterDeleteMessageShow() {
        // 本当に消していい？
        Snackbar.make(viewBinding.fragmentNicovideoCacheEmptyMessageTextView, getString(R.string.filter_clear_message), Snackbar.LENGTH_SHORT).apply {
            setAction(getString(R.string.reset)) {
                CacheJSON().deleteFilterJSONFile(context)
            }
            anchorView = viewBinding.fragmentNicovideoCacheFab
            show()
        }
    }

    /**
     * RecyclerView初期化
     * @param list NicoVideoDataの配列。RecyclerViewに表示させたい配列が別にある時だけ指定すればいいと思うよ
     * @param layoutManagerParcelable RecyclerViewのスクロール位置を復元できるらしい。RecyclerView#layoutManager#onSaveInstanceState()で取れる
     * https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
     * */
    fun initRecyclerView(list: ArrayList<NicoVideoData>, layoutManagerParcelable: Parcelable? = null) {
        viewBinding.fragmentNicovideoCacheRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            if (layoutManagerParcelable != null) {
                layoutManager?.onRestoreInstanceState(layoutManagerParcelable)
            }
            nicoVideoListAdapter = NicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

}