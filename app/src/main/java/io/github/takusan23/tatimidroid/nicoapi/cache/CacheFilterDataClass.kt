package io.github.takusan23.tatimidroid.nicoapi.cache

/**
 * キャッシュのフィルターの条件を保存しているデータクラス
 * @param titleContains タイトルの部分一致検索の値
 * @param uploaderName 投稿者ソートの値
 * @param tagItems タグの配列（文字列）
 * @param sort ソートの値
 * @param isTatimiDroidGetCache たちみどろいどで取得したキャッシュのみ利用する設定有効時
 * */
data class CacheFilterDataClass(
    val titleContains: String,
    val uploaderName: String,
    val tagItems: List<String>,
    val sort: String,
    val isTatimiDroidGetCache: Boolean
)