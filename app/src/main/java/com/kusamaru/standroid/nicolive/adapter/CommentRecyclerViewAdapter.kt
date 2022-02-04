package com.kusamaru.standroid.nicolive.adapter

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.bottomfragment.CommentLockonBottomFragment
import com.kusamaru.standroid.CommentJSONParse
import com.kusamaru.standroid.nicolive.CommentFragment
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.init.KotehanDBInit
import com.kusamaru.standroid.tool.CustomFont
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * CommentJSONParse配列を使う
 * @param commentList 表示するコメント
 * @param commentFragment ViewModel取得したりBottomFragment表示させる時に使う。[CommentFragment] or [com.kusamaru.standroid.nicolive.compose.JCNicoLiveFragment]
 * */
class CommentRecyclerViewAdapter(val commentList: ArrayList<CommentJSONParse>, private val commentFragment: Fragment) : RecyclerView.Adapter<CommentRecyclerViewAdapter.ViewHolder>() {

    //UserIDの配列。初コメを太字表示する
    private val userList = arrayListOf<String>()
    private lateinit var prefSetting: SharedPreferences

    /** コテハン。[setKotehanDBChangeObserve]で自動更新してる */
    private val kotehanMap = mutableMapOf<String, String>()

    /** 最初のTextViewの色 */
    private var defaultTextViewColor: ColorStateList? = null

    /** フォント設定 */
    private var font: CustomFont? = null

    /** [CommentFragment]のViewModel */
    private val commentFragmentViewModel by commentFragment.viewModels<NicoLiveViewModel>({ commentFragment })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_comment_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val commentJSONParse = commentList[position]
        val context = holder.parentView.context
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // 一度だけ
        if (defaultTextViewColor == null) {
            defaultTextViewColor = holder.commentTextView.textColors
        }
        // 一度だけ
        if (font == null) {
            font = CustomFont(context)
            setKotehanDBChangeObserve(context)
        }

        // コテハン。なければユーザーIDで
        val userId = kotehanMap[commentJSONParse.userId] ?: commentJSONParse.userId

        // 絶対時刻か相対時刻か
        var time = ""
        if (prefSetting.getBoolean("setting_zettai_zikoku_hyouzi", true)) {
            if (commentJSONParse.date.isNotEmpty()) {
                //相対時刻（25:25）など
                val programStartTime = commentFragmentViewModel.nicoLiveHTML.programStartTime
                val commentUnixTime = commentJSONParse.date.toLong()
                val calc = (commentUnixTime - programStartTime)
                time = DateUtils.formatElapsedTime(calc) // 時：分：秒　っていい感じにしてくれる
            } else {
                time = "0"
            }
        } else {
            // 絶対時刻（12:13:00）など
            // UnixTime -> Minute
            if (commentJSONParse.date.isNotEmpty()) {
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = commentJSONParse.date.toLong() * 1000L
                val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
                time = simpleDateFormat.format(commentJSONParse.date.toLong() * 1000L)
            }
        }

        // 部屋名。部屋の名前はNicoLiveCommentで決定する。でも自分のコメントのときは「私です」って表示する
        val roomName = if (commentJSONParse.yourPost) context.getString(R.string.yourpost) else commentJSONParse.roomName

        var info = "$roomName | $time | $userId"

        // 公式番組のコメントはコメント番号存在しない
        val comment = if (commentJSONParse.commentNo.isEmpty()) {
            if (commentJSONParse.uneiComment.isNotEmpty()) {
                commentJSONParse.uneiComment // 運営コメントをきれいにしたやつ
            } else {
                commentJSONParse.comment
            }
        } else {
            if (commentJSONParse.uneiComment.isNotEmpty()) {
                "${commentJSONParse.commentNo} : ${commentJSONParse.uneiComment}" // 運営コメントをきれいにしたやつ
            } else {
                "${commentJSONParse.commentNo} : ${commentJSONParse.comment}"
            }
        }

        //NGスコア表示するか
        if (prefSetting.getBoolean("setting_show_ng", false)) {
            if (commentJSONParse.score.isNotEmpty()) {
                info = "$info | ${commentJSONParse.score}"
            } else {
                info = info
            }
        } else {
            info = info
        }

        // プレ垢
        if (commentJSONParse.premium.isNotEmpty()) {
            info = "$info | ${commentJSONParse.premium}"
        } else {
            info = info
        }

        // UserIDの配列になければ配列に入れる。初コメ
        if (userList.indexOf(commentJSONParse.userId) == -1) {
            userList.add(commentJSONParse.userId)
            //初コメは太字にする
            holder.commentTextView.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.commentTextView.typeface = Typeface.DEFAULT
        }

        holder.commentTextView.text = comment
        holder.roomNameTextView.text = info

