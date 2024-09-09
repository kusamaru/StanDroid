package com.kusamaru.standroid.activity

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.nicoapi.login.NicoLoginDataClass
import com.kusamaru.standroid.nicoapi.login.NicoLoginTwoFactorAuth
import com.kusamaru.standroid.R
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.LanguageTool
import com.kusamaru.standroid.tool.getThemeColor
import com.kusamaru.standroid.tool.isDarkMode
import com.kusamaru.standroid.databinding.ActivityTwoFactorAuthBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 二段階認証を完了させるActivity
 * */
class TwoFactorAuthLoginActivity : AppCompatActivity() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { ActivityTwoFactorAuthBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ダークモード
        DarkModeSupport(this).setActivityTheme(this)
        setContentView(viewBinding.root)

        if (isDarkMode(this)) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(this)))
        }
        viewBinding.root.backgroundTintList = ColorStateList.valueOf(getThemeColor(this))

        // データを受け取る
        val loginData = intent.getSerializableExtra("login") as NicoLoginDataClass

        // 認証ボタン押す
        viewBinding.activityTwoFactorAuthLoginButton.setOnClickListener {

            // 認証コード
            val keyVisualArts = viewBinding.activityTwoFactorAuthKeyInput.text.toString()
            // このデバイスを信用する場合
            val isTrustDevice = viewBinding.activityTwoFactorAuthTrustCheck.isChecked

            // 例外
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            // 二段階認証開始
            lifecycleScope.launch(Dispatchers.Main + errorHandler) {
                val nicoLoginTwoFactorAuth = NicoLoginTwoFactorAuth(loginData)
                val (userSession, trustDeviceToken) = nicoLoginTwoFactorAuth.twoFactorAuth(keyVisualArts, isTrustDevice)

                // 保存
                PreferenceManager.getDefaultSharedPreferences(this@TwoFactorAuthLoginActivity).edit {
                    putString("user_session", userSession)
                    // デバイスを信頼している場合は次回からスキップできる値を保存
                    putString("trust_device_token", trustDeviceToken)
                    // もしログイン無しで利用するが有効の場合は無効にする
                    putBoolean("setting_no_login", false)
                }
                // ログインできたよ！
                Toast.makeText(this@TwoFactorAuthLoginActivity, getString(R.string.successful), Toast.LENGTH_SHORT).show()
                finish()
            }

        }
    }

    /**
     * クリップボードに認証コードがある場合は貼り付ける
     * Android 11から画面内の文字をコピーできるように（選択可能）なったけどもしかしてワンタイムパスワードのために付いた機能か！？
     * */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        if (clipdata?.getItemAt(0)?.text != null) {
            val clipboardText = clipdata.getItemAt(0).text
            if (clipboardText.matches(Regex("[0-9]+"))) {
                // 正規表現で数字だったときは貼り付ける
                viewBinding.activityTwoFactorAuthKeyInput.setText(clipboardText)
            }
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