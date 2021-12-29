package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nguploader.NGUploaderTool
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.nicoapi.NicoVideoCache
import io.github.takusan23.tatimidroid.nicoapi.XMLCommentJSON
import io.github.takusan23.tatimidroid.nicoad.NicoAdBottomFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.JCNicoVideoCommentOnlyFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoCacheFragmentViewModel
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoMyListListViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.service.BackgroundPlaylistCachePlayService
import io.github.takusan23.tatimidroid.service.startCacheService
import io.github.takusan23.tatimidroid.service.startVideoPlayService
import io.github.takusan23.tatimidroid.tool.NICOVIDEO_ID_REGEX
import io.github.takusan23.tatimidroid.tool.isNotLoginMode
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoListMenuBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * マイリスト、キャッシュ取得ボタンがあるBottomSheet。動画IDとキャッシュかどうかを入れてください。
 * 入れてほしいもの
 * video_id | String  | 動画ID。画面回転時に詰むのでこっちがいい？
 * is_cache | Boolean | キャッシュの場合はtrue
 * --- できれば（インターネット接続無いと詰む） ---
 * data         | NicoVideoData     | 入ってる場合はインターネットでの取得はせず、こちらを使います。
 * video_list   | NicoVideoData[]   | 連続再生で使う。なくてもいい
 * */
class NicoVideoListMenuBottomFragment : BottomSheetDialogFragment() {

    // データもらう
    lateinit var nicoVideoData: NicoVideoData
    lateinit var nicoVideoHTML: NicoVideoHTML
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // by lazy 使うか～（使うときまで lazy {} の中身は実行されない）
    val videoId by lazy { arguments?.getString("video_id")!! }
    val isCache by lazy { arguments?.getBoolean("is_cache") ?: false }

    lateinit var mediaBrowserCompat: MediaBrowserCompat
    lateinit var mediaControllerCompat: MediaControllerCompat

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoListMenuBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
        nicoVideoHTML = NicoVideoHTML()

        // MediaBrowserと接続
        initMediaBrowserConnect()

        // コルーチン内のエラーを捕まえる
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            requireActivity().runOnUiThread {
                Toast.makeText(context, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
            }
        }
        // データ取得するかどうか。
        lifecycleScope.launch(Dispatchers.Main + errorHandler) {
            // NicoVideoDataある時
            val serializeData = arguments?.getSerializable("data")
            if (serializeData != null) {
                nicoVideoData = serializeData as NicoVideoData
            } else {
                // 無い時はインターネットから取得
                withContext(Dispatchers.IO) {
                    // データ取得
                    val response = nicoVideoHTML.getHTML(videoId, userSession)
                    if (!response.isSuccessful) {
                        // 失敗時
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                    // ぱーさー
                    val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                    nicoVideoData = nicoVideoHTML.createNicoVideoData(jsonObject, isCache)
                }
            }

            // タイトル、ID設定
            viewBinding.bottomFragmentNicovideoListMenuTitleTextView.text = nicoVideoData.title
            viewBinding.bottomFragmentNicovideoListMenuIdTextView.text = nicoVideoData.videoId

            // コピーボタン
            initCopyButton()

            // マイリスト登録ボタン
            mylistButton()

            // キャッシュ関係
            cacheButton()

            // 再生、ポップアップ再生、バッググラウンド再生ボタン初期化
            playServiceButton()

            // ブラウザで開くボタン
            initOpenBrowser()

            // 連続再生
            initPlayListPlayButton()

            // コメント一覧のみを表示する
            initCommentListButton()

            // ニコニ広告
            initNicoAdButton()

            // NG投稿者
            initNGUploaderButton()
        }

    }

    private fun initNGUploaderButton() {
        if (prefSetting.getBoolean("nicovideo_ng_uploader_enable", false) && !isCache && videoId.contains("sm")) {
            // 投稿者NG機能有効時 + キャッシュ以外 + 公式以外
            viewBinding.bottomFragmentNicovideoListMenuNgUploaderTextView.setOnClickListener {
                // NG投稿者として追加
                val ngUploaderTool = NGUploaderTool(requireContext())
                lifecycleScope.launch { ngUploaderTool.addNGUploaderIdFromVideoId(videoId) }
            }
        } else {
            viewBinding.bottomFragmentNicovideoListMenuNgUploaderTextView.isVisible = false
        }
    }

    private fun initNicoAdButton() {
        // ニコニ広告のBottomFragmentを表示する
        viewBinding.bottomFragmentNicovideoListMenuNicoadButton.setOnClickListener {
            NicoAdBottomFragment().apply {
                arguments = Bundle().apply {
                    putString("content_id", videoId)
                }
            }.show(parentFragmentManager, "nicoad")
        }
    }