        // 詳細画面出す
        holder.parentView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", commentJSONParse.comment)
            bundle.putString("user_id", commentJSONParse.userId)
            bundle.putString("liveId", commentJSONParse.videoOrLiveId)
            bundle.putString("label", info)
            val commentLockonBottomFragment = CommentLockonBottomFragment()
            commentLockonBottomFragment.arguments = bundle
            commentLockonBottomFragment.show(commentFragment.childFragmentManager, "comment_menu")
        }

        // 部屋の色
        if (prefSetting.getBoolean("setting_room_color", true)) {
            // 自分のコメントと部屋投稿と流量制限の色を変える
            val roomColor = if (commentJSONParse.yourPost) {
                Color.argb(255, 172, 209, 94) // 自分のコメント
            } else {
                getRoomColor(commentJSONParse.roomName, context) // 部屋別+流量制限
            }
            holder.roomNameTextView.setTextColor(roomColor)
            holder.commentRoomColorView.setBackgroundColor(roomColor)
        }

        //ID非表示
        if (prefSetting.getBoolean("setting_id_hidden", false)) {
            //非表示
            holder.roomNameTextView.isVisible = false
            //部屋の色をつける設定有効時はコメントのTextViewに色を付ける
            if (prefSetting.getBoolean("setting_room_color", true)) {
                holder.commentTextView.setTextColor(getRoomColor(commentJSONParse.roomName, context))
            } else {
                holder.commentTextView.setTextColor(defaultTextViewColor)
            }
        } else {
            holder.roomNameTextView.isVisible = true
            holder.commentTextView.setTextColor(defaultTextViewColor)
        }
        //一行表示とか
        if (prefSetting.getBoolean("setting_one_line", false)) {
            holder.commentTextView.maxLines = 1
        } else {
            holder.commentTextView.maxLines = Int.MAX_VALUE
        }

        // ユーザーの設定したフォントサイズ
        font?.apply {
            holder.commentTextView.textSize = commentFontSize
            holder.roomNameTextView.textSize = userIdFontSize
            // ユーザーの設定したフォント
            setTextViewFont(holder.commentTextView)
            setTextViewFont(holder.roomNameTextView)
        }

    }

    override fun getItemCount(): Int {
        return commentList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentTextView: TextView = itemView.findViewById(R.id.adapter_comment_text_view)
        val roomNameTextView: TextView = itemView.findViewById(R.id.adapter_room_name_text_view)
        val commentRoomColorView: View = itemView.findViewById(R.id.adapter_comment_room_color)
        val parentView: View = itemView.findViewById(R.id.adapter_comment_parent)
    }

    /**
     * コテハンデータベースを監視する
     * */
    private fun setKotehanDBChangeObserve(context: Context?) {
        if (context is AppCompatActivity) {
            context.lifecycleScope.launch {
                val dao = KotehanDBInit.getInstance(context).kotehanDBDAO()
                dao.flowGetKotehanAll().collect { kotehanList ->
                    kotehanMap.clear()
                    // 変更があった
                    kotehanList.forEach { kotehan ->
                        kotehanMap[kotehan.userId] = kotehan.kotehan
                    }
                }
            }
        }
    }

    /**
     * コメビュの部屋の色。NCVに（勝手に）追従する
     * まあ後数日でここも使わなくなるんですけどね（部屋統合で）
     * もう立ち見部屋の数でどれだけ人気かどうかって見れたのにもう見れなくなるのか。
     * */
    private fun getRoomColor(room: String, context: Context): Int {
        return when (room) {
            context.getString(R.string.official_program) -> {
                Color.argb(255, 0, 153, 229)
            }
            context.getString(R.string.arena) -> {
                Color.argb(255, 0, 153, 229)
            }
            context.getString(R.string.room_limit) -> {
                Color.argb(255, 234, 90, 61) // コメント流量規制にかかったコメントはstoreに流れてくる
            }
            "立ち見1" -> {
                Color.argb(255, 234, 90, 61)
            }
            "立ち見2" -> {
                Color.argb(255, 172, 209, 94)
            }
            "立ち見3" -> {
                Color.argb(255, 0, 217, 181)
            }
            "立ち見4" -> {
                Color.argb(255, 229, 191, 0)
            }
            "立ち見5" -> {
                Color.argb(255, 235, 103, 169)
            }
            "立ち見6" -> {
                Color.argb(255, 181, 89, 217)
            }
            "立ち見7" -> {
                Color.argb(255, 20, 109, 199)
            }
            "立ち見8" -> {
                Color.argb(255, 226, 64, 33)
            }
            "立ち見9" -> {
                Color.argb(255, 142, 193, 51)
            }
            "立ち見10" -> {
                Color.argb(255, 0, 189, 120)
            }
            else -> {
                Color.argb(255, 0, 153, 229)
            }
        }
    }

}