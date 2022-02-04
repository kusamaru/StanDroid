package com.kusamaru.standroid.nicovideo.bottomfragment

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.nicovideo.compose.DarkColors
import com.kusamaru.standroid.nicovideo.compose.LightColors
import com.kusamaru.standroid.nicovideo.compose.ComememoScreen
import com.kusamaru.standroid.nicovideo.viewmodel.factory.ComememoViewModelFactory
import com.kusamaru.standroid.nicovideo.viewmodel.ComememoViewModel
import com.kusamaru.standroid.tool.PlayerCommentPictureTool
import com.kusamaru.standroid.tool.isDarkMode
import java.text.SimpleDateFormat

/**
 * コメメモ機能（動画スクショ機能）（コメント付きで画面メモ）
 *
 * 残念ですがAndroid 7以前のユーザーは利用できません。（PixelCopy APIがない）
 *
 * 入れてほしいもの
 * player_image_file_path    | String       | 映像の写真のファイルパス
 * comment_image_file_path   | String       | コメントの写真のファイルパス
 * file_name                 | String       | 保存するときにつける画像のファイル名。拡張子はpngで
 * draw_text_list            | List<String> | 右下に文字を書きたい場合は文字列の配列を入れてね。なくてもいい
 * */
class ComememoBottomFragment : BottomSheetDialogFragment() {

    /** ViewModel */
    private val viewModel by lazy {
        val playerImageFilePath = requireArguments().getString("player_image_file_path")!!
        val commentImageFilePath = requireArguments().getString("comment_image_file_path")!!
        val fileName = requireArguments().getString("file_name")!!
        val drawTextList = requireArguments().getStringArray("draw_text_list")?.toList()
        ViewModelProvider(this, ComememoViewModelFactory(requireActivity().application, playerImageFilePath, commentImageFilePath, drawTextList, fileName)).get(ComememoViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        ComememoScreen(viewModel) { dismiss() }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // とりあえず生成
        viewModel.makeBitmap(false)
    }

    companion object {

        /**
         * コメメモBottomFragmentを表示する。
         *
         * @param fragmentManager FragmentManager
         * @param surfaceView 映像を流してるSurfaceView
         * @param commentCanvas コメントを流してるView
         * @param title タイトル
         * @param contentId 動画IDなど
         * @param position 再生位置など
         * */
        suspend fun show(fragmentManager: FragmentManager, surfaceView: SurfaceView, commentCanvas: View, title: String, contentId: String, position: Long) {
            // 動画、コメントViewの保存先
            val (playerPath, commentPath) = PlayerCommentPictureTool.captureView(surfaceView, commentCanvas)
            ComememoBottomFragment().apply {
                arguments = Bundle().apply {
                    // ファイルパスとかを渡す
                    val timeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(System.currentTimeMillis())
                    val fileName = "${title}-${SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(System.currentTimeMillis())}.png"
                    // 再生時間
                    val currentPosFormat = DateUtils.formatElapsedTime(position / 1000L)
                    putString("player_image_file_path", playerPath)
                    putString("comment_image_file_path", commentPath)
                    putString("file_name", fileName)
                    putStringArray("draw_text_list", arrayOf(title, contentId, currentPosFormat, timeFormat))
                }
            }.show(fragmentManager, "kokosuki")
        }

    }

}