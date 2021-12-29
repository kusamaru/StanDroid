package io.github.takusan23.tatimidroid.nicoapi.cache

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * ソートの中身をJSON形式で保存・読み込みをする関数
 * */
class CacheJSON {

    /**
     * キャッシュ内容をJSON化して返す関数。多分もっといいベストプラクティスあるから仮です
     * */
    fun createJSON(cacheFilter: CacheFilterDataClass): JSONObject {
        // タグの配列->JSONArrayへ
        val jsonArray = JSONArray()
        cacheFilter.tagItems.forEach {
            jsonArray.put(it)
        }
        val jsonObject = JSONObject().apply {
            put("filter", JSONObject().apply {
                put("titleContains", cacheFilter.titleContains)
                put("uploaderName", cacheFilter.uploaderName)
                put("tagItems", jsonArray)
                put("sort", cacheFilter.sort)
                put("isTatimiDroidGetCache", cacheFilter.isTatimiDroidGetCache)
            })
        }
        return jsonObject
    }

    /**
     * JSONObjectを文字列化して保存する関数
     * @param context Context
     * @param jsonObject createJSON()の戻り値
     * */
    fun saveJSON(context: Context?, jsonObject: JSONObject) {
        val scopedStoragePath = context?.getExternalFilesDir(null)?.path
        val filterJSONFile = File("${scopedStoragePath}/filter.json")
        // ファイル作成
        if (filterJSONFile.exists()) {
            filterJSONFile.createNewFile()
        }
        // 書き込む
        filterJSONFile.writeText(jsonObject.toString(4))
    }

    /**
     * filter.jsonを読み込む関数
     * @param context Context
     * @param jsonObject createJSON()の戻り値
     * @return CacheFilterDataClass。filter.jsonがない場合はnullなので注意。
     * */
    fun readJSON(context: Context?): CacheFilterDataClass? {
        val scopedStoragePath = context?.getExternalFilesDir(null)?.path
        val filterJSONFile = File("${scopedStoragePath}/filter.json")
        // ファイルは何処へ・・・の場合はnull
        if (!filterJSONFile.exists()) {
            return null
        }
        filterJSONFile.createNewFile()
        val jsonObject = JSONObject(filterJSONFile.readText()).getJSONObject("filter")
        // JSONパース
        val titleContains = jsonObject.getString("titleContains")
        val uploaderName = jsonObject.getString("uploaderName")
        val tagItems = toList(jsonObject.getJSONArray("tagItems")).toList()
        val sort = jsonObject.getString("sort")
        val isTatimiDroidGetCache = jsonObject.getBoolean("isTatimiDroidGetCache")
        return CacheFilterDataClass(titleContains, uploaderName, tagItems, sort, isTatimiDroidGetCache)
    }

    // JSONArray -> ArrayList 変換関数
    private fun toList(jsonArray: JSONArray): ArrayList<String> {
        val list = arrayListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    /**
     * ソートJSONファイルを削除する関数
     * */
    fun deleteFilterJSONFile(context: Context?) {
        val scopedStoragePath = context?.getExternalFilesDir(null)?.path
        val filterJSONFile = File("${scopedStoragePath}/filter.json")
        if (filterJSONFile.exists()) {
            filterJSONFile.delete()
        }
    }

}