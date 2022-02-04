package com.kusamaru.standroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.NimadoActivity
import com.kusamaru.standroid.R
import java.util.*

class NimadoListRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) : RecyclerView.Adapter<NimadoListRecyclerViewAdapter.ViewHolder>() {

    var activity: NimadoActivity? = null
    var linearLayout: LinearLayout? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_nimado_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item: ArrayList<String> = arrayListArrayAdapter[position] as ArrayList<String>

        val title = item[1]
        val id = item[2]

        holder.programTitleTextView.text = title
        holder.programIDTextView.text = id

        //消すボタン
        holder.closeButton.setOnClickListener {
            //LinearLayoutと配列からけす
            activity?.apply {
                linearLayout?.removeViewAt(position)
                programList.removeAt(position)
                //RecyclerViewからも
                recyclerViewList.removeAt(position)
                nimadoListRecyclerViewAdapter.notifyDataSetChanged()
                //Fragmentも閉じる
                supportFragmentManager.findFragmentByTag(id)?.apply {
                    supportFragmentManager.beginTransaction().remove(this).commit()
                }
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var programTitleTextView: TextView
        var programIDTextView: TextView
        var closeButton: ImageButton

        init {
            programTitleTextView = itemView.findViewById(R.id.adapter_nimado_list_program_title)
            programIDTextView = itemView.findViewById(R.id.adapter_nimado_list_program_id)
            closeButton = itemView.findViewById(R.id.adapter_nimado_list_close_button)
        }
    }

}