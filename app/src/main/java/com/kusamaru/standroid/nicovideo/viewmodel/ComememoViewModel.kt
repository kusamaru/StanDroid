package com.kusamaru.standroid.nicovideo.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kusamaru.standroid.tool.PlayerCommentPictureTool
import kotlinx.coroutines.launch

/**
 * コメメモBottomFragmentで使うViewModel
 *
 * @param playerImageFilePath 映像の画像のパス
 * @param commentImageFilePath コメントの画像のパス
 * */
class ComememoViewModel(application: Application, private val playerImageFilePath: String, private val commentImageFilePath: String, private val textList: List<String>? = null, private val fileName: String) : AndroidViewModel(application) {

    private val context = application.applicationContext

    /** 完成したBitmapを送信するLiveData */
    val makeBitmapLiveData = MutableLiveData<Bitmap>()

    /** 保存先を入れる変数 */
    val saveFolderPath = PlayerCommentPictureTool.getSaveFolder()

    /**
     * Bitmapを作成する
     *
     * @param isWriteTextList 右下に文字を書き込む場合はtrue
     * */
    fun makeBitmap(isWriteTextList: Boolean) {
        viewModelScope.launch {
            makeBitmapLiveData.value?.recycle()
            makeBitmapLiveData.value = PlayerCommentPictureTool.makeBitmap(
                playerImageFilePath = playerImageFilePath,
                commentImageFilePath = commentImageFilePath,
                drawTextList = if (isWriteTextList) textList else null
            )
        }
    }

    /** [makeBitmapLiveData]のBitmapをMediaStoreに追加する（ギャラリーに保存する） */
    fun saveBitmapToMediaStore() {
        viewModelScope.launch {
            val bitmap = makeBitmapLiveData.value
            if (bitmap != null) {
                PlayerCommentPictureTool.saveMediaStore(context, fileName, bitmap)
                bitmap.recycle()
            }
        }
    }

}