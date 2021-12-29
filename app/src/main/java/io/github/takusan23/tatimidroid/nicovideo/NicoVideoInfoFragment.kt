package io.github.takusan23.tatimidroid.nicovideo

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.bottomfragment.NicoVideoLikeBottomFragment
import io.github.takusan23.tatimidroid.nicovideo.bottomfragment.NicoVideoLikeThanksMessageBottomFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSearchFragment
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.*
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoInfoBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 動画情報Fragment
 * */
class NicoVideoInfoFragment : Fragment() {

    val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    // NicoVideoFragmentのViewModelを取得する
    val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoInfoBinding.inflate(layoutInflater) }

    /** NicoVideoFragmentを取得する */
    private fun requireDevNicoVideoFragment(): NicoVideoFragment {
        return requireParentFragment() as NicoVideoFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.fragmentNicovideoInfoDescriptionTextview.movementMethod = LinkMovementMethod.getInstance()

        // LiveData監視
        setLiveData()

        // いいね機能
        setLike()

    }

    /**
     * LiveDataで動画情報をもらう
     * */
    private fun setLiveData() {
        // 動画情報
        viewModel.nicoVideoData.observe(viewLifecycleOwner) { nicovideoData ->
            viewBinding.fragmentNicovideoInfoTitleTextview.text = nicovideoData.title
            // 投稿日時、再生数 等
            viewBinding.fragmentNicovideoInfoUploadTextview.text = "${getString(R.string.post_date)}：${toFormatTime(nicovideoData.date)}"
            viewBinding.fragmentNicovideoInfoPlayCountTextview.text = "${getString(R.string.play_count)}：${nicovideoData.viewCount}"
            viewBinding.fragmentNicovideoInfoMylistCountTextview.text = "${getString(R.string.mylist)}：${nicovideoData.mylistCount}"
            viewBinding.fragmentNicovideoInfoCommentCountTextview.text = "${getString(R.string.comment_count)}：${nicovideoData.commentCount}"
            // 今日の日付から計算
            viewBinding.fragmentNicovideoInfoUploadDayCountTextview.text = "今日の日付から ${getDayCount(toFormatTime(nicovideoData.date))} 日前に投稿"
            // 一周年とか。
            val anniversary = calcAnniversary(toUnixTime(toFormatTime(nicovideoData.date))) // AnniversaryDateクラス みて
            when {
                anniversary == 0 -> {
                    viewBinding.fragmentNicovideoInfoUploadTextview.setTextColor(Color.RED)
                }
                anniversary != -1 -> {
                    viewBinding.fragmentNicovideoInfoUploadAnniversaryTextview.apply {
                        isVisible = true
                        text = AnniversaryDate.makeAnniversaryMessage(anniversary) // お祝いメッセージ作成
                    }
                }
            }
        }
        // 動画説明文
        viewModel.nicoVideoDescriptionLiveData.observe(viewLifecycleOwner) { description ->
            setLinkText(HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), viewBinding.fragmentNicovideoInfoDescriptionTextview)
        }
        // 投稿者情報
        viewBinding.fragmentNicovideoInfoOwnerTextview.isVisible = false
        viewModel.userDataLiveData.observe(viewLifecycleOwner) { userData ->
            viewBinding.fragmentNicovideoInfoOwnerTextview.isVisible = true
            viewBinding.fragmentNicovideoInfoOwnerTextview.text = userData.nickName
            // 押したとき
            viewBinding.fragmentNicovideoInfoOwnerCardview.setOnClickListener {
                if (!userData.userId.contains("co") && !userData.userId.contains("ch")) {
                    // ユーザー情報Fragmentへ飛ばす
                    val accountFragment = NicoAccountFragment().apply {
                        arguments = Bundle().apply {
                            putString("userId", userData.userId)
                        }
                    }
                    (requireActivity() as MainActivity).setFragment(accountFragment, "account")
                    requireDevNicoVideoFragment().viewBinding.fragmentNicovideoMotionLayout.transitionToState(R.id.fragment_nicovideo_transition_end)
                } else {
                    //チャンネルの時、ch以外にもそれぞれアニメの名前を入れても通る。例：te-kyu2 / gochiusa など
                    openBrowser("https://ch.nicovideo.jp/${userData.userId}")
                }
            }
            // 投稿者アイコン。インターネット接続時
            if (isConnectionInternet(context) && userData.largeIcon.isNotEmpty()) {
                // ダークモード対策
                viewBinding.fragmentNicovideoInfoOwnerImageview.imageTintList = null
                Glide.with(viewBinding.fragmentNicovideoInfoOwnerImageview)
                    .load(userData.largeIcon)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                    .into(viewBinding.fragmentNicovideoInfoOwnerImageview)
            }
        }
        // シリーズ。あれば
        viewModel.seriesHTMLDataLiveData.observe(viewLifecycleOwner) { seriesData ->
            seriesData ?: return@observe

            viewBinding.fragmentNicovideoInfoSeriesCardView.isVisible = true
            viewBinding.fragmentNicovideoInfoSeriesNameTextView.text = seriesData.seriesData.title
            Glide.with(viewBinding.fragmentNicovideoInfoSeriesThumbImageView)
                .load(seriesData.seriesData.thumbUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(viewBinding.fragmentNicovideoInfoSeriesThumbImageView)
            viewBinding.fragmentNicovideoInfoSeriesThumbImageView.imageTintList = null
            // 連続再生開始ボタン
            viewBinding.fragmentNicovideoInfoSeriesStartButton.setOnClickListener {
                // シリーズ連続再生押した時
                viewModel.addSeriesPlaylist(seriesId = seriesData.seriesData.seriesId)
            }
            // 最初、前後のボタン
            if (seriesData.firstVideoData != null) {
                viewBinding.fragmentNicovideoInfoSeriesFirstButton.text = """
                    ${getString(R.string.nicovideo_series_first_video)}
                    ${seriesData.firstVideoData.title}
                """.trimIndent()
                viewBinding.fragmentNicovideoInfoSeriesFirstButton.setOnClickListener {
                    viewModel.load(seriesData.firstVideoData.videoId, seriesData.firstVideoData.isCache, viewModel.isEco, viewModel.useInternet)
                }
            } else {
                viewBinding.fragmentNicovideoInfoSeriesFirstButton.isVisible = false
            }
            if (seriesData.prevVideoData != null) {
                viewBinding.fragmentNicovideoInfoSeriesPrevButton.text = """
                    ${getString(R.string.nicovideo_series_prev_video)}
                    ${seriesData.prevVideoData.title}
                """.trimIndent()
                viewBinding.fragmentNicovideoInfoSeriesPrevButton.setOnClickListener {
                    viewModel.load(seriesData.prevVideoData.videoId, seriesData.prevVideoData.isCache, viewModel.isEco, viewModel.useInternet)
                }
            } else {
                viewBinding.fragmentNicovideoInfoSeriesPrevButton.isVisible = false
            }
            if (seriesData.nextVideoData != null) {
                viewBinding.fragmentNicovideoInfoSeriesNextButton.text = """
                    ${getString(R.string.nicovideo_series_next_video)}
                    ${seriesData.nextVideoData.title}
                """.trimIndent()
                viewBinding.fragmentNicovideoInfoSeriesNextButton.setOnClickListener {
                    viewModel.load(seriesData.nextVideoData.videoId, seriesData.nextVideoData.isCache, viewModel.isEco, viewModel.useInternet)
                }
            } else {
                viewBinding.fragmentNicovideoInfoSeriesNextButton.isVisible = false
            }
        }
        // タグ
        viewModel.tagListLiveData.observe(viewLifecycleOwner) { tagList ->
            //たぐ
            viewBinding.fragmentNicovideoInfoTitleLinearlayout.removeAllViews()
            tagList.forEach { tagData ->
                val name = tagData.tagName
                val isDictionaryExists = tagData.hasNicoPedia //大百科があるかどうか
                val linearLayout = LinearLayout(context)
                linearLayout.orientation = LinearLayout.HORIZONTAL
                //ボタン
                val button = Button(context)
                //大きさとか
                val linearLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                linearLayoutParams.weight = 1F
                button.layoutParams = linearLayoutParams
                button.text = name
                button.isAllCaps = false
                linearLayout.addView(button)
                if (isDictionaryExists) {
                    val dictionaryButton = Button(context)
                    dictionaryButton.text = getString(R.string.dictionary)
                    linearLayout.addView(dictionaryButton)
                    //大百科ひらく
                    dictionaryButton.setOnClickListener {
                        openBrowser("https://dic.nicovideo.jp/a/$name")
                    }
                }
                viewBinding.fragmentNicovideoInfoTitleLinearlayout.addView(linearLayout)
                // タグ検索FragmentをViewPagerに追加する
                button.setOnClickListener {
                    // オフライン時は動かさない
                    if (isConnectionInternet(context)) {
                        val searchFragment = NicoVideoSearchFragment().apply {
                            arguments = Bundle().apply {
                                putString("search", name)
                                putBoolean("search_hide", true)
                                putBoolean("sort_show", true)
                            }
                        }
                        // 追加位置
                        val addPos = requireDevNicoVideoFragment().viewPagerAdapter.fragmentList.size
                        // ViewPager追加
                        requireDevNicoVideoFragment().viewPagerAdapter.addFragment(searchFragment, "${getString(R.string.tag)}：$name")
                        // ViewPager移動
                        requireDevNicoVideoFragment().viewBinding.fragmentNicovideoViewpager.currentItem = addPos
                    }
                    // 動画IDのとき。例：「後編→sm」とか
                    val id = IDRegex(name)
                    if (id != null) {
                        Snackbar.make(button, "${getString(R.string.find_video_id)} : $id", Snackbar.LENGTH_SHORT).apply {
                            setAction(R.string.play) {
                                (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId = id)
                            }
                            show()
                        }
                    }
                }
            }
        }
    }

    /** いいね機能用意 */
    private fun setLike() {
        if (viewModel.isOfflinePlay.value == false && isLoginMode(context)) {
            // キャッシュじゃない　かつ　ログイン必須モード
            viewBinding.fragmentNicovideoInfoLikeChip.isVisible = true
            // LiveData監視
            viewModel.isLikedLiveData.observe(viewLifecycleOwner) { isLiked ->
                viewModel.nicoVideoData.observe(viewLifecycleOwner) { data ->
                    // いいねボタン変更
                    setLikeChipStatus(isLiked, data)
                }
            }
            // お礼メッセージ監視
            viewModel.likeThanksMessageLiveData.observe(viewLifecycleOwner) {
                if (!viewModel.isAlreadyShowThanksMessage) {
                    val thanksMessageBottomFragment = NicoVideoLikeThanksMessageBottomFragment()
                    thanksMessageBottomFragment.show(parentFragmentManager, "thanks")
                }
            }
            // いいね押したとき
            viewBinding.fragmentNicovideoInfoLikeChip.setOnClickListener {
                if (viewModel.isLikedLiveData.value == true) {
                    requireDevNicoVideoFragment().showSnackbar(getString(R.string.unlike), getString(R.string.torikesu)) {
                        // いいね解除API叩く
                        viewModel.removeLike()
                    }
                } else {
                    // いいね開く
                    val nicoVideoLikeBottomFragment = NicoVideoLikeBottomFragment()
                    nicoVideoLikeBottomFragment.show(parentFragmentManager, "like")
                }
            }
        }
    }

    /** ハートのアイコン色とテキストを変更する関数 */
    private fun setLikeChipStatus(liked: Boolean, data: NicoVideoData) {
        val isShowLikeCount = prefSetting.getBoolean("setting_nicovideo_show_like_count", false)
        requireActivity().runOnUiThread {
            // いいね済み
            if (liked) {
                // いいね済み
                viewBinding.fragmentNicovideoInfoLikeChip.apply {
                    chipIconTint = ColorStateList.valueOf(Color.parseColor("#ffc0cb")) // ピンク
                    text = if (isShowLikeCount) {
                        "${getString(R.string.liked)}：${data.likeCount}"
                    } else {
                        getString(R.string.liked)
                    }
                }
            } else {
                viewBinding.fragmentNicovideoInfoLikeChip.apply {
                    chipIconTint = ColorStateList.valueOf(getThemeTextColor(context)) // テーマの色
                    text = if (isShowLikeCount) {
                        "${getString(R.string.like)}：${data.likeCount}"
                    } else {
                        getString(R.string.like)
                    }
                }
            }
        }
    }

    /** 投稿日のフォーマットをUnixTimeへ変換する */
    private fun toUnixTime(postedDateTime: String) = SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(postedDateTime).time

    /**
     * 動画投稿日が何日前か数えるやつ。
     * @param upDateTime yyyy/MM/dd HH:mm:ssの形式で。
     *
     * */
    private fun getDayCount(upDateTime: String): String {
        // UnixTime（ミリ秒）へ変換
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        // 時、分とかは切り捨てる（多分いらない。）
        val calendar = Calendar.getInstance().apply {
            time = simpleDateFormat.parse(upDateTime)
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        // 現在時刻から引く
        val calc = System.currentTimeMillis() - calendar.time.time
        // 計算で出す。多分もっといい方法ある。
        val second = calc / 1000    // ミリ秒から秒へ
        val minute = second / 60    // 秒から分へ
        val hour = minute / 60      // 分から時間へ
        val day = hour / 24         // 時間から日付へ
        return day.toString()
    }

    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)

    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * TextViewのリンク（mylist/数字）とか（sm157）とかを押したときブラウザ開くのではなくこのアプリ内で表示できるようにする。
     * */
    fun setLinkText(text: Spanned, textView: TextView) {
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
                    (requireActivity() as? MainActivity)?.setNicovideoFragment(videoId = id, isCache = false)
                }
            }, mather.start(), mather.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // マイリスト押せるように
        val mylistMatcher = NICOVIDEO_MYLIST_ID_REGEX.toPattern().matcher(text)
        while (mylistMatcher.find()) {
            val mylist = mylistMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // マイリスト表示FragmentをViewPagerに追加する
                    val mylistFragment = NicoVideoMyListListFragment().apply {
                        arguments = Bundle().apply {
                            putString("mylist_id", mylist.replace("mylist/", ""))// IDだけくれ
                            putBoolean("is_other", true)
                        }
                    }
                    requireDevNicoVideoFragment().apply {
                        // ViewPager追加
                        viewPagerAdapter.addFragment(mylistFragment, "${getString(R.string.mylist)}：$mylist")
                        // ViewPager移動
                        viewBinding.fragmentNicovideoViewpager.currentItem = viewPagerAdapter.fragmentTabName.size
                    }
                }
            }, mylistMatcher.start(), mylistMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // シリーズ押せるように
        val seriesMatcher = NICOVIDEO_SERIES_ID_REGEX.toPattern().matcher(text)
        while (seriesMatcher.find()) {
            val series = seriesMatcher.group()
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // マイリスト表示FragmentをViewPagerに追加する
                    val seriesFragment = NicoVideoSeriesFragment().apply {
                        arguments = Bundle().apply {
                            putString("series_id", series.replace("series/", "")) // IDだけくれ
                            putString("series_title", series) // シリーズのタイトル知らないのでIDでごめんね
                        }
                    }
                    requireDevNicoVideoFragment().apply {
                        // ViewPager追加
                        viewPagerAdapter.addFragment(seriesFragment, "${getString(R.string.series)}：$series")
                        // ViewPager移動
                        viewBinding.fragmentNicovideoViewpager.currentItem = viewPagerAdapter.fragmentTabName.size
                    }
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
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    startActivity(intent)
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
                    requireDevNicoVideoFragment().apply {
                        // 分：秒　を ミリ秒へ
                        val minute = time.split(":")[0].toLong() * 60
                        val second = time.split(":")[1].toLong()
                        exoPlayer.seekTo((minute + second) * 1000)
                    }
                }
            }, seekTimeMatcher.start(), seekTimeMatcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = span
        textView.movementMethod = LinkMovementMethod.getInstance();
    }

}
