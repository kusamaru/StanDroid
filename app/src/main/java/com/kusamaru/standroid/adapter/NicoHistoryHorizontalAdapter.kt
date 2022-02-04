package com.kusamaru.standroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.R

/**
 * 端末内履歴の合計数表示してるやつ
 *
 * @param textList 表示したい文字の配列
 * */
class NicoHistoryHorizontalAdapter(private val textList: List<String>) : RecyclerView.Adapter<NicoHistoryHorizontalAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.adapter_nicohistory_horizontal_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nico_history_horizontal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val text = textList[position]
            textView.text = text
        }
    }

    override fun getItemCount(): Int = textList.size
}