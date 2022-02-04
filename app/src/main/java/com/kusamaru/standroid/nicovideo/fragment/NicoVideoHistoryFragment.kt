package com.kusamaru.standroid.nicovideo.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.nicoapi.login.NicoLogin
import com.kusamaru.standroid.nicoapi.nicovideo.dataclass.NicoVideoData
import com.kusamaru.standroid.nicoapi.nicovideo.NicoVideoHistoryAPI
import com.kusamaru.standroid.nicovideo.adapter.NicoVideoListAdapter
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentNicovideoHistoryBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ニコ動履歴Fragment */
class NicoVideoHistoryFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    // RecyclerView
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    // API
    var userSession = ""
    val nicoVideoHistoryAPI = NicoVideoHistoryAPI()

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoHistoryBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        if (savedInstanceState == null) {
            // 履歴取る
            getHistory()
        } else {
            // 画面回転
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                recyclerViewList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // 引っ張った
        viewBinding.fragmentNicovideoHistorySwipeToRefresh.setOnRefreshListener {
            getHistory()
        }

    }

    // 履歴取得
    private fun getHistory() {
        recyclerViewList.clear()
        viewBinding.fragmentNicovideoHistorySwipeToRefresh.isRefreshing = true
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            val response = nicoVideoHistoryAPI.getHistory(userSession)
            when {
                response.isSuccessful -> {
                    withContext(Dispatchers.Default) {
                        nicoVideoHistoryAPI.parseHistoryJSONParse(response.body?.string()).forEach {
                            recyclerViewList.add(it)
                        }
                    }
                    nicoVideoListAdapter.notifyDataSetChanged()
                    viewBinding.fragmentNicovideoHistorySwipeToRefresh.isRefreshing = false
                }
                response.code == 401 -> {
                    // ログイン切れ。再ログイン勧める
                    Snackbar.make(viewBinding.fragmentNicovideoHistoryRecyclerView, R.string.login_disable_message, Snackbar.LENGTH_INDEFINITE).apply {
                        anchorView = (activity as MainActivity).viewBinding.mainActivityBottomNavigationView
                        setAction(R.string.login) {
                            // ログインする
                            lifecycleScope.launch {
                                userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
                                getHistory()
                            }
                        }
                        show()
                    }
                }
                else -> showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    // RecyclerView初期化
    fun initRecyclerView() {
        viewBinding.fragmentNicovideoHistoryRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

}