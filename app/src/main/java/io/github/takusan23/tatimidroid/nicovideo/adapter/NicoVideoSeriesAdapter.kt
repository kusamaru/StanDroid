package io.github.takusan23.tatimidroid.nicovideo.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.R

/**
 * シリーズ一覧表示Adapter
 *
 * @param fragmentManager Fragment遷移で使う。
 * @param fragmentHostId Fragmentを設置するViewのId
 * @param seriesList [NicoVideoSeriesData]の配列。これを一覧表示で使う
 * */
class NicoVideoSeriesAdapter(val seriesList: ArrayList<NicoVideoSeriesData>, val fragmentHostId: Int, val fragmentManager: FragmentManager) : RecyclerView.Adapter<NicoVideoSeriesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_series_thumb_image_view)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_series_title_text_view)
        val countTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_series_count_tex_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_series, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val series = seriesList[position]
            titleTextView.text = series.title
            countTextView.text = "${series.itemsCount} 件"
            thumbImageView.imageTintList = null
            Glide.with(thumbImageView).load(series.thumbUrl).into(thumbImageView)

            // 押した時
            (titleTextView.parent as View).setOnClickListener {
                // シリーズ動画一覧へ遷移
                val nicoVideoSeriesFragment = NicoVideoSeriesFragment().apply {
                    arguments = Bundle().apply {
                        putString("series_id", series.seriesId)
                    }
                }
                fragmentManager.beginTransaction().replace(fragmentHostId, nicoVideoSeriesFragment).addToBackStack("series_video").commit()
            }
        }
    }

    override fun getItemCount(): Int {
        return seriesList.size
    }

}