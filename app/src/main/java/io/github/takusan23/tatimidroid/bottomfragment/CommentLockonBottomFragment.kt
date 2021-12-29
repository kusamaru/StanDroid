package io.github.takusan23.tatimidroid.bottomfragment

import android.content.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicoapi.user.UserAPI
import io.github.takusan23.tatimidroid.nicolive.adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.nicolive.bottomfragment.ProgramMenuBottomSheet
import io.github.takusan23.tatimidroid.nicolive.CommentFragment
import io.github.takusan23.tatimidroid.nicolive.compose.JCNicoLiveFragment
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.nicovideo.bottomfragment.NicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.JCNicoVideoFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoAccountFragment
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.room.entity.NGDBEntity
import io.github.takusan23.tatimidroid.room.init.KotehanDBInit
import io.github.takusan23.tatimidroid.room.init.NGDBInit
import io.github.takusan23.tatimidroid.tool.DarkModeSupport
import io.github.takusan23.tatimidroid.tool.NICOLIVE_ID_REGEX
import io.github.takusan23.tatimidroid.tool.NICOVIDEO_ID_REGEX
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.BottomFragmentCommentLockonBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * ロックオン芸。生放送(CommentFragment)か動画(DevNicoVideoFragment)じゃないと動きません。
 * comment  | String  | コメント本文
 * user_id  | String  | ユーザーID
 * liveId   | String  | 生放送ID（動画なら動画ID）
 * label    | String  | 部屋名とか（コメント本文の上にあるユーザーID書いてある部分）
 * 動画なら
 * current_pos | Long   | コメントのvpos。1秒==100vpos
 * */
class CommentLockonBottomFragment : BottomSheetDialogFragment() {

    //  lateinit var commentFragment: CommentFragment
    lateinit var prefSetting: SharedPreferences
    private var userSession = ""

    //それぞれ
    private var comment = ""
    private var userId = ""

    //RecyclerView
    var recyclerViewList = arrayListOf<CommentJSONParse>()

    // 動画Fragmentかどうか
    var isNicoVideoFragment = false

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentCommentLockonBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // ダークモード
        val darkModeSupport = DarkModeSupport(requireContext())
        viewBinding.bottomFragmentCommentMenuParentLinearLayout.background = ColorDrawable(getThemeColor(darkModeSupport.context))

        // argmentから取り出す
        comment = arguments?.getString("comment") ?: ""
        userId = arguments?.getString("user_id") ?: ""

        // コメント表示
        viewBinding.bottomFragmentCommentInclude.adapterRoomNameTextView.text = arguments?.getString("label")
        viewBinding.bottomFragmentCommentInclude.adapterCommentTextView.text = comment

        // 複数行格納
        viewBinding.bottomFragmentCommentInclude.adapterCommentParent.setOnClickListener {
            if (viewBinding.bottomFragmentCommentInclude.adapterCommentTextView.maxLines == 1) {
                viewBinding.bottomFragmentCommentInclude.adapterCommentTextView.maxLines = Int.MAX_VALUE
            } else {
                viewBinding.bottomFragmentCommentInclude.adapterCommentTextView.maxLines = 1
            }
        }

