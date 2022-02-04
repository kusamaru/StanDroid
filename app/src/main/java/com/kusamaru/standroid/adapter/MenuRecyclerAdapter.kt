package com.kusamaru.standroid.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.kusamaru.standroid.R

/**
 * メニューで使えそうなAdapter。
 * このアプリについてで使ってる（てかこのために作った。）
 * ところでRecyclerViewってスクロールなし+wrap_content指定できるんか。有能
 * RecyclerViewに以下指定する必要あり
 *
 * ```xml
 * android:nestedScrollingEnabled="false"
 * android:overScrollMode="never"
 * ```
 * */
class MenuRecyclerAdapter(private val menuRecyclerList: ArrayList<MenuRecyclerAdapterDataClass>) : RecyclerView.Adapter<MenuRecyclerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_menu_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            // 値入れる
            val menu = menuRecyclerList[position]
            titleTextView.text = menu.title
            subTextView.text = menu.subTitle
            iconImageView.setImageDrawable(menu.icon)
            // Menu押したときはURL起動。
            parentLinearLayout.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, menu.launchUrl.toUri())
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = menuRecyclerList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.adapter_menu_title_textview)
        val subTextView: TextView = itemView.findViewById(R.id.adapter_menu_subtitle_textview)
        val iconImageView: ImageView = itemView.findViewById(R.id.adapter_menu_icon_imageview)
        val parentLinearLayout: LinearLayout = itemView.findViewById(R.id.adapter_menu_parent)
    }
}

/**
 * [MenuRecyclerAdapter]の引数に渡すやつ。配列にしてね
 * */
data class MenuRecyclerAdapterDataClass(
    val title: String,
    val subTitle: String,
    val icon: Drawable?,
    val launchUrl: String
)