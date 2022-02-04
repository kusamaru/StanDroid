package com.kusamaru.standroid.nicovideo.adapter

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.bottomfragment.CommentLockonBottomFragment
import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.nicoapi.nicovideo.NicoruAPI
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.init.KotehanDBInit
import com.kusamaru.standroid.tool.CustomFont
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動のコメント表示Adapter
 * @param isOffline trueにするとニコるくん押せなくします
 * @param arrayListArrayAdapter コメント配列
 * @param fragmentManager BottomFragmentを表示する際に使う
 * @param nicoruAPI ニコるくんAPIのために
 * */
class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<CommentJSONParse>, private val fragmentManager: FragmentManager, private val isOffline: Boolean, private val nicoruAPI: NicoruAPI?) : RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    lateinit var prefSetting: SharedPreferences
    lateinit var font: CustomFont

    var textColor = Color.parseColor("#000000")

    /** コテハン。DBに変更が入ると自動で更新されます（[setKotehanDBChangeObserve]参照）。ってかこれAdapterで持っとけば良くない？ */
    val kotehanMap = mutableMapOf<String, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val context = holder.commentTextView.context

        // しょっきかー
        if (!::font.isInitialized) {
            font = CustomFont(context)
            prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            textColor = TextView(context).textColors.defaultColor
            // コテハンデータベース監視
            setKotehanDBChangeObserve(context)
        }

        val item = arrayListArrayAdapter[position]
        val comment = item.comment
        val date = item.date
        val vpos = item.vpos
        val time = vpos.toFloat() / 100 //再生時間。100で割ればいいっぽい？
        // きれいな形へ
        val formattedTime = formatTime(time)
        val mail = item.mail

        holder.userNameTextView.maxLines = 1
        // たちみどろいど以外のキャッシュはCommentNoがないので？
        if (item.commentNo == "-1" || item.commentNo.isEmpty()) {
            holder.commentTextView.text = "$comment"
        } else {
            holder.commentTextView.text = "${item.commentNo}：$comment"
        }

        // ニコるくん表示
        val isShowNicoruButton = prefSetting.getBoolean("setting_nicovideo_nicoru_show", false)
        // mail（コマンド）がないときは表示しない
        val mailText = if (item.mail.isNotEmpty()) {
            "| $mail "
        } else {
            ""
        }

        // NGスコア表示するか
        val ngScore = if (prefSetting.getBoolean("setting_show_ng", false) && item.score.isNotEmpty()) {
            "| ${item.score} "
        } else {
            ""
        }

        // ユーザーの設定したフォントサイズ
        font.apply {
            holder.commentTextView.textSize = commentFontSize
            holder.userNameTextView.textSize = userIdFontSize
        }
        // フォント
        font.apply {
            setTextViewFont(holder.commentTextView)
            setTextViewFont(holder.userNameTextView)
        }

        // かんたんコメント（あらし機能）、通常コメ、投稿者コメ、ニコるカウントに合わせて色つける
        val color = when {
            item.fork == 1 -> Color.argb(255, 172, 209, 94)// 投稿者コメント
            item.fork == 2 -> Color.argb(255, 234, 90, 61) // かんたんコメント
            else -> Color.argb(255, 0, 153, 229) // それ以外（通常）
        }
        holder.nicoruColor.setBackgroundColor(getNicoruLevelColor(item.nicoru, color))

        // 一般会員にはニコる提供されてないのでニコる数だけ表示
        // あとDevNicoVideoFragmentはがめんスワイプしてたらなんか落ちたので
        val nicoruCount = if (!isShowNicoruButton && item.nicoru > 0) {
            "| ニコる ${item.nicoru} "
        } else {
            ""
        }

        // 動画でコテハン。DevNicoVideoFragment（第二引数）がnullなら動きません。
        val kotehanOrUserId = kotehanMap.get(item.userId) ?: item.userId

        holder.userNameTextView.text = "${setTimeFormat(date.toLong())} | $formattedTime $mailText$nicoruCount$ngScore| ${kotehanOrUserId}"
        holder.nicoruButton.text = item.nicoru.toString()

        // ロックオン芸（詳細画面表示）
        holder.parentLinearLayout.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", item.comment)
            bundle.putString("user_id", item.userId)
            bundle.putString("liveId", item.videoOrLiveId)
            bundle.putString("label", holder.userNameTextView.text.toString())
            bundle.putLong("current_pos", item.vpos.toLong())
            val commentLockonBottomFragment = CommentLockonBottomFragment()
            commentLockonBottomFragment.arguments = bundle
            commentLockonBottomFragment.show(fragmentManager, "comment_menu")
        }

        // ニコる押したとき
        holder.nicoruButton.setOnClickListener {
            postNicoru(context, holder, item)
        }

        // プレ垢はニコるくんつける
        if (nicoruAPI?.isPremium == true && isShowNicoruButton) {
            holder.nicoruButton.isVisible = true
            // ただしキャッシュ再生時は押せなくする
            if (isOffline) {
                holder.nicoruButton.apply {
                    isClickable = false
                    isEnabled = false
                }
            }
        }

    }

    /**
     * コテハンデータベースを監視する
     * */
    private fun setKotehanDBChangeObserve(context: Context?) {
        // ContextがActivityじゃないと
        if (context is AppCompatActivity) {
            context.lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    val dao = KotehanDBInit.getInstance(context).kotehanDBDAO()
                    dao.flowGetKotehanAll().collect { kotehanList ->
                        // コテハンDBに変更があった
                        kotehanMap.clear()
                        kotehanList.forEach { kotehan ->
                            // 令和最新版のコテハン配列を適用する
                            kotehanMap[kotehan.userId] = kotehan.kotehan
                        }
                    }
                }
            }
        }
    }

    /** ニコるくんニコる関数 */
    private fun postNicoru(context: Context, holder: ViewHolder, item: CommentJSONParse) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast(context, "${context.getString(R.string.error)}\n${throwable}")
        }
        GlobalScope.launch(errorHandler) {
            val userSession = prefSetting.getString("user_session", "")!!

            if (nicoruAPI?.isPremium == true) {
                val responseNicoru = nicoruAPI.postNicoru(item.commentNo, item.comment, "${item.date}.${item.dateUsec}")
                if (!responseNicoru.isSuccessful) {
                    // 失敗時
                    showToast(context, "${context.getString(R.string.error)}\n${responseNicoru.code}")
                    return@launch
                }
                val responseString = responseNicoru.body?.string()
                // 成功したか
                val jsonObject = JSONArray(responseString).getJSONObject(0)
                val status = nicoruAPI.nicoruResultStatus(jsonObject)
                when (status) {
                    0 -> {
                        // ニコれた
                        item.nicoru = nicoruAPI.nicoruResultNicoruCount(jsonObject)
                        val nicoruId = nicoruAPI.nicoruResultId(jsonObject)
                        showSnackbar(holder.commentTextView, "${context.getString(R.string.nicoru_ok)}：${item.nicoru}\n${item.comment}", context.getString(R.string.nicoru_delete)) {
                            // 取り消しAPI叩く
                            GlobalScope.launch(Dispatchers.Default) {
                                val deleteResponse = nicoruAPI.deleteNicoru(userSession, nicoruId)
                                if (deleteResponse.isSuccessful) {
                                    this@NicoVideoAdapter.showToast(context, context.getString(R.string.nicoru_delete_ok))
                                } else {
                                    this@NicoVideoAdapter.showToast(context, "${context.getString(R.string.error)}${deleteResponse.code}")
                                }
                            }
                        }
                        // ニコるボタンに再適用
                        holder.nicoruButton.post {
                            holder.nicoruButton.text = item.nicoru.toString()
                        }
                    }
                    2 -> {
                        // nicoruKey失効。
                        GlobalScope.launch(Dispatchers.Default) {
                            // 再取得
                            nicoruAPI.init()
                            postNicoru(context, holder, item)
                        }
                    }
                    else -> {
                        this@NicoVideoAdapter.showToast(context, context.getString(R.string.nicoru_error))
                    }
                }
            }
        }
    }


    /** Snackbarを表示する */
    private fun showSnackbar(view: View, message: String, action: String, click: () -> Unit) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).apply {
            setAction(action) {
                click()
            }
            getView().elevation = 30f // なんかBottomSheetから見えない
        }.show()
    }


    /**
     * 時間表記をきれいにする関数
     * */
    private fun formatTime(time: Float): String {
        val minutes = time / 60
        val hour = (minutes / 60).toInt()
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "${hour}:${simpleDateFormat.format(time * 1000)}"
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var commentTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_comment_textview)
        var userNameTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_user_textview)
        var nicoruButton: MaterialButton = itemView.findViewById(R.id.adapter_nicovideo_comment_nicoru)
        val nicoruColor: View = itemView.findViewById(R.id.adapter_nicovideo_nicoru_color)
        val parentLinearLayout: LinearLayout = itemView.findViewById(R.id.adapter_nicovideo_nicoru_parent)
    }

    fun setTimeFormat(date: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(date * 1000)
    }

    fun showToast(context: Context, message: String) {
        (context as AppCompatActivity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ニコるの数に応じた色。
     * @param nicoruCount ニコるの数
     * @param elseColor 3未満の場合に返す色
     * @return いろ
     * */
    private fun getNicoruLevelColor(nicoruCount: Int, elseColor: Int) = when {
        nicoruCount >= 9 -> Color.rgb(252, 216, 66)
        nicoruCount >= 6 -> Color.rgb(253, 235, 160)
        nicoruCount >= 3 -> Color.rgb(254, 245, 207)
        else -> elseColor
    }

}
