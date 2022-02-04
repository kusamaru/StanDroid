package com.kusamaru.standroid.tool

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Request
import java.io.File

/**
 * 分割ダウンロード（並列ダウンロード？）
 *
 * @param headers [Pair]の配列。一個目がヘッダー名、二個目がヘッダーに入れる中身。MapだとKeyがかぶると上書きされるので
 * @param splitCount 並列ダウンロードを同時に何個するか
 * @param splitFileFolder 一時的にファイルを保存しておくフォルダー。後に削除される
 * @param resultVideoFile 最終的にできる動画ファイル
 * */
class DownloadPocket(private val url: String, private val splitFileFolder: File, private val resultVideoFile: File, private val splitCount: Int = 4, private val headers: ArrayList<Pair<String, String>>? = null) {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /** 総書き込みバイト数 */
    private var currentBytes = 0L

    /** トータルバイト数 */
    private var totalBytes = 0L

    /** 進捗を送信するFlow。StateFlowは同じ値は流さないらしい */
    private val _progressFlow = MutableStateFlow(0)

    /** 外部に公開する[_progressFlow] */
    val progressFlow: StateFlow<Int> = _progressFlow

    /** ダウンロードを始める */
    suspend fun start() = withContext(Dispatchers.Default) {
        // HEADリクエストを飛ばす
        val headRequest = getVideoBytesContentLength()
        // 対応してるよね？
        if (headRequest.headers["Accept-Ranges"] != "bytes") return@withContext
        // 分割する
        totalBytes = headRequest.headers["Content-Length"]!!.toLong()
        val splitByteList = splitByteList()
        // 分割ダウンロード開始
        splitByteList
            .mapIndexed { index, pair ->
                // asyncで並列実行
                async { getVideoFile(pair.first, pair.second, index) }
            }.map { deferred ->
                // すべてのasyncを待つ
                deferred.await()
            }
        // くっつけて完成
        multipleFileToOneFile()
    }

    /**
     * これからダウンロードするファイルの大きさ（バイト単位）を取得する
     *
     * データは要らないのでHEADリクエストを飛ばす
     * @param userSession ユーザーセッション
     * @param nicoHistory レスポンスヘッダーのCookieにあるnicohistoryを入れてね。
     * @param url 動画URL
     * */
    private suspend fun getVideoBytesContentLength() = withContext(Dispatchers.IO) {
        // リクエスト
        val request = Request.Builder().apply {
            url(url)
            headers?.forEach { pair ->
                addHeader(pair.first, pair.second)
            }
            head() // bodyいらん
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }


    /**
     * 求めたバイトをRangeリクエストしやすい配列の形に変換する
     * 最後にあまりが入ると思う
     *
     * Range、0-500の次は 500-1000 じゃなくて、1足して 501-1000 と指定する必要がある
     *
     * ```
     * [
     *  Pair(0, 1348243),
     *  Pair(1348244, 2696486)
     * ]
     * ```
     * */
    private fun splitByteList(): ArrayList<Pair<Long, Long>> {
        // あまりが出ないほうがおかしいので余りを出す
        val amari = totalBytes % splitCount
        // あまり分を引いて一個のリクエストでのバイト数を決定
        val splitByte = (totalBytes - amari) / splitCount
        // 配列にして返す
        val byteList = arrayListOf<Pair<Long, Long>>()
        // 2回目のループなら1回目の値が入ってる。前の値
        var prevByte = 0L
        while (true) {
            // ピッタリ分けたいので
            if (totalBytes >= prevByte) {
                /***
                 * 最後余分に取得しないように。
                 * true(splitByte足しても足りない)ならsplitByteを足して、falseならtotalByteを渡して終了
                 * */
                val toByte = if (totalBytes > (prevByte + splitByte)) prevByte + splitByte else totalBytes
                byteList.add(Pair(prevByte, toByte))
                prevByte += splitByte + 1 // 1足して次のバイトからリクエストする
            } else break
        }
        return byteList
    }

    /**
     * 範囲リクエストを送信する。[progressFlow]は99パーセントまでしか行かないと思う（謎）
     *
     * @param startByte こっから
     * @param endByte ここまで
     * @param count 何個目か
     * */
    private suspend fun getVideoFile(startByte: Long, endByte: Long, count: Int) = withContext(Dispatchers.IO) {
        // リクエスト
        val request = Request.Builder().apply {
            url(url)
            headers?.forEach { pair ->
                addHeader(pair.first, pair.second)
            }
            addHeader("Range", "bytes=${startByte}-${endByte}")
            get()
        }.build()
        val response = okHttpClient.newCall(request).execute()
        val inputStream = response.body?.byteStream()
        // ファイル作成。拡張子に順番を入れる
        val splitFile = File(splitFileFolder, "pocket.${count}").apply { createNewFile() }
        val splitFileOutputStream = splitFile.outputStream()
        // 書き込む
        val buffer = ByteArray(1024 * 1024)
        while (true) {
            val read = inputStream?.read(buffer)
            if (read == -1 || read == null) {
                // もう取れない
                break
            }
            splitFileOutputStream.write(buffer, 0, read)
            currentBytes += read
            _progressFlow.emit(((currentBytes / totalBytes.toFloat()) * 100f).toInt()) // でもなんか99パーセントで止まる
        }
        inputStream?.close()
        splitFileOutputStream.close()
    }


    /** すべてのファイルを一つにまとめて完成 */
    private suspend fun multipleFileToOneFile() = withContext(Dispatchers.Default) {
        // 最終的なファイル
        val fileList = splitFileFolder.listFiles()?.sortedBy { file -> file.extension } ?: return@withContext // 並び替え。男女男男女男女
        val resultFileOutputStream = resultVideoFile.outputStream()
        // 書き込み先ファイルのOutputStream
        fileList.forEach { file ->
            val inputStream = file.inputStream()
            val byteArray = ByteArray(1024 * 1024)
            // KotlinのreadBytes()使ったらOOM吐いたので古典的な方法で
            while (true) {
                val read = inputStream.read(byteArray)
                if (read == -1) {
                    // もう取れない
                    break
                }
                resultFileOutputStream.write(byteArray, 0, read)
            }
            inputStream.close()
        }
        // とじる
        resultFileOutputStream.close()
        // フォルダを消す
        splitFileFolder.deleteRecursively()
        // 100パーセントにして送信
        _progressFlow.emit(100)
    }

}