package com.kusamaru.standroid.nicoapi.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.kusamaru.standroid.activity.NicoWebLoginActivity
import com.kusamaru.standroid.R
import com.kusamaru.standroid.nicoapi.user.UserAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Replacement for NicoLogin: keeps String?/preferences, but waits for browser/MFA completion. */
object NicoWebLogin {
    internal const val RESULT_RECEIVER = "web_login_result_receiver"
    internal const val CANCEL_RECEIVER = "web_login_cancel_receiver"
    internal const val READY = 1
    internal const val SUCCESS = 2
    internal const val CANCEL = 3

    // ponytail: one login window for the app; no account-routing framework.
    private val loginMutex = Mutex()

    suspend fun secureNicoLogin(context: Context?, force: Boolean = false): String? {
        if (context == null) return null
        val appContext = context.applicationContext
        return loginMutex.withLock {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            val saved = pref.getString("user_session", null)
            if (!force && !saved.isNullOrBlank() && isValidSession(saved)) return@withLock saved

            withContext(Dispatchers.Main) {
                val request = WebLoginRequest()
                var cancelReceiver: ResultReceiver? = null
                val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        when (resultCode) {
                            READY -> {
                                cancelReceiver = resultData?.getParcelable(CANCEL_RECEIVER)
                                request.markStarted()
                                if (!request.isActive) cancelReceiver?.send(CANCEL, null)
                            }
                            SUCCESS -> request.complete(true)
                            CANCEL -> request.complete(false)
                        }
                    }
                }
                try {
                    context.startActivity(Intent(context, NicoWebLoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(RESULT_RECEIVER, receiver)
                    })
                    // Background-Activity launch restrictions can silently prevent the window opening.
                    val started = withTimeoutOrNull(10_000L) { request.awaitStarted(); true } ?: false
                    if (!started) {
                        Toast.makeText(appContext, R.string.web_login_failed, Toast.LENGTH_LONG).show()
                        return@withContext null
                    }
                    if (request.await()) pref.getString("user_session", null) else null
                } catch (_: ActivityNotFoundException) {
                    null
                } catch (_: SecurityException) {
                    null
                } finally {
                    request.cancel()
                    cancelReceiver?.send(CANCEL, null)
                }
            }
        }
    }

    /** Reuse the existing API, but require both service success and a real user ID. */
    internal suspend fun isValidSession(session: String): Boolean = withContext(Dispatchers.IO) {
        try {
            UserAPI().getMyAccountUserData(session).use { response ->
                response.isSuccessful && isAuthenticatedUserResponse(response.body?.string())
            }
        } catch (_: IOException) {
            false
        }
    }

    internal fun saveSession(context: Context, session: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString("user_session", session)
            putBoolean("setting_no_login", false)
            // The official page owns password entry and device-trust cookies now.
            remove("password")
            remove("trust_device_token")
        }
    }
}
