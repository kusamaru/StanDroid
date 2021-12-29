package io.github.takusan23.tatimidroid.nicolive.compose

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.nicolive.CommentRoomFragment
import io.github.takusan23.tatimidroid.nicolive.CommentViewFragment
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.getThemeColor
import io.github.takusan23.tatimidroid.tool.isDarkMode
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveCommentOnlyBinding
import kotlinx.coroutines.launch

/**
 * 生放送のコメントのみのFragment
 * */
class JCNicoLiveCommentOnlyFragment : Fragment() {

    /** Preference */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    /** ViewModel初期化。ネットワークとかUI関係ないやつはこっちに書いていきます。 */
    val viewModel by lazy {
        val liveId = arguments?.getString("liveId")!!
        ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, true)).get(NicoLiveViewModel::class.java)
    }

    /** ViewBinding */
    private val viewBinding by lazy { FragmentNicoliveCommentOnlyBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    @ExperimentalAnimationApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // LiveData監視
        setLiveData()

        // Composeをセットする
        setCompose()

        // Fragmentをセットする
        setCommentListFragment()

        // 戻るキーイベント
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            finishFragment()
        }

    }

    /**
     * LiveDataを監視する
     * ちなこれを呼ばないとViewModelが生成されない(by lazy { } なので)
     * */
    private fun setLiveData() {
        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                "finish" -> finishFragment()
            }
        }
        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            showSnackBar(it)
        }
    }

    /** Snackbarを表示させる関数 */
    private fun showSnackBar(message: String) {
        Snackbar.make(viewBinding.fragmentNicoliveCommentOnlyCommentPostComposeView, message, Snackbar.LENGTH_LONG).apply {
            anchorView = viewBinding.fragmentNicoliveCommentOnlyCommentPostComposeView
            val textView = view.findViewById(R.id.snackbar_text) as TextView
            textView.maxLines = 5 // 複数行
            view.elevation = 30f
        }.show()
    }

    /** Fragmentを閉じる */
    private fun finishFragment() {
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    /**
     * コメント一覧Fragmentを追加する
     * */
    private fun setCommentListFragment() {
        childFragmentManager.beginTransaction().replace(viewBinding.framgnetNicoliveCommentOnlyFramgentHostFrameLayout.id, CommentViewFragment()).commit()
        // ダークモード
        viewBinding.framgnetNicoliveCommentOnlyFramgentHostFrameLayout.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /**
     * 来場者のUIとコメント投稿はJetpackComposeで書かれているのでそいつらを設置する
     * */
    @ExperimentalAnimationApi
    private fun setCompose() {
        // コメント投稿エリア
        viewBinding.fragmentNicoliveCommentOnlyCommentPostComposeView.apply {
            setContent {
                // コメント展開するかどうか
                val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
                // コルーチン
                val scope = rememberCoroutineScope()
                // コメント本文
                val commentPostText = remember { mutableStateOf("") }
                // 匿名で投稿するか
                val isTokumeiPost = remember { mutableStateOf(viewModel.nicoLiveHTML.isPostTokumeiComment) }
                // 文字の大きさ
                val commentSize = remember { mutableStateOf("medium") }
                // 文字の位置
                val commentPos = remember { mutableStateOf("naka") }
                // 文字の色
                val commentColor = remember { mutableStateOf("white") }
                // 複数行コメントを許可している場合はtrue。falseならEnterキーでコメント送信
                val isAcceptMultiLineComment = !prefSetting.getBoolean("setting_enter_post", true)

                NicoLiveCommentInputButton(
                    onClick = { viewModel.commentListShowLiveData.postValue(!isComment.value) },
                    isComment = isComment.value,
                    comment = commentPostText.value,
                    isShowCommentInfoChangeButton = false, // 番組情報無いのでfalse
                    onCommentChange = { commentPostText.value = it },
                    onPostClick = {
                        // コメント投稿
                        scope.launch {
                            viewModel.sendComment(commentPostText.value, commentColor.value, commentSize.value, commentPos.value)
                            commentPostText.value = "" // クリアに
                        }
                    },
                    position = commentPos.value,
                    size = commentSize.value,
                    color = commentColor.value,
                    onPosValueChange = { commentPos.value = it },
                    onSizeValueChange = { commentSize.value = it },
                    onColorValueChange = { commentColor.value = it },
                    is184 = isTokumeiPost.value,
                    onTokumeiChange = {
                        // 匿名、生ID切り替わった時
                        isTokumeiPost.value = !isTokumeiPost.value
                        prefSetting.edit { putBoolean("nicolive_post_tokumei", it) }
                        viewModel.nicoLiveHTML.isPostTokumeiComment = it
                    },
                    isMultiLine = isAcceptMultiLineComment,
                )
            }
        }
        // 来場者
        viewBinding.fragmentNicoliveCommentOnlyInfoComposeView.apply {
            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                ) {
                    Surface {
                        // 統計情報LiveData
                        val statisticsLiveData = viewModel.statisticsLiveData.observeAsState()
                        // アクティブ人数
                        val activeCountLiveData = viewModel.activeCommentPostUserLiveData.observeAsState()

                        NicoLiveStatisticsUI(
                            allViewer = statisticsLiveData.value?.viewers ?: 0,
                            allCommentCount = statisticsLiveData.value?.comments ?: 0,
                            activeCountText = activeCountLiveData.value ?: "計測中",
                            onClickRoomChange = {
                                // Fragment切り替え
                                val toFragment = when (childFragmentManager.findFragmentById(viewBinding.framgnetNicoliveCommentOnlyFramgentHostFrameLayout.id)) {
                                    is CommentRoomFragment -> CommentViewFragment()
                                    else -> CommentRoomFragment()
                                }
                                childFragmentManager.beginTransaction().replace(viewBinding.framgnetNicoliveCommentOnlyFramgentHostFrameLayout.id, toFragment).commit()
                            },
                            onClickActiveCalc = {
                                // アクティブ人数計算
                                viewModel.calcToukei(true)
                            }
                        )
                    }
                }
            }
        }
    }

}