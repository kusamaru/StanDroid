package com.kusamaru.standroid.activity

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.login.NICO_SESSION_URL
import com.kusamaru.standroid.nicoapi.login.NicoWebLogin
import com.kusamaru.standroid.nicoapi.login.isWebLoginUrl
import com.kusamaru.standroid.nicoapi.login.isWebLoginReturnUrl
import com.kusamaru.standroid.nicoapi.login.webLoginSession
import com.kusamaru.standroid.tool.DarkModeSupport
import com.kusamaru.standroid.tool.LanguageTool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** The official page handles Turnstile/password/MFA; no credential injection or JS bridge. */
class NicoWebLoginActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var status: TextView
    private lateinit var confirm: Button
    private var resultReceiver: ResultReceiver? = null
    private var resultSent = false
    private var verification: Job? = null
    private var sessionWatch: Job? = null
    private var rejectedSession: String? = null
    private var rejectedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = intent.getParcelableExtra(NicoWebLogin.RESULT_RECEIVER)
        if (resultReceiver == null) {
            finish()
            return
        }
        DarkModeSupport(this).setActivityTheme(this)
        title = getString(R.string.login)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply { setText(R.string.web_login_waiting) }
        confirm = Button(this).apply {
            setText(R.string.web_login_confirm)
            setOnClickListener { checkSession(manual = true) }
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            // Keep the WebView's default, consistent UA; never use the native API UA here.
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame || isWebLoginUrl(request.url.toString())) return false
                    status.setText(R.string.web_login_unsupported_page)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (isWebLoginUrl(url)) {
                        // Display only the host, never callback parameters or auth tokens.
                        supportActionBar?.subtitle = android.net.Uri.parse(url).host
                        checkSession(manual = false)
                    }
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) status.setText(R.string.web_login_failed)
                }
                // Default SSL error handling cancels; never call proceed().
            }
        }
        layout.addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(status)
        layout.addView(confirm)
        setContentView(layout)

        val cancellation = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == NicoWebLogin.CANCEL) finish()
            }
        }
        resultReceiver?.send(NicoWebLogin.READY, Bundle().apply {
            putParcelable(NicoWebLogin.CANCEL_RECEIVER, cancellation)
        })
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl("https://account.nicovideo.jp/spa/login/index.html?redirect_uri=https%3A%2F%2Fwww.nicovideo.jp%2F&response_type=session")
        }
    }

    override fun onStart() {
        super.onStart()
        if (!::webView.isInitialized) return
        // SPA/cookie updates need not coincide with onPageFinished. Watch only while visible.
        sessionWatch = lifecycleScope.launch {
            while (isActive) {
                checkSession(manual = false)
                delay(1_000L)
            }
        }
    }

    override fun onStop() {
        sessionWatch?.cancel()
        sessionWatch = null
        super.onStop()
    }

    private fun checkSession(manual: Boolean) {
        if (isFinishing || verification?.isActive == true || !isWebLoginUrl(webView.url)) return
        // Never finish password/MFA entry just because an older session cookie still exists.
        if (!manual && !isWebLoginReturnUrl(webView.url)) return
        val candidate = webLoginSession(CookieManager.getInstance().getCookie(NICO_SESSION_URL))
        if (candidate == null) {
            if (manual) status.setText(R.string.web_login_waiting)
            return
        }
        if (!manual && candidate == rejectedSession && SystemClock.elapsedRealtime() - rejectedAt < 10_000L) return
        verification = lifecycleScope.launch {
            confirm.isEnabled = false
            status.setText(R.string.loading)
            try {
                if (NicoWebLogin.isValidSession(candidate) && !isFinishing) {
                    NicoWebLogin.saveSession(this@NicoWebLoginActivity, candidate)
                    CookieManager.getInstance().flush()
                    resultSent = true
                    resultReceiver?.send(NicoWebLogin.SUCCESS, null)
                    finish()
                } else {
                    rejectedSession = candidate
                    rejectedAt = SystemClock.elapsedRealtime()
                    status.setText(R.string.web_login_failed)
                }
            } finally {
                confirm.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::webView.isInitialized) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (isFinishing && !resultSent) resultReceiver?.send(NicoWebLogin.CANCEL, null)
        verification?.cancel()
        if (::webView.isInitialized) {
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        super.onDestroy()
    }
}
