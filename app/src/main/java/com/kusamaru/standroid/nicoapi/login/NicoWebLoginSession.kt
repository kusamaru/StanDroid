package com.kusamaru.standroid.nicoapi.login

import kotlinx.coroutines.CompletableDeferred
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONException
import org.json.JSONObject

internal const val NICO_SESSION_URL = "https://www.nicovideo.jp/"

/** One result per request, including cancellation; a late Activity result cannot revive it. */
internal class WebLoginRequest {
    private val started = CompletableDeferred<Unit>()
    private val result = CompletableDeferred<Boolean>()
    val isActive: Boolean get() = result.isActive
    suspend fun await(): Boolean = result.await()
    suspend fun awaitStarted() = started.await()
    fun markStarted(): Boolean = started.complete(Unit)
    fun complete(success: Boolean): Boolean = result.complete(success)
    fun cancel() {
        started.cancel()
        result.cancel()
    }
}

internal fun webLoginSession(cookieHeader: String?): String? {
    if (cookieHeader.isNullOrBlank()) return null
    val headers = Headers.Builder()
    for (cookie in cookieHeader.split(';')) {
        if (cookie.substringBefore('=').trim() != "user_session" || cookie.any { it < ' ' || it > '~' }) continue
        headers.add("Set-Cookie", cookie.trim())
    }
    return findLoginCookie(headers.build(), NICO_SESSION_URL.toHttpUrl(), "user_session")?.value
}

internal fun isAuthenticatedUserResponse(body: String?): Boolean {
    if (body.isNullOrBlank()) return false
    return try {
        val json = JSONObject(body)
        json.optJSONObject("meta")?.optInt("status") == 200 &&
            (json.optJSONObject("data")?.optJSONObject("user")?.optLong("id", 0L) ?: 0L) > 0L
    } catch (_: JSONException) {
        false
    }
}

/** Password/MFA only. Do not embed arbitrary sites or external-provider sign-in pages. */
internal fun isWebLoginUrl(url: String?): Boolean {
    val parsed = url?.toHttpUrlOrNull() ?: return false
    return parsed.scheme == "https" && parsed.port == 443 && parsed.username.isEmpty() && parsed.password.isEmpty() &&
        (parsed.host == "nicovideo.jp" || parsed.host.endsWith(".nicovideo.jp") || parsed.host == "challenges.cloudflare.com")
}

/** A service page after leaving the authentication/challenge UI, not necessarily www. */
internal fun isWebLoginReturnUrl(url: String?): Boolean {
    if (!isWebLoginUrl(url)) return false
    val host = url!!.toHttpUrl().host
    return host != "account.nicovideo.jp" && host != "challenges.cloudflare.com"
}
