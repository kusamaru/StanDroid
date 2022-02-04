package com.kusamaru.standroid.nicolive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveGiftRankingUserData
import com.kusamaru.standroid.R

/**
 * 投げ銭のランキング一覧を表示するAdapter
 *
 * @param rankingUserData [NicoLiveGiftRankingUserData]の配列
 * */
class GiftRankingRecyclerViewAdapter(val rankingUserData: ArrayList<NicoLiveGiftRankingUserData>) : RecyclerView.Adapter<GiftRankingRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView = itemView.findViewById<TextView>(R.id.adapter_gift_ranking_rank_text_view)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_gift_ranking_name_text_view)
        val pointTextView = itemView.findViewById<TextView>(R.id.adapter_gift_ranking_point_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_gift_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val data = rankingUserData[position]
            rankTextView.text = "${data.rank} 位"
            nameTextView.text = "${data.advertiserName} さん"
            // 匿名でも投げられるらしいのでユーザーIDはあれば表示
            if (data.userId != null) {
                nameTextView.append("（ID：${data.userId}）")
            }
            pointTextView.text = "${data.totalContribution} 貢"
        }
    }

    override fun getItemCount(): Int {
        return rankingUserData.size
    }

}