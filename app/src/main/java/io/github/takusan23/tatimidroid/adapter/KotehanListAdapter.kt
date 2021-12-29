package io.github.takusan23.tatimidroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.room.init.KotehanDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

/**
 * コテハンの一覧表示に使うアダプター
 * */
class KotehanListAdapter(val kotehanList: ArrayList<KotehanDBEntity>) : RecyclerView.Adapter<KotehanListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_kotehan_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val kotehanData = kotehanList[position]
        holder.apply {
            val context = kotehanTextView.context
            // コテハン等入れる
            kotehanTextView.text = kotehanData.kotehan
            kotehanUserIdTextView.text = kotehanData.userId
            kotehanAddTimeTextView.text = toTimeFormat(kotehanData.addTime * 1000)
            // 削除ボタン押したとき
            kotehanDeleteButton.setOnClickListener {
                // 確認出す
                Snackbar.make(it, R.string.delete_message, Snackbar.LENGTH_SHORT).apply {
                    setAction(R.string.delete_ok) {
                        // 削除機能
                        GlobalScope.launch(Dispatchers.IO) {
                            KotehanDBInit.getInstance(context).kotehanDBDAO().delete(kotehanData)
                        }
                    }
                    show()
                }
            }
        }
    }

    override fun getItemCount() = kotehanList.size

    // UnixTime (ms) をきれいにする
    private fun toTimeFormat(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 (EEE)")
        return simpleDateFormat.format(time)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val kotehanTextView = itemView.findViewById<TextView>(R.id.adapter_kotehan_list_name)
        val kotehanUserIdTextView = itemView.findViewById<TextView>(R.id.adapter_kotehan_list_user_id)
        val kotehanAddTimeTextView = itemView.findViewById<TextView>(R.id.adapter_kotehan_list_add_time)
        val kotehanDeleteButton = itemView.findViewById<ImageView>(R.id.adapter_kotehan_list_delete_button)
    }
}
