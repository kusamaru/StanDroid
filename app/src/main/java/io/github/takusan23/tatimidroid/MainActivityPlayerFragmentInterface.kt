package io.github.takusan23.tatimidroid

/**
 *  MainActivityの上に表示させるFragmentにはこれを実装してほしい。(生放送と動画のことな)
 *
 *  何でかっていうと戻るボタンがFragmentでは取れない。
 *  からActivityでFragmentを取得することになるんだけど、
 *  生放送と動画が存在するからわざわざキャストできるか判定しないと行けないわけ。
 *
 *  ```
 *  // 実在しないけどこんな風
 *  val fragment = fragmentManager.findFragment()
 *  when(fragment){
 *      is CommentFragment -> { fragment.onBackKey() }
 *      is NicoVideoFragment -> { fragment.onBackKey() }
 *  }
 *  ```
 *
 *  でそれがめんどいから生放送Fragmentでも動画Fragmentでも同じ関数を持たせておく。
 *  これで一回キャストさせれば何も怖くない
 *
 * ```
 * // 実在しないけどこんな風
 * val fragment = fragmentManager.findFragment()
 * (fragment as MainActivityPlayerFragmentInterface).onBackButtonPress() // どちらのFragmentにも onBackButtonPress() が存在する
 * ```
 *
 *　継承のほうが良いとかは知らん。オブジェクト指向難しくて笑えない。初めて作ったインターフェイス
 * */
interface MainActivityPlayerFragmentInterface {

    /**
     * 戻るボタンを押した時に呼ばれる関数
     * なおMainActivity終了はFragmentが取れなくなったら。
     * */
    @Deprecated("onBackPressedDispatcherが使える")
    fun onBackButtonPress()

    /**
     * ミニプレイヤーで再生している場合はtrueを返してほしい
     * */
    fun isMiniPlayerMode(): Boolean

}