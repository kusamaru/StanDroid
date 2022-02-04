package com.kusamaru.standroid.nicovideo.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoMyListData
import com.kusamaru.standroid.nicovideo.fragment.NicoVideoMyListListFragment
import com.kusamaru.standroid.R

/**
 * マイリスト一覧表示Adapter
 * */
class NicoVideoMyListAdapter(private val myListDataList: ArrayList<NicoVideoMyListData>, private val fragmentHostId: Int, private val fragmentManager: FragmentManager) : RecyclerView.Adapter<NicoVideoMyListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val linearLayout = itemView.findViewById<ConstraintLayout>(R.id.adapter_nicovideo_mylist_parent)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_mylist_title)
        val countTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_mylist_count_text_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_mylist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val myListItem = myListDataList[position]
            titleTextView.text = "${myListItem.title}"
            countTextView.text = if (!myListItem.isAtodemiru) {
                // あとでみるは件数持ってないので非表示
                "${myListItem.itemsCount} 件"
            } else ""

            // マイリスト動画一覧へ遷移
            linearLayout.setOnClickListener {
                val myListFragment = NicoVideoMyListListFragment().apply {
                    arguments = Bundle().apply {
                        putString("mylist_id", myListItem.id)
                        putBoolean("mylist_is_me", myListItem.isMe)
                    }
                }
                fragmentManager.beginTransaction().replace(fragmentHostId, myListFragment).addToBackStack("mylist_video").commit()
            }

        }
    }

    override fun getItemCount(): Int {
        return myListDataList.size
    }

}