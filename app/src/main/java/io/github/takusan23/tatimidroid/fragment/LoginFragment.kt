package io.github.takusan23.tatimidroid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ログイン画面Fragment */
class LoginFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentLoginBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //保存していたら取得
        viewBinding.fragmentLoginMailEditText.setText(prefSetting.getString("mail", ""))
        viewBinding.fragmentLoginPasswordEditText.setText(prefSetting.getString("password", ""))

        //おしたとき
        viewBinding.fragmentLoginButton.setOnClickListener {
            // ログインAPI叩く
            lifecycleScope.launch(Dispatchers.Main) {
                val mail = viewBinding.fragmentLoginMailEditText.text.toString()
                val pass = viewBinding.fragmentLoginPasswordEditText.text.toString()
                // メアドを保存する
                NicoLogin.saveMailPassPreference(requireContext(), mail, pass)
                // ログインAPIを叩く
                val userSession = withContext(Dispatchers.Default) {
                    NicoLogin.secureNicoLogin(requireContext())
                }
                if (userSession != null) {
                    // 成功時
                    Toast.makeText(activity, getString(R.string.successful), Toast.LENGTH_SHORT).show()
                    //めあど、ぱすわーども保存する
                    prefSetting.edit {
                        putString("mail", mail)
                        putString("password", pass)
                        putString("user_session", userSession)
                        // もしログイン無しで利用するが有効の場合は無効にする
                        putBoolean("setting_no_login", false)
                    }
                }
            }
        }
    }

}