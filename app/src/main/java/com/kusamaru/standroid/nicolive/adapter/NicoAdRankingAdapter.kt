package com.kusamaru.standroid.nicolive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdRankingUserData
import com.kusamaru.standroid.R

/**
 * ニコニ広告の貢献度ランキングを表示するためのRecyclerViewAdapter。
 * */
class NicoAdRankingAdapter(val rankingList: ArrayList<NicoAdRankingUserData>) : RecyclerView.Adapter<NicoAdRankingAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankTextView = itemView.findViewById<TextView>(R.id.adapter_nicoad_ranking_rank_text_view)
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_nicoad_ranking_name_text_view)
        val pointTextView = itemView.findViewById<TextView>(R.id.adapter_nicoad_ranking_point_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicoad_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val data = rankingList[position]

            rankTextView.text = "${data.rank}位"
            nameTextView.text = "${data.advertiserName} さん"
            pointTextView.text = "${data.totalContribution}貢"
        }
    }

    override fun getItemCount(): Int {
        return rankingList.size
    }

}