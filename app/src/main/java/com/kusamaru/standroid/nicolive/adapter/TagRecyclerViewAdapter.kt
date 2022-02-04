package com.kusamaru.standroid.nicolive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoTagItemData
import com.kusamaru.standroid.nicolive.viewmodel.NicoLiveViewModel
import com.kusamaru.standroid.R
import java.util.*

/**
 * タグ編集で使う一覧表示Adapter
 *
 * @param nicoTagItemDataList タグ配列
 * @param nicoLiveViewModel タグを削除するときに、削除関数がViewModel側に書いてあるので
 * */
class TagRecyclerViewAdapter(private val nicoTagItemDataList: ArrayList<NicoTagItemData>, private val nicoLiveViewModel: NicoLiveViewModel) : RecyclerView.Adapter<TagRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_tag_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nicoTagItemDataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = nicoTagItemDataList[position]

        val tagName = item.tagName
        val isLocked = item.isLocked

        holder.textView.text = tagName

        //　削除できない場合
        if (isLocked) {
            holder.lockedButton.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.lockedButton.visibility = View.GONE
            holder.deleteButton.visibility = View.VISIBLE
        }

        // 削除ボタン
        holder.deleteButton.setOnClickListener {
            nicoLiveViewModel.deleteTag(tagName)
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView = itemView.findViewById<TextView>(R.id.adapter_tag_textview)
        var deleteButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_remove_button)
        var lockedButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_is_locked)
    }

}