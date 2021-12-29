package io.github.takusan23.tatimidroid.nicolive.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.bottomfragment.ProgramMenuBottomSheet
import io.github.takusan23.tatimidroid.tool.isDarkMode
import java.text.SimpleDateFormat
import java.util.*

/**
 * 番組一覧表示で使うRecyclerViewAdapter
 *
 * @param isDisableCache キャッシュを利用しない（毎度取りに行く）
 * */
class CommunityRecyclerViewAdapter(val programList: ArrayList<NicoLiveProgramData>, val isDisableCache: Boolean = false) : RecyclerView.Adapter<CommunityRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_community_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return programList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = holder.dateTextView.context

        val item = programList[position]
        val title = item.title
        val name = item.communityName
        val live_time = item.beginAt
        val liveId = item.programId
        val datetime = item.beginAt
        val liveNow = item.lifeCycle
        val thumb = item.thum
        val isOfficial = item.isOfficial // 公式ならtrue
        val isOnAir = liveNow.contains("ON_AIR") || liveNow.contains("Begun")

        // ニコ生版ニコニコ実況は時間が取れないので非表示へ
        holder.dateTextView.isVisible = live_time != "-1"

        //時間を文字列に
        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss EEE曜日")
        val time = simpleDateFormat.format(live_time.toLong())


        holder.titleTextView.text = title
        holder.communityNameTextView.text = "[${name}]"

        if (isOnAir) {
            //放送中
            holder.dateTextView.text = time
            holder.dateTextView.setTextColor(Color.RED)
            // 視聴モードボタン用意
            initWatchModeButton(holder, item)
        } else {
            //予約枠
            holder.dateTextView.text = time
            if (isDarkMode(content)) {
                holder.dateTextView.setTextColor(Color.parseColor("#ffffff"))
            } else {
                holder.dateTextView.setTextColor(-1979711488)   //デフォルトのTextViewのフォント色
            }
        }

        /*
        * 番組IDコピー機能
        * */
        holder.communityCard.setOnLongClickListener {
            Toast.makeText(content, "${content.getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT).show()
            val clipboardManager = content.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            true
        }

        // サムネ
        if (thumb.isNotEmpty()) {
            holder.thumbImageView.imageTintList = null
            Glide.with(holder.thumbImageView).load(thumb).apply {
                if (isDisableCache) {
                    // キャッシュを利用しない
                    diskCacheStrategy(DiskCacheStrategy.NONE)
                    skipMemoryCache(false)
                }
                transition(DrawableTransitionOptions.withCrossFade())
                transform(CenterCrop(), RoundedCorners(10))
                into(holder.thumbImageView)
            }
        } else {
            // URLがからなので非表示
            holder.thumbImageView.isVisible = false
        }

        // ON_AIRの部分
        holder.lifeCycleTextView.text = liveNow
        // 色
        holder.lifeCycleTextView.background = if (isOnAir) {
            ColorDrawable(Color.RED)
        } else {
            ColorDrawable(Color.BLUE)
        }.apply { alpha = (255 * 0.5).toInt() }

        // TS予約などのボタン
        holder.liveMenuIconImageView.setOnClickListener {
            val programMenuBottomSheet = ProgramMenuBottomSheet()
            val bundle = Bundle()
            bundle.putString("liveId", liveId)
            programMenuBottomSheet.arguments = bundle
            programMenuBottomSheet.show((it.context as AppCompatActivity).supportFragmentManager, "menu")
        }

    }

    fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var titleTextView: TextView = itemView.findViewById(R.id.adapter_community_title_textview)
        var dateTextView: TextView = itemView.findViewById(R.id.adapter_community_date_textview)
        var communityCard: CardView = itemView.findViewById(R.id.adapter_community_card)
        val thumbImageView: ImageView = itemView.findViewById(R.id.adapter_community_program_thumb)
        val liveMenuIconImageView: ImageView = itemView.findViewById(R.id.adapter_community_menu_icon)
        val communityNameTextView: TextView = itemView.findViewById(R.id.adapter_community_community_name_textview)
        val lifeCycleTextView: TextView = itemView.findViewById(R.id.adapter_community_lifecycle_textview)
    }

    // 視聴モード選択ボタン初期化
    private fun initWatchModeButton(itemHolder: ViewHolder, nicoLiveProgramData: NicoLiveProgramData) {
        val context = itemHolder.communityCard.context
        val mainActivity = context as MainActivity

        itemHolder.apply {
            communityCard.setOnClickListener {
                mainActivity.setNicoliveFragment(nicoLiveProgramData.programId, nicoLiveProgramData.isOfficial)
            }
        }
    }

}
