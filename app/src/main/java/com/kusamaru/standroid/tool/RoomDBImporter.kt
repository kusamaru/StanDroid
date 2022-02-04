package com.kusamaru.standroid.tool

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kusamaru.standroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * エクスポートしたZipファイルを取り込む
 * （なお、data/data/io,github.takusan23.tataimidroid/database に移動させるだけ）
 * */
class RoomDBImporter(val fragment: Fragment) {

    private val context by lazy { fragment.requireContext() }

    /** Activity Result API を使う。[AppCompatActivity.onActivityResult]の後継 */
    val callback = fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            // 重いので非同期
            fragment.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    // データベースが保存されているフォルダのパス。ContextCompatで後方互換性ヨシ！
                    val databaseFolder = File(ContextCompat.getDataDir(context), "databases")
                    // 一応前のデータを消す
                    databaseFolder.listFiles()?.forEach { file -> file.delete() }
                    // Zip展開
                    val inputStream = context.contentResolver.openInputStream(uri)
                    ZipInputStream(inputStream).let { zip ->
                        var zipEntry: ZipEntry?
                        // Zip内のファイルをなくなるまで繰り返す
                        while (zip.nextEntry.also { zipEntry = it } != null) {
                            if (zipEntry != null) {
                                // コピー先ファイル作成
                                val dbFile = File(databaseFolder, zipEntry!!.name)
                                dbFile.createNewFile()
                                // データを書き込む
                                dbFile.writeBytes(zip.readBytes())
                            }
                        }
                    }
                }
                // おわった
                Toast.makeText(context, context.getString(R.string.database_restore_successful), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Storage Access Frameworkを利用してZipファイルを選んでもらう
     * */
    fun start() {
        callback.launch(arrayOf("*/*"))
    }
}