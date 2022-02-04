package com.kusamaru.standroid.nicovideo.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.nicoapi.nicorepo.NicoRepoDataClass
import com.kusamaru.standroid.nicolive.bottomfragment.ProgramMenuBottomSheet
import com.kusamaru.standroid.nicovideo.bottomfragment.NicoVideoListMenuBottomFragment
import com.kusamaru.standroid.R
import java.text.SimpleDateFormat

/**
 * ニコレポ表示Fragment（[com.kusamaru.standroid.nicovideo.fragment.NicoVideoNicoRepoFragment]）で使うAdapter。
 *
 * 動画、生放送対応
 * */
class NicoRepoAdapter(val list: ArrayList<NicoRepoDataClass>) : RecyclerView.Adapter<NicoRepoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicorepo_title_text_view)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_nicorepo_name_text_view)
        val thumbImageView = itemView.findViewById<ImageView>(R.id.adapter_nicorepo_thumb_image_view)
        val typeImageView = itemView.findViewById<ImageView>(R.id.adapter_nicorepo_type_image_view)
        val menuImageView = itemView.findViewById<ImageView>(R.id.adapter_nicorepo_menu_image_view)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nicorepo_date_text_view)
        val cardView = itemView.findViewById<CardView>(R.id.adapter_nicorepo_parent_card_view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicorepo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val context = titleTextView.context

            // テキストセット
            val item = list[position]
            titleTextView.text = HtmlCompat.fromHtml("${item.message}<br>${item.title}", HtmlCompat.FROM_HTML_MODE_COMPACT)
            nameTextView.text = item.userName
            dateTextView.text = toFormatTime(item.date)

            // さむね
            thumbImageView.imageTintList = null
            Glide.with(thumbImageView).load(item.thumbUrl).into(thumbImageView)

            // 動画 or 生放送
            val typeIcon = if (item.isVideo) context.getDrawable(R.drawable.ic_local_movies_24px) else context.getDrawable(R.drawable.ic_outline_live_tv_24px_black)
            typeImageView.setImageDrawable(typeIcon)

            // メニュー押した時
            menuImageView.setOnClickListener {
                if (item.isVideo) {
                    // 動画
                    val menuBottomSheet = NicoVideoListMenuBottomFragment()
                    // データ渡す
                    val bundle = Bundle()
                    bundle.putString("video_id", item.contentId)
                    bundle.putBoolean("is_cache", false)
                    menuBottomSheet.arguments = bundle
                    (context as MainActivity).currentFragment()?.apply {
                        menuBottomSheet.show(this.childFragmentManager, "menu")
                    }
                } else {
                    // 生放送
                    val programMenuBottomSheet = ProgramMenuBottomSheet()
                    val bundle = Bundle()
                    bundle.putString("liveId", item.contentId)
                    programMenuBottomSheet.arguments = bundle
                    (context as MainActivity).currentFragment()?.apply {
                        programMenuBottomSheet.show(this.childFragmentManager, "menu")
                    }
                }
            }

            // 再生方法
            cardView.setOnClickListener {
                if (item.isVideo) {
                    // 動画
                    (context as? MainActivity)?.setNicovideoFragment(videoId = item.contentId)
                } else {
                    (context as? MainActivity)?.setNicoliveFragment(item.contentId, false)
                }
            }
        }
    }

    fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
        return simpleDateFormat.format(time)
    }

}