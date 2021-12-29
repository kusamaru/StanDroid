package io.github.takusan23.tatimidroid.tool

import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

/**
 * ニコニコ動画の説明TextViewを装飾する関数がある。
 * */
object NicoVideoDescriptionText {

    /** 押したリンクがニコ動のIDだった時 */
    const val DESCRIPTION_TYPE_NICOVIDEO = "sm"

    /** 押したリンクがニコ動のマイリストだった時 */
    const val DESCRIPTION_TYPE_MYLIST = "mylist"

    /** 押したリンクがニコ動のシリーズだった時 */
    const val DESCRIPTION_TYPE_SERIES = "series"

    /** 押したリンクがURLだった時 */
    const val DESCRIPTION_TYPE_URL = "url"

    /** 押したリンクが指定時間へジャンプだった時。これだけLong（ミリ秒） */
    const val DESCRIPTION_TYPE_SEEK = "seek"

    /**
     * TextViewのリンク（mylist/数字）とか（sm157）とかを押したときブラウザ開くのではなくこのアプリ内で表示できるようにする。
     * @param text [androidx.core.text.HtmlCompat.fromHtml]の結果
     * @param textView その名の通り
     * @param click リンクになったテキストを押すと呼ばれる。[DESCRIPTION_TYPE_NICOVIDEO]等を参照
     * */
    fun setLinkText(text: Spanned, textView: TextView, click: (id: String, type: String) -> Unit) {
        // リンクを付ける。
        val span = Spannable.Factory.getInstance().newSpannable(text.toString())
        // 動画ID押せるように。ちなみに↓の変数はニコ動の動画ID正規表現
        val mather = NICOVIDEO_ID_REGEX.toPattern().matcher(text)
        while (mather.find()) {
            // 動画ID取得
            val id = mather.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 再生画面表示
                    click(id, DESCRIPTION_TYPE_NICOVIDEO)
                }
            }, mather.start(), mather.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // マイリスト押せるように
        val mylistMatcher = NICOVIDEO_MYLIST_ID_REGEX.toPattern().matcher(text)
        while (mylistMatcher.find()) {
            val mylist = mylistMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 関数をよぶ
                    click(mylist.replace("mylist/", ""), DESCRIPTION_TYPE_MYLIST)
                }
            }, mylistMatcher.start(), mylistMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // シリーズ押せるように
        val seriesMatcher = NICOVIDEO_SERIES_ID_REGEX.toPattern().matcher(text)
        while (seriesMatcher.find()) {
            val series = seriesMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 関数をよぶ
                    click(series.replace("series/", ""), DESCRIPTION_TYPE_SERIES)
                }
            }, seriesMatcher.start(), seriesMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // URL押せるように
        val URL_REGEX = "https?://[\\w!?/\\+\\-_~=;\\.,*&@#\$%\\(\\)\\'\\[\\]]+"
        val urlMather = URL_REGEX.toPattern().matcher(text)
        while (urlMather.find()) {
            val url = urlMather.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // ブラウザ
                    click(url, DESCRIPTION_TYPE_URL)
                }
            }, urlMather.start(), urlMather.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // 再生時間へ移動。例：#25:25で25:25へ移動できる
        val SEEK_TIME_REGEX = "(#)([0-9][0-9]):([0-9][0-9])"
        val seekTimeMatcher = SEEK_TIME_REGEX.toPattern().matcher(text)
        while (seekTimeMatcher.find()) {
            val time = seekTimeMatcher.group().replace("#", "")
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 再生時間操作
                    // 分：秒　を ミリ秒へ
                    val minute = time.split(":")[0].toLong() * 60
                    val second = time.split(":")[1].toLong()
                    click(((minute + second) * 1000).toString(), DESCRIPTION_TYPE_SEEK)
                }
            }, seekTimeMatcher.start(), seekTimeMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance();
    }

}