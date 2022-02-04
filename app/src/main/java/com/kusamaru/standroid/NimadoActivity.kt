package com.kusamaru.standroid

import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kusamaru.standroid.adapter.NimadoListRecyclerViewAdapter
import com.kusamaru.standroid.nicolive.bottomfragment.NimadoLiveIDBottomFragment
import com.kusamaru.standroid.nicolive.CommentFragment
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.DisplaySizeTool
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.tool.isDarkMode
import com.kusamaru.standroid.databinding.ActivityNimadoBinding
import okhttp3.*
import okhttp3.Callback
import org.jsoup.Jsoup
import java.io.IOException


/*
* にまど！！！！
* */
class NimadoActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    lateinit var darkModeSupport: DarkModeSupport

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nimadoListRecyclerViewAdapter: NimadoListRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    //番組IDの配列
    var programList = ArrayList<String>()

    //視聴モードの配列
    var watchModeList = ArrayList<String>()

    //番組の名前
    var programNameList = arrayListOf<String>()

    //公式番組かどうか
    var officialList = arrayListOf<String>()

    // HTML
    var htmlList = arrayListOf<String>()

    var fragmentList = arrayListOf<Fragment>()

    /** findViewById駆逐 */
    private val viewBinding by lazy { ActivityNimadoBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード
        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setNimadoActivityTheme(this)

        setContentView(viewBinding.root)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        // println(savedInstanceState == null)


        //自作Toolbarを適用させる
        setSupportActionBar(viewBinding.nimadoActivityToolbar)

        //ステータスバーを透過する
        //window.statusBarColor = Color.TRANSPARENT

        //ダークモード対応
        if (isDarkMode(this)) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(darkModeSupport.context)))
        }

        //ハンバーガーアイコンを実装
        // sync drawer
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, viewBinding.drawerLayout, viewBinding.nimadoActivityToolbar, R.string.nimado, R.string.nimado
        )
        viewBinding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        //RecyclerView初期化
        initRecyclerView()

        //追加ボタン
        viewBinding.nimadoActivityAddLiveidButton.setOnClickListener {
            val nimadoLiveIDBottomFragment = NimadoLiveIDBottomFragment()
            nimadoLiveIDBottomFragment.show(supportFragmentManager, "nimado_liveid")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //値をonCreateの引数「savedInstanceState」に値を入れる
        outState.putStringArrayList("program_list", programList)
        outState.putStringArrayList("watch_mode_list", watchModeList)
        outState.putStringArrayList("program_name", programNameList)
        outState.putStringArrayList("official_list", officialList)
        outState.putStringArrayList("html_list", htmlList)
        outState.putSerializable("fragment_list", fragmentList)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        programList = savedInstanceState.getStringArrayList("program_list") as ArrayList<String>
        programNameList = savedInstanceState.getStringArrayList("program_name") as ArrayList<String>
        watchModeList =
            savedInstanceState.getStringArrayList("watch_mode_list") as ArrayList<String>
        officialList = savedInstanceState.getStringArrayList("official_list") as ArrayList<String>
        htmlList = savedInstanceState.getStringArrayList("html_list") as ArrayList<String>
        //復活させる
        for (index in 0 until programList.size) {
            val liveID = programList[index]
            val watchMode = watchModeList[index]
            val isOfficial = officialList[index].toBoolean()
            val html = htmlList[index]
            addNimado(liveID, watchMode, html, isOfficial, true)
        }

    }

    fun addNimado(liveId: String, watchMode: String, html: String, isOfficial: Boolean, isResume: Boolean = false) {
        //番組ID
        //二窓中の番組IDを入れる配列
        if (!programList.contains(liveId)) {
            programList.add(liveId)
            watchModeList.add(watchMode)
            officialList.add(isOfficial.toString())
            htmlList.add(html)
        }
        //動的にView作成
        val screenWidth = DisplaySizeTool.getDisplayWidth(this)

        //区切りがあれなのでCardViewの上にLinearLayoutを乗せる
        val cardView = CardView(this)
        val linearLayout = LinearLayout(this)

        val layoutParams = LinearLayout.LayoutParams(
            screenWidth / 2,    //半分の大きさにする
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        linearLayout.layoutParams = layoutParams

        //CardViewの丸みとViewの感覚
        layoutParams.setMargins(2, 2, 2, 2)
        cardView.layoutParams = layoutParams
        cardView.radius = 20f

        linearLayout.setPadding(20, 20, 20, 20)

        linearLayout.orientation = LinearLayout.VERTICAL

        linearLayout.id = View.generateViewId()

        //ScrollViewなLinearLayoutに入れる
        cardView.addView(linearLayout)
        //ダークモード時はCardView黒くする
        if (isDarkMode(this)) {
            cardView.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        }
        viewBinding.nimadoActivityLinearlayout.addView(cardView)


        // Fragment設置
        val commentFragment = CommentFragment()
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        bundle.putBoolean("isOfficial", isOfficial)
        commentFragment.arguments = bundle


        val trans = supportFragmentManager.beginTransaction()
        trans.replace(linearLayout.id, commentFragment, liveId)
        trans.commit()
        fragmentList.add(commentFragment)
        //RecyclerViewへアイテム追加
        //onResumeから来たときはAPIを叩かない（非同期処理は難しすぎる）
        if (isResume) {
            val pos = programList.indexOf(liveId)
            //RecyclerViewついか
            val item = arrayListOf<String>()
            item.add("")
            item.add(programNameList[pos])
            item.add(liveId)
            //非同期処理なので順番を合わせる
            recyclerViewList.add(item)
            runOnUiThread {
                nimadoListRecyclerViewAdapter.notifyDataSetChanged()
            }
        } else {
            addRecyclerViewItem(liveId)
        }

    }

    fun addRecyclerViewItem(liveId: String) {
        val user_session = pref_setting.getString("user_session", "") ?: ""
        //API叩いてタイトルを取得する
        //適当にAPI叩いて認証情報エラーだったら再ログインする
        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus?v=${liveId}")
            .header("Cookie", "user_session=${user_session}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //？
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                if (response.isSuccessful) {
                    val xml = Jsoup.parse(responseString)
                    val title = xml.getElementsByTag("title")[0].text()
                    //RecyclerViewついか
                    val item = arrayListOf<String>()
                    item.add("")
                    item.add(title)
                    item.add(liveId)
                    //非同期処理なので順番を合わせる
                    recyclerViewList.add(item)
                    programNameList.add(title)
                    runOnUiThread {
                        nimadoListRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun initRecyclerView() {
        viewBinding.nimadoActivityListRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@NimadoActivity)
            nimadoListRecyclerViewAdapter = NimadoListRecyclerViewAdapter(recyclerViewList)
            adapter = nimadoListRecyclerViewAdapter
            nimadoListRecyclerViewAdapter.linearLayout = viewBinding.nimadoActivityLinearlayout
            nimadoListRecyclerViewAdapter.activity = this@NimadoActivity
            //区切り線いれる
            val itemDecoration = DividerItemDecoration(this@NimadoActivity, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        //ドラッグできるようにする
        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val adapter = recyclerView.adapter as NimadoListRecyclerViewAdapter
                    val old = viewHolder.adapterPosition
                    val new = target.adapterPosition

                    //移動させる
                    adapter.notifyItemMoved(old, new)

                    //配列の値も入れ替える
                    //入れ替えて再設置する
                    val liveID = programList[old]
                    val watchMode = watchModeList[old]
                    val isOfficial = officialList[old]
                    val html = htmlList[old]
                    programList.removeAt(old)
                    watchModeList.removeAt(old)
                    officialList.removeAt(old)
                    htmlList.removeAt(old)
                    programList.add(new, liveID)
                    watchModeList.add(new, watchMode)
                    officialList.add(new, isOfficial)
                    htmlList.add(new, html)
                    //Fragmentが入るView再設置
                    //全部消すのでは無く移動するところだけ消す
                    val cardView = (viewBinding.nimadoActivityLinearlayout[old] as CardView)
                    val fragment = if (supportFragmentManager.findFragmentByTag(liveID) != null) {
                        supportFragmentManager.findFragmentByTag(liveID)
                    } else {
                        //Fragment再設置
                        val commentFragment = CommentFragment()
                        val bundle = Bundle()
                        bundle.putString("liveId", liveID)
                        commentFragment.arguments = bundle
                        commentFragment
                    }
                    viewBinding.nimadoActivityLinearlayout.removeView(cardView)
                    viewBinding.nimadoActivityLinearlayout.addView(cardView, new)
                    val trans = supportFragmentManager.beginTransaction()
                    //cardViewの0番目のViewがFragmentを入れるViewなので
                    if (fragment != null) {
                        trans.replace(cardView[0].id, fragment, liveID)
                    }
                    trans.commit()


                    //これだけ！RecyclerViewに圧倒的感謝だな！！！！！！！
                    return true
                }

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ACTION_STATE_DRAG) {
                        //ドラッグ中はItemを半透明にする
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    //半透明しゅーりょー
                    viewHolder.itemView.alpha = 1f
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    //スワイプしないのでいらない
                }
            })
        itemTouchHelper.attachToRecyclerView(viewBinding.nimadoActivityListRecyclerView)
    }

    /*
    * ヘッダーに
    * */

    /*
    * 閉じるボタン
    * */
    fun setCloseButton(liveId: String, linearLayout: LinearLayout): Button {
        val button =
            MaterialButton(this)
        button.text = "閉じる"
        button.setOnClickListener {
            val trans = supportFragmentManager.beginTransaction()
            val fragment = supportFragmentManager.findFragmentByTag(liveId)
            trans.remove(fragment!!)
            trans.commit()
            //けす
            (linearLayout.parent as LinearLayout).removeView(linearLayout)
        }
        return button
    }

    //戻るキーを押した時に本当に終わるか聞く
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.back_dialog))
            .setMessage(getString(R.string.back_dialog_description))
            .setPositiveButton(getString(R.string.end)) { dialogInterface: DialogInterface, i: Int ->
                finish()
                super.onBackPressed()
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            }
            .show()
    }

}
