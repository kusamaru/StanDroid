package com.kusamaru.standroid.nicolive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftHistoryUserData
import com.kusamaru.standroid.R
import java.util.*

/**
 * 投げ銭履歴を表示するRecyclerViewAdapter
 *
 * @param giftHistoryUserDataList [NicoLiveGiftHistoryUserData]の配列
 * */
class GiftHistoryRecyclerViewAdapter(val giftHistoryUserDataList: ArrayList<NicoLiveGiftHistoryUserData>) : RecyclerView.Adapter<GiftHistoryRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView = itemView.findViewById<ImageView>(R.id.adapter_gift_history_thumb_image_view)
        val pointTextView = itemView.findViewById<TextView>(R.id.adapter_gift_history_point_text_view)
        val userNameTextView = itemView.findViewById<TextView>(R.id.adapter_gift_history_user_name_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_gift_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val data = giftHistoryUserDataList[position]
            pointTextView.text = "${data.adPoint} pt"
            userNameTextView.text = "${data.advertiserName} さん"
            // 匿名でも投げられるらしいのでユーザーIDはあれば表示
            if (data.userId != null) {
                userNameTextView.append("（ID：${data.userId}）")
            }
            // 画像読み込み
            iconImageView.imageTintList = null
            Glide.with(iconImageView).load(data.itemThumbUrl).into(iconImageView)
        }
    }

    override fun getItemCount(): Int {
        return giftHistoryUserDataList.size
    }

}