        /**
         * ロックオンできるようにする
         * ロックオンとはある一人のユーザーのコメントだけ見ることである
         * 生主が効いたときによくある
         * 動画にも対応する・・・？
         * */
        val fragment = requireParentFragment()
        when (fragment) {
            is CommentFragment -> {
                val nicoLiveViewModel by viewModels<NicoLiveViewModel>({ fragment })
                // 生放送
                recyclerViewList = nicoLiveViewModel.commentList.filter { commentJSONParse -> if (commentJSONParse != null) commentJSONParse.userId == userId else false } as ArrayList<CommentJSONParse>
                // コメントが届いたら反映させる。
                nicoLiveViewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
                    if (comment.userId == userId && !recyclerViewList.contains(comment)) {
                        recyclerViewList.add(0, comment)
                        viewBinding.bottomFragmentCommentMenuRecyclerView.adapter?.notifyDataSetChanged()
                        showInfo()
                    }
                }
            }
            is JCNicoLiveFragment -> {
                val nicoLiveViewModel by viewModels<NicoLiveViewModel>({ fragment })
                // 生放送
                recyclerViewList = nicoLiveViewModel.commentList.filter { commentJSONParse -> if (commentJSONParse != null) commentJSONParse.userId == userId else false } as ArrayList<CommentJSONParse>
                // コメントが届いたら反映させる。
                nicoLiveViewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
                    if (comment.userId == userId && !recyclerViewList.contains(comment)) {
                        recyclerViewList.add(0, comment)
                        viewBinding.bottomFragmentCommentMenuRecyclerView.adapter?.notifyDataSetChanged()
                        showInfo()
                    }
                }
            }
            // 動画
            is NicoVideoFragment -> {
                recyclerViewList = fragment.viewModel.rawCommentList.filter { commentJSONParse -> commentJSONParse.userId == userId } as ArrayList<CommentJSONParse>
                isNicoVideoFragment = true
            }
            is JCNicoVideoFragment -> {
                val nicoLiveViewModel by viewModels<NicoVideoViewModel>({ fragment })
                recyclerViewList = nicoLiveViewModel.rawCommentList.filter { commentJSONParse -> commentJSONParse.userId == userId } as ArrayList<CommentJSONParse>
                isNicoVideoFragment = true
            }
        }

        // RecyclerView
        when (fragment) {
            is CommentFragment -> {
                // 生放送
                viewBinding.bottomFragmentCommentMenuRecyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    val commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList, fragment)
                    adapter = commentRecyclerViewAdapter
                    //区切り線いれる
                    val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    addItemDecoration(itemDecoration)
                }
            }
            is JCNicoLiveFragment -> {
                // 生放送
                viewBinding.bottomFragmentCommentMenuRecyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    val commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList, fragment)
                    adapter = commentRecyclerViewAdapter
                    //区切り線いれる
                    val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    addItemDecoration(itemDecoration)
                }
            }
            is NicoVideoFragment -> {
                // 動画
                viewBinding.bottomFragmentCommentMenuRecyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    val nicoVideoAdapter = NicoVideoAdapter(
                        arrayListArrayAdapter = recyclerViewList,
                        fragmentManager = fragment.childFragmentManager,
                        isOffline = fragment.viewModel.isOfflinePlay.value ?: false,
                        nicoruAPI = fragment.viewModel.nicoruAPI
                    )
                    adapter = nicoVideoAdapter
                    //区切り線いれる
                    val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    addItemDecoration(itemDecoration)
                }
            }
            is JCNicoVideoFragment -> {
                // 動画
                viewBinding.bottomFragmentCommentMenuRecyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    val nicoVideoAdapter = NicoVideoAdapter(
                        arrayListArrayAdapter = recyclerViewList,
                        fragmentManager = fragment.childFragmentManager,
                        isOffline = fragment.viewModel.isOfflinePlay.value ?: false,
                        nicoruAPI = fragment.viewModel.nicoruAPI
                    )
                    adapter = nicoVideoAdapter
                    //区切り線いれる
                    val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
                    addItemDecoration(itemDecoration)
                }
            }
        }

        // コテハンを読み出す
        lifecycleScope.launch(Dispatchers.Main) {
            // とりあえずユーザーID表示
            viewBinding.bottomFragmentCommentMenuKotehanEditText.setText(userId)
            // コテハン読み込み
            val kotehan = loadKotehan()
            // 存在すればテキストに入れる。なければnullになる
            if (kotehan != null) {
                viewBinding.bottomFragmentCommentMenuKotehanEditText.setText(kotehan.kotehan)
            }
            // コテハン登録
            viewBinding.bottomFragmentCommentMenuKotehanButton.setOnClickListener {
                if (fragment != null) {
                    registerKotehan()
                }
            }
        }

        // NGスコア表示など
        showInfo()

        // NG関係
        setNGClick()

        // コピー
        viewBinding.bottomFragmentCommentMenuCommentCopyButton.setOnClickListener {
            commentCopy()
        }

        // URL取り出し
        regexURL()

        // 動画の場合は「ここから再生」ボタンを表示する
        showJumpButton(fragment)

        // 生IDのみ、ユーザー名取得ボタン
        if ("([0-9]+)".toRegex().matches(userId)) { // 生IDは数字だけで構成されているので正規表現（じゃなくてもできるだろうけど）
            // ユーザー名取得
            viewBinding.bottomFragmentCommentMenuGetUserNameButton.isVisible = true
            viewBinding.bottomFragmentCommentMenuGetUserNameButton.setOnClickListener {
                getUserName(userId)
            }
            // ユーザーページ取得
            viewBinding.bottomFragmentCommentMenuOpenUserPageButton.isVisible = true
            viewBinding.bottomFragmentCommentMenuOpenUserPageButton.setOnClickListener {
                openUserPage(userId)
            }
        }

        /**
         * 公式の動画と生放送アプリが独立した今できなさそうなことを。アプリ内完結
         * 生放送で流れたコメントに動画IDが含まれている時に例えばマイリスト登録をしたり、ポップアップ再生をしたり、
         * 生放送IDでもTS予約、予定追加機能が使えるように。
         * */
        val videoIdRegex = NICOVIDEO_ID_REGEX.toRegex().find(comment)
        val liveIdRegex = NICOLIVE_ID_REGEX.toRegex().find(comment)
        when {
            videoIdRegex?.value != null -> {
                // どーが
                viewBinding.bottomFragmentCommentMenuNicoVideoMenuButton.isVisible = true
                viewBinding.bottomFragmentCommentMenuNicoVideoMenuButton.setOnClickListener {
                    // 動画メニュー出す
                    val nicoVideoMenuBottomFragment = NicoVideoListMenuBottomFragment()
                    val bundle = Bundle()
                    bundle.putString("video_id", videoIdRegex.value)
                    bundle.putBoolean("is_cache", false)
                    nicoVideoMenuBottomFragment.arguments = bundle
                    nicoVideoMenuBottomFragment.show(activity?.supportFragmentManager!!, "video_menu")
                }
            }
            liveIdRegex?.value != null -> {
                // なまほーそー
                viewBinding.bottomFragmentCommentMenuNicoLiveMenuButton.isVisible = true
                viewBinding.bottomFragmentCommentMenuNicoLiveMenuButton.setOnClickListener {
                    // 生放送メニュー出す
                    val programMenuBottomSheet = ProgramMenuBottomSheet()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveIdRegex.value)
                    programMenuBottomSheet.arguments = bundle
                    programMenuBottomSheet.show(activity?.supportFragmentManager!!, "video_menu")
                }
            }
        }

    }

    /** こっから再生 */
    private fun showJumpButton(fragment: Fragment) {
        // 移動先
        val currentPos = arguments?.getLong("current_pos", -1) ?: -1
        // ボタン表示。動画Fragmentでかつcurrent_posが-1以外のとき表示
        if (isNicoVideoFragment && currentPos != -1L) {
            viewBinding.bottomFragmentCommentMenuNicovideoSeekButton.isVisible = true
            // こっから再生
            val seekButtonText = "${getString(R.string.lockon_jump_button)}(${formatTime(currentPos / 100F)})"
            viewBinding.bottomFragmentCommentMenuNicovideoSeekButton.text = seekButtonText // 移動先時間追記。append()だとGalaxy S7 Edgeが落ちる
        }
        viewBinding.bottomFragmentCommentMenuNicovideoSeekButton.setOnClickListener {
            // こっから再生できるようにする
            if (fragment is NicoVideoFragment || fragment is JCNicoVideoFragment) {
                // LiveData経由でExoPlayerを操作
                val viewModel by viewModels<NicoVideoViewModel>({ fragment })
                viewModel.playerSetSeekMs.postValue(currentPos * 10)
            }
        }
    }

    private fun showInfo() {
        // NGスコア/個数など
        viewBinding.bottomFragmentCommentMenuCountTextView.text = "${getString(R.string.comment_count)}：${recyclerViewList.size}"
        viewBinding.bottomFragmentCommentMenuNgTextView.text = "${getString(R.string.ng_score)}：${recyclerViewList.firstOrNull()?.score}"
    }

    /** ユーザーページを開く */
    private fun openUserPage(userId: String) {
        val accountFragment = NicoAccountFragment().apply {
            arguments = Bundle().apply {
                putString("userId", userId)
            }
        }
        (requireActivity() as? MainActivity)?.setFragment(accountFragment, "account")
    }

    /** ユーザー名取得。非同期処理 */
    private fun getUserName(userId: String) = lifecycleScope.launch(Dispatchers.Main) {
        // API叩く
        val user = withContext(Dispatchers.IO) {
            val user = UserAPI()
            val response = user.getUserData(userSession, userId)
            if (!response.isSuccessful) return@withContext null
            user.parseUserData(response.body?.string())
        }
        viewBinding.bottomFragmentCommentMenuKotehanEditText.setText(user?.nickName)
    }

    //URL正規表現
    private fun regexURL() {
        //正規表現で取り出す
        val urlRegex =
            Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+")
                .matcher(SpannableString(comment))
        if (urlRegex.find()) {
            viewBinding.bottomFragmentCommentMenuCommentUrl.isVisible = true
            viewBinding.bottomFragmentCommentMenuCommentUrl.setOnClickListener {
                val uri = urlRegex.group().toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }
    }

    private fun commentCopy() {
        // コピーする
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", comment))
        Toast.makeText(context, getString(R.string.copy_successful), Toast.LENGTH_SHORT).show()
    }

    private fun setNGClick() {
        // コメントNG追加
        // 長押しで登録
        viewBinding.bottomFragmentCommentMenuCommentNgButton.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        viewBinding.bottomFragmentCommentMenuCommentNgButton.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    // NGコメント追加。あとは生放送/動画Fragmentでデータベースを監視してるのでこれで終わり
                    NGDBInit.getInstance(requireContext()).ngDBDAO().insert(NGDBEntity(value = comment, type = "comment", description = ""))
                }
                //とーすと
                showToast(getString(R.string.add_ng_comment_message))
                // 閉じる
                dismiss()
            }
            true
        }
        // ユーザーNG追加
        // 長押しで登録
        viewBinding.bottomFragmentCommentMenuUserNgButton.setOnClickListener {
            showToast(getString(R.string.long_click))
        }
        viewBinding.bottomFragmentCommentMenuUserNgButton.setOnLongClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    // NGユーザー追加
                    NGDBInit.getInstance(requireContext()).ngDBDAO().insert(NGDBEntity(value = userId, type = "user", description = ""))
                }
                // とーすと
                showToast(getString(R.string.add_ng_user_message))
                // 閉じる
                dismiss()
            }
            true
        }
    }

    //コテハン登録。非同期
    private fun registerKotehan() {
        val kotehan = viewBinding.bottomFragmentCommentMenuKotehanEditText.text.toString()
        if (kotehan.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                // データベース用意。ここでは追加のみすればいい。後は各Fragmentが変更を検知して最新のを適用してくれる(NicoVideoFragment.setKotehanDBChangeObserve()参照)
                val kotehanDB = KotehanDBInit.getInstance(requireContext())
                withContext(Dispatchers.IO) {
                    // すでに存在する場合・・・？
                    val kotehanData = kotehanDB.kotehanDBDAO().findKotehanByUserId(userId)
                    if (kotehanData != null) {
                        // 存在するなら上書き
                        val kotehanEntity = kotehanData.copy(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        kotehanDB.kotehanDBDAO().update(kotehanEntity)
                    } else {
                        // データ追加
                        val kotehanEntity = KotehanDBEntity(userId = userId, kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        kotehanDB.kotehanDBDAO().insert(kotehanEntity)
                    }
                }
                //登録しました！
                Toast.makeText(context, "${getString(R.string.add_kotehan)}\n${userId}->${kotehan}", Toast.LENGTH_SHORT).show()
                // 一覧更新など
                viewBinding.bottomFragmentCommentMenuRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    //コテハン読み込み
    private suspend fun loadKotehan() = withContext(Dispatchers.IO) {
        KotehanDBInit.getInstance(requireContext()).kotehanDBDAO().findKotehanByUserId(userId)
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 時間表記をきれいにする関数
     * @param time 秒。ミリ秒ではない
     * */
    private fun formatTime(time: Float): String {
        val minutes = time / 60
        val hour = (minutes / 60).toInt()
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "${hour}:${simpleDateFormat.format(time * 1000)}"
    }


}