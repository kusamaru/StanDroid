package com.kusamaru.standroid.tool

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import androidx.preference.PreferenceManager
import java.io.File

/**
 * フォント変更機能 / フォントサイズ変更機能 でよく使うやつ。
 *
 * フォントサイズ（ユーザーID）：setting_font_size_id　Float　初期値　12F
 * フォントサイズ（コメント）：setting_font_size_comment　Float 初期値　14F
 *
 * */
class CustomFont(val context: Context?) {

    private var prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** フォントがあるフォルダー */
    private var fontFolder: File = File("${context?.getExternalFilesDir(null)}/font")

    /** TypeFace */
    var typeface: Typeface? = null

    /** フォントサイズ（ゆーざーID） */
    var userIdFontSize = prefSetting.getFloat("setting_font_size_id", 12f)

    /** フォントサイズ（コメント） */
    var commentFontSize = prefSetting.getFloat("setting_font_size_comment", 14f)

    init {
        // フォントフォルダーには一つのファイル（フォントファイル）しか存在しないでーす
        if (fontFolder.exists() && fontFolder.listFiles() != null && fontFolder.listFiles()!!.isNotEmpty()) {
            // ファイルが存在する場合はTypeFaceつくる
            val fontFile = fontFolder.listFiles()!![0]
            typeface = Typeface.createFromFile(fontFile)
        }
    }

    /**
     * TextViewにフォントを設定する
     * */
    fun setTextViewFont(textView: TextView) {
        textView.typeface = typeface ?: Typeface.DEFAULT
    }

    /**
     * PaintにTypeFaceを設定する
     * */
    fun setPaintTypeFace(paint: Paint) {
        if (typeface == null) {
            //TypeFace初期化できない とき終了
            return
        }
        paint.typeface = typeface
    }

    /**
     * コメントファイルをコメント描画（CommentCanvas）にも適用するか？
     * @return CommentCanvasにも適用する設定有効時はtrue/そうじゃなければfalse
     * */
    val isApplyFontFileToCommentCanvas = prefSetting.getBoolean("setting_comment_canvas_font_file", false)

}