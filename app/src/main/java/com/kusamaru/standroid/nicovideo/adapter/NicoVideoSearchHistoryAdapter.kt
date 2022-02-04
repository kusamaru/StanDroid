package com.kusamaru.standroid.nicovideo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.entity.SearchHistoryDBEntity
import java.text.SimpleDateFormat

/**
 * 検索履歴一覧表示で使うAdapter
 *
 * @param historyList 検索履歴
 * @param onClick 項目押したら呼ばれる関数
 * @param onPinClick ピンを押したとき、解除したときに呼ばれる関数
 * */
class NicoVideoSearchHistoryAdapter(private val historyList: List<SearchHistoryDBEntity>, private val onClick: (SearchHistoryDBEntity) -> Unit, private val onPinClick: (SearchHistoryDBEntity) -> Unit) : RecyclerView.Adapter<NicoVideoSearchHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_search_history_type_icon_image_view)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_search_history_title_text_view)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_search_history_date_text_view)
        val parent = itemView.findViewById<ConstraintLayout>(R.id.adapter_nicovideo_search_history_parent)
        val pinImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_search_history_pin_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val context = parent.context
            val history = historyList[position]
            val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

            typeImageView.setImageDrawable(if (history.isTagSearch) context.getDrawable(R.drawable.ic_local_offer_24px) else context.getDrawable(R.drawable.ic_font_download_24px))
            titleTextView.text = history.text
            dateTextView.text = "${history.sort} / ${simpleDateFormat.format(history.addTime)}"

            // 押したら引数の関数を呼ぶ
            parent.setOnClickListener { onClick(history) }

            // ピン留めしているか
            if (history.pin) {
                pinImageView.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_done_24))
            } else {
                pinImageView.setImageDrawable(context.getDrawable(R.drawable.ic_push_pin_black_24dp))
            }

            // ピンを押したときに呼ばれる
            pinImageView.setOnClickListener { onPinClick(history) }

        }
    }

    override fun getItemCount(): Int {
        return historyList.size
    }
}