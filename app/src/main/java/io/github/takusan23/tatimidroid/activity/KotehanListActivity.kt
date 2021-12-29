package io.github.takusan23.tatimidroid.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.adapter.KotehanListAdapter
import io.github.takusan23.tatimidroid.room.entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.room.init.KotehanDBInit
import io.github.takusan23.tatimidroid.tool.LanguageTool
import io.github.takusan23.tatimidroid.databinding.ActivityKotehanListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * コテハン一覧あくてぃびてぃー
 * */
class KotehanListActivity : AppCompatActivity() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { ActivityKotehanListBinding.inflate(layoutInflater) }

    // RecyclerView
    val kotehanList = arrayListOf<KotehanDBEntity>()
    val kotehanListAdapter = KotehanListAdapter(kotehanList) // 新しい順に

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込む
        loadDB()

    }

    /**
     * データベースからコテハンを取り出す関数。監視機能付き
     * */
    fun loadDB() {
        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                KotehanDBInit.getInstance(this@KotehanListActivity).kotehanDBDAO().flowGetKotehanAll().collect { kotehanDBList ->
                    kotehanList.clear()
                    kotehanDBList.forEach { kotehan ->
                        kotehanList.add(0, kotehan)
                    }
                    withContext(Dispatchers.Main) {
                        kotehanListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        // RecyclerView初期化
        viewBinding.activityKotehanListRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@KotehanListActivity)
            adapter = kotehanListAdapter
            val itemDecoration = DividerItemDecoration(this@KotehanListActivity, DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
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