package com.kusamaru.standroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.activity.NGListActivity
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.entity.NGDBEntity
import com.kusamaru.standroid.room.init.NGDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * NG一覧表示RecyclerView。削除とかできるよ
 * */
class NGListRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<NGDBEntity>) : RecyclerView.Adapter<NGListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_ng_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position]
        val context = holder.nameTextView.context

        // 入れる
        val type = item.type
        val value = item.value
        holder.nameTextView.text = value
        // 削除
        holder.deleteButton.setOnClickListener {
            // Snackbar
            Snackbar.make(holder.nameTextView, context.getText(R.string.delete_message), Snackbar.LENGTH_SHORT).setAction(context.getText(R.string.delete)) {
                GlobalScope.launch(Dispatchers.Main) {
                    //削除
                    withContext(Dispatchers.IO) {
                        NGDBInit.getInstance(context).ngDBDAO().deleteByValue(value)
                    }
                    //再読み込み
                    if (context is NGListActivity) {
                        if (type == "comment") {
                            context.loadNGComment()
                        } else {
                            context.loadNGUser()
                        }
                    }
                    //消したメッセージ
                    Toast.makeText(context, context.getText(R.string.delete_successful), Toast.LENGTH_SHORT).show()
                }
            }.show()
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var nameTextView: TextView = itemView.findViewById(R.id.adapter_ng_list_text)
        var deleteButton: ImageView = itemView.findViewById(R.id.adapter_ng_list_delete_button)

    }
}
