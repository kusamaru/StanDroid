package com.kusamaru.standroid.nicovideo.bottomfragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoMyListData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoSPMyListAPI
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoAddMyListAdapter
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.BottomFragmentNicovideoAddMylistBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * マイリスト追加BottomFragment
 * */
class NicoVideoAddMylistBottomFragment : BottomSheetDialogFragment() {

    // アダプター
    lateinit var nicoVideoAddMyListAdapter: NicoVideoAddMyListAdapter
    val recyclerViewList = arrayListOf<NicoVideoMyListData>()

    // ユーザーセッション
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // スマホ版マイリストAPI
    private val spMyListAPI = NicoVideoSPMyListAPI()

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoAddMylistBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        // マイリスト取得
        coroutine()

    }


    fun coroutine() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            recyclerViewList.clear()
            // マイリスト一覧APIを叩く
            val myListListResponse = spMyListAPI.getMyListList(userSession)
            if (!myListListResponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${myListListResponse.code}")
                return@launch
            }
            // レスポンスをパースしてRecyclerViewに突っ込む
            withContext(Dispatchers.Default) {
                recyclerViewList.addAll(spMyListAPI.parseMyListList(myListListResponse.body?.string()))
            }
            // 一覧更新
            nicoVideoAddMyListAdapter.notifyDataSetChanged()
        }
    }

    fun initRecyclerView() {
        viewBinding.bottomFragmentNicovideoMylistRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoAddMyListAdapter = NicoVideoAddMyListAdapter(recyclerViewList)
            nicoVideoAddMyListAdapter.id = arguments?.getString("id", "") ?: ""
            nicoVideoAddMyListAdapter.mylistBottomFragment = this@NicoVideoAddMylistBottomFragment
            adapter = nicoVideoAddMyListAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}