package io.github.takusan23.tatimidroid.setting

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import io.github.takusan23.searchpreferencefragment.SearchPreferenceChildFragment
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.activity.KonoApp
import io.github.takusan23.tatimidroid.activity.LicenceActivity
import io.github.takusan23.tatimidroid.fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.nicoapi.NicoVideoCache
import io.github.takusan23.tatimidroid.room.init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.tool.RoomDBExporter
import io.github.takusan23.tatimidroid.tool.RoomDBImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

/**
 * 設定Fragment。
 * */
class SettingsFragment : SearchPreferenceFragment() {

    /** プライバシーポリシー */
    private val PRIVACY_POLICY_URL = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    /** データベースバックアップで使う */
    private val roomDBExporter = RoomDBExporter(this)

    /** データベースの取り込みで使う */
    private val roomDBImporter = RoomDBImporter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editText = view.findViewById<EditText>(R.id.search_fragment_input)
        // スクロールすると動くEditTextうざい（お前が作ったんだろ
        ((editText.parent.parent as View).layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
        editText.hint = getString(R.string.serch)
    }

    init {

        arguments = Bundle().apply {
            // 階層化されている場合
            val hashMap = hashMapOf<String, Int>()
            putSerializable(PREFERENCE_XML_FRAGMENT_NAME_HASH_MAP, hashMap)
            putInt(SearchPreferenceChildFragment.PREFERENCE_XML_RESOURCE_ID, R.xml.preferences)
        }

        /**
         * Preferenceを押したときに呼ばれるやつ
         * */
        onPreferenceClickFunc = { preference ->
            when (preference?.key) {
                "licence_preference" -> {
                    // ライセンス画面
                    startActivity(Intent(context, LicenceActivity::class.java))
                }
                "konoapp_preference" -> {
                    // このアプリについて
                    startActivity(Intent(context, KonoApp::class.java))
                }
                "konoapp_privacy" -> {
                    // プライバシーポリシー
                    startBrowser(PRIVACY_POLICY_URL)
                }
                "about_cache" -> {
                    startBrowser("https://takusan23.github.io/Bibouroku/2020/04/08/たちみどろいどのキャッシュ機能について/")
                }
                "first_time_preference" -> {
                    lifecycleScope.launch(Dispatchers.Main) {
                        // 最初に使った日特定
                        val list = withContext(Dispatchers.IO) {
                            NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().getAll()
                        }
                        if (list.isEmpty()) return@launch // なければ落とす
                        // 取り出す
                        val history = list.first()
                        val title = history.title
                        val time = history.unixTime
                        val id = history.serviceId
                        val service = history.type
                        // 今日からの何日前か。詳しくは NicoVideoInfoFragment#getDayCount() みて。ほぼ同じことしてる。
                        val dayCalc = ((System.currentTimeMillis() / 1000) - time) / 60 / 60 / 24
                        preference.summary = """
                                   最初に見たもの：$title ($id/ $service)
                                   一番最初に使った日：${toFormatTime(time * 1000)}
                                   今日から引くと：$dayCalc 日前
                                """.trimIndent()
                    }
                }
                "delete_history" -> {
                    // 端末内履歴を消すか
                    val buttons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                        add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete), R.drawable.ic_outline_delete_24px))
                        add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.RED))
                    }
                    DialogBottomSheet(getString(R.string.delete_message), buttons) { i, bottomSheetDialogFragment ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            // 吹っ飛ばす（全削除）
                            withContext(Dispatchers.IO) {
                                NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().deleteAll()
                            }
                            bottomSheetDialogFragment.dismiss()
                        }
                    }.show(getParentFragmentManager(), "delete")
                }
                "database_backup" -> {
                    // SAFを開く
                    startBackup()
                }
                "database_restore" -> {
                    // SAFを開く
                    roomDBImporter.start()
                }
                "setting_open_app_info" -> {
                    // 端末のアプリ設定画面を開く
                    launchSettingAppInfoScreen()
                }
                "setting_cache_folder_path" -> {
                    val nicoVideoCache = NicoVideoCache(requireContext())
                    preference.summary = nicoVideoCache.getCacheFolderPath()
                }
            }
        }
    }

    /** 端末設定のアプリ設定画面へ飛ばす */
    private fun launchSettingAppInfoScreen() {
        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(this)
        }
    }

    /** バックアップするよーダイアログを出す */
    private fun startBackup() {
        val buttons = arrayListOf(
            DialogBottomSheet.DialogBottomSheetItem(getString(R.string.database_backup_start), R.drawable.ic_backup_icon),
            DialogBottomSheet.DialogBottomSheetItem(getString(R.string.database_backup_cancel), R.drawable.ic_clear_black)
        )
        DialogBottomSheet(getString(R.string.database_backup_description), buttons) { i, bottomSheetDialogFragment ->
            if (i == 0) {
                roomDBExporter.start()
            }
            bottomSheetDialogFragment.dismiss()
        }.show(childFragmentManager, "backup_dialog")
    }

    /** UnixTime -> わかりやすい形式に */
    private fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒")
        return simpleDateFormat.format(time)
    }


    private fun startBrowser(link: String) {
        val i = Intent(Intent.ACTION_VIEW, link.toUri());
        startActivity(i);
    }

}