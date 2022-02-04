package com.kusamaru.standroid.nicolive.bottomfragment

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.kusamaru.standroid.MainActivity
import com.kusamaru.standroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import com.kusamaru.standroid.nicoapi.nicolive.NicoLiveHTML
import com.kusamaru.standroid.nicoapi.nicolive.NicoLiveTimeShiftAPI
import com.kusamaru.standroid.nicolive.activity.FloatingCommentViewer
import com.kusamaru.standroid.R
import com.kusamaru.standroid.service.startLivePlayService
import com.kusamaru.standroid.tool.ContentShareTool
import com.kusamaru.standroid.databinding.BottomFragmentProgramMenuBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 番組の
 * TS予約、予約枠自動入場
 * 入れてほしいもの↓
 * liveId   | String | 番組ID
 * */
class ProgramMenuBottomSheet : BottomSheetDialogFragment() {

    private lateinit var prefSetting: SharedPreferences
    lateinit var nicoLiveProgramData: NicoLiveProgramData

    // データ取得
    private val nicoLiveHTML = NicoLiveHTML()
    lateinit var nicoLiveJSONObject: JSONObject

    private var liveId = ""
    private var userSession = ""

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentProgramMenuBinding.inflate(layoutInflater) }

    // 共有
    private val contentShare by lazy { ContentShareTool(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        liveId = arguments?.getString("liveId", "") ?: ""
        userSession = prefSetting.getString("user_session", "") ?: ""

        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }

        lifecycleScope.launch(errorHandler) {
            withContext(Dispatchers.Main) {
                // 番組情報取得
                coroutine()

                // UI反映
                applyUI()

                // ポップアップ再生、バッググラウンド再生
                initLiveServiceButton()

                // フローティングコメビュ
                initFloatingCommentViewer()

                // カレンダー追加
                initCalendarButton()

                // 共有ボタン
                initShareButton()

                // IDコピーとか
                initCopyButton()

                // TS予約とか
                initTSButton()

                // コメントのみ
                initCommentOnlyButton()
            }
        }

    }

    private fun initCommentOnlyButton() {
        viewBinding.bottomFragmentProgramInfoCommentOnlyTextView.setOnClickListener {
            (requireActivity() as? MainActivity)?.setNicoliveFragment(liveId, null, true)
        }
    }

    private fun initFloatingCommentViewer() {
        // Android 10 以降 でなお 放送中の番組の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && nicoLiveHTML.status == "ON_AIR") {
            viewBinding.bottomFragmentProgramInfoFloaingCommentViewerTextView.visibility = View.VISIBLE
        }
        viewBinding.bottomFragmentProgramInfoFloaingCommentViewerTextView.setOnClickListener {
            // フローティングコメビュ起動
            FloatingCommentViewer.showBubbles(requireContext(), liveId, nicoLiveHTML.programTitle, nicoLiveHTML.thumb)
        }
    }

    private fun initTSButton() {
        val timeShiftAPI = NicoLiveTimeShiftAPI()
        viewBinding.bottomFragmentProgramInfoTimeshiftTextView.setOnClickListener {
            // エラー時
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                // TS予約
                val registerTS = timeShiftAPI.registerTimeShift(liveId, userSession)
                if (registerTS.isSuccessful) {
                    // 成功
                    showToast(getString(R.string.timeshift_reservation_successful))
                } else if (registerTS.code == 500) {
                    // 失敗時。500エラーは登録済み
                    // 削除するか？Snackbar出す
                    withContext(Dispatchers.Main) {
                        Snackbar.make(viewBinding.bottomFragmentProgramInfoTimeshiftTextView, R.string.timeshift_reserved, Snackbar.LENGTH_SHORT).setAction(R.string.timeshift_delete_reservation_button) {
                            lifecycleScope.launch {
                                // TS削除API叩く
                                val deleteTS = timeShiftAPI.deleteTimeShift(liveId, userSession)
                                if (deleteTS.isSuccessful) {
                                    showToast(getString(R.string.timeshift_delete_reservation_successful))
                                } else {
                                    showToast("${getString(R.string.error)}\n${deleteTS.code}")
                                }
                            }
                        }.show()
                    }
                }
            }
        }
    }

    private fun initCopyButton() {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        viewBinding.bottomFragmentProgramInfoCommunityCopyTextView.setOnClickListener {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", nicoLiveHTML.communityId))
            //コピーしました！
            Toast.makeText(context, "${getString(R.string.copy_communityid)} : ${nicoLiveHTML.communityId}", Toast.LENGTH_SHORT).show()
        }
        viewBinding.bottomFragmentProgramInfoIdCopyTextView.setOnClickListener {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            //コピーしました！
            Toast.makeText(context, "${getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initShareButton() {
        viewBinding.bottomFragmentProgramInfoShareTextView.setOnClickListener {
            contentShare.showShareContent(
                programId = liveId,
                programName = nicoLiveHTML.programTitle,
                fromTimeSecond = null
            )
        }
    }

    private fun initCalendarButton() {
        viewBinding.bottomFragmentProgramInfoAddCalendarTextView.setOnClickListener {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, nicoLiveHTML.programTitle)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, nicoLiveHTML.programOpenTime * 1000L) // ミリ秒らしい。
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, nicoLiveHTML.programEndTime * 1000L) // ミリ秒らしい。
            }
            startActivity(intent)
        }
    }

    /** データ取得。取得が終わるまで一時停止する系の関数です。 */
    private suspend fun coroutine() = withContext(Dispatchers.Default) {
        val response = nicoLiveHTML.getNicoLiveHTML(liveId, userSession)
        if (!response.isSuccessful) {
            // 失敗時
            showToast("${getString(R.string.error)}\n${response.code}")
            return@withContext
        }
        nicoLiveJSONObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string())
        nicoLiveHTML.initNicoLiveData(nicoLiveJSONObject)
        nicoLiveProgramData = nicoLiveHTML.getProgramData(nicoLiveJSONObject)
    }

    /** データ取得し終わったらUI更新 */
    private fun applyUI() {
        if (isAdded) {
            viewBinding.bottomFragmentProgramInfoTitleTextView.text = nicoLiveHTML.programTitle
            viewBinding.bottomFragmentProgramInfoIdTextView.text = nicoLiveHTML.liveId
            // 時間
            val formattedStartTime = nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programOpenTime)
            val formattedEndTime = nicoLiveHTML.iso8601ToFormat(nicoLiveHTML.programEndTime)
            viewBinding.bottomFragmentProgramInfoTimeTextView.text = "開場時刻：$formattedStartTime\n終了時刻：$formattedEndTime"
            // 項目表示
            viewBinding.bottomFragmentProgramInfoButtonsLinearlayout.isVisible = true
        }
    }

    private fun initLiveServiceButton() {
        // 放送中以外なら非表示
        if (nicoLiveHTML.status != "ON_AIR") {
            viewBinding.bottomFragmentProgramInfoPopupTextView.isVisible = true
            viewBinding.bottomFragmentProgramInfoBackgroundTextView.visibility = View.GONE
        }
        viewBinding.bottomFragmentProgramInfoPopupTextView.setOnClickListener {
            // ポップアップ再生
            startLivePlayService(context = context, mode = "popup", liveId = liveId)
        }
        viewBinding.bottomFragmentProgramInfoBackgroundTextView.setOnClickListener {
            // バッググラウンド再生
            startLivePlayService(context = context, mode = "background", liveId = liveId)
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}