    private fun initCommentListButton() {
        // コメント一覧のみを表示する
        viewBinding.bottomFragmentNicovideoListMenuCommmentOnly.setOnClickListener {
            val commentListHostFragment = JCNicoVideoCommentOnlyFragment().apply {
                arguments = Bundle().apply {
                    putString("id", videoId)
                }
            }
            (requireActivity() as? MainActivity)?.setFragment(commentListHostFragment, "comment_list")
            dismiss()
        }
    }

    private fun initPlayListPlayButton() {
        // 連続再生
        val videoList = arguments?.getSerializable("video_list") as? ArrayList<NicoVideoData>
        if (videoList != null) {
            viewBinding.bottomFragmentNicovideoListMenuPlaylistTextView.setOnClickListener {
                (requireActivity() as MainActivity).setNicovideoFragment(videoId = videoId, _videoList = videoList)
                // メニュー閉じる
                dismiss()
            }
        } else {
            // セットされてない場合は非表示
            viewBinding.bottomFragmentNicovideoListMenuPlaylistTextView.isVisible = false
        }
    }

    private fun initOpenBrowser() {
        // キャッシュ一覧では表示させない
        if (isCache) {
            viewBinding.bottomFragmentNicovideoListMenuBrowserTextView.visibility = View.GONE
        }
        // ブラウザで開く。公式アニメが暗号化で見れん時に使って。
        viewBinding.bottomFragmentNicovideoListMenuBrowserTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://nico.ms/${nicoVideoData.videoId}".toUri())
            startActivity(intent)
        }
    }


    // ポップアップ再生、バッググラウンド再生ボタン初期化
    private fun playServiceButton() {
        viewBinding.bottomFragmentNicovideoListMenuPopupTextView.setOnClickListener {
            val videoList = arguments?.getSerializable("video_list") as? ArrayList<NicoVideoData>
            startVideoPlayService(context = context, mode = "popup", videoId = nicoVideoData.videoId, isCache = isCache, playlist = videoList)
        }
        viewBinding.bottomFragmentNicovideoListMenuBackgroundTextView.setOnClickListener {
            startVideoPlayService(context = context, mode = "background", videoId = nicoVideoData.videoId, isCache = isCache)
        }
        viewBinding.bottomFragmentNicovideoListMenuPlayTextView.setOnClickListener {
            // 通常再生
            (requireActivity() as MainActivity).setNicovideoFragment(videoId = nicoVideoData.videoId, isCache = isCache)
        }
        // 強制エコノミーはキャッシュでは塞ぐ
        if (isCache) {
            viewBinding.bottomFragmentNicovideoListMenuEconomyPlayTextView.visibility = View.GONE
        }
        viewBinding.bottomFragmentNicovideoListMenuEconomyPlayTextView.setOnClickListener {
            // エコノミーで再生
            (requireActivity() as MainActivity).setNicovideoFragment(videoId = nicoVideoData.videoId, isEco = true)
        }
        // インターネットを利用して再生。キャッシュ以外でなお動画IDじゃないときは表示しない
        if (isCache && NICOVIDEO_ID_REGEX.toRegex().matches(videoId)) {
            viewBinding.bottomFragmentNicovideoListMenuInternetPlayTextView.apply {
                isVisible = true
                setOnClickListener {
                    (requireActivity() as MainActivity).setNicovideoFragment(videoId = nicoVideoData.videoId, useInternet = true)
                }
            }
        }
    }

    // IDコピーボタン
    private fun initCopyButton() {
        viewBinding.bottomFragmentNicovideoListMenuCopyTextView.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", nicoVideoData.videoId))
            Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${nicoVideoData.videoId}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // マイリスト。そのうち作る
    private fun mylistButton() {
        // 動画ID以外はマイリスト登録ボタンを消す
        if (nicoVideoData.videoId.contains("sm") || nicoVideoData.videoId.contains("so")) {
            viewBinding.bottomFragmentNicovideoListMenuMylistTextView.isVisible = true
            viewBinding.bottomFragmentNicovideoListMenuAtodemiruTextView.isVisible = true
        } else {
            viewBinding.bottomFragmentNicovideoListMenuMylistTextView.isVisible = false
            viewBinding.bottomFragmentNicovideoListMenuAtodemiruTextView.isVisible = false
        }
        // マイリスト画面の場合は消すに切り替える
        if (nicoVideoData.isMylist) {
            viewBinding.bottomFragmentNicovideoListMenuMylistTextView.text = getString(R.string.mylist_delete)
            viewBinding.bottomFragmentNicovideoListMenuMylistTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_outline_delete_24px, 0, 0, 0)
            viewBinding.bottomFragmentNicovideoListMenuAtodemiruTextView.isVisible = false
        }
        // 非ログインモード時も消す
        if (isNotLoginMode(context)) {
            viewBinding.bottomFragmentNicovideoListMenuMylistTextView.isVisible = false
            viewBinding.bottomFragmentNicovideoListMenuAtodemiruTextView.isVisible = false
        }
        viewBinding.bottomFragmentNicovideoListMenuMylistTextView.setOnClickListener {
            if (nicoVideoData.isMylist) {
                // 本当に消していい？
                val buttonItems = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete), R.drawable.ic_outline_delete_24px))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.parseColor("#ff0000")))
                }
                DialogBottomSheet(getString(R.string.mylist_video_delete), buttonItems) { i, bottomSheetDialogFragment ->
                    if (i == 0) {
                        // 消す
                        lifecycleScope.launch(Dispatchers.Main) {
                            // マイリスト削除API叩く。スマホ版のAPI
                            val nicoVideoSPMyListAPI = NicoVideoSPMyListAPI()
                            val deleteResponse = withContext(Dispatchers.IO) {
                                if (!nicoVideoData.isToriaezuMylist) {
                                    // マイリストから動画を削除
                                    nicoVideoSPMyListAPI.deleteMyListVideo(nicoVideoData.mylistId!!, nicoVideoData.mylistItemId, userSession)
                                } else {
                                    // とりあえずマイリストから動画を削除
                                    nicoVideoSPMyListAPI.deleteToriaezuMyListVideo(nicoVideoData.mylistItemId, userSession)
                                }
                            }
                            if (deleteResponse.isSuccessful) {
                                showToast(getString(R.string.mylist_delete_ok))
                                this@NicoVideoListMenuBottomFragment.dismiss()
                                // 再読み込み
                                val viewModel by viewModels<NicoVideoMyListListViewModel>({ parentFragmentManager.findFragmentById(R.id.fragment_video_list_linearlayout)!! })
                                viewModel.getMyListVideoList()
                            } else {
                                showToast("${getString(R.string.error)}\n${deleteResponse.code}")
                            }
                        }
                    }
                }.show(this@NicoVideoListMenuBottomFragment.parentFragmentManager, "delete")
            } else {
                // マイリスト一覧以外。追加ボタン
                val mylistBottomFragment = NicoVideoAddMylistBottomFragment()
                val bundle = Bundle().apply {
                    putString("id", nicoVideoData.videoId)
                }
                mylistBottomFragment.arguments = bundle
                mylistBottomFragment.show((activity as AppCompatActivity).supportFragmentManager, "mylist")
            }
        }
        // あとで見る（とりあえずマイリスト）に追加する
        viewBinding.bottomFragmentNicovideoListMenuAtodemiruTextView.setOnClickListener {
            // あとで見るに追加する
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                withContext(Dispatchers.Main) {
                    // 消せないように
                    isCancelable = false
                }
                // あとで見る追加APIを叩く
                val spMyListAPI = NicoVideoSPMyListAPI()
                val atodemiruResponse = spMyListAPI.addAtodemiruListVideo(userSession, videoId)
                if (!atodemiruResponse.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${atodemiruResponse.code}")
                    return@launch
                }
                // 成功したか
                when (atodemiruResponse.code) {
                    201 -> {
                        // 成功時
                        showToast(getString(R.string.atodemiru_ok))
                    }
                    200 -> {
                        // すでに追加済み
                        showToast(getString(R.string.already_added))
                    }
                    else -> {
                        // えらー
                        showToast(getString(R.string.error))
                    }
                }
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }
    }

    // キャッシュ再取得とか削除とか（削除以外未実装）
    private fun cacheButton() {
        // キャッシュ関係
        val nicoVideoCache = NicoVideoCache(context)

        if (isCache) {
            // キャッシュのときは再取得メニュー表示させる
            viewBinding.bottomFragmentNicovideoListMenuCacheMenu.visibility = View.VISIBLE
            viewBinding.bottomFragmentNicovideoListMenuGetCacheTextView.visibility = View.GONE
            viewBinding.bottomFragmentNicovideoListMenuGetCacheEconomyTextView.visibility = View.GONE
        } else {
            // キャッシュ無いときは取得ボタンを置く
            viewBinding.bottomFragmentNicovideoListMenuCacheMenu.visibility = View.GONE
            viewBinding.bottomFragmentNicovideoListMenuGetCacheTextView.visibility = View.VISIBLE
            viewBinding.bottomFragmentNicovideoListMenuGetCacheEconomyTextView.visibility = View.VISIBLE
        }

        // キャッシュ取得
        viewBinding.bottomFragmentNicovideoListMenuGetCacheTextView.setOnClickListener {
            // キャッシュ取得Service起動
            startCacheService(false)
        }

        // キャッシュ取得（エコノミーモード）
        viewBinding.bottomFragmentNicovideoListMenuGetCacheEconomyTextView.setOnClickListener {
            // キャッシュ取得Service起動
            startCacheService(true)
        }

        // キャッシュ削除
        viewBinding.bottomFragmentNicovideoListMenuDeleteCacheTextView.setOnClickListener {
            // 本当に消していいか聞くダイアログ作成
            val buttonItems = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cache_delete), R.drawable.ic_outline_delete_24px))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.parseColor("#ff0000")))
            }
            val okCancelBottomSheetFragment = DialogBottomSheet(getString(R.string.cache_delete_message), buttonItems) { i, bottomSheetDialogFragment ->
                if (i == 0) {
                    val viewModel by viewModels<NicoVideoCacheFragmentViewModel>({ requireParentFragment() })
                    viewModel.cacheVideoList
                    nicoVideoCache.deleteCache(nicoVideoData.videoId)
                    // 再読み込み
                    viewModel.init()
                    dismiss()
                }
            }
            okCancelBottomSheetFragment.show(parentFragmentManager, "delete_dialog")
        }

        // 動画ID以外は非表示にする処理
        if (NICOVIDEO_ID_REGEX.toRegex().matches(videoId)) {
            viewBinding.bottomFragmentNicovideoListMenuReGetCacheTextView.visibility = View.VISIBLE
        } else {
            viewBinding.bottomFragmentNicovideoListMenuReGetCacheTextView.visibility = View.GONE
        }
        // キャッシュの動画情報、コメント更新
        viewBinding.bottomFragmentNicovideoListMenuReGetCacheTextView.setOnClickListener {
            // キャッシュ取得中はBottomFragmentを消させないようにする
            this.isCancelable = false
            viewBinding.bottomFragmentNicovideoListMenuReGetCacheTextView.text = getString(R.string.cache_updateing)
            // 再取得
            lifecycleScope.launch {
                nicoVideoCache.getReGetVideoInfoComment(nicoVideoData.videoId, userSession, context)
                dismiss()
            }
        }

        // XML形式をJSON形式に変換する
        // コメントファイル（XML）があれば表示させる
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(nicoVideoData.videoId) != null) {
            viewBinding.bottomFragmentNicovideoListMenuXmlToJsonTextView.visibility = View.VISIBLE
        }
        viewBinding.bottomFragmentNicovideoListMenuXmlToJsonTextView.setOnClickListener {
            // BottomSheet消えないように。
            this@NicoVideoListMenuBottomFragment.isCancelable = false
            showToast(getString(R.string.wait))
            lifecycleScope.launch {
                val status = xmlCommentJSON.xmlToJSON(nicoVideoData.videoId).await()
                showToast(getString(R.string.xml_to_json_complete))
                // 消す
                activity?.runOnUiThread {
                    this@NicoVideoListMenuBottomFragment.dismiss()
                }
            }
        }

    }

    /**
     * キャッシュ取得関数（？）
     * まあService起動させてるだけなんですけどね。
     * @param isEconomy エコノミーモードで取得する場合はtrue。
     * */
    private fun startCacheService(isEconomy: Boolean = false) {
        // キャッシュ取得Service起動
        val result = startCacheService(context, nicoVideoData.videoId, isEconomy, false)
        // 閉じる
        dismiss()
        // 取得済みならToast出す
        if (!result) {
            Toast.makeText(context, getString(R.string.cache_has_video_file), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** 連続再生開始ボタン設定 */
    private fun initCachePlaylistPlay() {
        if (isCache) {
            viewBinding.bottomFragmentNicovideoListMenuPlaylistBackgroundTextView.visibility = View.VISIBLE
        }
        viewBinding.bottomFragmentNicovideoListMenuPlaylistBackgroundTextView.setOnClickListener {
            // ボタン押した時は動画IDを指定して再生
            mediaControllerCompat.transportControls.playFromMediaId(videoId, null)
        }
    }

    /** [BackgroundPlaylistCachePlayService]と接続する関数 */
    private fun initMediaBrowserConnect() {
        // MediaBrowser
        mediaBrowserCompat = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), BackgroundPlaylistCachePlayService::class.java), object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                super.onConnected()
                mediaControllerCompat = MediaControllerCompat(requireContext(), mediaBrowserCompat.sessionToken)
                // 連続再生ボタン押せるように
                initCachePlaylistPlay()
            }
        }, null)
        // 接続
        mediaBrowserCompat.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        nicoVideoHTML.destroy()
        mediaBrowserCompat.disconnect()
    }

}