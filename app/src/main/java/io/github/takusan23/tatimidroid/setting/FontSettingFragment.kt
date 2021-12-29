package io.github.takusan23.tatimidroid.setting

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentFontSettingBinding
import io.github.takusan23.tatimidroid.tool.CustomFont
import java.io.File

/**
 * フォント設定Fragment
 * */
class FontSettingFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentFontSettingBinding.inflate(layoutInflater) }

    /** onActivityResultを駆逐 */
    private val fontSelectCallBack = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // ファイル名を取得
        val fileName = getFileName(uri)
        // 削除
        resetFont()
        // コピー先フォルダー。ScopedStorageです。
        val fontFolder = File("${requireContext().getExternalFilesDir(null)}/font")
        fontFolder.mkdir()
        // ふぁいるこぴー。
        val scopedStorageFontFile = File("${fontFolder}/$fileName")
        scopedStorageFontFile.createNewFile()
        scopedStorageFontFile.outputStream().use {
            requireContext().contentResolver?.openInputStream(uri)?.copyTo(it)
        }
        // ファイル名
        viewBinding.fragmentFontSettingFontFileSelectButton.text = "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"
        // 更新
        updatePreview()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // フォントサイズEditTextへ
        fontSizeEditTextInit()

        // 人生リセットボタン
        viewBinding.fragmentFontSettingFontSizeResetButton.setOnClickListener {
            // フォントサイズリセット
            // フォントサイズが指定されていなければ空文字が入る。
            prefSetting.edit {
                putFloat("setting_font_size_id", 12F)
                putFloat("setting_font_size_comment", 14F)
                apply()
                fontSizeEditTextInit()
            }
        }

        // フォント選択SAF
        viewBinding.fragmentFontSettingFontFileSelectButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // フォント選択。Android 10以降はMIME Typeでフォントを指定可能
                fontSelectCallBack.launch(arrayOf("font/*"))
            } else {
                // MIME Typeが対応してないので
                fontSelectCallBack.launch(arrayOf("*/*"))
            }
        }
        // フォントファイル削除
        viewBinding.fragmentFontSettingFontFileResetButton.setOnClickListener {
            // 選択したフォントリセット
            resetFont()
            // 更新
            updatePreview()
            // ファイル名
            viewBinding.fragmentFontSettingFontFileSelectButton.text = "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"
        }

        // プレビュー更新
        updatePreview()
        // ファイル名
        viewBinding.fragmentFontSettingFontFileSelectButton.text = "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"

        // フォントファイルをCommentCanvasにも適用するか
        viewBinding.fragmentFontSettingApplyCommentCanvasSwitch.isChecked = prefSetting.getBoolean("setting_comment_canvas_font_file", false)
        viewBinding.fragmentFontSettingApplyCommentCanvasSwitch.setOnCheckedChangeListener { buttonView, isChecked -> prefSetting.edit { putBoolean("setting_comment_canvas_font_file", isChecked) } }

    }

    // プレビューの更新をするとき
    private fun updatePreview() {
        viewBinding.fragmentFontSettingInclude.adapterRoomNameTextView.text = "ここがIDです"
        viewBinding.fragmentFontSettingInclude.adapterCommentTextView.text = "ここがコメントです"
        // カスタムフォント、フォントサイズとか
        val customFont = CustomFont(context)
        // フォントサイズ適用
        viewBinding.fragmentFontSettingInclude.adapterRoomNameTextView.apply {
            textSize = customFont.userIdFontSize
        }
        viewBinding.fragmentFontSettingInclude.adapterCommentTextView.apply {
            textSize = customFont.commentFontSize
        }
        // TypeFace適用
        customFont.apply {
            setTextViewFont(viewBinding.fragmentFontSettingInclude.adapterRoomNameTextView)
            setTextViewFont(viewBinding.fragmentFontSettingInclude.adapterCommentTextView)
        }
    }

    // フォントサイズのEditText初期化
    private fun fontSizeEditTextInit() {
        val customFont = CustomFont(context)
        viewBinding.fragmentFontSettingFontSizeIdEditText.setText(customFont.userIdFontSize.toString())
        viewBinding.fragmentFontSettingFontSizeCommentEditText.setText(customFont.commentFontSize.toString())
        // テキスト監視して自動で保存
        setTextWatcher(viewBinding.fragmentFontSettingFontSizeCommentEditText, "setting_font_size_comment")
        setTextWatcher(viewBinding.fragmentFontSettingFontSizeIdEditText, "setting_font_size_id")
    }

    // EditTextを監視して保存するやーつ
    private fun setTextWatcher(textView: TextView, preferenceName: String) {
        textView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prefSetting.edit {
                    // からのときは動かない + 大きすぎないように
                    if (s.toString().isNotEmpty() && s.toString().toFloat() <= 100F) {
                        putFloat(preferenceName, s.toString().toFloat())
                        apply()
                        // 更新
                        updatePreview()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    // 選択したフォント削除
    private fun resetFont() {
        // 削除
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.exists()) {
            // 存在するとき削除
            fontFolder.listFiles()?.forEach {
                it.delete()
            }
        }
    }

    // 選択中のフォントの名前
    private fun getSelectFontName(): String {
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.listFiles()?.isEmpty() == true) {
            return "未設定"
        }
        return fontFolder.listFiles()?.get(0)?.name ?: ""
    }

    // Uriからファイル名取得。取得失敗時は空の文字が
    private fun getFileName(uri: Uri): String {
        val cursor = context?.contentResolver?.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.moveToFirst()
        // 取得
        var fileName = cursor?.getString(0) ?: ""
        cursor?.close()
        return fileName
    }

}