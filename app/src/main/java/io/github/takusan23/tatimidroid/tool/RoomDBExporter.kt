package io.github.takusan23.tatimidroid.tool

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Roomのデータベースファイルを取り出す
 * （なお、data/data/io,github.takusan23.tataimidroid/database の中身を全部持っていくだけのお仕事）
 * */
class RoomDBExporter(val fragment: Fragment) {

    private val context by lazy { fragment.requireContext() }

    /** Activity Result API を使う。[AppCompatActivity.onActivityResult]の後継 */
    val callback = fragment.registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        if (uri != null) {
            // 重くなるので非同期
            fragment.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    // ZIP作成。
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    ZipOutputStream(outputStream).let { zip ->
                        /**
                         * データベースフォルダをすべてZipに入れる
                         * */
                        context.databaseList()
                            .map { name -> context.getDatabasePath(name) }
                            // よくわからんファイルは持ってこない。もどしなさい
                            .filter { file -> !file.name.contains("com.google.android.datatransport") }
                            .forEach { file ->
                                val inputStream = file.inputStream() // ファイル読み出し
                                val entry = ZipEntry(file.name) // ファイル名
                                zip.putNextEntry(entry)
                                zip.write(inputStream.readBytes()) // 書き込む。Kotlinかんたんすぎい
                                inputStream.close()
                                zip.closeEntry()
                            }
                        // おしまい
                        zip.close()
                    }
                }
                // 終了
                Toast.makeText(context, context.getString(R.string.database_backup_successful), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * databaseフォルダを全部コピーする
     *
     * ユーザーにStorage Access Frameworkを利用してファイルを作成させる
     * */
    fun start() {
        // まずコピー先Zipファイルをユーザーに作成する（自由にファイルアクセスさせろや）
        val simpleDateFormat = SimpleDateFormat("yyyyMMdd")
        callback.launch("memories_${simpleDateFormat.format(System.currentTimeMillis())}.zip")
    }
}