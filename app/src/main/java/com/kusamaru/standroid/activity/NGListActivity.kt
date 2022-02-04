package com.kusamaru.standroid.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.kusamaru.standroid.adapter.NGListRecyclerViewAdapter
import com.kusamaru.standroid.R
import com.kusamaru.standroid.room.entity.NGDBEntity
import com.kusamaru.standroid.room.init.NGDBInit
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.LanguageTool
import com.kusamaru.standroid.databinding.ActivityNgListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NG一覧Activity
 * */
class NGListActivity : AppCompatActivity() {

    // RecyclerView関連
    var recyclerViewList = arrayListOf<NGDBEntity>()
    lateinit var ngListRecyclerViewAdapter: NGListRecyclerViewAdapter

    /** findViewById駆逐 */
    private val viewBinding by lazy { ActivityNgListBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //ダークモード
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)
        setContentView(viewBinding.root)

        supportActionBar?.title = getString(R.string.ng_list)

        viewBinding.activityNgListRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@NGListActivity)
            ngListRecyclerViewAdapter = NGListRecyclerViewAdapter(recyclerViewList)
            adapter = ngListRecyclerViewAdapter
            val itemDecoration = DividerItemDecoration(this@NGListActivity, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        // BottomNavigation
        viewBinding.activityNgBottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ng_menu_user -> loadNGUser()
                R.id.ng_menu_comment -> loadNGComment()
            }
            true
        }

        // はじめてはNGユーザー表示させる
        viewBinding.activityNgBottomNav.selectedItemId = R.id.ng_menu_user
    }

    //NGコメント読み込み
    fun loadNGComment() {
        recyclerViewList.clear()
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // データ読み出し
                NGDBInit.getInstance(this@NGListActivity).ngDBDAO().getNGCommentList().forEach {
                    recyclerViewList.add(it)
                }
            }
            // リスト更新
            ngListRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

    //NGユーザー読み込み
    fun loadNGUser() {
        recyclerViewList.clear()
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // データ読み出し
                NGDBInit.getInstance(this@NGListActivity).ngDBDAO().getNGUserList().forEach {
                    recyclerViewList.add(it)
                }
            }
            // リスト更新
            ngListRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}
