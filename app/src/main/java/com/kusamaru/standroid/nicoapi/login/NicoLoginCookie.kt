package com.kusamaru.standroid.nicoapi.login

import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl

/** Cookie positions and similarly named cookies are not a login result. */
internal fun findLoginCookie(headers: Headers, url: HttpUrl, name: String): Cookie? =
    headers.values("Set-Cookie").mapNotNull { Cookie.parse(url, it) }.firstOrNull {
        it.name == name && it.value.isNotEmpty() && it.value != "deleted" && it.expiresAt > System.currentTimeMillis()
    }
