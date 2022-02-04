package com.kusamaru.standroid.nicolive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.nicoapi.nicoad.NicoAdHistoryUserData
import com.kusamaru.standroid.R

/**
 * ニコニ広告の広告履歴を表示するRecyclerViewAdapter
 * */
class NicoAdHistoryAdapter(val nicoAdHistoryList: ArrayList<NicoAdHistoryUserData>) : RecyclerView.Adapter<NicoAdHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView = itemView.findViewById<TextView>(R.id.adapter_nicoad_history_name_text_view)
        val messageTextView = itemView.findViewById<TextView>(R.id.adapter_nicoad_history_message_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicoad_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val data = nicoAdHistoryList[position]
            nameTextView.text = "${data.advertiserName} さん：${data.contribution}"
            messageTextView.text = data.message
        }
    }

    override fun getItemCount(): Int {
        return nicoAdHistoryList.size
    }
}