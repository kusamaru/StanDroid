package io.github.takusan23.tatimidroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.NicoHistoryDBEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 端末内履歴のRecyclerViewのAdapter
 * */
class NicoHistoryAdapter(private val arrayListArrayAdapter: ArrayList<NicoHistoryDBEntity>) : RecyclerView.Adapter<NicoHistoryAdapter.ViewHolder>() {

    var editText: EditText? = null
    lateinit var bottomSheetDialogFragment: BottomSheetDialogFragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nico_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = arrayListArrayAdapter[position]
        val id = item.serviceId
        val type = item.type
        val date = item.unixTime
        val title = item.title
        val communityId = item.userId

        holder.titleTextView.text = "$title\n$id"
        holder.dateTextView.text = unixToDataFormat(date).toString()

        //コミュIDをいれる
        holder.parentConstraintLayout.setOnClickListener {
            if (editText != null) {
                val text = if (type == "live") {
                    communityId
                } else {
                    id // 動画用に
                }
                editText!!.setText(text)
            }
            //けす
            if (::bottomSheetDialogFragment.isInitialized) {
                bottomSheetDialogFragment.dismiss()
            }
        }
        // 長押しで番組ID
        holder.parentConstraintLayout.setOnLongClickListener {
            editText?.setText(id)
            //けす
            if (::bottomSheetDialogFragment.isInitialized) {
                bottomSheetDialogFragment.dismiss()
            }
            true
        }

        // アイコン
        val icon = if (type == "video") {
            holder.typeIcon.context.getDrawable(R.drawable.video_icon)
        } else {
            holder.typeIcon.context.getDrawable(R.drawable.live_icon)
        }
        holder.typeIcon.setImageDrawable(icon)
    }

    fun unixToDataFormat(unixTime: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyy/MM/dd\nHH:mm:ss")
        return simpleDateFormat.format(unixTime * 1000)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val parentConstraintLayout = itemView.findViewById<ConstraintLayout>(R.id.adapter_nico_history_parent)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nico_history_title)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nico_history_date)
        var typeIcon = itemView.findViewById<ImageView>(R.id.adapter_nico_history_icon)

    }
}
