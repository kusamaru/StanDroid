package io.github.takusan23.tatimidroid.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.dataclass.QualityData
import io.github.takusan23.tatimidroid.tool.getThemeTextColor
import java.security.cert.PolicyQualifierInfo

/**
 * 画質変更の一覧で使うAdapter
 *
 * @param onClick RecyclerView押したとき
 * @param qualityList 画質一覧。[QualityData]参照
 * */
class QualityAdapter(private val qualityList: List<QualityData>, val onClick: (QualityData) -> Unit) : RecyclerView.Adapter<QualityAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_quality_title_text_view)
        val descriptionTextView = itemView.findViewById<TextView>(R.id.adapter_quality_description_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_quality_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val qualityData = qualityList[position]
        val context = holder.titleTextView.context
        holder.apply {

            titleTextView.text = if (qualityData.isAvailable) {
                qualityData.title
            } else {
                "${qualityData.title} (プレ垢限定画質だから入って；；)"
            }
            descriptionTextView.text = qualityData.id
            // 選択中画質は色を変える
            if (qualityData.isSelected) {
                titleTextView.setTextColor(Color.parseColor("#0d46a0"))
                descriptionTextView.setTextColor(Color.parseColor("#0d46a0"))
            } else {
                titleTextView.setTextColor(getThemeTextColor(context))
                descriptionTextView.setTextColor(getThemeTextColor(context))
            }

            // 利用できないならグレーアウト
            titleTextView.isEnabled = qualityData.isAvailable
            descriptionTextView.isEnabled = qualityData.isAvailable
            (titleTextView.parent as View).isEnabled = qualityData.isAvailable
            if (qualityData.isAvailable) {
                titleTextView.alpha = 1f
                descriptionTextView.alpha = 1f
            } else {
                titleTextView.alpha = 0.5f
                descriptionTextView.alpha = 0.5f
            }

            // 押したとき
            (titleTextView.parent as View).setOnClickListener {
                if (qualityData.isAvailable) {
                    onClick(qualityData)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return qualityList.size
    }

}