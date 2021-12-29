package io.github.takusan23.tatimidroid.nicoapi

import android.content.Context
import io.github.takusan23.tatimidroid.CommentJSONParse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader


/**
 * xml形式のコメントをJSON形式に変換する。
 * */
class XMLCommentJSON(val context: Context?) {
    /** 保存先関係 */
    private val nicoVideoCache = NicoVideoCache(context)

    /**
     * xmlのコメントをJSON形式に変換する。多分重いからコルーチン
     * @param fileName ファイル名。
     * @return 0=成功 / 1=ファイル無いよ
     * */
    fun xmlToJSON(fileName: String): Deferred<Int> = GlobalScope.async {
        println(System.currentTimeMillis())

        // コメントXML
        val xmlPath = commentXmlFilePath(fileName) ?: return@async 1
        val xmlFile = File(xmlPath)

        // 出力JSON
        val jsonArray = JSONArray()

        /**
         * Android標準でXMLをパースする。
         * 本当はJsoup使いたかったんだけど遅すぎた
         * */
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        var xmlText = xmlFile.readText()
        // ニコ生新配信録画ツール（仮　で取得したコメントに先頭に見えない文字（BOM付きってやつらしい）が有るのでもしあれば消す
        if (Integer.toHexString(xmlText[0].toInt()) == "feff") {
            // 先頭一文字を除く
            xmlText = xmlText.substring(1)
        }
        parser.setInput(StringReader(xmlText))
        var eventType = parser.eventType
        // 終了まで繰り返す
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // コメントのみ選ぶ
            if (parser.name == "chat") {
                val thread = parser.getAttributeValue(null, "thread")
                val no = parser.getAttributeValue(null, "no")
                val vpos = parser.getAttributeValue(null, "vpos")
                val date = parser.getAttributeValue(null, "date")
                val date_usec = parser.getAttributeValue(null, "date_usec")
                val userId = parser.getAttributeValue(null, "user_id")
                val anonymcommenty = parser.getAttributeValue(null, "anonymcommenty")
                val score = if (parser.getAttributeValue(null, "score") != null) {
                    parser.getAttributeValue(null, "score")
                } else {
                    ""
                }
                val mail = parser.getAttributeValue(null, "mail")
                val origin = parser.getAttributeValue(null, "origin")
                val premium = parser.getAttributeValue(null, "premium")
                val content = parser.nextText()
                // NGコメントの場合はvposが0になる
                if (vpos.isNotEmpty()) {
                    // JSONのchatオブジェクト作成
                    val chatObject = JSONObject().apply {
                        put("thread", thread)
                        put("no", no)
                        put("vpos", vpos)
                        put("leaf", 1)
                        put("date", date)
                        put("date_usec", date_usec)
                        put("anonymcommenty", anonymcommenty)
                        put("user_id", userId)
                        put("mail", mail)
                        put("origin", origin)
                        put("score", score)
                        put("content", content)
                        put("premium", premium)
                    }
                    jsonArray.put(JSONObject().put("chat", chatObject))
                }
            }
            eventType = parser.next()
        }

        // 保存。
        val jsonFile = File("${nicoVideoCache.getCacheFolderPath()}/$fileName/${fileName}_comment.json")
        jsonFile.writeText(jsonArray.toString())

        println(System.currentTimeMillis())
        return@async 0
    }

    /**
     * xml形式のコメントをCommentJSONParseの配列に変換する関数。コルーチンです
     * @param xmlString XML形式のコメントファイル
     * */
    fun xmlToArrayList(xmlString: String): Deferred<ArrayList<CommentJSONParse>> =
        GlobalScope.async {
            val list = arrayListOf<CommentJSONParse>()

            /**
             * Android標準でXMLをパースする。
             * 本当はJsoup使いたかったんだけど遅すぎた
             * */
            val factory =
                XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlString));
            var eventType = parser.eventType
            // 終了まで繰り返す
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // コメントのみ選ぶ
                if (parser.name == "chat") {
                    val thread = parser.getAttributeValue(null, "thread")
                    val no = parser.getAttributeValue(null, "no")
                    val vpos = parser.getAttributeValue(null, "vpos")
                    val date = parser.getAttributeValue(null, "date")
                    val date_usec = parser.getAttributeValue(null, "date_usec")
                    val userId = parser.getAttributeValue(null, "user_id")
                    val anonymcommenty = parser.getAttributeValue(null, "anonymcommenty")
                    val score = if (parser.getAttributeValue(null, "score") != null) {
                        parser.getAttributeValue(null, "score")
                    } else {
                        ""
                    }
                    val mail = parser.getAttributeValue(null, "mail")
                    val origin = parser.getAttributeValue(null, "origin")
                    val premium = parser.getAttributeValue(null, "premium")
                    val content = parser.nextText()
                    // JSONのchatオブジェクト作成
                    val chatObject = JSONObject().apply {
                        put("thread", thread)
                        put("no", no)
                        put("vpos", vpos)
                        put("leaf", 1)
                        put("date", date)
                        put("date_usec", date_usec)
                        put("anonymcommenty", anonymcommenty)
                        put("user_id", userId)
                        put("mail", mail)
                        put("origin", origin)
                        put("score", score)
                        put("content", content)
                        put("premium", premium)
                    }
                    val jsonObject = JSONObject().put("chat", chatObject)
                    list.add(CommentJSONParse(jsonObject.toString(), "てすと", ""))
                }
                eventType = parser.next()
            }
            return@async list
        }


    /**
     * CommentJSONParseの配列をJSONの配列に変換する。コルーチンです。関数名なっが
     * @param commentList CommentJSONParseの配列
     * */
    fun CommentJSONParseArrayToJSONString(commentList: ArrayList<CommentJSONParse>): Deferred<JSONArray> =
        GlobalScope.async {
            val jsonArray = JSONArray()
            commentList.forEach {
                jsonArray.put(JSONObject(it.commentJson))
            }
            return@async jsonArray
        }


    /**
     * コメントXMLファイルのパスを返す関数。ない場合はnull
     * @param fileName フォルダ名。基本動画ID
     * */
    fun commentXmlFilePath(fileName: String): String? {
        // ふぉるだ
        val folderPath = nicoVideoCache.getCacheFolderPath()
        // フォルダの中身を探していく
        val videoFile = File(folderPath, fileName)
        if (videoFile.listFiles() != null) {
            videoFile.listFiles()?.forEach {
                if (it.extension == "xml") {
                    return it.path
                }
            }
        }
        return null
    }

    /**
     * JSON形式のコメントが存在するか。存在するとtrue
     * @param fileName ファイル名。基本動画ID
     * */
    fun commentJSONFileExists(fileName: String): Boolean {
        // ファイル存在するか
        return File("${nicoVideoCache.getCacheFolderPath()}/$fileName/${fileName}_comment.json").exists()
    }

}