package io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass

/**
 * 好みタグのデータクラス
 *
 * @param name 名前
 * @param tagId タグのID。検索のときに使う
 * @param followersCount フォロワー数。NicoLiveHTMLのパーサーでは取れない
 * @param isFollowing フォローしているか。APIを叩いた時点ではフォロー中でもfalseになります
 * */
data class NicoLiveKonomiTagData(
    val name: String,
    val tagId: String,
    val followersCount: Int,
    val isFollowing: Boolean = false,
)