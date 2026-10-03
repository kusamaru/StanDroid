package com.kusamaru.standroid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kusamaru.standroid.nicoapi.login.NicoWebLogin
import com.kusamaru.standroid.R
import com.kusamaru.standroid.databinding.FragmentLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** ログイン画面Fragment */
class LoginFragment : Fragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentLoginBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //おしたとき
        viewBinding.fragmentLoginButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                viewBinding.fragmentLoginButton.isEnabled = false
                try {
                    if (NicoWebLogin.secureNicoLogin(requireContext(), force = true) != null) {
                        Toast.makeText(activity, getString(R.string.successful), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    viewBinding.fragmentLoginButton.isEnabled = true
                }
            }
        }
    }

}
